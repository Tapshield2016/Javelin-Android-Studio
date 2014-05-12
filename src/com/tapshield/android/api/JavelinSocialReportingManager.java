package com.tapshield.android.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.util.Log;

import com.tapshield.android.api.JavelinComms.JavelinCommsCallback;
import com.tapshield.android.api.JavelinComms.JavelinCommsRequestResponse;

public class JavelinSocialReportingManager {

	//commented out unsupported types due to lack of icons
	public static final String[] TYPE_LIST = new String[] {
			//"Abuse",
			"Assault",
			"Car Accident",
			//"Disturbance",
			"Drugs",
			"Harrasment",
			"Mental Health",
			"Other",
			//"Repair Needed",
			//"Suggestion",
			"Suspicious Activity",
			"Theft",
			"Vandalism"};
	
	private static final String[] TYPE_CODES = new String[] {
			//"AB",
			"AS",
			"CA",
			//"DI",
			"DR",
			"H",
			"MH",
			"O",
			//"RN",
			//"S",
			"SA",
			"T",
			"V"};
	
	private static final String PARAM_REPORTER = "reporter";
	private static final String PARAM_BODY = "body";
	private static final String PARAM_TYPE = "report_type";
	private static final String PARAM_LATITUDE = "report_latitude";
	private static final String PARAM_LONGITUDE = "report_longitude";
	
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
			final String latitude, final String longitude, final SocialReportingListener l) {
		
		final String typeCode = getCodeByType(reportTypeName);
		
		JavelinCommsCallback callback = new JavelinCommsCallback() {
			
			@Override
			public void onEnd(JavelinCommsRequestResponse response) {
				Log.i("aaa", "report status=" + response.response);
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
		
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(PARAM_REPORTER, reporter));
		params.add(new BasicNameValuePair(PARAM_BODY, body));
		params.add(new BasicNameValuePair(PARAM_TYPE, typeCode));
		params.add(new BasicNameValuePair(PARAM_LATITUDE, latitude));
		params.add(new BasicNameValuePair(PARAM_LONGITUDE, longitude));
		
		JavelinComms.httpPost(
				url,
				JavelinClient.HEADER_AUTH,
				JavelinClient.HEADER_VALUE_TOKEN_PREFIX + userToken,
				params,
				callback);
	}
	
	private String getCodeByType(String type) {
		String code = TYPE_CODES[0];
		
		for (int i = 0; i < TYPE_LIST.length; i++) {
			String innerType = TYPE_LIST[i];
			
			if (innerType.toLowerCase().trim().equals(type.toLowerCase().trim())) {
				code = TYPE_CODES[i];
				break;
			}
		}
		
		return code;
	}
	
	public interface SocialReportingListener {
		void onReport(boolean ok, int code, String errorIfNotOk);
	}
}
