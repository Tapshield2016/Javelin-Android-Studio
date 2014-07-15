package com.tapshield.android.api;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.model.Agency;
import com.tapshield.android.api.model.User;
import com.tapshield.android.api.model.UserProfile;

public class JavelinUserManager {

	public static final String ACTION_AGENCY_REFRESHED = "com.tapshield.android.action.AGENCY_REFRESHED";
	public static final String ACTION_AGENCY_LOGOS_UPDATED = "com.tapshield.android.action.AGENCY_LOGOS_UPDATED";
	
	public static final int CODE_OK = 1;
	public static final int CODE_ERROR_WRONG_CREDENTIALS = 2;
	public static final int CODE_ERROR_UNVERIFIED_EMAIL = 3;
	public static final int CODE_ERROR_OTHER = 4;
	
	private static final String HEADER_ERROR_NAME = "Auth-Response";
	public static final String HEADER_ERROR_VALUE_CREDENTIALS = "Login failed";
	public static final String HEADER_ERROR_VALUE_UNVERIFIED = "Email unverified";
	public static final String HEADER_ERROR_USER_INACTIVE = "Account inactive";
	public static final String HEADER_ERROR_OTHER = "Other error";
	
	private static final String PREFERENCES_NAME = "com.tapshield.android.preferences.name";
	private static final String PREFERENCES_KEY_USER = "com.tapshield.android.preferences.key.user";
	private static final String PREFERENCES_KEY_PASSWORD = "com.tapshield.android.preferences.key.password";
	private static final String PREFERENCES_KEY_TOKEN_API = "com.tapshield.android.preferences.key.token.api";
	private static final String PREFERENCES_KEY_TOKEN_DEVICE = "com.tapshield.android.preferences.key.token.device";
	
	private enum Status {
		NO_USER,
		LOGGED_IN,
		VERIFIED
	}

	private static JavelinUserManager mInstance;
	private static JavelinConfig mConfig;
	private Context mContext;
	private SharedPreferences mSharedPreferences;
	
	private Status mStatus;
	private User mUser;
	private String mTempPassword;
	
	static JavelinUserManager getInstance(Context context, JavelinConfig config) {
		if (mInstance == null) {
			mInstance = new JavelinUserManager(context, config);
		}
		return mInstance;
	}
	
	private JavelinUserManager(Context context, JavelinConfig config) {
		mContext = context.getApplicationContext();
		mConfig = config;
		mStatus = Status.NO_USER;
		mSharedPreferences = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		getCachePassword();
		getCache();
	}
	
	private void getCache() {
		
		boolean userCached = mSharedPreferences.contains(PREFERENCES_KEY_USER);
		String userSer = mSharedPreferences.getString(PREFERENCES_KEY_USER, null);
		User user;
		
		if (userCached && userSer != null) {
			try {
				user = User.deserialize(userSer);
			} catch (JSONException e) {
				Log.e("javelin", "Error deserializing cached user", e);
				return;
			}
			mUser = user;
			mStatus = Status.VERIFIED;
		}
	}
	
	private void setCache() {
		
		mStatus = Status.NO_USER;
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		
		if (mUser == null) {
			editor.remove(PREFERENCES_KEY_USER);
		} else {
			mStatus = Status.VERIFIED;
			try {
				editor.putString(PREFERENCES_KEY_USER, User.serialize(mUser));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		editor.commit();
	}
	
	public boolean isPresent() {
		return mStatus == Status.LOGGED_IN || mStatus == Status.VERIFIED;
	}
	
	public boolean isVerified() {
		return mStatus == Status.VERIFIED;
	}
	
	void setCachePassword(String password) {
		mTempPassword = password;
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(PREFERENCES_KEY_PASSWORD, password);
		editor.commit();
	}
	
	boolean hasCachePassword() {
		return mTempPassword != null || mSharedPreferences.contains(PREFERENCES_KEY_PASSWORD);
	}
	
	/**
	 * Method for retrieving stored password only accessible by package-level entities
	 * @return stored password if any
	 */
	String getCachePassword() {
		if (mTempPassword == null && mSharedPreferences.contains(PREFERENCES_KEY_PASSWORD)) {
			mTempPassword = mSharedPreferences.getString(PREFERENCES_KEY_PASSWORD, null);
		}
		return mTempPassword;
	}
	
	void clearCachePassword() {
		mTempPassword = null;
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(PREFERENCES_KEY_PASSWORD);
		editor.commit();
	}
	
	String getApiToken() {
		return mSharedPreferences.getString(PREFERENCES_KEY_TOKEN_API, null);
	}
	
	boolean hasApiToken() {
		return mSharedPreferences.contains(PREFERENCES_KEY_TOKEN_API);
	}
	
	private void setApiToken(String apiToken) {
		if (apiToken == null) {
			return;
		}
		
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(PREFERENCES_KEY_TOKEN_API, apiToken);
		editor.commit();
	}
	
	private void removeApiToken() {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(PREFERENCES_KEY_TOKEN_API);
		editor.commit();
	}
	
	private String getDeviceToken() {
		return mSharedPreferences.getString(PREFERENCES_KEY_TOKEN_DEVICE, null);
	}
	
	private boolean hasDeviceToken() {
		return mSharedPreferences.contains(PREFERENCES_KEY_TOKEN_DEVICE);
	}
	
	private void setDeviceToken(String deviceToken) {
		if (deviceToken == null) {
			return;
		}
		
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(PREFERENCES_KEY_TOKEN_DEVICE, deviceToken);
		editor.commit();
	}
	
	private void removeDeviceToken() {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(PREFERENCES_KEY_TOKEN_DEVICE);
		editor.commit();
	}
	public void setUser(User user) {
		mUser = user;
		setCache();
	}
	
	public User getUser() {
		
		if (mStatus == Status.NO_USER) {
			try {
				throw new Exception("No user present. Check via isPresent()");
			} catch (Exception e) {
				Log.e("javelin", e.getMessage());
			}
		}
		return mUser;
	}
	
	
	
	public void signUp(final Agency agency, final String email, final String password, final String phoneNumber, 
			final String disarmCode, final String firstName, final String lastName, final OnUserSignUpListener l) {

		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					l.onUserSignUp(response.successful, null);
				} else {
					String errorMessage = new String();
					if (response.code == 400) {
						try {
							JSONArray a = response.jsonResponse.getJSONArray("email");
							for (int i = 0; i < a.length(); i++) {
								if (!a.isNull(i)) {
									errorMessage = errorMessage.concat(a.getString(i));
								}
							}
						} catch (JSONException je) {
							Log.e("javelin", "Error parsing value for email key in error response when signing up", je);
							errorMessage = new String();
						}
						
						//return if message was parsed
						if (errorMessage.trim().length() > 0) {
							l.onUserSignUp(false, new Throwable(errorMessage));
							return;
						}
					}

					//past this point email is not a key, cycle through and return all errors

					try {
						Iterator<String> keys = response.jsonResponse.keys();
						while (keys.hasNext()) {
							String key = keys.next();
							if (response.jsonResponse.has(key)) {
								errorMessage = errorMessage
										.concat(response.jsonResponse.getString(key)
												.concat(new String(" ")));
							}
						}
					} catch (Exception je) {
						Log.e("javelin", "Error parsing values in error response when signing up", je);
					}

					if (errorMessage.trim().length() > 0) {
						l.onUserSignUp(false, new Throwable(errorMessage));
						return;
					}

					//set response while checking for any possible null json response
					String possibleResponse =
							response.jsonResponse == null ?
									new String() :
										response.jsonResponse.toString();
					
					l.onUserSignUp(false,
							new Throwable("Unknown error: code:" + response.code
									+ " response:" + possibleResponse));
				}
			}
		};
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(JavelinClient.PARAM_USERNAME, email));
		params.add(new BasicNameValuePair(JavelinClient.PARAM_EMAIL, email));
		params.add(new BasicNameValuePair(JavelinClient.PARAM_PASSWORD, password));
		params.add(new BasicNameValuePair(JavelinClient.PARAM_CODE, disarmCode));

		if (phoneNumber != null && phoneNumber.length() > 0) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_PHONE, phoneNumber));
		}
		
		if (agency != null) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_AGENCY, Integer.toString(agency.id)));
		}
		
		if (firstName != null && firstName.length() > 0) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_FIRST_NAME, firstName));
		}
		
		if (lastName != null && lastName.length() > 0) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_LAST_NAME, lastName));
		}
		
		JavelinComms.httpPost(
				JavelinUtils.buildFinalUrl(mConfig, JavelinClient.URL_REGISTRATION),
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + mConfig.getMasterToken(),
				params,
				callback);
	}
	
	public void logIn(final String username, final String password, final OnUserLogInListener l) {
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					User user = null;
					try {
						user = User.deserialize(response.jsonResponse.toString());
					} catch (Exception e) {
						l.onUserLogIn(false, null, CODE_ERROR_OTHER, new Throwable("Error reading incoming user data:" + e));
						return;
					}
					
					mUser = user;
					user.setPassword(password);
					setCache();
					setCachePassword(password);
					getApiTokenForLoggedUser(l);
				} else {
					clearCachePassword();
					removeApiToken();
					removeDeviceToken();

					String message = new String();
					if (response.code == 401) {
						for (Header header : response.headers) {
							if (header.getName().equals(HEADER_ERROR_NAME)) {
								message = header.getValue();
								break;
							}
						}
						
						if (message.equals(HEADER_ERROR_VALUE_CREDENTIALS)) {
							l.onUserLogIn(false, null, CODE_ERROR_WRONG_CREDENTIALS, new Throwable(message));
							return;
						} else if (message.equals(HEADER_ERROR_VALUE_UNVERIFIED)) {
							l.onUserLogIn(false, null, CODE_ERROR_UNVERIFIED_EMAIL, new Throwable(message));
							return;
						}
						logIn(username, password, l);
					} else if (response.code == 403) {
						message = HEADER_ERROR_USER_INACTIVE;
						l.onUserLogIn(false, null, CODE_ERROR_OTHER, new Throwable(message));
						return;
					}
					message = HEADER_ERROR_OTHER;
					l.onUserLogIn(false, null, CODE_ERROR_OTHER, new Throwable(message));
				}
			}
		};
		
		Log.i("javelin", "login: logging user in...");
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(JavelinClient.PARAM_USERNAME, username));
		params.add(new BasicNameValuePair(JavelinClient.PARAM_PASSWORD, password));
		
		JavelinComms.httpPost(
				JavelinUtils.buildFinalUrl(mConfig, JavelinClient.URL_LOGIN),
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + mConfig.getMasterToken(),
				params,
				callback);
	}
	
	public void logInWithGooglePlus(String accessToken, String refreshToken,
			final OnUserLogInListener l) {
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				Log.i("javelin", "g+ login"
						+ " code= "+ response.code
						+ " ok=" + response.successful
						+ " message=" + response.response.toString()
						+ " error=" + (!response.successful ? response.exception.getMessage() : new String()));
				if (response.successful) {
					User user = null;
					try {
						user = User.deserialize(response.jsonResponse.toString());
					} catch (Exception e) {
						l.onUserLogIn(false, null, CODE_ERROR_OTHER, new Throwable("Error reading incoming user data:" + e));
						return;
					}
					
					mUser = user;
					//user.setPassword(password);
					setCache();
					//setCachePassword(password);
					getApiTokenForLoggedUser(l);
				} else {
					clearCachePassword();
					removeApiToken();
					removeDeviceToken();

					String message = new String();
					if (response.code == 401) {
						for (Header header : response.headers) {
							if (header.getName().equals(HEADER_ERROR_NAME)) {
								message = header.getValue();
								break;
							}
						}
						
						if (message.equals(HEADER_ERROR_VALUE_CREDENTIALS)) {
							l.onUserLogIn(false, null, CODE_ERROR_WRONG_CREDENTIALS, new Throwable(message));
							return;
						} else if (message.equals(HEADER_ERROR_VALUE_UNVERIFIED)) {
							l.onUserLogIn(false, null, CODE_ERROR_UNVERIFIED_EMAIL, new Throwable(message));
							return;
						}
					} else if (response.code == 403) {
						message = HEADER_ERROR_USER_INACTIVE;
						l.onUserLogIn(false, null, CODE_ERROR_OTHER, new Throwable(message));
						return;
					}
					message = HEADER_ERROR_OTHER;
					l.onUserLogIn(false, null, CODE_ERROR_OTHER, new Throwable(message));
				}
			}
		};
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(JavelinClient.PARAM_ACCESS_TOKEN, accessToken));
		params.add(new BasicNameValuePair(JavelinClient.PARAM_REFRESH_TOKEN, refreshToken));
		
		Log.i("javelin", "url=" + JavelinUtils.buildFinalUrl(mConfig,
				JavelinClient.URL_LOGIN_SOCIAL_GOOGLE_PLUS));
		Log.i("javelin", "master token=" + mConfig.getMasterToken());
		Log.i("javelin", "g+ params=" + params.toString());
		
		JavelinComms.httpPost(
				JavelinUtils.buildFinalUrl(mConfig,
				JavelinClient.URL_LOGIN_SOCIAL_GOOGLE_PLUS),
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + mConfig.getMasterToken(),
				params,
				callback);
	}
	
	public void updateRequiredInformation(final OnUserRequiredInformationUpdateListener l) {
		if (!isPresent()) {
			l.onUserRequiredInformationUpdate(false, new Throwable("Error updating required information in the back-end. No user is present."));
		}
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					l.onUserRequiredInformationUpdate(response.successful, null);
				} else {
					l.onUserRequiredInformationUpdate(response.successful,
								new Exception("Error updating required information in the back-end."
										+ response.exception.getMessage()));
				}
			}
		};
		
		User user = getUser();
		String url = user.url + JavelinClient.URL_UPDATE_SUFFIX;
		
		//this request specifically needs the numerical id of the agency instead of the url
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(JavelinClient.PARAM_CODE, user.getDisarmCode()));
		
		if (user.belongsToAgency()) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_AGENCY, Integer.toString(user.agency.id)));
		}
		
		if (user.phoneNumber != null && user.phoneNumber.length() > 0) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_PHONE, user.phoneNumber));
		}
		
		if (user.firstName != null && user.firstName.length() > 0) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_FIRST_NAME, user.firstName));
		}
		
		if (user.lastName != null && user.lastName.length() > 0) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_LAST_NAME, user.lastName));
		}
		
		JavelinComms.httpPost(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + getApiToken(),
				params,
				callback);
	}
	
	
	public void setUserProfile(UserProfile userProfile) {
		if (!isPresent()) {
			return;
		}
		
		getUser().profile = userProfile;
		setCache();
	}
	
	
	public void uploadUserProfile() {
		//upload user information (text-only)
		// if successful, upload user picture

		final UserProfile profile = getUser().profile;
		
		final OnUserPictureUploadListener picUploadListener = new OnUserPictureUploadListener() {
			
			@Override
			public void onUserPictureUpload(boolean successful, final String uploadUrl, Throwable e) {
				if (successful) {
					notifyBackendOfPictureUpload(uploadUrl);
					Log.d("javelin", "User picture uploaded at " + uploadUrl);
				} else {
					Log.e("javelin", "Error uploading picture", e);
				}
			}
		};
		
		OnUserProfileUploadListener infoUploadListener = new OnUserProfileUploadListener() {

			@Override
			public void onUserProfileUpload(boolean successful, UserProfile userProfile, Throwable e) {
				if (successful) {
					uploadUserPicture(picUploadListener, profile);
				} else {
					Log.e("javelin", "Error uploading user info", e);
				}
			}
		};
		
		uploadUserInformation(infoUploadListener);
	}
	
	public void uploadUserInformation(final OnUserProfileUploadListener l) {
		final User user = getUser();
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					try {
						String url = response.jsonResponse.getString("url");
						getUser().profile.setUrl(url);
					} catch (Exception e) {
						Log.w("javelin", "Profile url not parsed.");
					}
					Log.d("javelin", "Profile uploaded.");
					l.onUserProfileUpload(true, user.profile, null);
				} else {
					Log.e("javelin", "Error uploading profile response=" + response.response,
							response.exception);
					l.onUserProfileUpload(false, user.profile,
							new Throwable("Error uploading profile."
									+ response.exception.getMessage()));
				}
			}
		};
		
		String url = JavelinUtils.buildFinalUrl(mConfig, JavelinClient.URL_PROFILES);
		
		List<NameValuePair> params = user.profile.getAvailableRequestParams();
		params.add(new BasicNameValuePair(JavelinClient.PARAM_PROFILE_USER, user.url));
		
		JavelinComms.httpPost(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + getApiToken(),
				params,
				callback);
	}
	
	private void uploadUserPicture(final OnUserPictureUploadListener l, final UserProfile p) {
		if (!UserProfile.hasPicture(mContext)) {
			Log.d("javelin", "No picture to upload");
			l.onUserPictureUpload(true, null, null);
			return;
		}
		
		File f = UserProfile.getPictureFile(mContext);
		
		new AsyncTask<File, String, Void>() {
			
			@Override
			protected Void doInBackground(File... files) {
				File f = files[0];
				
				String bucketName = mConfig.getAwsS3Bucket();
				
				final String key = UUID.randomUUID().toString() + ".jpg";
				final String url = "http://" + bucketName + ".s3.amazonaws.com/" + key;
				final AmazonS3Client s3Client = new AmazonS3Client(
						new BasicAWSCredentials(
								mConfig.getAwsS3AccessKey(), mConfig.getAwsS3SecretKey()));

				try {
					PutObjectRequest request = new PutObjectRequest(bucketName, key, f);
					request.setCannedAcl(CannedAccessControlList.PublicRead);
					s3Client.putObject(request);
				} catch (Exception e) {
					l.onUserPictureUpload(false, null, e);
					return null;
				}
				l.onUserPictureUpload(true, url, null);
				return null;
			}
		}.execute(f);
	}
	
	private void notifyBackendOfPictureUpload(String pictureUploadUrl) {
		Log.i("javelin", "pictureUrl=" + pictureUploadUrl);
		getUser().profile.setPictureUrl(pictureUploadUrl);

		final OnUserProfileUploadListener l = new OnUserProfileUploadListener() {
			@Override
			public void onUserProfileUpload(boolean successful, UserProfile userProfile, Throwable e) {
				if (successful) {
					Log.d("javelin", "Backend notified of picture upload (url)");
				} else {
					Log.e("javelin", "Error notifying backend of picture upload", e);
				}
			}
		};

		if (!getUser().profile.hasUrl()) {
			return;
		}

		JavelinCommsCallback callback = new JavelinCommsCallback() {

			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					l.onUserProfileUpload(response.successful, getUser().profile, null);
				} else {
					l.onUserProfileUpload(response.successful, null, response.exception);
				}
			}
		};

		String url = getUser().profile.getUrl();

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("profile_image_url", getUser().profile.getPictureUrl()));

		Log.d("javelin", "notifyBackend");
		Log.d("javelin", "  url=" + url);
		Log.d("javelin", "  " + params.get(0).getName() + "=" + params.get(0).getValue());

		JavelinComms.httpPatch(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + getApiToken(),
				params,
				callback);

		//uploadUserInformation(l);
	}
	
	private void getApiTokenForLoggedUser(final OnUserLogInListener l) {
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					String token = null;
					try {
						token = response.jsonResponse.getString(JavelinClient.PARAM_TOKEN);
					} catch (Exception e) {
						l.onUserLogIn(false, getUser(), CODE_ERROR_OTHER, new Throwable("Error parsing incoming data" + e));
						return;
					}
					
					Log.i("javelin", "apitoken: retrieved=" + token);
					Log.i("javelin", "apitoken: setting apitoken and enabling gcm...");
					setApiToken(token);
					enableGCM();
					l.onUserLogIn(true, getUser(), CODE_OK, null);
				} else {
					Log.i("javelin", "login: error getting api token=" + response.response
							+ "(" + response.exception.getMessage() + ")");
					clearCachePassword();
					removeApiToken();
					removeDeviceToken();
					l.onUserLogIn(false, getUser(), CODE_ERROR_OTHER,
							new Throwable("Error getting api token:" + response.exception.getMessage()));
				}
			}
		};
		
		Log.i("javelin", "apitoken: retrieving...");
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(JavelinClient.PARAM_USERNAME, getUser().email));
		if (hasCachePassword()) {
			params.add(new BasicNameValuePair(JavelinClient.PARAM_PASSWORD, getCachePassword()));
		}
		
		JavelinComms.httpPost(
				JavelinUtils.buildFinalUrl(mConfig, JavelinClient.URL_TOKEN_RETRIEVAL),
				null,
				null,
				params,
				callback);
		
	}
	
	public void getTwilioTokenForLoggedUser(final OnTwilioTokenFetchListener l) {
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					String token = null;
					try {
						token = response.jsonResponse.getString(JavelinClient.PARAM_TOKEN);
					} catch (Exception e) {
						l.onTwilioTokenFetch(false, null, e);
					}
					
					if (token == null || token.trim().length() <= 0) {
						l.onTwilioTokenFetch(false, null, new Throwable("Twilio capability token retrieved is empty. Response="
								+ response.response.toString()));
						return;
					}
					l.onTwilioTokenFetch(true, token, null);
				} else {
					l.onTwilioTokenFetch(false, null, response.exception);
				}
			}
		};
		
		JavelinComms.httpGet(
				JavelinUtils.buildFinalUrl(mConfig, JavelinClient.URL_TWILIO_TOKEN_RETRIEVAL),
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + getApiToken(),
				null,
				callback);
	}
	
	public void resendPhoneNumberVerificationSms() {
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				String result = response.successful ? "Success" : "Failure";
				Log.i("javelin", result + " requesting sms response="
						+ response.response.toString());
			}
		};
		
		//get phone number directly since the user needs to be logged in
		String url = getUser().url + JavelinClient.URL_VERIFICATION_CODE_REQUEST_SUFFIX;
		
		String headerVal = JavelinClient.HEADER_VALUE_TOKEN_PREFIX + getApiToken();
		String phoneNumber = getUser().phoneNumber;
		
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair(JavelinClient.PARAM_PHONE, phoneNumber));
		
		JavelinComms.httpPost(
				url,
				JavelinClient.HEADER_AUTH,
				headerVal,
				params,
				callback);
	}

	public void verifyPhoneNumberWithCode(String code, final OnPhoneNumberVerificationSmsCodeVerifiedListener l) {
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				String reason = new String();
				try {
					reason = response.jsonResponse.getString("message");
				} catch (Exception e) {
					reason = "Unknown";
				}
				
				l.onNewPhoneNumberVerificationSmsCodeVerified(response.successful, reason);
				
				if (response.successful) {
					User user = getUser();
					user.setPhoneNumberVerified();
					setUser(user);
				}
				
				Log.i("javelin", "Phone verification success=" + response.successful
						+ " response=" + response.jsonResponse.toString());
			}
		};
		
		String url = getUser().url + JavelinClient.URL_VERIFICATION_CODE_CHECK_SUFFIX;
		
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair(JavelinClient.PARAM_SMS_CODE, code));
		
		JavelinComms.httpPost(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + getApiToken(),
				params,
				callback);
	}
	
	public void refreshCurrentAgency() {
		if (!isPresent() || !getUser().belongsToAgency()) {
			return;
		}

		final OnAgencyFetchListener l = new OnAgencyFetchListener() {
			
			@Override
			public void onAgencyFetch(Boolean successful, Agency agency, Throwable e) {
				if (successful) {
					User user = getUser();
					
					//if different it means while refreshing, the agency changed
					if (user.agency.id != agency.id) {
						Log.w("javelin", "Different agency, aborting refreshing...");
						return;
					}
					
					user.agency = agency;
					setUser(user);
					
					//notify broadcast listeners
					Intent broadcast = new Intent(ACTION_AGENCY_REFRESHED);
					mContext.sendBroadcast(broadcast);
					
					updateAgencyLogos();
				} else {
					Log.e("javelin", "Error refreshing current agency", e);
				}
			}
		};
		
		String url = getUser().agency.url;
		getAgencyWithUrl(url, l);
	}
	
	private void updateAgencyLogos() {
		String logoUrl = getUser().agency.logoAlternate;
		
		if (logoUrl == null || logoUrl.isEmpty()) {
			return;
		}
		
		String logoName = FilenameUtils.getName(logoUrl);
		
		File directory = getLogosDirectory();
		File logo = new File(directory, logoName);
		
		if (logo.exists()) {
			return;
		}
		
		if (!directory.exists()) {
			directory.mkdir();
		}
		
		//delete all other files to avoid collecting older files
		for (File f : directory.listFiles()) {
			f.delete();
		}
		
		new JavelinUtils.AsyncImageDownloaderToFile(mContext, logoUrl, logo,
				ACTION_AGENCY_LOGOS_UPDATED, false).execute();
	}
	
	private File getLogosDirectory() {
		return new File(mContext.getExternalFilesDir(null).getAbsoluteFile() + "/logos");
	}
	
	public boolean hasAlternateLogo() {
		File dir = getLogosDirectory();
		
		if (!dir.exists()) {
			return false;
		}
		
		return dir.list().length > 0;
	}
	
	public Bitmap getAlternateLogo() {
		if (!hasAlternateLogo()) {
			return null;
		}
		
		File dir = getLogosDirectory();
		File file = dir.listFiles()[0];
		return BitmapFactory.decodeFile(file.getAbsolutePath());
	}
	
	public void getAgencyWithId(final int id, final OnAgencyFetchListener l) {
		String url = JavelinClient.URL_AGENCIES + id + "/";
		getAgencyWithUrl(url, l);
	}

	
	public void getAgencyWithUrl(final String url, final OnAgencyFetchListener l) {
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (response.successful) {
					Agency agency = null;

					try {
						agency = Agency.deserializeFromJson(response.jsonResponse);
					} catch (Exception e) {
						JavelinCommsRequestResponse errorResponse = new JavelinCommsRequestResponse();
						errorResponse.successful = false;
						errorResponse.exception = new Exception("Error parsing agency data="
								+ response.exception.getMessage());
						onEnd(errorResponse);
						return;
					}

					l.onAgencyFetch(true, agency, null);
				} else {
					l.onAgencyFetch(false, null, new Throwable("Error getting agency information="
							+ response.exception.getMessage()));
				}
			}
		};
		
		String finalUrl = JavelinUtils.buildFinalUrl(mConfig, url);
		
		Log.i("javelin", "refresh current agency at: " + finalUrl);
		
		JavelinComms.httpGet(
				finalUrl,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + getApiToken(),
				null,
				callback);
	}
	
	public void enableGCM() {
		boolean loggedIn = isPresent();
		
		if (!loggedIn) {
			return;
		}
		
		Log.i("javelin", "user present, enabling gcm...");
		
		new AsyncTask<Void, Void, String>() {
			
			@Override
			protected String doInBackground(Void... args) {
				GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(mContext);
				String token = null;
				
				int attempts = 1;
				
				do {
					attempts++;
					Log.i("javelin", "gcm: reg attempt " + attempts);
					try {
						token = gcm.register(mConfig.getGcmSenderId());
						break;
					} catch (IOException e) {
						Log.e("javelin", "gcm: reg error:", e);
					}
				} while (attempts <= 3);
				
				if (token == null || token.length() == 0) {
					return null;
				}
				
				if (hasDeviceToken() && token.equals(getDeviceToken())) {
					Log.i("javelin", "gcm: same token=stop");
					return null;
				}

				Log.i("javelin", "gcm: different token");
				setDeviceToken(token);
				Log.i("javelin", "gcm: stored device token=" + token);
				return token;
			}
			
			protected void onPostExecute(String token) {
				if (token == null) {
					return;
				}
				
				JavelinCommsCallback callback = new JavelinCommsCallback() {
					
					@Override
					public void onEnd(JavelinCommsRequestResponse response) {
						if (response.successful) {
							Log.i("javelin", "gcm: javelin notified response=" + response);
						} else {
							Log.e("javelin", "gcm: error notifying javelin error="
									+ response.exception.getMessage()
											+ " response=" + response.response);
						}
					}
				};
				
				User user = getUser();
				
				Log.i("javelin", "gcm: user (" + user.id + ") obtained:" + user.toString());
				
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair(JavelinClient.PARAM_TOKEN_DEVICE, token));
				params.add(new BasicNameValuePair(JavelinClient.PARAM_DEVICE, JavelinClient.DEVICE_ANDROID));
				
				JavelinComms.httpPost(
						JavelinUtils.buildFinalDeviceTokenUrl(user),
						JavelinClient.HEADER_AUTH,
						JavelinClient.HEADER_VALUE_TOKEN_PREFIX + getApiToken(),
						params,
						callback);
			};
			
		}.execute();
	}
	
	public void logOut(OnUserLogOutListener l) {
		
		if (mStatus != Status.NO_USER) {
			mStatus = Status.NO_USER;
			mUser = null;
			clearCachePassword();
			setCache();
			removeApiToken();
			removeDeviceToken();
		}
		l.onUserLogOut(true, null);
	}
	
	public static String syncGetActiveAlertForLoggedUser(final Context context) {
		JavelinUserManager userManager = JavelinUserManager.getInstance(context, mConfig);
		boolean userIsPresent = userManager.isPresent();
		if (!userIsPresent) {
			return null;
		}
		
		String alertId = null;
		String response = null;
		
		//check for status 'new'
		response = syncGetAlertForLoggedUserWithStatus(context, "N");
		alertId = getUrlOffListOfAlerts(response);
		
		if (alertId != null) {
			return alertId;
		}
		
		//check now for status 'available'
		response = syncGetAlertForLoggedUserWithStatus(context, "A");
		alertId = getUrlOffListOfAlerts(response);
		
		if (alertId != null) {
			return alertId;
		}
		
		//check now for last possible status, 'pending'
		response = syncGetAlertForLoggedUserWithStatus(context, "P");
		alertId = getUrlOffListOfAlerts(response);

		return alertId;
	}
	
	private static String getUrlOffListOfAlerts(String response) {
		String alertId = null;
		if (response != null) {
			try {
				JSONObject o = new JSONObject(response);
				int count = o.getInt("count");
				if (count > 0) {
					JSONArray a = o.getJSONArray("results");
					JSONObject alert = a.getJSONObject(0);
					if (alert.has("url")) {
						alertId = alert.getString("url");
					}
				}
			} catch (Exception e) {
				Log.e("javelin", "Error getting URL off results", e);
				alertId = null;
			}
		}
		return alertId;
	}
	
	private static String syncGetAlertForLoggedUserWithStatus(Context context, String alertStatus) {
		JavelinUserManager userManager = JavelinUserManager.getInstance(context, mConfig);
		
		HttpsURLConnection connection = null;
		String params = "agency_user=" + Integer.toString(userManager.getUser().id) + "&status=" + alertStatus;
		String response;
		try {
			URL url = new URL(JavelinUtils.buildFinalUrl(mConfig, JavelinClient.URL_ALERTS) + "?" + params);
			Log.i("javelin", "getactivealert url=" + url.toString());
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.addRequestProperty(JavelinClient.HEADER_AUTH,
					JavelinClient.HEADER_VALUE_TOKEN_PREFIX + userManager.getApiToken());
			connection.setDoInput(true);
			int code = connection.getResponseCode();
			if (code == 200) {
				response = IOUtils.toString(connection.getInputStream());
			} else {
				Log.e("Javelin", "JavelinUserManager error: Http response code" + code);
				response = null;
			}
		} catch (Exception e) {
			Log.e("Javelin", "JavelinUserManager error retrieving active alert for logged user (sync)", e);
			response = null;
		}
		
		if (connection != null) {
			connection.disconnect();
		}
		return response;
	}
	
	public static interface OnUserLogInListener {
		void onUserLogIn(boolean successful, User user, int errorCode, Throwable e);
	}
	
	public static interface OnUserRequiredInformationUpdateListener {
		void onUserRequiredInformationUpdate(boolean successful, Throwable e);
	}
	
	public static interface OnUserSignUpListener {
		void onUserSignUp(boolean successful, Throwable e);
	}
	
	public static interface OnUserLogOutListener {
		void onUserLogOut(boolean successful, Throwable e);
	}
	
	public static interface OnAgencyFetchListener {
		void onAgencyFetch(Boolean successful, Agency agency, Throwable e);
	}
	
	public static interface OnUserProfileUploadListener {
		void onUserProfileUpload(boolean successful, UserProfile userProfile, Throwable e);
	}
	
	public static interface OnUserPictureUploadListener {
		void onUserPictureUpload(boolean successful, String uploadUrl, Throwable e);
	}
	
	public static interface OnTwilioTokenFetchListener {
		void onTwilioTokenFetch(boolean successful, String token, Throwable e);
	}
	
	public static interface OnPhoneNumberVerificationSmsCodeVerifiedListener {
		void onNewPhoneNumberVerificationSmsCodeVerified(boolean success, String reason);
	}
}
