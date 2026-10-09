# Site candidate promotion

Sliding Tasks owns the static content and candidate image. Infra-platform owns
production desired state, promotion review and rollout.

The main-branch `Build Sliding Tasks site image` workflow validates a local image,
publishes an immutable multi-architecture candidate, pulls that candidate by
digest and validates its amd64 routes. Only then does it upload
`sliding-tasks-site-handoff-<revision>-<attempt>` and dispatch infra-platform's
`sliding-tasks-site-promotion.yml` with `run_id`.

The JSON handoff binds source repository/revision, run ID/attempt/URL, the exact
ECR digest, published platforms, source-commit change reference, and the passed
route receipt for that image. Checks cover both landing/privacy languages,
stylesheet, favicon, health, and rejection of unrelated paths. Arm64 is published
but route smoke validation runs on amd64.

The receiver downloads the artifact from this repository and verifies the
successful main-branch publication run before opening/updating a draft stack
promotion PR. It waits for the producer to finish because dispatch precedes
workflow completion. Reviewed stack merge triggers infra's existing deployment
workflow; publication or intake success alone does not prove deployment.

Credentials: `INFRA_PLATFORM_PROMOTION_TOKEN` grants the producer dispatch access
to the infra workflow. Infra's `SLIDING_TASKS_PROMOTION_PR_TOKEN` grants source
Actions read and infra Contents/Pull requests write. Neither changes AWS
publisher/deployment authority. Configure the receiver on main before enabling
the producer. Tokens are configured through GitHub secrets, never in artifacts.

Retries: dispatch the receiver with the same successful run ID to recover a
handoff failure. Artifact and immutable tags include run attempts; a source
workflow rerun publishes a distinct tag. Repeated intake of the same handoff is
idempotent. If source dispatch fails, inspect the already-uploaded artifact and
rerun the workflow or dispatch a subsequently successful producer run.
