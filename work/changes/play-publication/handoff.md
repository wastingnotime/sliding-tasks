# Sliding Tasks Play publication handoff

Campaign: https://github.com/wastingnotime/sliding-tasks/issues/1

This repository owns the Android package `org.wastingnotime.slidingtasks`,
the offline privacy screen, the policy source in `docs/privacy-policy.md`, and
the static site image under `apps/site`. The product and privacy routes are
`/sliding-tasks/` and `/sliding-tasks/privacy/`.

`infra-platform` owns the ECR repository, selected immutable image digest,
Swarm service, public Traefik route, and rollout evidence. The site image
intentionally returns 404 for paths outside its product routes and health
checks so it cannot become the root-domain site by accident.

The Play Console app can be created using the matching package ID. The owner
must personally review Play's policy, signing, and export declarations.
