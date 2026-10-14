# Change Log

## 0.5.0

Major modernization release with **breaking API changes**:

- Java baseline is now **Java 25** (was Java 8)
- Upgraded to **Vert.x 5.2.1** (code was written against Vert.x 3.x)
- Extension points are now Future-returning instead of completing a passed-in `Future`:
  - `AbstractAuth.login()` returns `Future<AuthInfo>`
  - `AbstractSFDCDedupVerticle.checkForDuplicate(JsonObject)` returns `Future<Void>` and must fail
    with a `DuplicateMessageException` for a confirmed duplicate - any other failure is read as the
    dedup store being unavailable, and the message is forwarded rather than dropped
  - `SFDCVerticle.stopListening()` returns `Future<Void>`
  - Verticles extend `VerticleBase` (`Future<?> start()`) instead of `AbstractVerticle`
- Logging switched from the removed `io.vertx.core.logging` API to SLF4J
  (bring your own backend; `fatal` calls became `error`)
- Dependencies: dropped joda-time (java.time), junit 4 + vertx-unit (JUnit Jupiter + vertx-junit5),
  added explicit jackson-databind; mustache.java and all build plugins to current versions
- Publishing migrated from the sunset OSSRH to the Sonatype Central Portal
- Bug fixes:
  - Dedup verticles never forwarded messages (broken `Future.future(handler)` usage) - dedup routing works now
  - RedisDedup glued the `NX EX 86400` options into the key, so duplicates were never detected and keys never expired
  - ApplicationStarter deployed every verticle twice and published the start signal twice
  - `PORT` environment override never reached the web server port; dedup entries missed env overrides (key typo)
  - RestConsumer treats its configured absolute URL correctly (`postAbs`) and no longer forces TLS on http URLs
  - Web server bind failures now fail the startup instead of passing silently
  - `JwtOAuth.login()` fails fast instead of hanging callers (still unimplemented)
  - CometD listener shutdown no longer hangs when the listener never started
- Test suite expanded from 1 to 47 tests, including a scripted mock Salesforce CometD server

## 0.3.4

- Added JWTOAuth authentication
- Updated configuration to hot-reload

## 0.3.3

- Updated vert.x to 3.5.2