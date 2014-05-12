/* 
 * JavelinClient.java API Client for Javelin (back-end of TapShield)
 * February 2014
 */

package com.tapshield.android.api;

import java.io.DataOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.model.Agency;

public class JavelinClient {

	static final String URL_API = "api/";
	
	static final String API_VERSION = "1";
	static final String API_VERSION_URL = "v" + API_VERSION + "/";
	
	static final String URL_REGISTRATION = URL_API + "register/";
	static final String URL_LOGIN = URL_API + "login/";
	static final String URL_UPDATE_SUFFIX = "update_required_info/";
	static final String URL_VERIFY = URL_API + "verified/";
	static final String URL_TOKEN_RETRIEVAL = URL_API + "retrieve-token/";
	static final String URL_RESEND_VERIFICATION = URL_API + "resend-verification/";
	static final String URL_DEVICE_TOKEN_SUFFIX = "update_device_token/";
	static final String URL_TWILIO_TOKEN_RETRIEVAL = URL_API + "twilio-call-token/";
	
	static final String URL_AGENCIES = URL_API + API_VERSION_URL + "agencies/";
	static final String URL_MASSALERTS = URL_API + API_VERSION_URL + "mass-alerts/";
	static final String URL_ALERTS = URL_API + API_VERSION_URL + "alerts/";
	static final String URL_USERS = URL_API + API_VERSION_URL + "users/";
	static final String URL_LOCATIONS = URL_API + API_VERSION_URL + "alert-locations/";
	static final String URL_PROFILES = URL_API + API_VERSION_URL + "user-profiles/";
	static final String URL_DISARM_SUFFIX = "disarm/";
	static final String URL_VERIFICATION_CODE_REQUEST_SUFFIX = "send_sms_verification_code/";
	static final String URL_VERIFICATION_CODE_CHECK_SUFFIX = "check_sms_verification_code/";
	static final String URL_ENTOURAGE_MEMBERS = URL_API + API_VERSION_URL + "entourage-members/";
	static final String URL_ENTOURAGE_MESSAGE = "message_entourage/";
	static final String URL_REPORT_SOCIAL = URL_API + API_VERSION_URL + "social-crime-reports/";
	
	static final String URL_RESET_PASSWORD_SUFFIX = "accounts/password/reset/";
	
	static final String URL_LOGIN_SOCIAL_GOOGLE_PLUS = URL_API + "create-google-user/";
	
	static final String HEADER_AUTH = "Authorization";
	static final String HEADER_VALUE_TOKEN_PREFIX = "Token ";
	
	static final String PARAM_USERNAME = "username";
	static final String PARAM_PROFILE_USER = "user";
	static final String PARAM_EMAIL = "email";
	static final String PARAM_PASSWORD = "password";
	static final String PARAM_PHONE = "phone_number";
	static final String PARAM_CODE = "disarm_code";
	static final String PARAM_FIRST_NAME = "first_name";
	static final String PARAM_LAST_NAME = "last_name";
	static final String PARAM_AGENCY = "agency";
	static final String PARAM_TOKEN = "token";
	static final String PARAM_TOKEN_DEVICE = "deviceToken";
	static final String PARAM_DEVICE = "deviceType";
	static final String PARAM_SMS_CODE = "code";
	static final String PARAM_NEARBY_LATITUDE = "latitude";
	static final String PARAM_NEARBY_LONGITUDE = "longitude";
	static final String PARAM_NEARBY_DISTANCE = "distance_within";
	static final String PARAM_ACCESS_TOKEN = "access_token";
	static final String PARAM_REFRESH_TOKEN = "refresh_token";
	
	static final String DEVICE_ANDROID = "A";
	
	private static Context mContext;
	private static JavelinClient mInstance;
	private static JavelinConfig mConfig;
	
	public static JavelinClient getInstance(Context context, JavelinConfig config) {
		if (config == null) {
			return null;
		}
		
		if (mInstance == null) {
			mInstance = new JavelinClient(context, config);
		}
		return mInstance;
	}
	
	private JavelinClient(Context context, JavelinConfig config) {
		mContext = context.getApplicationContext();
		mConfig = config;
	}
	
	//get other managers via context and config
	
	public JavelinAlertManager getAlertManager() {
		return JavelinAlertManager.getInstance(mContext, mConfig);
	}
	
	public JavelinChatManager getChatManager() {
		return JavelinChatManager.getInstance(mContext, mConfig);
	}
	
	public JavelinMassAlertManager getMassAlertManager() {
		return JavelinMassAlertManager.getInstance(mContext, mConfig);
	}
	
	public JavelinUserManager getUserManager() {
		return JavelinUserManager.getInstance(mContext, mConfig);
	}
	
	public JavelinEntourageManager getEntourageManager() {
		return JavelinEntourageManager.getInstance(mContext, mConfig);
	}
	
	public JavelinSocialReportingManager getSocialReportingManager() {
		return JavelinSocialReportingManager.getInstance(mContext, mConfig);
	}
	
	public void fetchAgencies(final OnAgenciesFetchListener l) {
		
		final JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					l.onAgenciesFetch(response.successful, Agency.jsonToList(response.jsonResponse), null);
				} else {
					Log.i("javelin", response.exception.getMessage());
					l.onAgenciesFetch(response.successful, null, response.exception);
				}
			}
		};
		
		JavelinComms.httpGet(
				JavelinUtils.buildFinalUrl(mConfig, URL_AGENCIES),
				HEADER_AUTH,
				HEADER_VALUE_TOKEN_PREFIX + mConfig.getMasterToken(),
				null,
				callback);
	}
	
	public void fetchAgenciesNearby(final double latitude, final double longitude,
			final float distance, final OnAgenciesFetchListener l) {
		
		final JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					l.onAgenciesFetch(response.successful, Agency.jsonToList(response.jsonResponse), null);
				} else {
					Log.i("javelin", response.exception.getMessage());
					l.onAgenciesFetch(response.successful, null, response.exception);
				}
			}
		};
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_NEARBY_LATITUDE, Double.toString(latitude)));
		params.add(new BasicNameValuePair(PARAM_NEARBY_LONGITUDE, Double.toString(longitude)));
		params.add(new BasicNameValuePair(PARAM_NEARBY_DISTANCE, Float.toString(distance)));
		
		JavelinComms.httpGet(
				JavelinUtils.buildFinalUrl(mConfig, URL_AGENCIES),
				HEADER_AUTH,
				HEADER_VALUE_TOKEN_PREFIX + mConfig.getMasterToken(),
				params,
				callback);
	}
	
	public void verifyUser(String username, final OnUserVerificationListener l) {
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					l.onUserVerification(response.successful, response.response);
				} else {
					String errorMessage =
							response.exception == null ? 
									response.response : 
											response.exception.getMessage();
					l.onUserVerification(response.successful, errorMessage);
				}
			}
		};
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_EMAIL, username));
		
		JavelinComms.httpGet(
				JavelinUtils.buildFinalUrl(mConfig, URL_VERIFY),
				HEADER_AUTH,
				HEADER_VALUE_TOKEN_PREFIX + mConfig.getMasterToken(),
				params,
				callback);
	}
	
	public void resendVerificationEmail(String email, final OnVerificationEmailRequestListener l) {
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (l == null) {
					return;
				}
				
				if (response.successful) {
					l.onVerificationEmailRequest(response.successful, null);
				} else {
					l.onVerificationEmailRequest(response.successful,
							new Exception(response.response + " " + response.exception.getMessage()));
				}
			}
		};
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_EMAIL, email));
		
		JavelinComms.httpPost(
				JavelinUtils.buildFinalUrl(mConfig, URL_RESEND_VERIFICATION),
				HEADER_AUTH,
				HEADER_VALUE_TOKEN_PREFIX + mConfig.getMasterToken(),
				params,
				callback);
	}
	
	public void sendPasswordResetEmail(final String email, final OnRequestPasswordResetListener l) {
		String url = mConfig.getBaseUrl() + JavelinClient.URL_RESET_PASSWORD_SUFFIX;
		
		Log.i("javelin", "cookie request url=" + url);
		
		new PasswordResetHttp(l, email).execute(url);
	}
	
	private class PasswordResetHttp extends AsyncTask<String, String, Boolean> {

		private OnRequestPasswordResetListener listener;
		private String email, errorMessage;
		
		public PasswordResetHttp(OnRequestPasswordResetListener l, String email) {
			listener = l;
			this.email = email;
		}
		
		private void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		
		private String getErrorMessage() {
			return this.errorMessage;
		}
		
		@Override
		protected Boolean doInBackground(String... urls) {
			HttpsURLConnection connection = null;
			String cookie = "";
			try {
				URL url = new URL(urls[0]);
				Log.i("javelin", "password reset url=" + url.toString());
				connection = (HttpsURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setDoInput(true);
				int code = connection.getResponseCode();
				publishProgress("step1 code=" + code);
				if (code == 200) {
					String response = IOUtils.toString(connection.getInputStream());
					cookie = connection.getHeaderField("Set-Cookie");
					publishProgress(response);
				} else {
					Log.e("Javelin", "JavelinUserManager error: Http response code" + code);
					setErrorMessage("Request of cookie for password reset could not be made.");
					return false;
				}
			} catch (Exception e) {
				setErrorMessage("Request of cookie for password reset could not be made.");
				return false;
			}
			
			if (connection != null) {
				connection.disconnect();
			}

			int firstEqual = cookie.indexOf("=");
			int firstSemiColon = cookie.indexOf(";");
			
			String cookieName = cookie.substring(0, firstEqual);
			String cookieValue = cookie.substring(firstEqual + 1, firstSemiColon);
			
			publishProgress(cookie);
			publishProgress(cookieName);
			publishProgress(cookieValue);
			
			try {
				String params = "email=" + email;

				publishProgress(params);

				HttpsURLConnection.setFollowRedirects(false);
				
				URL resetUrl = new URL(urls[0]);
				connection = (HttpsURLConnection) resetUrl.openConnection();
				connection.setRequestMethod("POST");
				connection.addRequestProperty("Referer", urls[0]);
				connection.addRequestProperty("X-CSRFToken", cookieValue);
				connection.addRequestProperty("Cookie", cookieName + "=" + cookieValue);
				connection.setDoInput(true);
				connection.setDoOutput(true);

				DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
				dos.write(params.getBytes());
				dos.flush();
				dos.close();

				int code = connection.getResponseCode();
				publishProgress("step2 code=" + code);
				
				//other than 302 means a failure in the redirection of a well-formed request
				if (code != 302) {
					publishProgress(IOUtils.toString(connection.getInputStream()));
					setErrorMessage("Error committing final step in password resetting.");
					return false;
				}
			} catch (Exception e) {
				setErrorMessage("Error committing final step in password resetting.");
				return false;
			}

			if (connection != null) {
				connection.disconnect();
			}
			return true;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			for(String value : values) {
				Log.d("javelin", "'password reset' update=" + value);
			}
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPostExecute(Boolean ok) {
			this.listener.onRequestPasswordReset(ok, ok ? null : new Throwable(getErrorMessage()));
		}
	}
	
	public static interface OnUserVerificationListener {
		void onUserVerification(boolean successful, String message);
	}
	
	public static interface OnAgenciesFetchListener {
		void onAgenciesFetch(boolean successful, List<Agency> agencies, Throwable exception);
	}
	
	public static interface OnVerificationEmailRequestListener {
		void onVerificationEmailRequest(boolean successful, Throwable e);
	}
	
	public static interface OnRequestPasswordResetListener {
		void onRequestPasswordReset(boolean successful, Throwable e);
	}
}
