# Repository conventions

## Commit messages

- Documentation-only changes (plan docs, AGENTS.md, README, etc.) in the **parent (root) repo** must have a `[skip ci]` prefix in the commit subject, e.g. `[skip ci] Update SoftEtherClient submodule: ...`.
- Do NOT use `[skip ci]` inside the `SoftEtherClient` submodule — it has no CI config, so the prefix serves no purpose there.
- Code changes must NOT use `[skip ci]`.

## Prompt files

- When asked to create a prompt file for copy/paste purposes (e.g. a kick-off prompt to feed to another agent), place it in a **temp folder** (`/tmp/opencode`), not in the repo.
