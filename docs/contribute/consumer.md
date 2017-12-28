# Write your own Consumer

## You are here: [Home](../index.md):Write your own:[Consumer](consumer.md)

See also:
- Write your own [Listener](listener.md)
- Write your own [Deduplication](dedup.md)
- Write your own [Auth](auth.md)

## Why you want to do that
Most likely you want to do this - prepare the data so a target system can digest it. This is your life stream ETL

## How to implement

The only thing a consumer must do: consume JSON data from the eventbus. So any verticle that does `this.getVertx.EventBus().consumer(....)` can be used as "consumer".

To be more specific: a consumer would listen to startup and shutdown events on the eventbus (see the details about the [EventBus](/eventbus.md)) and use one (if needed) of the Auth verticles for credentials (see [Authentication and Authorization](../auth.md)) and take advantage of the [ConsumerConfig](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/config/ConsumerConfig.java) class.

Fastest results:

- Extent the [AbstractSFDCConsumer](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/consumer/AbstractSFDCConsumer.java) class
- If your verticle needs to extend the router (e.g. the Websocket consumer) implement the [SFDCRouterExtension](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/SFDCRouterExtension.java) interface.

## Sample code

```
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class ConsoleConsumer extends AbstractSDFCConsumer implements SFDCConsumer {

    @Override
    // Just write out to the console
    protected void processIncoming(final Message<JsonObject> incomingData) {
        final JsonObject body = incomingData.body();
        this.logger.info(body.encodePrettily());
    }
```