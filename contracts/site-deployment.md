# Sliding Tasks static site deployment

## Ownership

This repository owns the public content, Cloudflare Worker route, and rollout
for `https://wastingnotime.org/sliding-tasks/`. The site is static and has no
application API, secrets, writable volumes, or runtime environment variables.

## Runtime interface

- The Worker matches the exact `/sliding-tasks` path and its descendants.
- Workers Static Assets mounts the generated contents of
  `apps/site/sliding-tasks/` at `/sliding-tasks`.
- The public pages are `/sliding-tasks/`, `/sliding-tasks/privacy/`,
  `/sliding-tasks/pt-br/`, and `/sliding-tasks/pt-br/privacy/`.
- `htmlHandling: auto-trailing-slash` serves canonical directory pages.
- Legacy cached HTML may reference `/sliding-tasks/styles.<sha256>.css`; the
  Worker routes these requests to the current `styles.css`, preserving the
  previous nginx fallback behavior.
- The page links `/sliding-tasks/favicon.svg`; the domain-level favicon remains
  owned by the parent site.

## Build and publication

`.github/workflows/deploy-site.yml` runs `cf build` for pull requests and main
branch changes. Deployment is manually dispatched from `main` and requires the
`CLOUDFLARE_API_TOKEN` repository secret with permission to deploy Workers and
routes in the WNT Cloudflare account. Deploy only after the `wastingnotime.org`
zone is active.

Static asset requests and storage have no additional Workers charge under the
published plan. Cloudflare currently requires the public static prefix feature
supported by the pinned Cloudflare CLI and Vite plugin versions in
`package.json`.
