# Lessons

Patterns worth remembering, so the same mistake is not repeated.
Kept in `tasks/` rather than `docs/tasks/` on purpose: `docs/` is the
published GitHub Pages source, and internal notes should not ship with
the public documentation site.

## Codacy

### `.codacy/codacy.config.json` does not configure Codacy Cloud

It is a **local-only** config, used by `codacy-analysis` on this machine.
Committing or pushing it has no effect on the Cloud analysis that gates a
PR. To change what Cloud reports, either toggle the pattern on the
repository Code patterns page, or import the local config explicitly with
`codacy tools ... --import`.

**Why:** easy to waste a cycle committing a config file and expecting the
PR gate to change.

**How to apply:** when asked to silence a Codacy finding, first establish
*where* the rule lives (Cloud UI / coding standard / local config). Do not
assume a repo file can do it.

### A PMD ruleset file is also not a free win

Codacy does detect a root `ruleset.xml` for PMD, but only from the
**default branch**, and only once "use configuration file" is toggled on
for the tool in the UI. Enabling it hands the *entire* PMD pattern
selection to that file, so it is not a way to subtract a single rule.

### Codacy's PMD is 6.55.0, not PMD 7

The `toolInfo.uuid` on every issue identifies the tool. On this repo it is
PMD 6.55.0, even though Codacy also offers a separate "PMD7" (7.27.0)
tool. This matters: `extraAssertMethodNames` - which would teach
`JUnitTestsShouldIncludeAssert` that `testContext.completeNow()` counts as
a verification - only exists in PMD 7.1+. On PMD 6 the rule can only be
excluded, not taught.

**How to apply:** check the tool version via the Codacy API before
proposing a rule-configuration fix, instead of assuming latest.

### PMD findings can be read without a token

For a public repo the analysis API answers unauthenticated:

```
curl "https://app.codacy.com/api/v3/analysis/organizations/gh/<org>/repositories/<repo>/pull-requests/<pr>/issues?limit=100"
```

Filter on `deltaType == "Added"` for the new issues a PR introduces. The
GitHub check run only carries a count, never the list, and Codacy posts no
annotations. The endpoint is flaky - it returned partial pages on repeated
calls - so re-fetch until the count matches the check run before trusting
a tally.

### PMD's own escape hatches beat blanket suppression

`EmptyCatchBlock` has `allowExceptionNameRegex`, default
`^(ignored|expected)$`. Renaming the caught variable to `ignored` is the
rule's sanctioned way to express intent - no annotation, no config. Prefer
that kind of in-rule affordance over `@SuppressWarnings`, and reserve the
annotation for cases where the finding is genuinely inapplicable (such as
deliberate allocation inside a test loop).

## Build

### `gpg` is required by `mvn verify`

`maven-gpg-plugin` is bound to `verify`, so `mvn clean verify` fails on a
machine without `gpg` - on `master` as much as on any branch. Use
`-Dgpg.skip=true` for a local verification run, and do not report that
failure as a regression introduced by a branch.

### PMD 6 cannot parse this codebase locally

Since the Java 25 upgrade, `maven-pmd-plugin` 3.21.0 (PMD 6.55.0) raises
`PMDException` on every source file, so a local PMD run reproduces nothing
and validates nothing. Verify Codacy changes by pushing and reading the
re-analysed check run, which is the actual gate.
