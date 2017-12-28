# Rest Consumer

## You are here: [Home](../index.md):Verticles:[Rest Consumer](restconsumer.md)

See also:

- [Authentication Verticles](auth.md)
- [Subscription to platform events Verticles](platform.md)
- [Send result to console Verticle](console.md)
- [Send result to websocket Verticle](websocket.md)
- [Main/Setup Verticle](main.md)
- [Deduplication Verticles](dedup.md)
- [Redis Dedup Service](redis.md)

## Overview

The Rest consumer takes incoming messages and forwards them to a http(s) endpoint.
It uses an [Auth](../auth.md) verticle to obtain the `Authorization` header before posting the payload.

Optionally, before posting, a [Mustache](http://mustache.github.io/) template gets applied that transforms
the incoming JSON in whatever is required. This allows to POST to HTML forms, SOAP services etc.

The Rest Consumer will use a proxy setting if provided and retry failed connections 10 times in 10 seconds intervals.
A connection is considered failed if the response code is not between 200 and 299.

It does not take values back -> that requires a custom verticle

## Configuration

```
    {
        "verticleName": "net.wissel.salesforce.vertx.consumer.RestConsumer",
        "instanceName": "SampleRest",
        "eventBusAddress": "SFDC:SampleEvents",
        "deployAsWorker": true,
        "autoStart": true,
        "parameters":
        {
            "template": "/sample.mustache",
            "interval": "10000",
            "maxRetryCount" :"10",
            "Content-Type" : "text/plain"
        },
        "url": "https://someendpoint.com/submission"
    }
```

**Note**: When a template name is provided it needs to follow vert.x's [file system rules](http://vertx.io/docs/vertx-core/java/#_using_the_file_system_with_vert_x) to be found.

## Source code

Check [RestConsumer.java](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/vertx-sfdc-core/src/main/java/net/wissel/salesforce/vertx/consumer/RestConsumer.java) on github