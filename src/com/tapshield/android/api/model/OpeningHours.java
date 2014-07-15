package com.tapshield.android.api.model;

import com.google.gson.annotations.SerializedName;

public class OpeningHours {

	@SerializedName("url")
	public String mUrl;
	
	@SerializedName("dispatch_center")
	public String mDispatchCenterUrl;
	
	//1 = sunday, ..., 7 = saturday
	@SerializedName("day")
	public String mDay;
	
	//time elements in 24-hr format (hh:mm:ss)
	
	@SerializedName("open")
	public String mOpen;
	
	@SerializedName("close")
	public String mClose;
}
