[ ![Codeship Status for Stwissel/vertx-sfdc-platformevents](https://app.codeship.com/projects/65890500-b1cd-0135-81e1-7645507f84f6/status?branch=master)](https://app.codeship.com/projects/257955)

# [Salesforce vert.x integration](https://stwissel.github.io/vertx-sfdc-platformevents)

Collection of verticles that enable interaction with the Salesforce API

## Current released version

The current released version is `0.3.0`<br />
Development might have newer snapshots

## Available Verticles

- Authentication
- Subscription to platform events
- Process platform events
- Main/Setup Verticle
- Deduplication Verticle (messages already processed get discarded

Check the [full documentation](https://stwissel.github.io/vertx-sfdc-platformevents)!

## Setup and Configuration

Add this dependencies to your `pom.xml`:

```
<dependency>
	<groupId>net.wissel.salesforce</groupId>
	<artifactId>vertx-sfdc-core</artifactId>
	<version>0.3.0</version>
</dependency>
<!-- Optional for deduplication service -->
<dependency>
	<groupId>net.wissel.salesforce</groupId>
	<artifactId>vertx-sfdc-redis</artifactId>
	<version>0.3.0</version>
</dependency>
```
Check the [full documentation](https://stwissel.github.io/vertx-sfdc-platformevents)!

Artifacts on [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22net.wissel.salesforce.vertx%22)

## Issues and feedback

I would love to hear about your implementation or ideas or challenges, so 
[open an issue on GitHub](https://github.com/Stwissel/vertx-sfdc-platformevents/issues) - even for sharing success. Don't be shy!
