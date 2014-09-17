
package com.tapshield.android.api.model;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.annotations.SerializedName;
import com.tapshield.android.api.JavelinSocialReportingManager;

public class SocialCrime {

	@SerializedName("url")
	private String mUrl;
	
	@SerializedName("distance")
	private float mDistance;
	
	@SerializedName("creation_date")
	private String mDate;
	
	@SerializedName("body")
	private String mBody;
	
	@SerializedName("report_type")
	private String mTypeCode;
	
	@SerializedName("reporter")
	private String mReporter;
	
	@SerializedName("report_latitude")
	private double mLatitude;
	
	@SerializedName("report_longitude")
	private double mLongitude;

	@SerializedName("report_audio_url")
	private String mReportAudio;
	
	@SerializedName("report_image_url")
	private String mReportImage;
	
	@SerializedName("report_video_url")
	private String mReportVideo;
	
	@SerializedName("viewed_by")
	private String mViewedBy;
	
	@SerializedName("viewed_time")
	private String mViewedTime;
	
	@SerializedName("dispatcher_name")
	private String mViewedByName;
	
	public String getUrl() {
		return mUrl;
	}
	
	public float getDistance() {
		return mDistance;
	}
	
	public DateTime getDate() {
		 return ISODateTimeFormat.dateTime().parseDateTime(mDate);
	}
	
	public String getTypeCode() {
		return mTypeCode;
	}
	
	public String getTypeName() {
		return JavelinSocialReportingManager.getTypeByCode(getTypeCode());
	}
	
	public String getBody() {
		return mBody;
	}
	
	public String getReporter() {
		return mReporter;
	}
	
	public double getLatitude() {
		return mLatitude;
	}
	
	public double getLongitude() {
		return mLongitude;
	}
	
	public String getReportAudio() {
		return mReportAudio;
	}
	
	public String getReportImage() {
		return mReportImage;
	}
	
	public String getReportVideo() {
		return mReportVideo;
	}
	
	public boolean isViewed() {
		return mViewedTime != null;
	}
	
	public String getViewedBy() {
		return mViewedBy;
	}
	
	public String getViewedByName() {
		return mViewedByName;
	}
	
	public class SocialCrimes {
		
		@SerializedName("results")
		private List<SocialCrime> mCrimes;
		
		public List<SocialCrime> getSocialCrimes() {
			return mCrimes;
		}
	}
}
