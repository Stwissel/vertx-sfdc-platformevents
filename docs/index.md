# Salesforce vert.x integration

Collection of verticles that enable interaction with the Salesforce API.

## Available Verticles

- Authentication
- Subscription to platform events
- Send result to console
- Send result to websocket
- Main/Setup Verticle
- Deduplication Verticle

## [Eventbus](eventbus.md)

The SFDC verticles make heavy use of the [eventbus](eventbus.md).

 After loading all configured verticles, the main verticle sends a start signal on the bus. The SFDC verticles can be configured to start listening only after the start signal has been sent. This allows to spin up the consumers before the listeners start taking incoming events.

 The authentication verticles listen to the [eventbus](eventbus.md) to provide the HTTP header and destination server for listener or consumer verticles. Listener or consumer verticles are shielded from the details of authentication/authorization

 The listeners publish their received data onto the [eventbus](eventbus.md). They either use the configured destination address or the address of a [deduplication service](deduplication.md), which prevents identical events from being forwarded to consumers twice.

 Last not least: the main verticle uses the [eventbus](eventbus.md) to signal the verticles to stop listening. This allows to shutdown the listeners and let the bus drain from messages before shutting down.

## Flow

One or more listener Verticles connect to a Salesforce instance. This can be development, sandbox or production.
The listeners will register for the configured events and publish incoming data to the vert.x eventbus.

Zero or more consumers listen to the eventbus and do with the incoming data what they have to do.

As sample a Consumer that forwards the payload to a websocket and one that just prints to the console is provided. 

The flow is supported by one or more Authentication verticles.
The main one of type SoapApi retrieves the session information from Salesforce.

A second one "Basic" is provided to allow Verticles to retrieve basic Auth. e.g. for use in a JSON POST in a listener.

Finally the ApplicationStarter Verticle loads the configuration file "SFDCOptions.json" and all verticles described in it.

## Setup

Add this dependencies to your `pom.xml`:

```
<dependency>
	<groupId>net.wissel.salesforce</groupId>
	<artifactId>net.wissel.salesforce.vertx</artifactId>
	<version>0.2.0</version>
</dependency>
```

Then [configure the `sfdcoptions.json`](configure.md) file

## Start
Start the verticle `net.wissel.salesforce.vertx.ApplicationStarter` to launch the configured listener and consumers

## Feedback
I would love to hear about your implementation or ideas or challenges, so 
[open an issue on GitHub](https://github.com/Stwissel/vertx-sfdc-platformevents/issues) - even for sharing success. Don't be shy!