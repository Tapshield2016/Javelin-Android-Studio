package com.tapshield.android.api.model;

import com.google.gson.annotations.SerializedName;

public class ClosedDate {

	@SerializedName("url")
	public String mUrl;
	
	//utc-formatted dates
	
	@SerializedName("start_date")
	public String mStartDate;
	
	@SerializedName("end_date")
	public String mEndDate;
}
