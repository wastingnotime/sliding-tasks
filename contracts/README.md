# Sliding Tasks contracts

This directory contains stable contracts exported by the repository, including
the task model and the user-facing backup format.

## Surface map

- `sliding-tasks-model.md` — accepted model contract.
- `task-backup-format.md` — versioned JSON format used by task exports and backups.

The contract was accepted after model EGD review on 2026-09-18. Consumers may
build technology adapters from it, while experimental projection fields remain
explicitly marked in the contract.

- [Site candidate promotion](site-promotion.md): validated immutable image handoff and infra-owned draft promotion intake.
