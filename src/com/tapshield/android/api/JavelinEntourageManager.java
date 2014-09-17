package com.tapshield.android.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;

public class JavelinEntourageManager {

	private static final String REGEX_NONDIGIT = "\\D";
	private static final String PARAM_USER = "user";
	private static final String PARAM_NAME = "name";
	private static final String PARAM_PHONE = "phone_number";
	private static final String PARAM_EMAIL = "email_address";
	private static final String PARAM_MESSAGE = "message";
	private static final String PARAM_MEMBERS = "entourage_members";
	
	private static JavelinEntourageManager mInstance;
	private Context mContext;
	private JavelinConfig mConfig;
	
	static JavelinEntourageManager getInstance(Context context, JavelinConfig config) {
		if (mInstance == null) {
			mInstance = new JavelinEntourageManager(context, config);
		}
		return mInstance;
	}
	
	private JavelinEntourageManager(Context context, JavelinConfig config) {
		mContext = context.getApplicationContext();
		mConfig = config;
	}
	
	public void addMemberWithEmail(final String name, final String email, final EntourageListener l) {
		addMemberWithData(name, PARAM_EMAIL, email, l);
	}
	
	public void addMemberWithPhone(final String name, final String phoneNumber, final EntourageListener l) {
		final String cleanedPhoneNumber = phoneNumber.trim().replaceAll(REGEX_NONDIGIT, "");
		addMemberWithData(name, PARAM_PHONE, cleanedPhoneNumber, l);
	}
	
	private void addMemberWithData(final String name, final String dataKey, final String dataValue, final EntourageListener l) {
		String baseUrl = mConfig.getBaseUrl() + JavelinClient.URL_ENTOURAGE_MEMBERS;
		
		JavelinUserManager userManager = JavelinClient
				.getInstance(mContext, mConfig)
				.getUserManager();
		
		String userUrl = userManager.getUser().url;
		String authToken = userManager.getApiToken();
		
		if (userUrl.contains(mConfig.getBaseUrl())) {
			userUrl = userUrl.substring(mConfig.getBaseUrl().length());
		}
		
		if (!userUrl.startsWith("/")) {
			userUrl = "/" + userUrl;
		}
		
		Log.i("aaa", "entourage base=" + baseUrl + " user=" + userUrl + " auth=" + authToken);

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_USER, userUrl));
		params.add(new BasicNameValuePair(PARAM_NAME, name));
		params.add(new BasicNameValuePair(dataKey, dataValue));
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				Log.i("aaa", "entourage add success=" + response.successful);
				
				int memberId = -1;
				
				if (response.successful) {
					Log.i("aaa", response.response);
					try {
						JSONObject o = new JSONObject(response.response);
						String url = o.getString("url");
						memberId = JavelinUtils.extractLastIntOfString(url);
					} catch (Exception e) {}
				} else {
					Log.i("aaa", response.exception.getMessage());
				}
				
				l.onMemberAdded(response.successful, memberId, response.exception.getMessage());
			}
		};
		
		JavelinComms.httpPost(
				baseUrl,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + authToken,
				params,
				callback);
	}
	
	public void removeMemberWithId(final int id, final EntourageListener l) {
		String url = mConfig.getBaseUrl() + JavelinClient.URL_ENTOURAGE_MEMBERS
				+ Integer.toString(id) + "/";
		
		String token = JavelinClient
				.getInstance(mContext, mConfig)
				.getUserManager()
				.getApiToken();
		
		Log.i("aaa", "entourage remove url=" + url);
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				Log.i("aaa", "entourage delete success=" + response.successful + " response=" + response.response);
				String error = response.successful ? response.response : response.exception.getMessage();
				l.onMemberRemoved(response.successful, id, response.exception.getMessage());
			}
		};
		
		JavelinComms.httpDelete(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + token,
				callback);
	}
	
	public void messageMembers(final String message, final EntourageListener l) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_MESSAGE, message));

		JavelinUserManager userManager = JavelinClient.getInstance(mContext, mConfig)
				.getUserManager();
		
		String userUrl = userManager.getUser().url;
		String token = userManager.getApiToken();
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				Log.i("aaa", "entourage message success=" + response.successful + " response=" + response.response);
				l.onMembersMessage(response.successful, response.response, response.exception.getMessage());
			}
		};
		
		String url = userUrl + JavelinClient.URL_ENTOURAGE_MESSAGE;
		Log.i("aaa", "entourage message url=" + url);
		
		JavelinComms.httpPost(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + token,
				params,
				callback);
	}
	
	public void fetchMembers(final EntourageListener l) {
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				
				boolean ok = response.successful;
				String message = null;
				String error = null;
				
				if (response.successful) {
					try {
						message = response.response;
					} catch (Exception e) {
						ok = false;
						message = null;
						error = e.getMessage();
					}
				} else {
					error = response.exception.getMessage();
				}
				
				l.onMembersFetch(ok, message, error);
			}
		};
		
		JavelinUserManager userManager = JavelinClient.getInstance(mContext, mConfig)
				.getUserManager();
		
		String userUrl = userManager.getUser().url;
		String token = userManager.getApiToken();
		
		JavelinComms.httpGet(
				userUrl,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + token,
				null,
				callback);
	}
	
	public interface EntourageListener {
		void onMemberAdded(boolean ok, int memberId, String errorIfNotOk);
		void onMemberRemoved(boolean ok, int memberId, String errorIfNotOk);
		void onMembersMessage(boolean ok, String message, String errorIfNotOk);
		void onMembersFetch(boolean ok, String message, String errorIfNotOk);
	}
}
