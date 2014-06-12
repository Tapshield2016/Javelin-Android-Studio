package com.tapshield.android.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;

import com.google.gson.Gson;
import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;
import com.tapshield.android.api.model.SocialCrime;
import com.tapshield.android.api.model.SocialCrime.SocialCrimes;

public class JavelinSocialReportingManager {

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
	
	static JavelinSocialReportingManager getInstance(Context context, JavelinConfig config) {
		if (mInstance == null) {
			mInstance = new JavelinSocialReportingManager(context, config);
		}
		return mInstance;
	}
	
	private JavelinSocialReportingManager(Context context, JavelinConfig config) {
		mContext = context.getApplicationContext();
		mConfig = config;
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
				
				l.onDetails(response.successful, response.code,
						new Gson().fromJson(response.response, SocialCrime.class),
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
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				
				l.onFetch(response.successful, response.code,
						new Gson().fromJson(response.response, SocialCrimes.class),
						response.exception.getMessage());
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
	
	public interface SocialReportingListener {
		void onReport(boolean ok, int code, String errorIfNotOk);
		void onFetch(boolean ok, int code, SocialCrimes socialCrimes, String errorIfNotOk);
		void onDetails(boolean ok, int code, SocialCrime socialCrime, String errorIfNotOk);
	}
}
