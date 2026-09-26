# Sliding Tasks public site

Static product and privacy pages for `https://wastingnotime.org/sliding-tasks/`.
This repository owns the page content and portable image. `infra-platform`
owns the public route, selected image digest, and production rollout.

The app's policy source is [`docs/privacy-policy.md`](../../docs/privacy-policy.md).
Keep the offline app text and public HTML consistent when data behavior changes.

Build and check locally:

```bash
docker build -t sliding-tasks-site:local apps/site
docker run --rm -p 127.0.0.1:8080:80 sliding-tasks-site:local
```

The image serves `/sliding-tasks/`, `/sliding-tasks/privacy/`,
`/sliding-tasks/pt-br/`, `/sliding-tasks/pt-br/privacy/`,
`/health/live`, and `/health/ready`. All unrelated paths return 404.

The English landing page sends first-time visitors whose browser prefers
Portuguese to the pt-BR page. The language links save an explicit choice in
browser storage. Keep both privacy pages aligned with `docs/privacy-policy.md`.
The pages link `/sliding-tasks/favicon.svg` explicitly; the domain-level
favicon remains owned by the parent site.
