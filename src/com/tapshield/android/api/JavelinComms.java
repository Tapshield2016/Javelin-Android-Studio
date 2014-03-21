package com.tapshield.android.api;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;

import com.amazonaws.org.apache.http.client.methods.HttpPatch;

public class JavelinComms {
	
	//put
	static void httpPut(String url, String header, String headerContent, List<NameValuePair> paramList, final JavelinCommsCallback callback) {
		String paramAll = buildParams(paramList, false);
		new JavelinCommsRequest(callback).execute(url, header, headerContent, paramAll, HttpPut.METHOD_NAME);
	}
	
	//post
	static void httpPost(String url, String header, String headerContent, List<NameValuePair> paramList, final JavelinCommsCallback callback) {
		String paramAll = buildParams(paramList, false);
		new JavelinCommsRequest(callback).execute(url, header, headerContent, paramAll, HttpPost.METHOD_NAME);
	}
	
	//get
	public static void httpGet(String url, String header, String headerContent, List<NameValuePair> paramList, final JavelinCommsCallback callback) {
		String paramAll = buildParams(paramList, true);
		new JavelinCommsRequest(callback).execute(url, header, headerContent, paramAll, HttpGet.METHOD_NAME);
	}
	
	//patch
	static void httpPatch(String url, String header, String headerContent, List<NameValuePair> paramList, final JavelinCommsCallback callback) {
		String paramAll = buildParams(paramList, false);
		new JavelinCommsRequest(callback).execute(url, header, headerContent, paramAll, HttpPatch.METHOD_NAME);
	}
	
	private static String buildParams(List<NameValuePair> paramList, boolean urlEncode) {
		if (paramList == null || paramList.isEmpty()) {
			return new String();
		}
		
		String paramAll = new String();
		//maybe urlencode via urlencoder static method.encode()?
		Iterator<NameValuePair> paramIt = paramList.iterator();
		while (paramIt.hasNext()) {
			if (paramAll != null && paramAll.length() > 0) {
				paramAll = paramAll.concat("&");
			}

			NameValuePair param = paramIt.next();
			String paramName = param.getName();
			String paramValue = param.getValue();
			
			if (urlEncode) {
				paramName = Uri.encode(paramName);
				paramValue = Uri.encode(paramValue);
			}
			
			paramAll = paramAll.concat(paramName + "=" + paramValue);
		}
		return paramAll;
	}
	
	public interface JavelinCommsCallback {
		void onEnd(JavelinCommsRequestResponse response);
	}
	
	private static class JavelinCommsRequest extends AsyncTask<String, Void, JavelinCommsRequestResponse> {

		private JavelinCommsCallback callback;
		
		public JavelinCommsRequest(JavelinCommsCallback callback) {
			this.callback = callback;
		}
		
		@Override
		protected JavelinCommsRequestResponse doInBackground(String... args) {
			String url = args[0];
			String headerName = args[1];
			String headerContent = args[2];
			String params = args[3];
			String method = args[4];
			
			if (params != null) {
				params = params.trim();
			}
			
			//check if get method to set url/print params correctly
			boolean isGet = method.equals(HttpGet.METHOD_NAME);
			
			if (isGet && params != null && params.length() > 0) {
				url = url.concat("?".concat(params));
			}
			
			HttpsURLConnection connection = null;
			JavelinCommsRequestResponse response = new JavelinCommsRequestResponse();
			
			try {
				URL urlObject = new URL(url);
				connection = (HttpsURLConnection) urlObject.openConnection();
				connection.setRequestMethod(method);
				if (headerName != null && headerContent != null) {
					connection.addRequestProperty(headerName, headerContent);
				}
				connection.setDoInput(true);
				
				
				//output only if not get (get requests have params attached to url instead)
				if (!isGet && params != null && params.length() > 0) {
					connection.setDoOutput(true);
					connection.getOutputStream().write(params.getBytes());
				}
				
				int code = connection.getResponseCode();
				response.code = code;

				//build array of response headers
				int numHeaders = connection.getHeaderFields().size();
				Header[] headers = new Header[numHeaders];
				for (int i = 0; i < numHeaders; i++) {
					String key = connection.getHeaderFieldKey(i);
					String val = connection.getHeaderField(i);
					if (key != null && val != null) {
						headers[i] = new BasicHeader(key, val);
					}
				}
				
				if (headers != null) {
					response.headers = headers;
				}
				
				//set successful if code fall in the range of the 2xx (OKs)
				response.successful = (code >= 200 && code < 300);
				
				InputStream is = null;
				
				//use errorstream for error codes 4xx - 5xx
				if (code >= 400 && code < 600) {
					is = connection.getErrorStream();
				} else {
					is = connection.getInputStream();
				}
				
				if (is != null) {
					response.response = IOUtils.toString(is);
				} else {
					response.response = new String();
				}
				
				response.exception = new Exception(response.response);
				
				try {
					response.jsonResponse = new JSONObject(response.response);
				} catch (Exception e) {
					response.jsonResponse = null;
				}
				
			} catch (Exception e) {
				response.successful = false;
				response.response = e.getMessage();
				response.exception = e;
			}
			
			if (connection != null) {
				connection.disconnect();
			}
			
			return response;
		}
		
		@Override
		protected void onPostExecute(JavelinCommsRequestResponse result) {
			callback.onEnd(result);
		}
	}
	
	public static class JavelinCommsRequestResponse {
		public boolean successful = false;
		public int code = -1;
		public Header[] headers;
		public String response = null;
		public Exception exception = null;
		public JSONObject jsonResponse;
	}
}
