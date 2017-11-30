# Salesforce vert.x integration

Collection of verticles that enable interaction with the Salesforce API

## Available Verticles

- Authentication
- Subscription to platform events
- Send result to console
- Send result to websocket
- Main/Setup Verticle

## Flow
One or more listner Verticles connect to a Salesforce instance. This can be development, sandbox or production.
The listeners will register for the configured events and publish incoming data to the vert.x eventbus.

Zero or more consumers listen to the eventbus and do with the incoming data what they have to do. 
As sample a Consumer that forwards the payload to a websocket and one that just prints to the console is provided. 

The flow is suppored by one or more Authentication verticles.
The main one of type SoapApi retrieves the session information from Salesforce.

A second one "Basic" is provided to allow Verticles to retrieve basic Auth. e.g. for use in a JSON POST in a listener

Finally the ApplicationStarter Verticle loads the configuration file "SFDCOptions.json" and all verticles described in it

## Setup

Add this dependencies to your `pom.xml`:

```
<dependency>
	<groupId>net.wissel.salesforce</groupId>
	<artifactId>net.wissel.salesforce.vertx</artifactId>
	<version>0.1.0</version>
</dependency>
```
Then [configure the `sfdcoptions.json`](configure.md) file

## Start
Start the verticle `net.wissel.salesforce.vertx.ApplicationStarter` to launch the configured listener and consumers

## Feedback
Open an issue