package com.tapshield.android.api.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.tapshield.android.api.JavelinUtils;

public class UserProfile {

	public static final String ACTION_USER_PICTURE_UPDATED = "com.tapshield.android.action.USER_PICTURE_UPDATED";
	
	public static String REGEX_DOB = "\\d{4}/\\d{2}/\\d{2}";
	public static String REGEX_1_OR_2_CAPITAL_LETTERS = "[A-Z]{1,2}";
	public static String REGEX_HEIGHT = "\\d{1}'\\d{1,2}\"";
	public static String REGEX_WEIGHT = "\\d{2,3}\\slbs.";
	
	public static final String GENDER_MALE = "Male";
	public static final String GENDER_FEMALE = "Female";
	
	private static final int PICTURE_DIMENSION_MAX = 300;
	
	private static final String KEY_DOB = "birthday";
	private static final String KEY_ADDRESS = "address";
	private static final String KEY_ADDRESS1 = "address1";
	private static final String KEY_ADDRESS2 = "address2";
	private static final String KEY_HAIRCOLOR = "hair_color";
	private static final String KEY_GENDER = "gender";
	private static final String KEY_RACE = "race";
	private static final String KEY_HEIGHT = "height";
	private static final String KEY_WEIGHT = "weight";
	private static final String KEY_ALLERGIES = "known_allergies";
	private static final String KEY_MEDICATIONS = "medications";
	private static final String KEY_EMERGENCY_FIRSTNAME = "emergency_contact_first_name";
	private static final String KEY_EMERGENCY_LASTNAME = "emergency_contact_last_name";
	private static final String KEY_EMERGENCY_PHONE = "emergency_contact_phone_number";
	private static final String KEY_EMERGENCY_RELATIONSHIP = "emergency_contact_relationship";
	private static final String KEY_IMAGE_URL = "profile_image_url";
	
	private static final String PICTURE_FILENAME = "/user.jpg";
	private static final String PICTURE_FILENAME_TEMP = "/temp.jpg";
	
	private String
			url;
	
	private String
			dateOfBirth,
			address1,
			address2,
			hairColor,
			gender,
			race,
			height,
			allergies,
			medications,
			emergencyFirstName,
			emergencyLastName,
			emergencyPhoneNumber,
			emergencyRelationship;
	
	private int
			weight;
	
	private String
			pictureUrl;
	
	public UserProfile() {
		weight = Integer.MIN_VALUE;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
	
	public boolean hasUrl() {
		return url != null && url.trim().length() > 0;
	}
	
	public void setDateOfBirth(String dateOfBirth) {
		if (dateOfBirth == null) {
			return;
		}
		this.dateOfBirth = dateOfBirth;
	}
	
	public String getDateOfBirth() {
		return dateOfBirth;
	}
	
	public boolean hasDateOfBirth() {
		return dateOfBirth != null && dateOfBirth.length() > 0;
	}
	
	public void setAddress1(String address) {
		if (address == null) {
			return;
		}
		this.address1 = address;
	}
	
	public String getAddress1() {
		return address1;
	}
	
	public boolean hasAddress1() {
		return address1 != null && address1.length() > 0;
	}
	
	public void setAddress2(String address) {
		if (address == null) {
			return;
		}
		this.address2 = address;
	}
	
	public String getAddress2() {
		return address2;
	}
	
	public boolean hasAddress2() {
		return address2 != null && address2.length() > 0;
	}
	
	public void setHairColor(String hairColor) {
		if (hairColor == null) {
			return;
		}
		this.hairColor = hairColor;
	}
	
	public String getHairColor() {
		return hairColor;
	}

	public boolean hasHairColor() {
		return hairColor != null && hairColor.length() > 0;
	}
	
	public void setGender(String gender) {
		if (gender == null ) {
			return;
		}
		this.gender = gender;
	}
	
	public String getGender() {
		return gender;
	}
	
	public boolean hasGender() {
		return gender != null && gender.length() > 0;
	}
	
	public void setRace(String race) {
		if (race == null) {
			return;
		}
		
		this.race = race;
	}
	
	public String getRace() {
		return race;
	}
	
	public boolean hasRace() {
		return race != null && race.length() > 0;
	}
	
	public void setHeight(String height) {
		if (height == null) {
			return;
		}
		this.height = height;
	}
	
	public String getHeight() {
		return height;
	}
	
	public boolean hasHeight() {
		return height != null && height.length() > 0;
	}
	
	public void setWeight(int weight) {
		if (weight == -1 || weight == Integer.MAX_VALUE || weight == Integer.MIN_VALUE) {
			return;
		}
		this.weight = weight;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public boolean hasWeight() {
		return weight != -1 && weight != Integer.MIN_VALUE && weight != Integer.MAX_VALUE;
	}
	
	public void setAllergies(String allergies) {
		if (allergies == null) {
			return;
		}
		this.allergies = allergies;
	}
	
	public String getAllergies() {
		return allergies;
	}
	
	public boolean hasAllergies() {
		return allergies != null && allergies.length() > 0;
	}
	
	public void setMedications(String medications) {
		if (medications == null) {
			return;
		}
		this.medications = medications;
	}
	
	public String getMedications() {
		return medications;
	}
	
	public boolean hasMedications() {
		return medications != null && medications.length() > 0;
	}
	
	public void setEmergencyContactFirstName(String emergencyContactFirstName) {
		if (emergencyContactFirstName == null) {
			return;
		}
		this.emergencyFirstName = emergencyContactFirstName;
	}
	
	public String getEmergencyContactFirstName() {
		return emergencyFirstName;
	}
	
	public boolean hasEmergencyContactFirstName() {
		return emergencyFirstName != null && emergencyFirstName.length() > 0;
	}
	
	public void setEmergencyContactLastName(String emergencyContactLastName) {
		if (emergencyContactLastName == null) {
			return;
		}
		this.emergencyLastName = emergencyContactLastName;
	}
	
	public String getEmergencyContactLastName() {
		return emergencyLastName;
	}
	
	public boolean hasEmergencyContactLastName() {
		return emergencyLastName != null && emergencyLastName.length() > 0;
	}
	
	public void setEmergencyContactPhoneNumber(String emergencyContactPhoneNumber) {
		if (emergencyContactPhoneNumber == null) {
			return;
		}
		this.emergencyPhoneNumber = emergencyContactPhoneNumber;
	}
	
	public String getEmergencyContactPhoneNumber() {
		return emergencyPhoneNumber;
	}
	
	public boolean hasEmergencyContactPhoneNumber() {
		return emergencyPhoneNumber != null && emergencyPhoneNumber.length() > 0;
	}
	
	public void setEmergencyContactRelationship(String emergencyContactRelationship) {
		if (emergencyContactRelationship == null) {
			return;
		}
		this.emergencyRelationship = emergencyContactRelationship;
	}
	
	public String getEmergencyContactRelationship() {
		return emergencyRelationship;
	}
	
	public boolean hasEmergencyContactRelationship() {
		return emergencyRelationship != null && emergencyRelationship.length() > 0;
	}
	
	public void setPictureUrl(String url) {
		if (url == null || url.trim().length() == 0) {
			return;
		}
		pictureUrl = url;
	}
	
	public String getPictureUrl() {
		return pictureUrl;
	}
	
	public boolean hasPictureUrl() {
		return pictureUrl != null && pictureUrl.length() > 0;
	}
	
	private static File getPictureDirectory(Context context) {
		return context.getExternalFilesDir(null);
	}
	
	public static File getPictureFile(Context context) {
		File dir = getPictureDirectory(context);
		if (dir == null) {
			return null;
		}
		return new File(dir.getPath() + PICTURE_FILENAME);
	}
	
	public static boolean hasPicture(Context context) {
		File p = getPictureFile(context);
		return p != null && p.exists();
	}
	
	public static void setPicture(Context context, File f) {
		if (f == null || !f.exists()) {
			return;
		}
		
		File old = getPictureFile(context);
		if (old != null && old.exists()) {
			old.delete();
		}

		Log.d("javelin", "setPicture old=" + old.getPath());
		Log.d("javelin", "setPicture new=" + f.getPath());
		
		JavelinUtils.scaleDownFileImage(context, f, PICTURE_DIMENSION_MAX);
		
		f.renameTo(old);
	}
	
	public static Bitmap getPicture(Context context) {
		File f = getPictureFile(context);
		
		if (f == null || !f.exists()) {
			return null;
		}
		return BitmapFactory.decodeFile(f.getPath());
	}
	
	public static File getTemporaryPictureFile(Context context) {
		File dir = getPictureDirectory(context);
		if (dir == null) {
			return null;
		}
		File temp = new File(dir.getPath() + PICTURE_FILENAME_TEMP);
		if (!temp.exists()) {
			try {
				temp.createNewFile();
			} catch (IOException e) {}
		}
		return temp;
	}
	
	public List<NameValuePair> getAvailableRequestParams() {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		
		if (hasPictureUrl()) params.add(new BasicNameValuePair(KEY_IMAGE_URL, getPictureUrl()));
		if (hasDateOfBirth()) {
			String dob = getDateOfBirth();
			String[] parts = dob.split("\\/");
			
			//from [m]m/[d]d/yyyy to yyyy/mm/dd
			
			if (parts[0].length() == 1) {
				parts[0] = "0" + parts[0];
			}
			
			if (parts[1].length() == 1) {
				parts[1] = "0" + parts[1];
			}
			
			dob = parts[2] + "-" + parts[0] + "-" + parts[1];
			params.add(new BasicNameValuePair(KEY_DOB, dob));
			Log.i("javelin", "dob=" + dob);
		}
		if (hasAddress1() || hasAddress2()) {
			String addr = getAddress1() + " " + getAddress2();
			params.add(new BasicNameValuePair(KEY_ADDRESS, addr));
		}
		if (hasHairColor()) {
			String hairColor = getHairColor();
			if (hairColor.trim().equals("Blonde")) {
				hairColor = "Y";
			} else if (hairColor.trim().equals("Brown")) {
				hairColor = "BR";
			} else if (hairColor.trim().equals("Black")) {
				hairColor = "BL";
			} else if (hairColor.trim().equals("Red")) {
				hairColor = "R";
			} else if (hairColor.trim().equals("Bald")) {
				hairColor = "BA";
			} else if (hairColor.trim().equals("Gray")) {
				hairColor = "GR";
			} else if (hairColor.trim().equals("Other")) {
				hairColor = "O";
			}
			
			params.add(new BasicNameValuePair(KEY_HAIRCOLOR, hairColor));
		}
		if (hasGender()) {
			String gender = getGender().trim();
			if (gender.equals(GENDER_MALE)) {
				gender = "M";
			} else if (gender.equals(GENDER_FEMALE)) {
				gender = "F";
			}
			params.add(new BasicNameValuePair(KEY_GENDER, gender));
		}
		if (hasRace()) {
			String r = getRace().trim();
			
			if (r.equals("Black/African Descent")) {
				r = "BA";
			} else if (r.equals("White/Caucasian")) {
				r = "WC";
			} else if (r.equals("East Indian")) {
				r = "EI";
			} else if (r.equals("Asian")) {
				r = "AS";
			} else if (r.equals("Latino/Hispanic")) {
				r = "LH";
			} else if (r.equals("Middle Eastern")) {
				r = "ME";
			} else if (r.equals("Pacific Islander")) {
				r = "PI";
			} else if (r.equals("Native American")) {
				r = "NA";
			} else if (r.equals("Other")) {
				r = "O";
			}
			
			params.add(new BasicNameValuePair(KEY_RACE, r));
		}
		if (hasHeight()) {
			String height = getHeight().trim();
			params.add(new BasicNameValuePair(KEY_HEIGHT, height));
		}
		
		Log.i("javelin", "stored weight=" + getWeight());
		if(hasWeight()) params.add(new BasicNameValuePair(KEY_WEIGHT, Integer.toString(getWeight())));
		if (hasAllergies()) params.add(new BasicNameValuePair(KEY_ALLERGIES, getAllergies()));
		if (hasMedications()) params.add(new BasicNameValuePair(KEY_MEDICATIONS, getMedications()));
		if (hasEmergencyContactFirstName()) params.add(new BasicNameValuePair(KEY_EMERGENCY_FIRSTNAME, getEmergencyContactFirstName()));
		if (hasEmergencyContactLastName()) params.add(new BasicNameValuePair(KEY_EMERGENCY_LASTNAME, getEmergencyContactLastName()));
		if (hasEmergencyContactPhoneNumber()) params.add(new BasicNameValuePair(KEY_EMERGENCY_PHONE, getEmergencyContactPhoneNumber()));
		if (hasEmergencyContactRelationship()) {
			String r = getEmergencyContactRelationship().trim();
			
			if (r.equals("Father")) {
				r = "F";
			} else if (r.equals("Mother")) {
				r = "M";
			} else if (r.equals("Spouse")) {
				r = "S";
			} else if (r.equals("Brother")) {
				r = "B";
			} else if (r.equals("Sister")) {
				r = "S";
			} else if (r.equals("Friend")) {
				r = "FR";
			}
			
			params.add(new BasicNameValuePair(KEY_EMERGENCY_RELATIONSHIP, r));
		}
		
		return params;
	}
	
	public static JSONObject serializeToJson(UserProfile p) {
		JSONObject o = new JSONObject();
		
		if (p == null) {
			return o;
		}
		
		try {
			if (p.hasPictureUrl()) o.put(KEY_IMAGE_URL, p.getPictureUrl());
			if (p.hasDateOfBirth()) o.put(KEY_DOB, p.getDateOfBirth());
			if (p.hasAddress1()) o.put(KEY_ADDRESS1, p.getAddress1());
			if (p.hasAddress2()) o.put(KEY_ADDRESS2, p.getAddress2());
			if (p.hasHairColor()) o.put(KEY_HAIRCOLOR, p.getHairColor());
			if (p.hasGender()) o.put(KEY_GENDER, p.getGender());
			if (p.hasRace()) o.put(KEY_RACE, p.getRace());
			if (p.hasHeight()) o.put(KEY_HEIGHT, p.getHeight());
			if (p.hasWeight()) o.put(KEY_WEIGHT, p.getWeight());
			if (p.hasAllergies()) o.put(KEY_ALLERGIES, p.getAllergies());
			if (p.hasMedications()) o.put(KEY_MEDICATIONS, p.getMedications());
			if (p.hasEmergencyContactFirstName()) o.put(KEY_EMERGENCY_FIRSTNAME, p.getEmergencyContactFirstName());
			if (p.hasEmergencyContactLastName()) o.put(KEY_EMERGENCY_LASTNAME, p.getEmergencyContactLastName());
			if (p.hasEmergencyContactPhoneNumber()) o.put(KEY_EMERGENCY_PHONE, p.getEmergencyContactPhoneNumber());
			if (p.hasEmergencyContactRelationship()) o.put(KEY_EMERGENCY_RELATIONSHIP, p.getEmergencyContactRelationship());
		} catch (Exception e) {
			return null;
		}
		return o;
	}
	
	public static UserProfile deserializeFromJson(JSONObject o) {
		UserProfile p = new UserProfile();
		
		if (o == null) {
			return p;
		}
		
		try {
			if (o.has(KEY_IMAGE_URL)) p.setPictureUrl(o.getString(KEY_IMAGE_URL));
			if (o.has(KEY_DOB)) p.setDateOfBirth(o.getString(KEY_DOB));
			if (o.has(KEY_ADDRESS1)) p.setAddress1(o.getString(KEY_ADDRESS1));
			if (o.has(KEY_ADDRESS2)) p.setAddress2(o.getString(KEY_ADDRESS2));
			if (o.has(KEY_HAIRCOLOR)) p.setHairColor(o.getString(KEY_HAIRCOLOR));
			if (o.has(KEY_GENDER)) p.setGender(o.getString(KEY_GENDER));
			if (o.has(KEY_RACE)) p.setRace(o.getString(KEY_RACE));
			if (o.has(KEY_HEIGHT)) p.setHeight(o.getString(KEY_HEIGHT));
			if (o.has(KEY_WEIGHT)) p.setWeight(o.getInt(KEY_WEIGHT));
			if (o.has(KEY_ALLERGIES)) p.setAllergies(o.getString(KEY_ALLERGIES));
			if (o.has(KEY_MEDICATIONS)) p.setMedications(o.getString(KEY_MEDICATIONS));
			if (o.has(KEY_EMERGENCY_FIRSTNAME)) p.setEmergencyContactFirstName(o.getString(KEY_EMERGENCY_FIRSTNAME));
			if (o.has(KEY_EMERGENCY_LASTNAME)) p.setEmergencyContactLastName(o.getString(KEY_EMERGENCY_LASTNAME));
			if (o.has(KEY_EMERGENCY_PHONE)) p.setEmergencyContactPhoneNumber(o.getString(KEY_EMERGENCY_PHONE));
			if (o.has(KEY_EMERGENCY_RELATIONSHIP)) p.setEmergencyContactRelationship(o.getString(KEY_EMERGENCY_RELATIONSHIP));
		} catch (Exception e) {
			p = new UserProfile();
		}
		return p;
	}
}
