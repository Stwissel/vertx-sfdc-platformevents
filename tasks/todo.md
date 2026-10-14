# Upgrade: Java 25 / Vert.x 5.2.1 / deps / logging / tests

Plan reference: ~/.claude/plans/glowing-juggling-swan.md (approved 2026-10-13)

## Phase 1 — Build flip (poms)
- [x] Parent pom: release=25, plugin upgrades, junit-bom 6.1.3 + vertx-dependencies 5.2.1 BOM, Central Portal publishing, jacoco executions
- [x] Core pom: versionless vertx deps, +slf4j-api +jackson-databind, -joda -lorem, test stack vertx-junit5/jupiter/logback
- [x] Redis pom: versionless vertx-redis-client, same test stack
- [x] Park old test sources (deleted; in git history)
- [x] Gate: mvn dependency:resolve — all new deps resolve at target versions

## Phase 2 — Core main-source migration
- [x] Logging swap to SLF4J everywhere (fatal→error, error(t)→error(msg,t))
- [x] Base contracts: SFDCVerticle, AbstractSFDCVerticle→VerticleBase, AbstractAuth, AbstractSFDCConsumer, AbstractSFDCDedupVerticle (+forwarding fix)
- [x] Leaves: Basic, SoapApi(+seam), JwtOAuth(fail-fast), MemoryDedup, ConsoleConsumer, RestConsumer(postAbs+retry shape), WebSocketConsumer(SockJS subRouter), CometD(+seam, stop drain+guard), CometDChangeObject, Runner, ApplicationStarter(compose chain, double-deploy fix, env+port seams, java.time)
- [x] Gate: mvn -pl vertx-sfdc-core compile — BUILD SUCCESS (note: joda was only in ApplicationStarter, not Utils)

## Phase 3 — Pure unit tests + fixtures
- [x] logback-test.xml, TestUtilities fixture
- [x] UtilsTest, AuthInfoCodecTest, ConfigTest (replaces main() demos), EnvironmentOverrideTest
- [x] Gate: 25 tests green

## Phase 4 — Verticle tests
- [x] BasicAuthTest, AbstractAuthTest (CountingAuth), MemoryDedupTest (direct + bus-flow regression)
- [x] Gate: 34 tests green

## Phase 5 — HTTP-backed tests
- [x] SoapApiTest (mock server, port 0), EchoServer fixture, RestConsumerTest (retry, mustache, plain-http)
- [x] Gate: 40 tests green

## Phase 6 — CometD + end-to-end
- [x] ChaosMonkeyCometDServer rebuild (scripted, port 0, chaos math fix)
- [x] CometDFlowTest (flow, dedup variant, stop mid-poll)
- [x] ApplicationStarterTest rewrite (port 0, Verticles size 1)
- [x] Gate: 44 tests green

## Phase 7 — Redis module
- [x] RedisDedup rewrite (RedisAPI, NX/EX fix, HexFormat, storeKey seam)
- [x] RedisDedupTest (seam-based, no docker)
- [x] Gate: full reactor mvn verify green (47 tests)

## Phase 8 — Release hygiene + docs
- [x] Source + javadoc jars build on JDK 25 (part of verify)
- [x] README (Java 25 requirement note), docs/changelog.md 0.5.0 entry, docs/index.md (dead Codeship badge removed, version), docs/contribute/auth.md + dedup.md (new API samples)
- [x] settings.xml: ossrh server → central (CENTRAL_USER_TOKEN / CENTRAL_PWD_TOKEN env vars)

## Review

- Final state: `mvn clean verify` green on Temurin JDK 25.0.3 / Maven 3.9.10 — 47 tests (was 1), jacoco 0.8.15 reports generated, source+javadoc jars build.
- Versions: Java 25, Vert.x 5.2.1 (BOM), JUnit Jupiter 6.1.3 + vertx-junit5, SLF4J 2.0.20 (+logback 1.6.5 test), mustache 0.9.14, jackson-databind via vertx BOM; joda-time and lorem removed; all maven plugins current; Central Portal publishing (central-publishing-maven-plugin 0.11.0).
- Breaking API (0.5.0): VerticleBase lifecycle; `Future<AuthInfo> login()`, `Future<Void> checkForDuplicate(JsonObject)`, `Future<Void> stopListening()`.
- Bug fixes shipped (each pinned by a test): double verticle deployment, dead dedup forwarding, Redis NX/EX-in-key, env PORT key mismatch + dedupConfigruations typo, RestConsumer postAbs/ssl, JwtOAuth hang→fast fail, CometD stop-hang guard, silent web server bind failure. Logging hygiene: no credential/payload logging at info+.
- Verification gaps (acceptable): RedisDedup tested via seam, not against a live Redis; CometD reconnect/replay still unimplemented (out of scope, TODOs remain).
- Release prerequisite: create a Sonatype Central Portal token and expose it as CENTRAL_USER_TOKEN/CENTRAL_PWD_TOKEN (settings.xml now expects server id `central`).
