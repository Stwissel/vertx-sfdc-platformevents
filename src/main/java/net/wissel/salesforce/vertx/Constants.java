package net.wissel.salesforce.vertx;

public interface Constants {
	String DEFAULT_LISTENER = "net.wissel.salesforce.vertx.listener.CometD";
	String DEFAULT_AUTH_VERTICLE = "net.wissel.salesforce.vertx.auth.SoapApi";
	String PRODUCTION = "login.salesforce.com";
	String SANDBOX = "test.salesforce.com";
	String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	String OPTION_FILE_NAME = "SFDCOptions.json";
	String MESSAGE_START = "Rock it cowboys";
	String BUS_START_STOP = "SFDC:CommandLine";
	String MESSAGE_STOP = "Party is over";
	String DELIMITER = ":";
	String API_ROOT = "/api";
	String CONTENT_HEADER = "Content-Type";
	String CONTENT_TYPE_JSON = "application/json";
	
}
