package com.tapshield.android.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class ChatMessage {

	private static final String KEY_ID = "id";
	private static final String KEY_ALERTID = "alertId";
	private static final String KEY_MESSAGE = "message";
	private static final String KEY_SENDERID = "senderId";
	private static final String KEY_SENDERNAME = "senderName";
	private static final String KEY_TRANSMITTING = "transmitting";
	private static final String KEY_TIMESTAMP = "timestamp";
	
	public String
			id,
			alertId,
			message,
			senderId,
			senderName;
	public boolean
			transmitting = true;
	public long
			timestamp;
	
	public ChatMessage(String message, String senderName, String senderId) {
		id = UUID.randomUUID().toString();
		this.message = message;
		this.senderName = senderName;
		this.senderId = senderId;
		this.timestamp = getCurrentUTCSeconds();
	}
	
	public ChatMessage(String message, String senderName, String senderId, long timestamp) {
		this(message, senderName, senderId);
		this.timestamp = timestamp;
	}
	
	private long getCurrentUTCSeconds() {
		long utc = System.currentTimeMillis();
		return utc/1000;
	}
	
	public static String serializeToString(ChatMessage m) {
		return serializeToJson(m).toString();
	}
	public static JSONObject serializeToJson(ChatMessage m) {
		JSONObject o = new JSONObject();
		
		try {
			o.put(KEY_ID, m.id);
			o.put(KEY_ALERTID, m.alertId);
			o.put(KEY_MESSAGE, m.message);
			o.put(KEY_SENDERID, m.senderId);
			o.put(KEY_SENDERNAME, m.senderName);
			o.put(KEY_TRANSMITTING, m.transmitting);
			o.put(KEY_TIMESTAMP, m.timestamp);
			
			
		} catch (Exception e) {
			Log.e("javelin", "Error serializing ChatMessage.");
		}
		
		return o;
	}
	
	public static ChatMessage deserializeFromString(String s) {
		JSONObject j;
		try {
			j = new JSONObject(s);
		} catch (Exception e) {
			return null;
		}
		return deserializeFromJson(j);
	}
	
	public static ChatMessage deserializeFromJson(JSONObject j) {
		ChatMessage c = new ChatMessage("", "", "");
		
		try {
			c.id = j.getString(KEY_ID);
			c.alertId = j.getString(KEY_ALERTID);
			c.message = j.getString(KEY_MESSAGE);
			c.senderId = j.getString(KEY_SENDERID);
			c.senderName = j.getString(KEY_SENDERNAME);
			//c.transmitting = j.getBoolean(KEY_TRANSMITTING);
			c.transmitting = false;
			c.timestamp = j.getLong(KEY_TIMESTAMP);
		} catch (Exception e) {
			Log.e("javelin", "Error deserializing ChatMessage.");
		}
		
		return c;
	}
	
	public static String serializeListToString(List<ChatMessage> l) {
		return serializeListToJson(l).toString();
	}
	
	public static JSONArray serializeListToJson(List<ChatMessage> l) {
		JSONArray a = new JSONArray();
		
		for (ChatMessage m : l) {
			a.put(serializeToJson(m));
		}
		
		return a;
	}
	
	public static List<ChatMessage> deserializeListFromString(String s) {
		JSONArray a;
		try {
			a = new JSONArray(s);
		} catch (Exception e) {
			return null;
		}
		return deserializeListFromJson(a);
	}
	
	public static List<ChatMessage> deserializeListFromJson(JSONArray a) {
		List<ChatMessage> l = new ArrayList<ChatMessage>();
		
		try {
			for (int i = 0; i < a.length(); i++) {
				l.add(deserializeFromJson(a.getJSONObject(i)));
			}
		} catch (Exception e) {
			return null;
		}
		
		return l;
	}
}
