package com.tapshield.android.api;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
	
	public static final String syncUploadFileToS3WithUri(Context context, JavelinConfig config,
			final Uri uri, final S3UploadListener l) {
		String url = null;

		if (context == null || config == null || uri == null) {
			return url;
		}
		
		//first obtain all necessary information of the file
		ContentResolver contentResolver = context.getContentResolver();
		
		String[] fileInfo = {OpenableColumns.SIZE};
		
		Cursor cursor = contentResolver.query(uri, fileInfo, null, null, null);
		
		if (cursor == null || !cursor.moveToFirst()) {
			return url;
		}
		
		int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
		
		final Long size = cursor.isNull(sizeIndex) ? null : cursor.getLong(sizeIndex);
		
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(contentResolver.getType(uri));
		
		if (size != null) {
			metadata.setContentLength(size);
		}
		
		cursor.close();
		
		//now build and make the s3 request
		String bucketName = config.getAwsS3Bucket();
		String extension = MimeTypeMap
				.getSingleton()
				.getExtensionFromMimeType(contentResolver.getType(uri));
		final String key = UUID.randomUUID().toString() + "." + extension;
		
		url = "http://" + bucketName + ".s3.amazonaws.com/" + key;
		
		final AmazonS3Client s3Client = new AmazonS3Client(
				new BasicAWSCredentials(config.getAwsS3AccessKey(), config.getAwsS3SecretKey()));

		try {
			PutObjectRequest request = new PutObjectRequest(bucketName, key,
					contentResolver.openInputStream(uri), metadata);
			request.setGeneralProgressListener(new ProgressListener() {
				
				@Override
				public void progressChanged(ProgressEvent progressEvent) {
					if (l != null) {
						l.onUploading(uri, progressEvent.getBytesTransferred(), (long) size);
					}
				}
			});
			request.setCannedAcl(CannedAccessControlList.PublicRead);
			s3Client.putObject(request);
		} catch (Exception e) {
			if (l != null) {
				l.onError(uri, e.getMessage());
			}
			url = null;
			return url;
		}
		return url;
	}
	
	public interface S3UploadListener {
		void onUploading(Uri uri, long newBytesTransferred, long total);
		void onError(Uri uri, String error);
	}
	
	public static class AsyncImageDownloaderToFile extends AsyncTask<Void, Void, Boolean> {

		private Context mContext;
		private String mUrl;
		private File mFile;
		private String mAction;
		private boolean mRetry;
		
		/**
		 * @param url Url to download the image from.
		 * @param to File to save the image after download.
		 * @param action Action to be broadcasted after successful download.
		 * @param retryOnFail Boolean value to have it retry after a failed attempt to download
		 * a remotely-available image.
		 */
		public AsyncImageDownloaderToFile(Context context, String url, File to, String action,
				boolean retryOnFail) {
			mContext = context;
			mUrl = url;
			mFile = to;
			mAction = action;
			mRetry = retryOnFail;
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			
			Bitmap bitmap = null;
			
			try {
				URL url = new URL(mUrl);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.connect();
				int responseCode = connection.getResponseCode();
				if (responseCode != HttpURLConnection.HTTP_OK) {
					Log.e("tapshield","Error fetching image (" + mUrl + ")" +
							"Response code NOT OK (" + responseCode + ")");
					return false;
				}
				
				InputStream is = connection.getInputStream();
				bitmap = BitmapFactory.decodeStream(is);
				is.close();
				connection.disconnect();
			} catch (Exception e1) {
				Log.e("tapshield", "Error fetching image (" + mUrl + ")", e1);
				return false;
			}
			
			try {
				if (bitmap != null) {
					
					if (mFile.exists()) {
						mFile.delete();
					}
					
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mFile));
					bitmap.compress(CompressFormat.PNG, 80, bos);
					bos.flush();
					bos.close();
				}
			} catch (Exception e2) {
				Log.e("tapshield", "Error storing image (" + mUrl + ")", e2);
				return false;
			}
			
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				Intent broadcast = new Intent(mAction);
				mContext.sendBroadcast(broadcast);
			}
			super.onPostExecute(result);
		}
	}
}
