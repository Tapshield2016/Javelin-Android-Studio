package com.tapshield.android.api.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.tapshield.android.api.JavelinUtils;

public class User {

	private static final String KEY_AGENCY = "agency";
	private static final String KEY_PROFILE = "profile";
	private static final String KEY_URL = "url";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_EMAIL = "email";
	private static final String KEY_FIRST_NAME = "first_name";
	private static final String KEY_LAST_NAME = "last_name";
	private static final String KEY_PHONE_NUMBER = "phone_number";
	private static final String KEY_DISARM_CODE = "disarm_code";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_PHONE_NUMBER_VERIFIED = "phone_number_verified";
	private static final String KEY_SECONDARY_EMAILS = "secondary_emails";
	
	public int id;
	
	public String
			url,
			username,
			email,
			firstName,
			lastName,
			phoneNumber;
	
	public Emails allEmails;
	
	private String
			disarmCode,
			password;
	
	private boolean
			phoneNumberVerified = false;
	
	public Agency
			agency;
	public UserProfile
			profile;
	
	public User() {}
	
	public User(String url, String username, String email, Agency agency) {
		this.url = url;
		this.id = JavelinUtils.extractLastIntOfString(this.url);
		this.username = username;
		this.email = email;
		this.agency = agency;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public boolean equalsPassword(String attemptedPassword) {
		return this.password.equals(attemptedPassword);
	}
	
	public void setDisarmCode(String disarmCode) {
		this.disarmCode = disarmCode;
	}
	
	public String getDisarmCode() {
		return this.disarmCode;
	}
	
	public boolean belongsToAgency() {
		return agency != null;
	}
	
	public boolean isPhoneNumberVerified() {
		return phoneNumberVerified;
	}
	
	public void setPhoneNumberVerified() {
		phoneNumberVerified = true;
	}
	
	public void resetPhoneNumberVerified() {
		phoneNumberVerified = false;
	}
	
	public boolean equalsDisarmCode(String attemptedDisarmCode) {
		return this.disarmCode.equals(attemptedDisarmCode);
	}
	
	public static final String serialize(User u) throws JSONException {
		JSONObject o = new JSONObject();
		o.put(KEY_AGENCY, Agency.serializeToJson(u.agency));
		o.put(KEY_PROFILE, UserProfile.serializeToJson(u.profile));
		o.put(KEY_URL, u.url);
		o.put(KEY_USERNAME, u.username);
		o.put(KEY_EMAIL, u.email);
		o.put(KEY_FIRST_NAME, u.firstName);
		o.put(KEY_LAST_NAME, u.lastName);
		o.put(KEY_PHONE_NUMBER, u.phoneNumber);
		o.put(KEY_DISARM_CODE, u.disarmCode);
		o.put(KEY_PHONE_NUMBER_VERIFIED, u.phoneNumberVerified);
		
		

		if (u.password != null && u.password.trim().length() > 0)	o.put(KEY_PASSWORD, u.password);
		
		return o.toString();
	}
	
	public static final User deserialize(String s) throws JSONException {
		JSONObject o = new JSONObject(s);
		
		Log.i("aaa", "user deser=" + o.toString());
		
		User u = new User();
		
		if(o.has(KEY_AGENCY) && !o.isNull(KEY_AGENCY)) {
			u.agency = Agency.deserializeFromJson(o.getJSONObject(KEY_AGENCY));
		}
		
		if (o.has(KEY_PROFILE)) {
			u.profile = UserProfile.deserializeFromJson(o.getJSONObject(KEY_PROFILE));
		}
		
		if (u.profile == null) {
			u.profile = new UserProfile();
		}
		
		u.url = o.getString(KEY_URL);
		u.id = JavelinUtils.extractLastIntOfString(u.url);
		u.username = o.getString(KEY_USERNAME);
		u.email = o.getString(KEY_EMAIL);

		Gson gson = new Gson();
		
		if (o.has(KEY_SECONDARY_EMAILS) && !o.isNull(KEY_SECONDARY_EMAILS)) {
			u.allEmails = gson.fromJson(s, Emails.class);
		}
		
		if (o.has(KEY_FIRST_NAME)) {
			u.firstName = o.getString(KEY_FIRST_NAME);
		}
		
		if (o.has(KEY_LAST_NAME)) {
			u.lastName = o.getString(KEY_LAST_NAME);
		}
		
		if (o.has(KEY_PHONE_NUMBER)) {
			u.phoneNumber = o.getString(KEY_PHONE_NUMBER);
		}
		
		u.disarmCode = o.getString(KEY_DISARM_CODE);
		u.phoneNumberVerified = o.getBoolean(KEY_PHONE_NUMBER_VERIFIED);
		
		if (o.has(KEY_PASSWORD)) u.password = o.getString(KEY_PASSWORD);
		
		return u;
	}
	
	public static final User deserializeWithEmptyAgency(String s) throws JSONException {
		User u = deserialize(s);
		u.agency = new Agency();
		return u;
	}
	
	public class Emails {
		
		@SerializedName("secondary_emails")
		private List<Email> mEmails;
		
		public List<Email> getList() {
			return mEmails;
		}
		
		public boolean isEmpty() {
			return mEmails.isEmpty();
		}
	}
	
	public class Email {
		
		@SerializedName("email")
		private String mEmail;
		
		@SerializedName("is_primary")
		private boolean mIsPrimary;
		
		@SerializedName("is_active")
		private boolean mIsActive;
		
		@SerializedName("is_activation_sent")
		private boolean mIsActivationSent;
		
		public String get() {
			return mEmail;
		}
		
		public boolean isPrimary() {
			return mIsPrimary;
		}
		
		public boolean isActive() {
			return mIsActive;
		}
		
		public boolean isActivationEmailSent() {
			return mIsActivationSent;
		}
	}
}
