
package com.tapshield.android.api;

public class JavelinConfig {

	private String mMasterToken;
	private String mBaseUrl;
	private String mGcmSenderId;
	private String mAwsSqsAccess;
	private String mAwsSqsSecret;
	private String mAwsSqsQueue;
	private String mAwsS3Access;
	private String mAwsS3Secret;
	private String mAwsS3Bucket;
	private String mAwsDynamoDbAccess;
	private String mAwsDynamoDbSecret;
	private String mAwsDynamoDbTable;
	
	//private constructor to lead user to create using the builder
	private JavelinConfig() {}
	
	//no modifier for most of the methods to just allow access to current object and package
	
	String getMasterToken() {
		return mMasterToken;
	}
	
	public String getBaseUrl() {
		return mBaseUrl;
	}
	
	String getGcmSenderId() {
		return mGcmSenderId;
	}
	
	String getAwsSqsAccessKey() {
		return mAwsSqsAccess;
	}
	
	String getAwsSqsSecretKey() {
		return mAwsSqsSecret;
	}
	
	String getAwsSqsQueue() {
		return mAwsSqsQueue;
	}
	
	String getAwsS3AccessKey() {
		return mAwsS3Access;
	}
	
	String getAwsS3SecretKey() {
		return mAwsS3Secret;
	}
	
	String getAwsS3Bucket() {
		return mAwsS3Bucket;
	}
	
	String getAwsDynamoDbAccessKey() {
		return mAwsDynamoDbAccess;
	}
	
	String getAwsDynamoDbSecretKey() {
		return mAwsDynamoDbSecret;
	}
	
	String getAwsDynamoDbTable() {
		return mAwsDynamoDbTable;
	}
	
	public static class Builder {
		
		private String masterToken;
		private String baseUrl;
		private String gcmSenderId;
		private String awsSqsAccess;
		private String awsSqsSecret;
		private String awsSqsQueue;
		private String awsS3Access;
		private String awsS3Secret;
		private String awsS3Bucket;
		private String awsDynamoDbAccess;
		private String awsDynamoDbSecret;
		private String awsDynamoDbTable;
		
		public Builder masterToken(String masterToken) {
			this.masterToken = masterToken;
			return this;
		}
		
		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}
		
		public Builder gcmSenderId(String senderId) {
			this.gcmSenderId = senderId;
			return this;
		}
		
		public Builder awsSqsAccessKey(String accessKey) {
			this.awsSqsAccess = accessKey;
			return this;
		}
		
		public Builder awsSqsSecretKey(String secretKey) {
			this.awsSqsSecret = secretKey;
			return this;
		}
		
		public Builder awsSqsQueueName(String queue) {
			this.awsSqsQueue = queue;
			return this;
		}
		
		public Builder awsS3AccessKey(String accessKey) {
			this.awsS3Access = accessKey;
			return this;
		}
		
		public Builder awsS3SecretKey(String secretKey) {
			this.awsS3Secret = secretKey;
			return this;
		}
		
		public Builder awsS3Bucket(String bucket) {
			this.awsS3Bucket = bucket;
			return this;
		}

		public Builder awsDynamoDbAccessKey(String accessKey) {
			this.awsDynamoDbAccess = accessKey;
			return this;
		}
		
		public Builder awsDynamoDbSecretKey(String secretKey) {
			this.awsDynamoDbSecret = secretKey;
			return this;
		}
		
		public Builder awsDynamoDbTable(String tableName) {
			this.awsDynamoDbTable = tableName;
			return this;
		}
		
		public JavelinConfig build() {
			JavelinConfig config = new JavelinConfig();
			config.mMasterToken = masterToken;
			config.mBaseUrl = baseUrl;
			config.mGcmSenderId = gcmSenderId;
			config.mAwsSqsAccess = awsSqsAccess;
			config.mAwsSqsSecret = awsSqsSecret;
			config.mAwsSqsQueue = awsSqsQueue;
			config.mAwsS3Access = awsS3Access;
			config.mAwsS3Secret = awsS3Secret;
			config.mAwsS3Bucket = awsS3Bucket;
			config.mAwsDynamoDbAccess = awsDynamoDbAccess;
			config.mAwsDynamoDbSecret = awsDynamoDbSecret;
			config.mAwsDynamoDbTable = awsDynamoDbTable;
			return config;
		}
	}
}
