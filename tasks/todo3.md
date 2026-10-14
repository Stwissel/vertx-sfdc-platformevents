# JavaDoc pass over all Java sources

Stacked on `fix/pr70-review-feedback`, because several of the classes
documented here (such as `DuplicateMessageException`) only exist on that
branch.

## Approach

`javadoc`'s own doclint was used as the worklist rather than reading files
and guessing: `mvn javadoc:javadoc` reported **102 warnings** across 19
files at the start. That gave an objective definition of done, and each
warning names the exact member that lacks documentation.

- [x] Doc blocks that opened straight into `@return` / `@param` got a main
      description (doclint's "no main description")
- [x] Undocumented members got one (doclint's "no comment"): the 32
      constants in `Constants`, the protected state in the abstract bases,
      the enum constants in `JsonMergeOptions`, the protected hooks in
      `CometD`, `AbstractAuth`, `AbstractSFDCConsumer`
- [x] Class-level descriptions written for the classes that carried only
      an `@author` tag: `CometD`, `Utils`, `AuthInfo`, `AuthInfoCodec`,
      `SoapApi`, `JwtOAuth`, `RedisDedup`, `AbstractSFDCConsumer`,
      `ConsoleConsumer`, `WebSocketConsumer`, `CometDChangeObject`
- [x] 24 explicit constructors replacing implicit default ones, each
      documented. `Utils` and `Runner` got **private** constructors, since
      both are static-only - that also settles PMD's `UseUtilityClass`
- [x] `Utils.mergeObjects` was missing `@param mergeHow` and had two bare
      `@param` tags
- [x] Class-level docs for the 4 test classes that had none
- [x] Two pre-existing typos fixed in `AppConfig` ("Consumer config to
      ass" -> "to add")

## Verification

- [x] `mvn clean verify -Dgpg.skip=true` green: **53 tests**, 0 failures
- [x] **javadoc warnings 102 -> 0**
- [x] No behavioural change: comments, 24 constructor bodies that do
      nothing, and one annotation reordering

## Review

Three placement bugs were caught and fixed during the pass, all from the
same cause - a doc block or constructor has to go in front of a member's
annotations, not between them:

1. `RestConsumer` - the generated constructor landed inside the nested
   `PostRetry` class, which failed to compile ("invalid method
   declaration; return type required"). Moved to class level.
2. `ConsoleConsumer` - the constructor was inserted between `@Override`
   and its method, so the annotation bound to the constructor
   ("annotation interface not applicable to this kind of declaration").
   Hoisted above, and the method it displaced is now documented too.
3. `Utils.mappifyJsonArray` - the doc block went *below*
   `@SuppressWarnings`, where javadoc ignores it, so the warning survived
   even though the comment was visibly present. Hoisted above the
   annotation.

Both compile failures were caught by the build; the third only by the
warning count not reaching zero, which is the argument for treating the
count as the completion criterion rather than eyeballing the files.

Constructors were deliberately placed after the field declarations, not
at the top of each class, to avoid re-triggering the
`FieldDeclarationsShouldBeAtStartOfClass` rule that was cleared earlier on
`ApplicationStarter`.
