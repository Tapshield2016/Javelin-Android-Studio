package com.tapshield.android.api.model;

import com.google.gson.annotations.SerializedName;

public class Region {

	@SerializedName("url")
	public String mUrl;
	
	@SerializedName("name")
	public String mName;
	
	@SerializedName("primary_dispatch_center")
	public String mPrimaryDispatchCenterUrl;
	
	@SerializedName("secondary_dispatch_center")
	public String mSecondaryDispatchCenterUrl;
	
	@SerializedName("fallback_dispatch_center")
	public String mFallbackDispatchCenterUrl;
	
	@SerializedName("boundaries")
	public String mBoundaries;
	
	@SerializedName("center_latitude")
	public String mCenterLatitude;
	
	@SerializedName("center_longitude")
	public String mCenterLongitude;
	
	public boolean hasBoundaries() {
		return mBoundaries != null && !mBoundaries.isEmpty();
	}
	
	public boolean hasPrimaryDispatcher() {
		return mPrimaryDispatchCenterUrl != null;
	}
	
	public boolean hasSecondaryDispatcher() {
		return mSecondaryDispatchCenterUrl != null;
	}
	
	public boolean hasFallbackDispatcher() {
		return mFallbackDispatchCenterUrl != null;
	}
}
