# Repository conventions

## Commit messages

- Documentation-only changes (plan docs, AGENTS.md, README, etc.) in the **parent (root) repo** must have a `[skip ci]` prefix in the commit subject, e.g. `[skip ci] Update SoftEtherClient submodule: ...`.
- This applies to commits that update the `SoftEtherClient` submodule pointer when the submodule change is doc-only. Doc-only commits inside the `SoftEtherClient` submodule itself may also use `[skip ci]` when they would otherwise trigger CI.
- Code changes must NOT use `[skip ci]`.
