package com.tapshield.android.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class MassAlert {

	private static final String KEY_URL = "url";
	private static final String KEY_AGENCY = "agency";
	private static final String KEY_MESSAGE = "message";
	private static final String KEY_TIMESTAMP = "creation_date";

	public String
			url,
			agency,
			message,
			timestamp;
	
	public MassAlert(String url, String timestamp, String agency, String message) {
		this.url = url;
		this.timestamp = timestamp;
		this.agency = agency;
		this.message = message;
	}
	
	public long getTimestampInSeconds() {
		Date d = utcStringToDate(this.timestamp);
		return (d.getTime()/1000);
	}
	private Date utcStringToDate(String utc) {
		try {
			DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			DateTime dt = dtf.withZoneUTC().parseDateTime(timestamp);
			return dt.withZone(DateTimeZone.UTC).toDate();
		} catch (Exception e) {
			Log.e("javelin",  "Error parsing string timestamp, setting current time");
			return null;
		}
	}
	
	//de/serializing of single
	public static String serializeToString(MassAlert m) {
		return serializeToJson(m).toString();
	}
	
	public static JSONObject serializeToJson(MassAlert m) {
		JSONObject o = new JSONObject();
		try {
			o.put(KEY_URL, m.url);
			o.put(KEY_AGENCY, m.agency);
			o.put(KEY_MESSAGE, m.message);
			o.put(KEY_TIMESTAMP, m.timestamp);
		} catch (Exception e) {
			Log.e("javelin", "Error serializing mass alert to json");
			return null;
		}
		return o;
	}
	
	public static MassAlert deserializeFromString(String s) {
		JSONObject o = null;
		try {
			o = new JSONObject(s);
		} catch (Exception e) {
			Log.e("javelin", "Error deserializing mass alert from string");
			return null;
		}
		return deserializeFromJson(o);
	}
	
	public static MassAlert deserializeFromJson(JSONObject o) {
		MassAlert m = new MassAlert("", "",  "",  "");
		try {
			m.url = o.getString(KEY_URL);
			m.agency = o.getString(KEY_AGENCY);
			m.message = o.getString(KEY_MESSAGE);
			m.timestamp = o.getString(KEY_TIMESTAMP);
		} catch (Exception e) {
			Log.e("javelin", "Error deserializing mass alert from json", e);
			return null;
		}
		return m;
	}
	
	//de/serializing of list
	public static String serializeListToString(List<MassAlert> l) {
		return serializeListToJson(l).toString();
	}
	
	public static JSONArray serializeListToJson(List<MassAlert> l) {
		JSONArray a = null;
		try {
			a = new JSONArray();
			for (MassAlert m : l) {
				a.put(serializeToJson(m));
			}
		} catch (Exception e) {
			Log.e("javelin", "Error serializing list of mass alerts to json");
			return null;
		}
		return a;
	}
	
	public static List<MassAlert> deserializeListFromString(String s) {
		JSONArray a = null;
		try {
			a = new JSONArray(s);
		} catch (Exception e) {
			Log.e("javelin", "Error deserializing list of mass alerts from string");
			return null;
		}
		return deserializeListFromJson(a);
	}
	
	public static List<MassAlert> deserializeListFromJson(JSONArray a) {
		List<MassAlert> l = new ArrayList<MassAlert>();
		try {
			for(int i = 0; i < a.length(); i++) {
				l.add(deserializeFromJson(a.getJSONObject(i)));
			}
		} catch (Exception e) {
			Log.e("javelin", "Error deserializing list of mass alerts from json");
			return null;
		}
		return l;
	}
}
