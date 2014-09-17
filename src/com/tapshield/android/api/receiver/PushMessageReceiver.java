package com.tapshield.android.api.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.tapshield.android.api.JavelinAlertManager;
import com.tapshield.android.api.JavelinChatManager;
import com.tapshield.android.api.JavelinClient;
import com.tapshield.android.api.JavelinMassAlertManager;
import com.tapshield.android.api.JavelinUserManager;

public class PushMessageReceiver extends WakefulBroadcastReceiver {

	private static final String EXTRA_TYPE = "alert_type"; 
	private static final String EXTRA_ALERT_ID = "alert_id";
	private static final String EXTRA_MESSAGE = "message";
	
	private static final String TYPE_ALERT_RECEIVED = "alert-received";
	private static final String TYPE_MESSAGE_AVAILABLE = "chat-message-available";
	private static final String TYPE_MASS_ALERT = "mass-alert";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("javelin", "Received push notification intent=" + intent);
		
		Bundle extras = intent.getExtras();
		
		boolean wrongMessage = extras == null || !extras.containsKey(EXTRA_TYPE); 
		
		if (wrongMessage) {
			return;
		}
		
		JavelinClient javelin = JavelinClient.getInstance(context, null);
		String type = extras.getString(EXTRA_TYPE);
		String alertId = extras.getString(EXTRA_ALERT_ID);
		String message = extras.getString(EXTRA_MESSAGE);

		if (type.equals(TYPE_ALERT_RECEIVED)) {
			Log.i("javelin", "Alert acknowledged - id available (" + alertId + ")");
			//JavelinAlertManager.getInstance(context).notifyId(alertId);
		} else if (type.equals(TYPE_MESSAGE_AVAILABLE)) {
			JavelinUserManager userManager = javelin.getUserManager();
			JavelinAlertManager alertManager = javelin.getAlertManager();
			
			//if emergency disarmed, do not report
			if (!alertManager.isRunning()) {
				return;
			}
			
			String completeMessage = userManager.getUser().agency.completeMessage;
			boolean complete = message.equals(completeMessage);
			
			if (complete) {
				Log.i("javelin", "Complete message received");
				//JavelinNotificationManager.getInstance(context).notifyCompletion();
				alertManager.notifyCompletion();
			}
			
			Log.i("javelin", "Message(s) available");
			
			JavelinChatManager chatManager = javelin.getChatManager();
			chatManager.fetchMessagesSinceLastCheck();
			//emergencyManager.scheduleUpdater();
		} else if (type.equals(TYPE_MASS_ALERT)) {
			Log.i("javelin", "Mass alert available");
			JavelinMassAlertManager massAlertManager = javelin.getMassAlertManager();
			massAlertManager.fetch();
		}
	}
}
