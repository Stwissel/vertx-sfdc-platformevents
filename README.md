[ ![Codeship Status for Stwissel/vertx-sfdc-platformevents](https://app.codeship.com/projects/65890500-b1cd-0135-81e1-7645507f84f6/status?branch=master)](https://app.codeship.com/projects/257955)
[![Known Vulnerabilities](https://snyk.io/test/github/Stwissel/vertx-sfdc-platformevents/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/Stwissel/vertx-sfdc-platformevents?targetFile=pom.xml)
[![Known Vulnerabilities](https://snyk.io/test/github/Stwissel/vertx-sfdc-platformevents/badge.svg?targetFile=vertx-sfdc-core%2Fpom.xml)](https://snyk.io/test/github/Stwissel/vertx-sfdc-platformevents?targetFile=vertx-sfdc-core%2Fpom.xml)
[![Known Vulnerabilities](https://snyk.io/test/github/Stwissel/vertx-sfdc-platformevents/badge.svg?targetFile=vertx-sfdc-redis%2Fpom.xml)](https://snyk.io/test/github/Stwissel/vertx-sfdc-platformevents?targetFile=vertx-sfdc-redis%2Fpom.xml)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b6619f054b1d4535a9c9a5235928da4c)](https://www.codacy.com/app/Stwissel/vertx-sfdc-platformevents?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Stwissel/vertx-sfdc-platformevents&amp;utm_campaign=Badge_Grade)


# [Salesforce vert.x integration](https://stwissel.github.io/vertx-sfdc-platformevents)

Collection of verticles that enable interaction with the Salesforce API
Check the [full documentation](https://stwissel.github.io/vertx-sfdc-platformevents)!

## Current released version

The current released version is `0.3.3`<br />
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
	<version>0.3.3</version>
</dependency>
<!-- Optional for deduplication service if Redis is your choice -->
<dependency>
	<groupId>net.wissel.salesforce</groupId>
	<artifactId>vertx-sfdc-redis</artifactId>
	<version>0.3.3</version>
</dependency>
```
Check the [full documentation](https://stwissel.github.io/vertx-sfdc-platformevents)!

Artifacts on [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22net.wissel.salesforce%22)

## Change Log
List of [changes](https://stwissel.github.io/vertx-sfdc-platformevents/changelog.html) in reverse order
## Issues and feedback

I would love to hear about your implementation or ideas or challenges, so 
[open an issue on GitHub](https://github.com/Stwissel/vertx-sfdc-platformevents/issues) - even for sharing success. Don't be shy!
