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

The image build gives the shared stylesheet a content-hashed filename and updates
all landing and privacy pages to use it. This keeps new sections and headings
paired with their CSS when visitors or the CDN have cached an older stylesheet.
Edit `sliding-tasks/styles.css` normally; the build generates the URL. The video
poster and iframe fill the same responsive 16:9 frame before and after loading.
Requests for a stylesheet hash from an older image fall back to the current CSS
so previously cached HTML remains styled after a rollout.

## Candidate delivery

See [the site promotion contract](../../contracts/site-promotion.md).
Successful main-branch site builds publish and validate a digest-pinned image,
then automatically dispatch infra-platform intake. Intake opens a draft promotion
PR; review and merge start the infra-owned rollout. The receiver must be present
on infra-platform main before the producer workflow is merged.
