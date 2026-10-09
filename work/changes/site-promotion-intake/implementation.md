# Site promotion intake

Campaign: https://github.com/wastingnotime/sliding-tasks/issues/8
Receiver: https://github.com/wastingnotime/infra-platform/issues/732

Use the Wishlist run-ID/artifact pattern with automatic producer dispatch and
infra-owned draft promotion PRs. Credentials are configured in both repositories.
Receiver must merge first. Production promotion remains a reviewed stack change.

Validation: site image built locally; both language landing/privacy routes,
stylesheet, favicon, health endpoints and unrelated-path 404 checks passed.
Producer/receiver actionlint passed. Receipt binds the published image digest.
Live end-to-end activation requires the two workflow PRs on main.

Source issue-open/close notification wrappers were missing; tracked separately
in https://github.com/wastingnotime/sliding-tasks/issues/9. A campaign notice was
posted to #coordination via the coordinate-campaign skill.
