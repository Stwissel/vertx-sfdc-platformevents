# EventBus

## You are here: [Home](index.md):[Event Bus](eventbus.md)

The SFDC verticles make heavy use of the eventbus for communication. Addresses and address fragments used in the eventbus can be found in the file [Constants.java](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/Constants.java), using the `BUS_` and `MESSAGE_` prefixes.

## System startup

The main verticle loads all configured verticles (see [the configuration](configure.md) for details). To be able to orchestrate a proper start sequence (getting consumers ready before the listeners start sending messages), the verticles don't start their task, but start listening to the address `BUS_START_STOP` which has the value `SFDC:CommandLine`.

If the Verticle has been configured for "auto start", it will start listening on the EventBus (for consumers, auth verticles) or externally (listeners) when they receive the `MESSAGE_START` message (which is currently `Rock it cowboys`).

To indicate that the `MESSAGE_START` is sent as part of the initial bootstrap, the message header `MESSAGE_ISSTARTUP` returns `TRUE` (as String) (current value of `MESSAGE_ISSTARTUP` is `StartupMessage`)

## System shutdown

The main verticle, when ordered to shut down, will send individual shutdown messages to each verticle. Currently the shutdown address a verticle is listening to is computed as:

```
Constants.BUS_START_STOP + Constants.DELIMITER + this.getClass().getName();
```

For the default CometD listener this translates to: `SFDC:CommandLine:net.wissel.salesforce.auth.listener.ComentD`

The computation might change to allow individual instances to be shut down.

## Auth request

Any listener or consumer verticle can request an authorization header from an Auth verticle. Currently there are two auth verticles: SOAP and Basic. The SOAP one is working with the Salesforce API, while the Basic one is intended for web service use.

The Auth verticles listen to `BUS_AUTHREQUEST+AuthName`. The AuthName is retrieved from the [configuration](configure.md). For the default Auth verticle this translates to `SFDC:Auth:default`

The Auth verticle either replies with a failure or an [AuthInfo](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/auth/AuthInfo.java) object. The AuthInfo object contains the value for the `Authorization` http header and the final server. This allows to use the generic login/test Salesforce URL, but then interact with the actual instance.

When the request header contains `AUTH_RESET` (value `RESET`) a cached auth information or a failed authentication is cleared and a new authentication is attempted

## Listener producing data

A listener listens to external events and propagates them to the eventbus. Currently there is are two CometD listeners, the default and one customized as example. A listener has three relevant [configuration settings](configure.md):

- a list of destination addresses to send the messages to
- a boolean value: use Deduplication service
- the address of the deduplication service

When a listener receives data it will publish the payload to the eventbus. First all destinations will be added to the message header as `BUS_FINAL_DESTINATION` header values (value: `SFDCFinalDestination`). Thereafter the step depends on the deduplication setting:

- True: the incoming data is published to the **one** deduplication service address.
- False: the incoming data is published to each configured address

The data is **published**, so there can be more than one subscriber listening.

## Consumer listening

Consumers listen to **one** configured address on the event bus. Messages are consumed without confirmation to allow multiple subscriptions of consumers to one listener published address.

Consumers might, as the need arises, request auth from an auth verticle
