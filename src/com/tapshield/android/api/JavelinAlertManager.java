package com.tapshield.android.api;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;

public class JavelinAlertManager {

	//types of emergency for public access
	public static final int TYPE_TIMER_AUTO = 0;
	public static final int TYPE_TIMER_SLIDER = 1;
	public static final int TYPE_START_DELAYED = 2;
	public static final int TYPE_START_REQUESTED = 3;
	public static final int TYPE_HEADSET_UNPLUGGED = 4;
	public static final int TYPE_CHAT = 5;
	public static final int TYPE_911 = 6;
	
	private static final String PARAM_TYPE_EMERGENCY = "E";
	private static final String PARAM_TYPE_CHAT = "C";
	private static final String PARAM_TYPE_TIMER = "T";
	private static final String PARAM_TYPE_911 = "N";
	private static final String PARAM_TYPE_YANK = "Y";
	
	private static final String PARAM_USER = "user";
	private static final String PARAM_ALERT_ID = "alert";
	private static final String PARAM_ALERT_TYPE = "alert_type";
	private static final String PARAM_ACCURACY_INIT = "location_accuracy";
	private static final String PARAM_ACCURACY_UPDATE = "accuracy";
	private static final String PARAM_ALTITUDE_INIT = "location_altitude";
	private static final String PARAM_ALTITUDE_UPDATE = "altitude";
	private static final String PARAM_LATITUDE_INIT = "location_latitude";
	private static final String PARAM_LATITUDE_UPDATE = "latitude";
	private static final String PARAM_LONGITUDE_INIT = "location_longitude";
	private static final String PARAM_LONGITUDE_UPDATE = "longitude";
	private static final String PARAM_CALL_LENGTH = "call_length";
	
	private static final long DISARM_ATTEMPT_INTERVAL = 5000;
	
	private enum Status {
		IDLE,
		AWAITING_ID,
		ESTABLISHED
	}
	
	private static JavelinAlertManager mInstance;
	private static JavelinConfig mConfig;
	private static Context mContext;
	private static AlertListener mAlertListener;
	private static Status mStatus = Status.IDLE;
	private static String mId;
	private static boolean mCompleted = false;
	
	private static OnDispatcherAlertedListener mListener;
	
	static JavelinAlertManager getInstance(Context context, JavelinConfig config) {
		if (mInstance == null) {
			mInstance = new JavelinAlertManager(context, config);
		}
		return mInstance;
	}
	
	private JavelinAlertManager(Context context, JavelinConfig config) {
		mContext = context.getApplicationContext();
		mConfig = config;
	}
	
	public void create(int type, Location l) {
		Log.d("javelin", "Creating...");
		if (mStatus != Status.IDLE) {
			return;
		}
		
		String discreteType = getDiscreteType(type);
		
		mStatus = Status.AWAITING_ID;
		
		if (mAlertListener != null) {
			mAlertListener.onConnecting();
		}

		RequestHandler2 handler = new RequestHandler2() {
			
			@Override
			public void beforeRequest(Request<?> arg0) {}

			@Override
			public void afterResponse(Request<?> request, Response<?> response) {
				Log.i("javelin", "Dispatcher alerted");
			}

			@Override
			public void afterError(Request<?> request, Response<?> response, Exception e) {
				Log.e("javelin", "Error alerting dispatcher:" + e);
				mStatus = Status.IDLE;
				notifyListenerIfPresent(e);
			}
		};
		
		new AwsSqsAlertAsync(handler).setType(discreteType).execute(l);
	}
	
	public void update(Location l) {
		
		if (mId == null || mId.length() == 0) {
			return;
		}
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				Log.e("javelin", "Error updating location, attempting again in the next update",
						response.exception);
			}
		};
		
		
		String apiToken = JavelinUserManager.getInstance(mContext, mConfig).getApiToken();
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_ALERT_ID, mId));
		params.add(new BasicNameValuePair(PARAM_ACCURACY_UPDATE, Float.toString(l.getAccuracy())));
		params.add(new BasicNameValuePair(PARAM_ALTITUDE_UPDATE, Double.toString(l.getAltitude())));
		params.add(new BasicNameValuePair(PARAM_LATITUDE_UPDATE, Double.toString(l.getLatitude())));
		params.add(new BasicNameValuePair(PARAM_LONGITUDE_UPDATE, Double.toString(l.getLongitude())));
		
		JavelinComms.httpPost(
				JavelinUtils.buildFinalUrl(mConfig, JavelinClient.URL_LOCATIONS),
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + apiToken,
				params,
				callback);
	}
	
	public boolean isCompleted() {
		return mCompleted;
	}
	
	public void notifyCompletion() {
		if (mCompleted) {
			return;
		}
		
		mCompleted = true;
		if (mAlertListener != null) {
			mAlertListener.onCompleted();
		}
	}
	
	public void notifyCompletion(String completedId) {
		if (getId() != null && !getId().isEmpty()
				&& completedId != null && !completedId.isEmpty()
				&& matchesId(completedId)) {
			notifyCompletion();
		}
	}
	
	public void notifyId(String id) {
		Log.d("javelin", "Notifying of new ID via push. Is it running? " + isRunning());
		mId = id;
		boolean cancelled = !isRunning();
		
		if (cancelled) {
			notifyOfDisarm();
			return;
		}
		
		mStatus = Status.ESTABLISHED;
		
		JavelinChatManager chatManager = JavelinChatManager.getInstance(mContext, mConfig);
		chatManager.notifyId(mId);
		
		if (mAlertListener != null) {
			mAlertListener.onBackEndNotified();
		}
		
		notifyListenerIfPresent(null);
		JavelinUserManager.getInstance(mContext, mConfig).uploadUserProfile();
	}
	
	private boolean matchesId(String id) {
		
		if (!isRunning()) {
			return false;
		}
		
		return JavelinUtils.extractLastIntOfString(mId) == JavelinUtils.extractLastIntOfString(id);
	}
	
	public void cancel() {
		Log.d("javelin", "Cancelling...");
		if (mStatus == Status.IDLE) {
			return;
		}
		
		mStatus = Status.IDLE;
		
		JavelinChatManager chatManager = JavelinChatManager.getInstance(mContext, mConfig);
		chatManager.notifyEnd();
		
		if (mAlertListener != null) {
			mAlertListener.onCancel();
		}
		
		mCompleted = false;
		
		notifyOfDisarm();
	}
	
	private void notifyOfDisarm() {
		if (mId == null) {
			return;
		}
		
		final OnDispatcherAlertedDisarmListener l = new JavelinAlertManager.OnDispatcherAlertedDisarmListener() {
			
			@Override
			public void onDispatcherAlertedDisarm(Exception e) {
				if (e == null) {
					Log.i("javelin", "Disarmed alert");
					mId = null;
				} else {
					Log.e("javelin", "Error notifying backend of disarmed alert", e);
					Log.e("javelin", "Retrying to notify of disarmed alert...");
					notifyOfDisarm();
				}
			}
		};
		
		String apiToken = JavelinUserManager.getInstance(mContext, mConfig).getApiToken();
		String disarmUrl = JavelinUtils.buildFinalUrl(mConfig,
				getId().concat(JavelinClient.URL_DISARM_SUFFIX));
		Log.d("javelin", "disarm attempt disarmUrl=" + disarmUrl);
		
		new DisarmNotifier(l).execute(disarmUrl, apiToken);
	}
	
	private String getId() {
		return mId;
	}
	
	public boolean isRunning() {
		return mStatus != Status.IDLE;
	}
	
	public boolean isEstablished() {
		return mStatus == Status.ESTABLISHED;
	}
	
	public void setEmergencyCallDuration(final int seconds) {
		if (!isEstablished() || getId() == null || getId().isEmpty() || seconds < 0) {
			return;
		}

		JavelinCommsCallback callback = new JavelinCommsCallback() {

			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				Log.i("javelin", "Call length notification " + response.response);
				if (!response.successful) {
					//setEmergencyCallDuration(seconds);
				}
			}
		};

		String url = JavelinUtils.buildFinalUrl(mConfig, getId());
		String apiToken = JavelinUserManager.getInstance(mContext, mConfig).getApiToken();

		Log.i("javelin", "About to notify at " + url + " about call length of " + seconds);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_CALL_LENGTH, Integer.toString(seconds)));
		
		JavelinComms.httpPatch(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + apiToken,
				params,
				callback);
	}
	
	private class AwsSqsAlertAsync extends AsyncTask<Location, Void, Void> {

		private RequestHandler2 mHandler; 
		private String mType;
		
		public AwsSqsAlertAsync(RequestHandler2 handler) {
			mHandler = handler;
		}
		
		public AwsSqsAlertAsync setType(String type) {
			mType = type;
			return this;
		}

		private String getType() {
			return mType;
		}
		
		@Override
		protected Void doInBackground(Location... locations) {
			Location location = locations[0];
			
			AWSCredentials credentials = new BasicAWSCredentials(
					mConfig.getAwsSqsAccessKey(),
					mConfig.getAwsSqsSecretKey());
			AmazonSQSClient sqs = new AmazonSQSClient(credentials);
			sqs.addRequestHandler(mHandler);
			
			String messageRequestBody = "";
			try {
				JSONObject alert = new JSONObject()
					.put(PARAM_USER, JavelinUserManager.getInstance(mContext, mConfig).getUser().email)
					.put(PARAM_ACCURACY_INIT, Float.toString(location.getAccuracy()))
					.put(PARAM_ALTITUDE_INIT, Double.toString(location.getAltitude()))
					.put(PARAM_LATITUDE_INIT, Double.toString(location.getLatitude()))
					.put(PARAM_LONGITUDE_INIT, Double.toString(location.getLongitude()))
					.put(PARAM_ALERT_TYPE, getType());
				
				messageRequestBody = JavelinUtils.getAmqpMessageStringWithJsonObject(alert);
			} catch (JSONException e) {
				notifyListenerIfPresent(e);
				return null;
			}
			
			if (messageRequestBody == null || messageRequestBody.length() == 0) {
				notifyListenerIfPresent(new Exception("Message request body null or empty"));
				return null;
			}
			
			try {
				GetQueueUrlRequest queueRequest = new GetQueueUrlRequest(mConfig.getAwsSqsQueue()); 
				GetQueueUrlResult queueResult = sqs.getQueueUrl(queueRequest);
				SendMessageRequest sendMessageRequest = new SendMessageRequest(queueResult.getQueueUrl(), messageRequestBody);

				//make a final check before sending the request to make sure it was disarmed just now
				if (!isRunning()) {
					return null;
				}

				sqs.sendMessage(sendMessageRequest);

			} catch (Exception e) {
				notifyListenerIfPresent(e);
				return null;
			}
			return null;
		}
	}
	
	private String getDiscreteType(int type) {
		switch(type) {
		case TYPE_START_DELAYED:
			return PARAM_TYPE_EMERGENCY;
		case TYPE_START_REQUESTED:
			return PARAM_TYPE_EMERGENCY;
		case TYPE_TIMER_AUTO:
			return PARAM_TYPE_TIMER;
		case TYPE_TIMER_SLIDER:
			return PARAM_TYPE_EMERGENCY;
		case TYPE_CHAT:
			return PARAM_TYPE_CHAT;
		case TYPE_HEADSET_UNPLUGGED:
			return PARAM_TYPE_YANK;
		case TYPE_911:
			return PARAM_TYPE_911;
		}
         return PARAM_TYPE_EMERGENCY;
	}
	
	private void notifyListenerIfPresent(Exception e) {
		if (mListener == null) {
			return;
		}
		mListener.onDispatcherAlerted(e);
	}
	
	public void setOnDispatcherAlertedListener(OnDispatcherAlertedListener l) {
		mListener = l;
	}
	
	private void removeOnDispatcherAlerted() {
		mListener = null;
	}
	
	public void removeOnDispatcherAlertedListener(OnDispatcherAlertedListener l) {
		if (mListener != null && mListener.equals(l)) {
			mListener = null;
		}
	}
	
	public static interface OnDispatcherAlertedListener {
		void onDispatcherAlerted(Exception e);
	}
	
	public static interface OnDispatcherAlertedDisarmListener {
		void onDispatcherAlertedDisarm(Exception e);
	}
	
	private class DisarmNotifier extends AsyncTask<String, Void, Exception> {

		private OnDispatcherAlertedDisarmListener l;
		private String response;
		
		public DisarmNotifier(OnDispatcherAlertedDisarmListener l) {
			this.l = l;
		}
		
		@Override
		protected Exception doInBackground(String... params) {
			String disarmUrl = params[0];
			String apiToken = params[1];
			
			HttpsURLConnection connection = null;
			int code = -1;
			Exception error = null;
			try {
				URL url = new URL(disarmUrl);
				connection = (HttpsURLConnection) url.openConnection();
				connection.setRequestMethod(HttpPost.METHOD_NAME);
				connection.addRequestProperty(JavelinClient.HEADER_AUTH,
						JavelinClient.HEADER_VALUE_TOKEN_PREFIX + apiToken);
				code = connection.getResponseCode();
				if (code == HttpsURLConnection.HTTP_OK) {
					response = IOUtils.toString(connection.getInputStream());
				} else {
					response = null;
					error = new Exception("Error notifying disarm. reason="
							+ IOUtils.toString(connection.getErrorStream()));
				}
			} catch (Exception e) {
				error = e;
			}
			
			if (connection != null) {
				connection.disconnect();
			}
			
			//if error, wait for set interval before notifying for a retry
			if (error != null) {
				try {
					Thread.sleep(DISARM_ATTEMPT_INTERVAL);
				} catch (InterruptedException e) {}
			}
			
			return error;
		}
		
		@Override
		protected void onPostExecute(Exception result) {
			l.onDispatcherAlertedDisarm(result);
			super.onPostExecute(result);
		}
	}
	
	public void setAlertListener(AlertListener l) {
		mAlertListener = l;
	}
	
	public void removeAlertListener(AlertListener l) {
		if (mAlertListener != null && mAlertListener.equals(l)) {
			mAlertListener = null;
		}
	}
	
	public static interface AlertListener {
		void onConnecting();
		void onBackEndNotified();
		void onCancel();
		void onCompleted();
	}
}
