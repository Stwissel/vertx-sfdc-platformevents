# Platform Event Listener Verticles

## You are here: [Home](/index.md):Verticles:[Redis Dedup Service](redis.md)

See also:

- [Authentication Verticles](auth.md)
- [Subscription to platform events Verticles](platform.md)
- [Send result to console Verticle](console.md)
- [Send result to websocket Verticle](websocket.md)
- [Main/Setup Verticle](main.md)
- [Deduplication Verticles](dedup.md)
- [Redis Dedup Service](redis.md)

## Setup

The Redis deduplication service relies on the availability of a [Redis](https://redis.io/) key value store.
To use it, you need to add the dependency to your `pom.xml`:

```
<dependency>
	<groupId>net.wissel.salesforce</groupId>
	<artifactId>vertx-sfdc-redis</artifactId>
	<version>0.3.1</version>
</dependency>
```

**Note**: check[Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22net.wissel.salesforce%22) for the latest version of the plugin

## Configuration

All parameters for the service are configured in [sfdcOptions.json](/configure.md). A sample looks like this:

```
"dedupConfigurations": [
    {
    "instanceName": "redisDedup",
    "verticleName": "net.wissel.salesforce.vertx.dedup.RedisDedup",
    "serverURL": "some.address.local",
    "sfdcUser": "User",
    "sfdcPassword": "pwd"
    }
    ]
```

**Note**: You dont' want to specify username/password in the JSON file and rather have them pulled from the environment.
In this example the Verticle would try to read `redisDedup_sfdcUser` and `redisDedup_sfdcPassword` from the environment. 


## Operation
It will cache events for 24h, wich is also the maximum retention time for Salesforce platform events.
This ensures, event with an extended operational break, no event is processed twice.

The deduplication service will listen to `BUS_DEDUP_PREFIX+instanceName` in our case that would be:
`SFDC:Dedup:redisDedup`