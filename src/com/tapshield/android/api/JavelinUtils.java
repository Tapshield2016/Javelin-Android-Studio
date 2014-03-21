package com.tapshield.android.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.tapshield.android.api.model.User;

public class JavelinUtils {
	public static final String buildFinalUrl(JavelinConfig config, String url) {
		final String base = config.getBaseUrl();
		final String slash = "/";

		if (!url.contains(base)) {
			if (url.startsWith(slash)) {
				url = url.substring(1);
			}
			return base.concat(url);
		}
		return url;
	}
	
	static final String buildFinalDeviceTokenUrl(User user) {
		return user.url + JavelinClient.URL_DEVICE_TOKEN_SUFFIX;
	}
	
	public static int extractLastIntOfString(String input) {
		String group = "-1";
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(input);
		while(matcher.find()) {
			group = matcher.group();
		}
		return Integer.parseInt(group);
	}
	
	public static String getAmqpMessageStringWithJsonObject(JSONObject jsonObject) {
		/*
		 * -add alert data to args key to the body json object
		 * -encode body
		 * -attach encoded body to body key in message
		 * -attach delivery info and properties to specific keys
		 * -encode full message
		 * -set that encoded message in amqpMessageString and return it
		 */
		
		String amqpMessageString = null;
		
		try {
			JSONArray args = new JSONArray();
			args.put(jsonObject);
			
			JSONObject body = new JSONObject()
					.put("id", UUID.randomUUID())
					.put("task", "core.tasks.new_alert")
					.put("args", args); //attach array with passed json object
			
			String encodedBody = getBase64StringFromJsonObject(body);
			
			JSONObject deliveryInfo = new JSONObject()
					.put("priority", 0)
					.put("routing_key", "core.new_alert")
					.put("exchange", "new_alert");
			
			JSONObject properties = new JSONObject()
					.put("body_encoding", "base64")
					.put("delivery_info", deliveryInfo) //attach deliveryInfo json object
					.put("delivery_mode", 2)
					.put("delivery_tag", UUID.randomUUID());
			
			JSONObject message = new JSONObject()
					.put("body", encodedBody)
					.put("headers", null)
					.put("content-type", "application/json")
					.put("properties", properties) //attach properties json object
					.put("content-encoding", "binary");
			
			//Log.i("javelin", "Full decoded message:" + message.toString());
			
			String encodedMessage = getBase64StringFromJsonObject(message);
			
			//Log.i("javelin", "Full encoded message:" + encodedMessage.toString());
			
			amqpMessageString = encodedMessage;
			
		} catch (Exception e) {
			Log.e("javelin", "Error getting amqpMessageString:", e);
		}
		
		return amqpMessageString;
	}
	
	public static String getBase64StringFromJsonObject(JSONObject jsonObject) {
		byte[] bytes = jsonObject.toString().getBytes();
		return Base64.encodeToString(bytes, 0);
	}
	
	public static JSONObject getJsonObjectFromBase64String(String base64String) {
		JSONObject jsonObject = null;
		
		try {
			byte[] bytes = Base64.decode(base64String, 0);
			String string = new String(bytes);
			jsonObject = new JSONObject(string);
		} catch (Exception e) {
			jsonObject = null;
			Log.e("javelin", "Error decoding base64 string to json object", e);
		}
		
		return jsonObject;
	}
	
	/**
	 * Scales down the image File if any of its dimensions is greater than the arg maximumDimension. The
	 * aspect of the image file will be kept.
	 * @param context with which the image file will be scaled down. 
	 * @param imageFile image file to scale down.
	 * @param maximumDimension maximum dimension of any of the dimensions of the image file.
	 * @return true if scaled, false otherwise.
	 */
	public static boolean scaleDownFileImage(Context context, File imageFile, int maximumDimension) {
		try {
			if (imageFile == null || !imageFile.exists()) {
				return false;
			}
			//load bitmap
			Uri uri = Uri.fromFile(imageFile);
	
			ContentResolver cr = context.getContentResolver();
			InputStream in = cr.openInputStream(uri);
			BitmapFactory.Options options = new BitmapFactory.Options();
		    options.inSampleSize=2;
			
			Bitmap preScaled = BitmapFactory.decodeStream(in,null,options);
			
			//set new size if required
			int width = preScaled.getWidth();
			int height = preScaled.getHeight();
			float newScale;
			
			if (Math.max(width, height) <= maximumDimension) {
				return false;
			}
			
			if (width > height) {
				newScale = (float)maximumDimension / (float)width;
			} else {
				newScale = (float)maximumDimension / (float)height;
			}
			
			Matrix matrix = new Matrix();
			matrix.postScale(newScale, newScale);
			
			Bitmap postScaled = Bitmap.createBitmap(preScaled, 0, 0, width, height, matrix, true);
			
			//save bitmap
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			postScaled.compress(Bitmap.CompressFormat.JPEG, 50, bytes);
			FileOutputStream fo = new FileOutputStream(imageFile);
			fo.write(bytes.toByteArray());
			fo.close();
		} catch (Exception e) {
			Log.e("javelin", "Error scaling down image file", e);
			return false;
		}
		return true;
	}
}
