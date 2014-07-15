package com.tapshield.android.api.model;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.annotations.SerializedName;

public class DispatchCenter {
	
	private static final String FORMAT_CLOSED_DATES =  "yyyy-MM-dd-HH:mm:ss";
	private static final String DIVIDER_TIME = ":";
	
	@SerializedName("url")
	public String mUrl;
	
	@SerializedName("name")
	public String mName;
	
	@SerializedName("phone_number")
	public String mPhone;
	
	@SerializedName("closed_date")
	public List<ClosedDate> mClosedDates;
	
	@SerializedName("opening_hours")
	public List<OpeningHours> mOpeningHours;
	
	public boolean hasPhone() {
		return mPhone != null;
	}
	
	public boolean hasClosedDates() {
		return mClosedDates != null && !mClosedDates.isEmpty();
	}
	
	public boolean hasOpeningHours() {
		return mOpeningHours != null && !mOpeningHours.isEmpty();
	}
	
	public boolean isOpen() {
		
		DateTime now = DateTime.now();
		
		//check closed dates for fast discarding of being the case
		if (hasClosedDates()) {
			DateTimeFormatter formatter = DateTimeFormat.forPattern(FORMAT_CLOSED_DATES);
			
			for (ClosedDate cd : mClosedDates) {
				
				String startDate = cd.mStartDate;
				String endDate = cd.mEndDate;
				
				if (startDate.endsWith("Z")) {
					startDate = startDate.replace("Z", "");
				}
				
				if (startDate.contains("T")) {
					startDate = startDate.replace("T", "-");
				}
				
				if (endDate.endsWith("Z")) {
					endDate = endDate.replace("Z", "");
				}
				
				if (endDate.contains("T")) {
					endDate = endDate.replace("T", "-");
				}
				
				DateTime closedStart = formatter.parseDateTime(startDate);
				DateTime closedEnd = formatter.parseDateTime(endDate);
				
				//Log.i("javelin", "start=" + closedStart.toString("yyyy-MM-dd HH:mm:ss"));
				//Log.i("javelin", "end=" + closedEnd.toString("yyyy-MM-dd HH:mm:ss"));
				//Log.i("javelin", "now=" + now.toString("yyyy-MM-dd HH:mm:ss"));
				
				if ((now.isEqual(closedStart) || now.isAfter(closedStart)) && now.isBefore(closedEnd)) {
					return false;
				}
			}
		}
		
		//now compare the opening hours if available
		
		//no times means always open
		if (!hasOpeningHours()) {
			return true;
		}
		
		for (OpeningHours oh : mOpeningHours) {
			
			//opening hours is set as 1 = sunday, ..., 7 = saturday.
			// while joda-time lib returns 1 = monday, ..., 7 = sunday
			//values have to be normalized to matching 'standard' joda-time TO opening hours
			
			int dayOfWeek = now.getDayOfWeek();
			
			//if it is joda-time sunday, set to opening-hours sunday (1), else, incr by 1 to match 
			if (dayOfWeek == DateTimeConstants.SUNDAY) {
				dayOfWeek = 1;
			} else {
				dayOfWeek += 1;
			}
			
			//normalized day of week
			
			//proceed if same day
			if (oh.mDay.equals(Integer.toString(dayOfWeek))) {
				
				//build local times to compare opening hours
				String[] openParts = oh.mOpen.split(DIVIDER_TIME);
				String[] closeParts = oh.mClose.split(DIVIDER_TIME);
				
				LocalTime open = new LocalTime(
						Integer.parseInt(openParts[0]),
						Integer.parseInt(openParts[1]),
						Integer.parseInt(openParts[2]));
				
				LocalTime close = new LocalTime(
						Integer.parseInt(closeParts[0]),
						Integer.parseInt(closeParts[1]),
						Integer.parseInt(closeParts[2]));

				LocalTime nowTime = LocalTime.now();

				//Log.i("javelin", "open=" + open.toString() + " close=" + close.toString() + " now=" + nowTime.toString());
				
				//return true of within the opening hours
				if ((nowTime.isEqual(open) || nowTime.isAfter(open)) && nowTime.isBefore(close)) {
					return true;
				}
			}
		}
		
		return false;
	}
}
