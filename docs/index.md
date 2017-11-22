# Salesforce vert.x integration

Collection of verticles that enable interaction with the Salesforce API

## Available Verticles

- Authentication
- Subscription to platform events
- Process platform events
- Main/Setup Verticle

## Setup

Add this dependencies to your `pom.xml`:

```
<dependency>
	<groupId>net.wissel.salesforce</groupId>
	<artifactId>net.wissel.salesforce.vertx</artifactId>
	<version>${sfdc.version}</version>
</dependency>
```
Then [configure the `sfdcoptions.json`](configure.md) file