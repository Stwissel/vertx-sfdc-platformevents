# PR #70 review feedback

Addresses the 7 unresolved CodeRabbit threads on PR #70. Each finding was
verified against the current code before any change; none were taken on
faith from the review text.

Branch: `fix/pr70-review-feedback`, based on `upgrade_vertx`.

## Findings and fixes

- [x] 1. `docs/index.md` (Minor) - setup examples still install `0.3.3`
      while line 4 announces `0.5.0`. Verified: lines 82 and 88.
      Fix: bump both to `0.5.0`.

- [x] 2. `AbstractAuth` (Minor, performance) - the cache stays empty until
      `login()` resolves, so every concurrent auth request starts its own
      login. With `SoapApi` that is one SOAP session per caller. Verified
      at line 93.
      Fix: keep the in-flight `Future<AuthInfo>` and hand it to every
      request that arrives while it is pending.

- [x] 3. `SoapApi.resultOfAuthentication` (Minor) - parses the body for
      every status code, so a non-2xx response loses its status and
      surfaces as a generic failure. Verified at line 197.
      Fix: fail with the status code before parsing.

- [x] 4. `RestConsumer` (Major) - `initWebPostRequest` dereferences
      `this.authInfo.sessionToken`. `startListening` requests auth without
      waiting, so a message arriving first throws NPE, drops the payload
      and schedules no retry. Same when `authName` is null. Verified at
      line 202.
      Fix: send the auth header only when present, and re-enqueue the
      payload while auth is still missing.

- [x] 5. `AbstractSFDCDedupVerticle` (Major) - a failed Redis command and a
      genuine duplicate both arrive as a failed future, so a Redis outage
      is logged as "Dropped duplicate" and the event is lost. Verified at
      line 118.
      Fix: a distinct `DuplicateMessageException` for real duplicates;
      storage failures log the cause and forward (fail open) rather than
      silently dropping the event.

- [x] 6. `CometD` (Major) - `startListening` sets `listening = true` while
      auth runs async. Terminal failures never set `shutdownCompleted`, so
      `completeWhenDrained` polls forever and `stopListening()` never
      completes. Verified: 7 such paths (lines 75, 304, 323, 329, 343,
      360, 375) against the long-poll handlers, which do set it.
      Fix: one `terminalFailure` helper that logs and marks the listener
      stopped; use it on every terminal path.

- [x] 7. `RedisDedup` (Major) - the password is concatenated into the
      `redis://` URI, so a password containing `@` or `#` changes how the
      connection string parses and auth fails. Verified at line 97.
      Fix: build the URI without credentials and pass the password via
      `RedisOptions.setPassword` (confirmed present in Vert.x 5.2.1).

## Verification

- [x] Regression test per behavioural fix (2, 4, 5, 6, 7)
- [x] `mvn clean verify -Dgpg.skip=true` green
- [x] Test count rises from 47

## Review

All 7 threads addressed. `mvn clean verify -Dgpg.skip=true` green on Temurin
JDK 25.0.3 / Maven 3.9.11: 53 tests (49 core + 4 redis), up from 47.

Each behavioural fix was proven by reverting it and watching the new test
fail, rather than assuming the test covers it:

| test | against unfixed code |
| --- | --- |
| `concurrentRequestsShareOneLogin` | 5 logins instead of 1 |
| `consumerWithoutAuthPostsWithoutAuthHeader` | timeout - NPE drops the payload, no retry |
| `stopListeningCompletesAfterStartupFailure` | timeout - `stopListening()` never completes |
| `storeFailureForwardsInsteadOfDropping` | "Event lost when the dedup store failed" |

`passwordTravelsOutsideTheConnectionUri` asserts the endpoint directly, so
it needs no such check.

### Notes for the reviewer

- The first attempt at the `AbstractAuth` fix was wrong in a way worth
  recording: `this.pendingLogin = this.login().andThen(r -> this.pendingLogin = null)`
  breaks when `login()` returns an already-completed future, because
  `andThen` runs its callback *during* the call, before the assignment
  lands. The field was nulled first and then overwritten with a completed
  future that was reused forever. Two existing tests caught it. The field
  is now published before the reset handler is registered, and a local
  reference keeps each request attached to the right future.
- `DuplicateMessageException` is a new public type in
  `net.wissel.salesforce.vertx.dedup` and part of the `checkForDuplicate`
  contract: implementations outside this repo that signal duplicates with
  a plain failure will now be read as a store outage and have their
  messages forwarded. Worth a line in the 0.5.0 changelog if that matters.
- The dedup store-failure policy is **fail open**: on an unknown outcome
  the event is forwarded rather than dropped, on the grounds that a
  duplicate downstream is recoverable and a lost event is not. That is a
  behavioural choice, not a forced one - say so if you want fail-closed or
  a config switch instead.
