package net.wissel.salesforce.vertx;

public interface Constants {
	String DEFAULT_LISTENER = "net.wissel.salesforce.vertx.listener.CometD";
	String DEFAULT_AUTH_VERTICLE = "net.wissel.salesforce.vertx.auth.SoapApi";
	String PRODUCTION = "login.salesforce.com";
	String SANDBOX = "test.salesforce.com";
}
