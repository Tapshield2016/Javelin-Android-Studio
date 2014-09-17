package com.tapshield.android.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.model.SocialCrime;
import com.tapshield.android.api.model.SocialCrime.SocialCrimes;

public class JavelinSocialReportingManager {

	public static final String KEY_PUSHMESSAGE_TITLE = "title";
	
	//commented out unsupported types
	public static final String[] TYPE_LIST = new String[] {
			"Abuse",
			"Assault",
			"Car Accident",
			"Disturbance",
			"Drugs",
			"Harassment",
			"Mental Health",
			"Other",
			//"Repair Needed",
			"Suggestion",
			"Suspicious Activity",
			"Theft",
			"Vandalism"};
	
	private static final String[] TYPE_CODES = new String[] {
			"AB",
			"AS",
			"CA",
			"DI",
			"DR",
			"H",
			"MH",
			"O",
			//"RN",
			"S",
			"SA",
			"T",
			"V"};
	
	private static final String PARAM_REPORTER = "reporter";
	private static final String PARAM_BODY = "body";
	private static final String PARAM_TYPE = "report_type";
	private static final String PARAM_LATITUDE = "report_latitude";
	private static final String PARAM_LONGITUDE = "report_longitude";
	private static final String PARAM_ANONYMOUSLY = "report_anonymous";
	private static final String PARAM_MEDIA_AUDIO = "report_audio_url";
	private static final String PARAM_MEDIA_IMAGE = "report_image_url";
	private static final String PARAM_MEDIA_VIDEO = "report_video_url";
	private static final String PARAM_GET_LATITUDE = "latitude";
	private static final String PARAM_GET_LONGITUDE = "longitude";
	private static final String PARAM_GET_DISTANCE = "distance_within";
	
	private static JavelinSocialReportingManager mInstance;
	private Context mContext;
	private JavelinConfig mConfig;
	private List<SocialReportingMessageListener> mMessageListeners;
	
	static JavelinSocialReportingManager getInstance(Context context, JavelinConfig config) {
		if (mInstance == null) {
			mInstance = new JavelinSocialReportingManager(context, config);
		}
		return mInstance;
	}
	
	private JavelinSocialReportingManager(Context context, JavelinConfig config) {
		mContext = context.getApplicationContext();
		mConfig = config;
		mMessageListeners = new ArrayList<SocialReportingMessageListener>();
	}
	
	public void report(final String body, final String reportTypeName,
			final String latitude, final String longitude, boolean anonymous,
			final SocialReportingListener l, String mediaUrlAudio, String mediaUrlImage,
			String mediaUrlVideo) {
		
		final String typeCode = getCodeByType(reportTypeName);
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				l.onReport(response.successful, response.code, response.exception.getMessage());
			}
		};
		
		final JavelinUserManager userManager  =
				JavelinClient
				.getInstance(mContext, mConfig)
				.getUserManager();
		final String url = mConfig.getBaseUrl() + JavelinClient.URL_REPORT_SOCIAL;
		final String reporter = userManager.getUser().url;
		final String userToken = userManager.getApiToken();
		
		final String verifiedBody = (body == null || body.isEmpty()) ? " " : body;
		
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_REPORTER, reporter));
		params.add(new BasicNameValuePair(PARAM_BODY, verifiedBody));
		params.add(new BasicNameValuePair(PARAM_TYPE, typeCode));
		params.add(new BasicNameValuePair(PARAM_LATITUDE, latitude));
		params.add(new BasicNameValuePair(PARAM_LONGITUDE, longitude));
		params.add(new BasicNameValuePair(PARAM_ANONYMOUSLY, Boolean.toString(anonymous)));
		
		if (mediaUrlAudio != null) {
			params.add(new BasicNameValuePair(PARAM_MEDIA_AUDIO, mediaUrlAudio));
		}
		
		if (mediaUrlImage != null) {
			params.add(new BasicNameValuePair(PARAM_MEDIA_IMAGE, mediaUrlImage));
		}
		
		if (mediaUrlVideo != null) {
			params.add(new BasicNameValuePair(PARAM_MEDIA_VIDEO, mediaUrlVideo));
		}
		
		JavelinComms.httpPost(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + userToken,
				params,
				callback);
	}
	
	public void details(final SocialCrime socialCrime, final SocialReportingListener l) {
		details(socialCrime.getUrl(), l);
	}
	
	public void details(final String url, final SocialReportingListener l) {
		final JavelinUserManager userManager  =
				JavelinClient
				.getInstance(mContext, mConfig)
				.getUserManager();
		
		final String userToken = userManager.getApiToken();
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				
				if (l == null) {
					return;
				}
				
				SocialCrime socialCrime;
				
				try {
					socialCrime = new Gson().fromJson(response.response, SocialCrime.class);
				} catch (Exception e) {
					socialCrime = null;
				}
				
				l.onDetails(response.successful, response.code, socialCrime, 
						response.exception.getMessage());
			}
		};
		
		JavelinComms.httpGet(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + userToken,
				null,
				callback);
	}
	
	public void getReportsAt(final double latitude, final double longitude,
			final float radiusInMiles, final SocialReportingListener l) {
		
		final JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				
				if (l == null) {
					Log.e("aaa", "social reporting listener is null");
					return;
				}
				
				final boolean ok = response.successful;
				final int code = response.code;
				
				Log.i("aaa", "Social report response");
				Log.i("aaa", "   success=" + ok);
				Log.i("aaa", "   response=" + response.response);
				
				if (ok) {
					SocialCrimes crimes = null;
					try {
						crimes = new Gson().fromJson(response.response, SocialCrimes.class);
					} catch (Exception e) {
						Log.i("aaa", " successful BUT " + e.getMessage());
						l.onFetch(ok, code, null, "Successful BUT " + e.getMessage());
					}
					
					if (crimes != null) {
						l.onFetch(ok, code, crimes, null);
					}
				} else {
					l.onFetch(ok, code, null, response.exception.getMessage());
				}
			}
		};
		
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_GET_LATITUDE, Double.toString(latitude)));
		params.add(new BasicNameValuePair(PARAM_GET_LONGITUDE, Double.toString(longitude)));
		params.add(new BasicNameValuePair(PARAM_GET_DISTANCE, Float.toString(radiusInMiles)));
		
		final String url = mConfig.getBaseUrl() + JavelinClient.URL_REPORT_SOCIAL;
		final JavelinUserManager userManager  =
				JavelinClient
				.getInstance(mContext, mConfig)
				.getUserManager();
		
		final String userToken = userManager.getApiToken();
		
		JavelinComms.httpGet(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + userToken,
				params,
				callback);
	}
	
	public void deleteReport(final SocialCrime socialCrime, final SocialReportingListener l) {
		if (socialCrime == null) {
			return;
		}
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				if (l == null) {
					return;
				}
				
				l.onDelete(response.successful, response.code, socialCrime,
						response.exception.getMessage());
			}
		};
		
		final String userToken = JavelinClient
				.getInstance(mContext, mConfig)
				.getUserManager()
				.getApiToken();
		
		JavelinComms.httpDelete(
				socialCrime.getUrl(),
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + userToken,
				callback);
	}
	
	public static String getCodeByType(String type) {
		String code = TYPE_CODES[0];
		
		for (int i = 0; i < TYPE_LIST.length; i++) {
			String innerType = TYPE_LIST[i];
			
			if (innerType.toLowerCase(Locale.getDefault()).trim()
					.equals(type.toLowerCase(Locale.getDefault()).trim())) {
				code = TYPE_CODES[i];
				break;
			}
		}
		
		return code;
	}
	
	public static String getTypeByCode(String typeCode) {
		String type = TYPE_LIST[0];
		
		for (int i = 0; i < TYPE_CODES.length; i++) {
			String innerCode = TYPE_CODES[i];
			
			if (innerCode.toLowerCase(Locale.getDefault()).trim()
					.equals(typeCode.toLowerCase(Locale.getDefault()).trim())) {
				type = TYPE_LIST[i];
				break;
			}
		}
		
		return type;
	}
	
	public void addMessageListener(SocialReportingMessageListener l) {
		mMessageListeners.add(l);
	}
	
	public void removeMessageListener(SocialReportingMessageListener l) {
		mMessageListeners.remove(l);
	}
	
	public void notifyMessage(final String message, final String id, final Bundle extras) {
		for (SocialReportingMessageListener l : mMessageListeners) {
			if (l != null) {
				l.onMessageReceive(message, id, extras);
			}
		}
	}
	
	public interface SocialReportingListener {
		void onReport(boolean ok, int code, String errorIfNotOk);
		void onFetch(boolean ok, int code, SocialCrimes socialCrimes, String errorIfNotOk);
		void onDetails(boolean ok, int code, SocialCrime socialCrime, String errorIfNotOk);
		void onDelete(boolean ok, int code, SocialCrime socialCrime, String errorIfNotOk);
	}
	
	public interface SocialReportingMessageListener {
		void onMessageReceive(String message, String id, Bundle extras);
	}
}
