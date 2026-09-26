# Localized Sliding Tasks site promotion

- Source revision: `ca8d4d20fa610d4a14be9d4a53b2af0173778f03` (`feat: localize app and safeguard task migration`).
- Candidate workflow: https://github.com/wastingnotime/sliding-tasks/actions/runs/36257511807 (successful).
- Immutable image: `590183855481.dkr.ecr.us-east-1.amazonaws.com/sliding-tasks-site@sha256:652f0d14077fa5f265cc9705677cd1b0bb66c17a69973b3cde7e057e336334fd`.
- Route decision: preserve `/sliding-tasks` and descendants on `wastingnotime.org` with the existing priority 130.

The site now serves English and Brazilian Portuguese landing and privacy pages.
The English landing page offers the Portuguese page to browsers that prefer it,
and explicit language selection is remembered. The Sliding Tasks routes use
their own favicon; the domain-level favicon is unaffected. The privacy text
keeps the existing data and Crashlytics alpha disclosures.

Validation: the site image built locally, the four page routes and health
routes were checked, and the multi-architecture candidate build and publish
completed in CI. Production rollout is owned by `infra-platform`; verify the
public English and Portuguese routes, privacy pages, and favicon after it
deploys.
