# Write your own Deduplication

## You are here: [Home](../index.md):Write your own:[Deduplication](dedup.md)

See also:
- Write your own [Listener](listener.md)
- Write your own [Auth](auth.md)
- Write your own [Consumer](consumer.md) 

## Why you want to do that
The base package provides an in-memory deduplication (limited) and the redis package one based on Redis. You might have other ideas on what is a duplicate or what key-value store to use (I like [AeroSpike](https://www.aerospike.com/)).

## How to implement
A verticle listening to incoming data on the eventbus. The header of the incoming data contains one or more `BUS_FINAL_DESTINATION` header values (value: `SFDCFinalDestination`).

It then processes all messages and drop the duplicates (that's implementation specific) and send the surviving ones to all destinations provided in the header.

Fastest results: Extend the [AbstractSFDCDedupVerticle](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/consumer/AbstractSFDCDedupVerticle.java) class

## Sample code

```
package net.wissel.salesforce.vertx.consumer;

import java.util.LinkedList;
import java.util.Queue;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class MemoryDedup extends AbstractSFDCDedupVerticle {
    
    private final Queue<String> memoryQueue = new LinkedList<String>();
    private static final int MAX_MEMBERS = 100;

    /**
     * @see net.wissel.salesforce.vertx.consumer.AbstractSFDCDedupVerticle#checkForDuplicate(io.vertx.core.Future, io.vertx.core.json.JsonObject)
     */
    @Override
    protected void checkForDuplicate(final Future<Void> failIfDuplicate, final JsonObject messageBody) {
        final String candidate = messageBody.encode();
        if (this.memoryQueue.contains(candidate)) {
            // We have a duplicate and fail the future
            failIfDuplicate.fail("Duplicate");
        } else {
            this.memoryQueue.offer(candidate);
            // Limit the size of the queue
            while (this.memoryQueue.size() > MAX_MEMBERS) {
                this.memoryQueue.poll();
            }
            failIfDuplicate.complete();
        }

    }
}
```