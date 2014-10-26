package com.tapshield.android.api.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

import com.google.gson.Gson;
import com.tapshield.android.api.JavelinUtils;

public class Agency {

	private static String KEY_ID = "id";
	private static String KEY_DISTANCE = "distance";
	private static String KEY_URL = "url";
	private static String KEY_NAME = "name";
	private static String KEY_DOMAIN = "domain";
	private static String KEY_NUMBER_PRIMARY = "dispatcher_phone_number";
	private static String KEY_NUMBER_SECONDARY = "dispatcher_secondary_phone_number";
	private static String KEY_SCHEDULE_START = "dispatcher_schedule_start";
	private static String KEY_SCHEDULE_END = "dispatcher_schedule_end";
	private static String KEY_CENTER_LATITUDE = "agency_center_latitude";
	private static String KEY_CENTER_LONGITUDE = "agency_center_longitude";
	private static String KEY_BOUNDARIES = "agency_boundaries";
	private static String KEY_DISPATCH_CENTERS = "dispatch_center";
	private static String KEY_REGIONS = "region";
	private static String KEY_COMPLETE_MESSAGE = "alert_completed_message";
	private static String KEY_DISPLAY_COMMANDALERT = "display_command_alert";
	private static String KEY_REQUIRED_DOMAIN_EMAIL = "require_domain_emails";
	private static String KEY_SHOW_AGENCY_NAME = "show_agency_name_in_app_navbar";
	private static String KEY_INFO_URL = "agency_info_url";
	private static String KEY_RSS_URL = "agency_rss_url";
	private static String KEY_ALERT_MODE_NAME = "alert_mode_name";
	private static String KEY_LOGO = "agency_logo";
	private static String KEY_LOGO_ALTERNATE = "agency_alternate_logo";
	private static String KEY_LOGO_SMALL = "agency_small_logo";
	private static String KEY_THEME = "agency_theme";
	private static String KEY_FEATURE_EMERGENCY_CALL = "emergency_call_available";
	private static String KEY_FEATURE_ALERT = "alert_available";
	private static String KEY_FEATURE_CHAT = "chat_available";
	private static String KEY_FEATURE_YANK = "yank_available";
	private static String KEY_FEATURE_ENTOURAGE = "entourage_available";
	private static String KEY_FEATURE_MASS = "mass_alert_available";
	
	private static String KEY_LIST = "results";
	
	public int
			id;
	public String
			url,
			name,
			domain,
			primaryNumber,
			secondaryNumber,
			scheduleStart,
			scheduleEnd,
			completeMessage,
			infoUrl,
			rssUrl,
			alertModeName,
			logo,
			logoAlternate,
			logoSmall,
			themeJsonString;
	public boolean
			displayCommandAlert,
			requiredDomainEmails,
			showAgencyName = false,
			featureEmergencyCall,
			featureAlert,
			featureChat,
			featureYank,
			featureEntourage,
			featureMass;
	public float
			distance = Float.MAX_VALUE;
	public double
			centerLatitude,
			centerLongitude;
	private List<String>
			boundaries;
	public List<DispatchCenter>
			dispatchCenters;
	public List<Region>
			regions;
	
	public Agency() {}
	
	public boolean hasSchedule() {
		return scheduleStart != null && scheduleEnd != null;
	}
	
	public boolean hasBoundaries() {
		return boundaries != null && !boundaries.isEmpty();
	}
	
	public List<Location> getBoundaries() {
		List<Location> b = new ArrayList<Location>();
		try {
			for (String coordinates : boundaries) {
				Location l = new Location("");
				String[] degrees = coordinates.split(",");
				String lat = degrees[0];
				String lon = degrees[1];
				l.setLatitude(Double.parseDouble(lat));
				l.setLongitude(Double.parseDouble(lon));
				b.add(l);
			}
		} catch (Exception e) {
			Log.e("javelin", "Error retrieving boundaries from Agency object:", e);
			b.clear();
		}
		
		return b;
	}
	
	public boolean hasRegions() {
		return regions != null && !regions.isEmpty();
	}
	
	public static List<Location> getBoundariesOfRegion(Region r) {
		List<Location> b = new ArrayList<Location>();
		
		if (r == null || r.mBoundaries == null || r.mBoundaries.isEmpty()) {
			return b;
		}
		
		try {
			JSONArray points = new JSONArray(r.mBoundaries);
			
			for (int i = 0; i < points.length(); i++) {
				String point = points.getString(i);
				String[] degrees = point.split(",");
				String lat = degrees[0];
				String lon = degrees[1];
				
				Location l = new Location("");
				l.setLatitude(Double.parseDouble(lat));
				l.setLongitude(Double.parseDouble(lon));
				
				b.add(l);
			}
		} catch (Exception e) {
			Log.e("javelin", "Error retrieving boundaries of static region:", e);
			b.clear();
		}
		
		return b;
	}
	
	public static final String serializeToString(Agency a) {
		return serializeToJson(a).toString();
	}
	
	public static final JSONObject serializeToJson(Agency a) {
		if (a == null) {
			return null;
		}
		
		JSONObject o = new JSONObject();
		try {
			o.put(KEY_URL, a.url);
			o.put(KEY_DISTANCE, a.distance);
			o.put(KEY_NAME, a.name);
			o.put(KEY_DOMAIN, a.domain);
			o.put(KEY_NUMBER_PRIMARY, a.primaryNumber);
			o.put(KEY_NUMBER_SECONDARY, a.secondaryNumber);
			o.put(KEY_SCHEDULE_START, a.scheduleStart);
			o.put(KEY_SCHEDULE_END, a.scheduleEnd);
			o.put(KEY_COMPLETE_MESSAGE, a.completeMessage);
			o.put(KEY_DISPLAY_COMMANDALERT, a.displayCommandAlert);
			o.put(KEY_REQUIRED_DOMAIN_EMAIL, a.requiredDomainEmails);
			o.put(KEY_SHOW_AGENCY_NAME, a.showAgencyName);
			o.put(KEY_INFO_URL, a.infoUrl);
			o.put(KEY_RSS_URL, a.rssUrl);
			o.put(KEY_ALERT_MODE_NAME, a.alertModeName);
			o.put(KEY_LOGO, a.logo);
			o.put(KEY_LOGO_ALTERNATE, a.logoAlternate);
			o.put(KEY_LOGO_SMALL, a.logoSmall);
			o.put(KEY_THEME, a.themeJsonString);
			o.put(KEY_CENTER_LATITUDE, a.centerLatitude);
			o.put(KEY_CENTER_LONGITUDE, a.centerLongitude);
			o.put(KEY_FEATURE_EMERGENCY_CALL, a.featureEmergencyCall);
			o.put(KEY_FEATURE_ALERT, a.featureAlert);
			o.put(KEY_FEATURE_CHAT, a.featureChat);
			o.put(KEY_FEATURE_YANK, a.featureYank);
			o.put(KEY_FEATURE_ENTOURAGE, a.featureEntourage);
			o.put(KEY_FEATURE_MASS, a.featureMass);
			
			if (a.boundaries != null) {
				JSONArray list = new JSONArray();
				for (String coordinates : a.boundaries) {
					list.put(coordinates);
				}
				
				if (list.length() > 0) {
					o.put(KEY_BOUNDARIES, list.toString());
					Log.d("javelin", "plugged boundaries=" + list.toString());
				}
			}
			
			//additions for multiple geofences and dispatch centers
			
			Gson gson = new Gson();
			
			if (a.dispatchCenters != null && !a.dispatchCenters.isEmpty()) {
				
				JSONArray centers = new JSONArray();
				
				for (DispatchCenter center : a.dispatchCenters) {
					centers.put(
							new JSONObject(
									gson.toJson(center)));
				}
				
				o.put(KEY_DISPATCH_CENTERS, centers);
			}
			
			if (a.regions != null && !a.regions.isEmpty()) {
				JSONArray regions = new JSONArray();
				
				for (Region region : a.regions) {
					regions.put(
							new JSONObject(
									gson.toJson(region)));
				}
				
				o.put(KEY_REGIONS, regions);
			}
			
		} catch (Exception e) {
			Log.e("javelin", "Error serializing agency to json", e);
			return null;
		}
		return o;
	}
	
	public static final Agency deserializeFromString(String s) {
		JSONObject o;
		try {
			o = new JSONObject(s);
		} catch (Exception e) {
			Log.e("javelin", "Error deserializing agency from string", e);
			return null;
		}
		return deserializeFromJson(o);
	}
	
	public static final Agency deserializeFromJson(JSONObject o) {
		Agency a = new Agency();
		try {

			if (o.has(KEY_DISTANCE)) {
				String distance = o.getString(KEY_DISTANCE);
				if (!distance.equals("null")) {
					//format distance to two decimal places
					DecimalFormat twoDForm = new DecimalFormat("#.##");
					a.distance = Float.valueOf(twoDForm.format(Float.parseFloat(distance)));
				}
			}
			
			a.url = o.getString(KEY_URL);
			a.id = JavelinUtils.extractLastIntOfString(a.url);
			a.name = o.getString(KEY_NAME);
			a.domain = o.getString(KEY_DOMAIN);
			a.primaryNumber = o.getString(KEY_NUMBER_PRIMARY);
			a.secondaryNumber = o.getString(KEY_NUMBER_SECONDARY);
			a.scheduleStart = o.getString(KEY_SCHEDULE_START);
			a.scheduleEnd = o.getString(KEY_SCHEDULE_END);
			a.completeMessage = o.getString(KEY_COMPLETE_MESSAGE);
			a.displayCommandAlert = o.getBoolean(KEY_DISPLAY_COMMANDALERT);
			a.requiredDomainEmails = o.getBoolean(KEY_REQUIRED_DOMAIN_EMAIL);
			a.showAgencyName = o.getBoolean(KEY_SHOW_AGENCY_NAME);
			a.themeJsonString = o.getString(KEY_THEME);
			a.logo = o.getString(KEY_LOGO);
			a.logoAlternate = o.getString(KEY_LOGO_ALTERNATE);
			a.logoSmall = o.getString(KEY_LOGO_SMALL);
			a.centerLatitude = o.getDouble(KEY_CENTER_LATITUDE);
			a.centerLongitude = o.getDouble(KEY_CENTER_LONGITUDE);
			a.featureEmergencyCall = o.getBoolean(KEY_FEATURE_EMERGENCY_CALL);
			a.featureAlert = o.getBoolean(KEY_FEATURE_ALERT);
			a.featureChat = o.getBoolean(KEY_FEATURE_CHAT);
			a.featureYank = o.getBoolean(KEY_FEATURE_YANK);
			a.featureEntourage = o.getBoolean(KEY_FEATURE_ENTOURAGE);
			a.featureMass = o.getBoolean(KEY_FEATURE_MASS);

			if (!o.isNull(KEY_ALERT_MODE_NAME)) {
				a.alertModeName = o.getString(KEY_ALERT_MODE_NAME);
			}
			
			if (!o.isNull(KEY_INFO_URL)) {
				
				final String infoUrl = o.getString(KEY_INFO_URL);
				if (!infoUrl.isEmpty()) {
					a.infoUrl = infoUrl;
				}
			}
			
			if (!o.isNull(KEY_RSS_URL)) {
				
				final String rssUrl = o.getString(KEY_RSS_URL);
				if (!rssUrl.isEmpty()) {
					a.rssUrl = rssUrl;
				}
			}
			
			List<String> boundaries = new ArrayList<String>();
			
			String boundariesString = null;
			
			if (o.has(KEY_BOUNDARIES)) {
				boundariesString = o.getString(KEY_BOUNDARIES);
			}
			
			if (boundariesString != null && boundariesString.trim().length() > 0) {
				JSONArray boundariesJson = new JSONArray(boundariesString);
				for(int k = 0; k < boundariesJson.length(); k++) {
					boundaries.add(boundariesJson.getString(k));
				}
				
				if (!boundaries.isEmpty()) {
					a.boundaries = boundaries;
				}
			} else {
				a.boundaries = boundaries;
			}
			
			//additions for multiple geofences and dispatch centers
			
			Gson gson = new Gson();
			
			if (o.has(KEY_DISPATCH_CENTERS) && !o.isNull(KEY_DISPATCH_CENTERS)) {
				JSONArray centers = o.getJSONArray(KEY_DISPATCH_CENTERS);
				
				if (centers.length() > 0) {
					a.dispatchCenters = new ArrayList<DispatchCenter>();
					
					for (int i = 0; i < centers.length(); i++) {
						JSONObject center = centers.getJSONObject(i);
						a.dispatchCenters.add(
								gson.fromJson(center.toString(), DispatchCenter.class));
					}
				}
			}
			
			if (o.has(KEY_REGIONS) && !o.isNull(KEY_REGIONS)) {
				JSONArray regions = o.getJSONArray(KEY_REGIONS);
				
				if (regions.length() > 0) {
					a.regions = new ArrayList<Region>();
					
					for (int i = 0; i < regions.length(); i++) {
						JSONObject region = regions.getJSONObject(i);
						a.regions.add(
								gson.fromJson(region.toString(), Region.class));
					}
				}
			}
			
		} catch (Exception e) {
			Log.e("javelin", "Error deserializing from json:", e);
			return null;
		}
		return a;
	}
	
	public static List<Agency> jsonToList(JSONObject o) {
		List<Agency> agencies = new ArrayList<Agency>();
		
		try {
			JSONArray list = o.getJSONArray(KEY_LIST);
			for (int i = 0; i < list.length(); i++) {
				JSONObject ja = list.getJSONObject(i);
				Agency a = deserializeFromJson(ja);
				if (a != null) {
					agencies.add(a);
				}
			}
		} catch (Exception e) {
			Log.e("javelin", "Error parsing list of agencies", e);
			return null;
		}
		return agencies;
	}
}
