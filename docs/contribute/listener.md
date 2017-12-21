# Write your own Listener

## You are here: [Home](/index.md):Write your own:[Listener](listener.md)

See also:
- Write your own [Deduplication](dedup.md)
- Write your own [Auth](auth.md)
- Write your own [Consumer](consumer.md) 

## Why you want to do that
The default listener debulkifies incoming messages. It puts the incoming JSON unaltered onto the eventbus for processing. It listens to the Salesforce CometD interface. So you might want to have your own listener when:
- You don't want to debulkify
- Listen to something else
- Preprocess the JSON
- Like to write code yourself

## How to implement
The only thing a listener must do: provide JSON data onto the eventbus. So any verticle that does `this.getVertx.EventBus().publish(....)` can be used as "listener".

To be more specific: a listener would listen to startup and shutdown events on the eventbus (see the details about the [EventBus](/eventbus.md)) and use one of the Auth verticles for credentials (see [Authentication and Authorization](/auth.md)) and take advantage of the [ListenerConfig](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/config/ListenerConfig.java) class.

- If the listener will listen to the Salesforce CometD API (Platform Events, Streaming API, Generic Stream events) the easiest way is to extend the default listener [CometD](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/listener/CometD.java).
- If something else shall be listened to (e.g. a custom web service call from Salesforce), extending [AbstractSFDCVerticle](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/AbstractSFDCVerticle.java) gets results fastest.
- If your verticle needs to extend the router (e.g. endpoint for a OBM ) implement the [SFDCRouterExtension](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/SFDCRouterExtension.java) interface.


## Sample code

```
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class SampleCustomCometDListener extends CometD {

    /**
     * @see net.wissel.salesforce.vertx.listener.CometD#processOneResult(io.vertx.core.json.JsonObject)
     */
    @Override
    protected void processOneResult(JsonObject dataChange) {
        final JsonObject data = dataChange.getJsonObject("data");
        final JsonObject payload = data.getJsonObject("payload");
        final String objectType = payload.getString("ObjectType__c");
        // We send it off to the eventbus
        final EventBus eb = this.getVertx().eventBus();
        this.getListenerConfig().getEventBusAddresses().forEach(destination -> {
            try {
                eb.publish(destination+objectType, payload);
                this.logger.info("Sending to [" + destination+objectType + "]:" + payload.toString());
            } catch (final Throwable t) {
                this.logger.error(t.getMessage(), t);
            }
        });
    }
}
```