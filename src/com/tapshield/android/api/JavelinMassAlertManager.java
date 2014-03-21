package com.tapshield.android.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.model.MassAlert;

public class JavelinMassAlertManager {

	private static final String PREF = "com.tapshield.android.api.preferences.name";
	private static final String PREF_KEY_MESSAGES = "com.tapshield.android.api.massalertmanager.preferences.key.messages";
	private static final String PREF_KEY_LASTCHECK = "com.tapshield.android.api.massalertmanager.preferences.key.lastcheck";
	
	private static final String KEY_LIST = "results";
	
	private static JavelinMassAlertManager mInstance;
	private static Context mContext;
	private static JavelinConfig mConfig;
	private static SharedPreferences mPreferences;
	private static List<MassAlert> mMassAlerts;
	private static List<OnMassAlertUpdateListener> mListeners;
	private static OnNewMassAlertListener mNewMassAlertListener;
	private static long mLastCheck;
	
	public static JavelinMassAlertManager getInstance(Context context, JavelinConfig config) {
		if (mInstance == null) {
			mInstance = new JavelinMassAlertManager(context, config);
		}
		return mInstance;
	}
	
	private JavelinMassAlertManager(Context context, JavelinConfig config) {
		mContext = context.getApplicationContext();
		mConfig = config;
		mPreferences = mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE);
		mMassAlerts = new ArrayList<MassAlert>();
		mListeners = new ArrayList<OnMassAlertUpdateListener>();
		mLastCheck = 0;
		getCache();
	}
	
	private void setCache() {
		if (mMassAlerts == null || mMassAlerts.isEmpty()) {
			return;
		}
		
		String serializedList = MassAlert.serializeListToString(mMassAlerts);
		
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(PREF_KEY_MESSAGES, serializedList);
		editor.commit();
	}
	
	private void getCache() {
		if (mMassAlerts != null && !mMassAlerts.isEmpty()) {
			mMassAlerts.clear();
		}
		
		String serializedList = mPreferences.getString(PREF_KEY_MESSAGES, null);
		
		if (serializedList == null) {
			return;
		}
		
		mMassAlerts.addAll(MassAlert.deserializeListFromString(serializedList));
		
		mLastCheck = mPreferences.getLong(PREF_KEY_LASTCHECK, 0);
	}
	
	private void setLastCheck(long lastCheck) {
		mLastCheck = lastCheck;

		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putLong(PREF_KEY_LASTCHECK, lastCheck);
		editor.commit();
	}
	
	private long getLastCheck() {
		return mLastCheck;
	}
	
	public void fetch() {
		fetch(null);
	}
	
	public void fetch(final OnMassAlertFetchListener l) {

		JavelinCommsCallback callback = new JavelinCommsCallback() {

			@Override
			public void onEnd(JavelinCommsRequestResponse response) {

				if (response.successful) {
					Log.i("javelin", "mass alerts=" + response.toString());

					List<MassAlert> list = null;

					try {
						list = new ArrayList<MassAlert>();
						JSONArray a = response.jsonResponse.getJSONArray(KEY_LIST);
						for(int i = 0; i < a.length(); i++) {
							JSONObject o = a.getJSONObject(i);
							MassAlert m = MassAlert.deserializeFromJson(o);
							list.add(m);
						}
					} catch (Exception e) {
						JavelinCommsRequestResponse errorResponse = new JavelinCommsRequestResponse();
						errorResponse.successful = false;
						errorResponse.exception = new Exception("Error parsing retrieved mass alerts data");
						return;
					}

					if (list.isEmpty()) {
						return;
					}

					long latestTimestamp = list.get(0).getTimestampInSeconds();
					boolean nothingNew = getLastCheck() == latestTimestamp;

					if (nothingNew) {
						return;
					}

					mMassAlerts.clear();
					mMassAlerts.addAll(list);
					
					notifyListeners();
					if (mNewMassAlertListener != null) {
						mNewMassAlertListener.onNewMassAlert();
					}
					
					
					setLastCheck(latestTimestamp);
					setCache();

					if (l != null) {
						l.onMassAlertFetch(true, null);
					}
				} else {
					if (l != null) {
						l.onMassAlertFetch(response.successful, response.exception);
					}
				}

				Log.i("javelin", "mass alerts=" + response.toString());
			}
		};

		JavelinUserManager userManager = JavelinUserManager.getInstance(mContext, mConfig);

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(
				new BasicNameValuePair(
						JavelinClient.PARAM_AGENCY, Integer.toString(userManager.getUser().agency.id)));

		JavelinComms.httpGet(
				JavelinUtils.buildFinalUrl(mConfig, JavelinClient.URL_MASSALERTS),
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + userManager.getApiToken(),
				params,
				callback);
	}
	
	private void notifyListeners() {
		if (mMassAlerts == null || mMassAlerts.isEmpty()) {
			return;
		}
		
		for (OnMassAlertUpdateListener l : mListeners) {
			if (l != null) {
				l.onMassAlertUpdate(mMassAlerts);
			}
		}
	}
	
	public void setOnNewMassAlertListener(OnNewMassAlertListener l) {
		mNewMassAlertListener = l;
	}
	
	public void removeOnNewMassAlertListener(OnNewMassAlertListener l) {
		if (mNewMassAlertListener != null && mNewMassAlertListener.equals(l)) {
			mNewMassAlertListener = null;
		}
	}
	
	public interface OnNewMassAlertListener {
		void onNewMassAlert();
	}
	
	public void addOnMassAlertUpdateListener(OnMassAlertUpdateListener l) {
		mListeners.add(l);
		l.onMassAlertUpdate(mMassAlerts);
		fetch();
	}
	
	public void removeOnMassAlertUpdateListener(OnMassAlertUpdateListener l) {
		mListeners.remove(l);
	}
	
	public interface OnMassAlertUpdateListener {
		void onMassAlertUpdate(List<MassAlert> allMassAlerts);
	}
	
	public interface OnMassAlertFetchListener {
		void onMassAlertFetch(boolean success, Throwable e);
	}
}
