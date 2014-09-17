package com.tapshield.android.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.tapshield.android.api.model.ChatMessage;

public class JavelinChatManager {

	private static final String PREF = "com.tapshield.android.api.chatmanager.preferences.name";
	private static final String PREF_KEY_MESSAGES = "com.tapshield.android.api.chatmanager.preferences.key.messages";
	private static final String PREF_KEY_LASTCHECK = "com.tapshield.android.api.chatmanager.preferences.key.lastcheck";
	private static final String PREF_KEY_LASTSEEN = "com.tapshield.android.api.chatmanager.preferences.key.lastseen";
	
	private static JavelinChatManager mInstance;
	
	private static Context mContext;
	private static JavelinConfig mConfig;
	private static SharedPreferences mPreferences;
	private static AmazonDynamoDBClient mDdb;
	private static List<ChatMessage> mMessages;
	private static List<OnNewChatMessageListener> mNewMessageListeners;
	private static OnNewIncomingChatMessagesListener mNewIncomingMessagesListener;
	private static long mLastCheck, mLastSeen;
	
	private static String mAlertId;
	private static List<ChatMessage> mOutbox;
	private static int mOutboxIndex;

	private boolean mSeeing;
	
	public static JavelinChatManager getInstance(Context context, JavelinConfig config) {
		if (mInstance == null) {
			mInstance = new JavelinChatManager(context, config);
		}
		return mInstance;
	}
	
	private JavelinChatManager(Context context, JavelinConfig config) {
		mContext = context.getApplicationContext();
		mConfig = config;
		mPreferences = mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE);
		AWSCredentials credentials = new BasicAWSCredentials(
				mConfig.getAwsDynamoDbAccessKey(),
				mConfig.getAwsDynamoDbSecretKey());
		mDdb = new AmazonDynamoDBClient(credentials);
		mMessages = new ArrayList<ChatMessage>();
		mNewMessageListeners = new ArrayList<OnNewChatMessageListener>();
		mSeeing = false;
		getCache();
		notifyNewMessages();
	}
	
	private void getCache() {
		if (mMessages != null && !mMessages.isEmpty()) {
			mMessages.clear();
		}
		
		String serializedList = mPreferences.getString(PREF_KEY_MESSAGES, null);
		
		if (serializedList == null) {
			return;
		}
		
		mMessages.addAll(ChatMessage.deserializeListFromString(serializedList));
		
		mLastCheck = mPreferences.getLong(PREF_KEY_LASTCHECK, 0);
		mLastSeen= mPreferences.getLong(PREF_KEY_LASTSEEN, 0);
	}
	
	private void setCache() {
		if (mMessages == null || mMessages.isEmpty()) {
			return;
		}
		
		String serializedList = ChatMessage.serializeListToString(mMessages);
		
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(PREF_KEY_MESSAGES, serializedList);
		editor.commit();
	}
	
	public void send(String message) {
		Log.i("javelin", "sendmessage: creating message and enqueueing");
		
		JavelinUserManager userManager = JavelinUserManager.getInstance(mContext, mConfig);
		ChatMessage chatMessage = new ChatMessage(message, "local", Integer.toString(userManager.getUser().id));
		
		enqueue(chatMessage);
		addBasedOnTimestamp(chatMessage);
		notifyNewMessages();
		setCache();
	}
	
	private void enqueue(ChatMessage chatMessage) {
		initializeIfNecessary();
		
		Log.i("javelin", "enqueuemessage: added to outgoing queue and all messages queue");
		
		mOutbox.add(chatMessage);
		
		sendPendingMessages();
	}
	
	private void initializeIfNecessary() {
		Log.i("javelin", "initilizing if necessary...");
		if (mOutbox == null) {
			mOutbox = new ArrayList<ChatMessage>();
		}
		
		if (mOutboxIndex == -1) {
			mOutboxIndex = 0;
		}
	}
	
	private void sendPendingMessages() {
		//stop if no alert, empty queues, or no pending messages to be sent
		if (mAlertId == null || mAlertId.length() == 0 || mOutbox == null
				|| mOutbox.isEmpty() || mOutboxIndex > mOutbox.size()-1) {
			return;
		}

		Log.i("javelin", "sendpendingmessages: alertid=" + mAlertId + " queue.size=" + mOutbox.size() + " queue.index=" + mOutboxIndex);
		
		/*
		final RequestHandler handler = new RequestHandler() {

			@Override
			public void beforeRequest(Request<?> request) {}

			@Override
			public void afterResponse(Request<?> request, Object arg1, TimingInfo arg2) {
				Log.i("javelin", "sendpendingmessages @ after response");
				PutItemRequest insertion = (PutItemRequest) request.getOriginalRequest();
				completeDeliveryOf(insertion.getItem().get("message_id").getS());
			}

			@Override
			public void afterError(Request<?> request, Exception e) {
				Log.e("javelin", "sendpendingmessages @ after error:" + request + " -- " + e);
			}
		};
		*/
		
		new ChatMessageDeliveryAsync().execute();
	}
	
	private void addBasedOnTimestamp(ChatMessage m) {
		if (m == null) {
			return;
		}
		
		boolean added = false;
		for(int i = 0; i < mMessages.size(); i++) {
			if (mMessages.get(i).timestamp > m.timestamp) {
				mMessages.add(i, m);
				added = true;
			}
		}
		
		if (!added) {
			mMessages.add(m);
		}
	}
	
	private long getLastCheck() {
		return mLastCheck;
	}
	
	private long getLastSeen() {
		return mLastSeen;
	}
	
	private void setLastCheck(long lastCheck) {
		mLastCheck = lastCheck;
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putLong(PREF_KEY_LASTCHECK, mLastCheck);
		editor.commit();
	}
	
	public void notifyId(String alertId) {
		Log.i("javelin", "chat manager: alert-id=" + alertId);
		mAlertId = alertId;
		sendPendingMessages();
	}
	
	public void notifyEnd() {
		mAlertId = null;
		mOutbox = null;
		mOutboxIndex = -1;
		mMessages.clear();
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.remove(PREF_KEY_MESSAGES);
		editor.commit();
	}
	
	private void notifyNewMessages() {
		if (mMessages == null || mMessages.isEmpty()) {
			return;
		}
		
		for (OnNewChatMessageListener l : mNewMessageListeners) {
			if (l != null) {
				l.onNewChatMessage(mMessages);
			}
		}

		//if user is reported to be seeing, do not report new incoming--no need of a notification
		if (mSeeing) {
			return;
		}
		
		if (mNewIncomingMessagesListener != null) {
			mNewIncomingMessagesListener.onNewIncomingChatMessages(getNewIncomingMessages());
		}
	}
	
	private List<String> getNewIncomingMessages() {
		List<String> messages = new ArrayList<String>();
		String userId = Integer.toString(JavelinUserManager.getInstance(mContext, mConfig).getUser().id);
		
		for(ChatMessage m : mMessages) {
			if (m.timestamp > getLastSeen() && !m.senderId.equals(userId)) {
				messages.add(m.message);
			}
		}
		return messages;
	}
	
	public void notifySeeing() {
		mSeeing = true;
		//report new incoming NULL messages asthe user has seen them
		if (mNewIncomingMessagesListener != null) {
			mNewIncomingMessagesListener.onNewIncomingChatMessages(null);
		}
	}
	
	public void notifyNotSeeing() {
		mSeeing = false;
		mLastSeen = System.currentTimeMillis()/1000;
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putLong(PREF_KEY_LASTSEEN, mLastSeen);
		editor.commit();
	}
	
	public void fetchMessages() {
		fetchMessagesSince(0);
	}
	
	public void fetchMessagesSinceLastCheck() {
		fetchMessagesSince(getLastCheck());
	}
	
	public void fetchMessagesSince(long since) {
		boolean alertActive = JavelinAlertManager.getInstance(mContext, mConfig).isRunning();
		
		Log.d("javelin", "alertId=" + mAlertId + " alertActive=" + alertActive);
		
		if (mAlertId == null || !alertActive) {
			return;
		}
		
		final int numericId = JavelinUtils.extractLastIntOfString(mAlertId);
		final long timestampSeconds = (since / 1000);
		
		Condition idCondition = new Condition()
				.withComparisonOperator(ComparisonOperator.EQ)
				.withAttributeValueList(new AttributeValue().withN(Integer.toString(numericId)));

		Condition timeCondition = new Condition()
				.withComparisonOperator(ComparisonOperator.GE)
				.withAttributeValueList(new AttributeValue().withN(Long.toString(timestampSeconds)));

		Map<String, Condition> allConditions = new HashMap<String, Condition>();
		allConditions.put("alert_id", idCondition);
		allConditions.put("timestamp", timeCondition);

		QueryRequest messagesQuery = new QueryRequest(mConfig.getAwsDynamoDbTable())
				.withKeyConditions(allConditions);
		new ChatMessageRetrieval().execute(messagesQuery);
	}
	
	private void completeDeliveryOf(String messageId) {
		boolean changed = false;
		for(ChatMessage message : mMessages) {
			if (message.id.equals(messageId)) {
				message.transmitting = false;
				Log.d("javelin", "completed id=" + message.id);
				changed = true;
			}
		}
		
		if (changed) {
			notifyNewMessages();
		}
	}
	
	private class ChatMessageRetrieval extends AsyncTask<QueryRequest, Void, List<ChatMessage>> {
		@Override
		protected List<ChatMessage> doInBackground(QueryRequest... queryRequests) {
			Log.d("javelin", "Messages retrieved:");
			
			List<ChatMessage> messages = new ArrayList<ChatMessage>();
			
			QueryResult messagesResult = mDdb.query(queryRequests[0]);
			for (Map<String, AttributeValue> item : messagesResult.getItems()) {
				
				ChatMessage message = new ChatMessage(item.get("message").getS(), "", item.get("sender_id").getN());
				message.alertId = mAlertId;
				message.id = item.get("message_id").getS();
				message.transmitting = false;
				
				try {
					//amazon dynamodb returns the timestamp with decimals? keep integer part of the timestamp
					String timestampObtained = item.get("timestamp").getN();
					if (timestampObtained.contains(".")) {
						String firstPart = timestampObtained.split("\\.")[0];
						
						if (firstPart.length() > 0) {
							timestampObtained = firstPart;
						} else {
							timestampObtained = "0";
						}
					}
					
					message.timestamp = Long.parseLong(timestampObtained);
				} catch (Exception e) {
					Log.e("javelin", "Error parsing timestamp=" + item.get("timestamp").getN());
					message.timestamp = 0;
				}

				messages.add(message);
				
				Log.d("javelin", "id=" + message.id);
				Log.d("javelin", " timestamp=" + message.timestamp);
				Log.d("javelin", " message=" + message.message);
				Log.d("javelin", " senderId=" + message.senderId);
				Log.d("javelin", " alertId=" + message.alertId);
			}
			
			return messages;
		}
		
		@Override
		protected void onPostExecute(List<ChatMessage> messages) {
			Log.i("javelin", "Final alert messages=" + messages.toString() + "(size=" + messages.size() + ")");
			mMessages.clear();
			mMessages.addAll(messages);
			notifyNewMessages();
			long newLastCheck = 0;
			if (mMessages != null && !mMessages.isEmpty()) {
				newLastCheck = mMessages.get(mMessages.size()-1).timestamp;
			}
			setLastCheck(newLastCheck);
			setCache();
		}
	}
	
	private class ChatMessageDeliveryAsync extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... args) {
			
			for (int indexToSend = mOutboxIndex; indexToSend < mOutbox.size(); indexToSend++) {
				
				final ChatMessage chatMessage = mOutbox.get(indexToSend);

				Log.i("javelin", "sending pending message async " + indexToSend + "/" + (mOutbox.size()-1));
				Log.i("javelin", "  id=" + chatMessage.id + " message=" + chatMessage.message + " ");
				
				chatMessage.alertId = mAlertId;
				int numericalId = JavelinUtils.extractLastIntOfString(chatMessage.alertId);
				
				final AttributeValue
						message = new AttributeValue().withS(chatMessage.message),
						messageId = new AttributeValue().withS(chatMessage.id),
						alertId = new AttributeValue().withN(Integer.toString(numericalId)),
						senderId = new AttributeValue().withN(chatMessage.senderId),
						timestamp = new AttributeValue().withN(Long.toString(chatMessage.timestamp));

				HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
				item.put("message", message);
				item.put("message_id", messageId);
				item.put("alert_id", alertId);
				item.put("sender_id", senderId);
				item.put("timestamp", timestamp);

				PutItemRequest insertion = new PutItemRequest()
						.withTableName(mConfig.getAwsDynamoDbTable())
						.withItem(item)
						.withReturnValues(ReturnValue.ALL_OLD);
				
				try {
					Log.i("aaa", "pre-put with item=" + insertion.getItem().toString());
					mDdb.putItem(insertion);
					Log.i("aaa", "resultMessageId=" + chatMessage.id);
					completeDeliveryOf(chatMessage.id);
				} catch (AmazonClientException e) {
					Log.e("javelin", "Error sending message.", e);
					
					if (JavelinAlertManager.getInstance(mContext, mConfig).isRunning()) {
						Log.i("javelin", "Retrying...");
						sendPendingMessages();
					}
				}
			}
			
			return null;
		}
		
	}
	
	public void setNewIncomingMessagesListener(OnNewIncomingChatMessagesListener l) {
		mNewIncomingMessagesListener = l;
	}
	
	public void removeNewIncomingMessagesListener(OnNewIncomingChatMessagesListener l) {
		if (mNewIncomingMessagesListener != null && mNewIncomingMessagesListener.equals(l)) {
			mNewIncomingMessagesListener = null;
		}
	}
	
	public static interface OnNewIncomingChatMessagesListener {
		void onNewIncomingChatMessages(List<String> incomingMessages);
	}
	
	public void addOnNewChatMessageListener(OnNewChatMessageListener l) {
		mNewMessageListeners.add(l);
		l.onNewChatMessage(mMessages);
	}
	
	public void removeOnNewChatMessageListener(OnNewChatMessageListener l) {
		mNewMessageListeners.remove(l);
	}
	
	public static interface OnNewChatMessageListener {
		void onNewChatMessage(List<ChatMessage> allMessages);
	}
}
