# Sliding Tasks contracts

This directory is the model-release boundary for the simulation. It describes
stable domain semantics without exporting Python implementation details or
committing the repository to a mobile, API, or persistence technology.

## Surface map

- `sliding-tasks-model.md` — accepted model contract.

The contract was accepted after model EGD review on 2026-09-18. Consumers may
build technology adapters from it, while experimental projection fields remain
explicitly marked in the contract.
