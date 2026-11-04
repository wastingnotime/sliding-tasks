# Sliding Tasks public site

Static product and privacy pages for `https://wastingnotime.org/sliding-tasks/`.
This repository owns the page content, Cloudflare Worker route, and production
rollout.

The app's policy source is [`docs/privacy-policy.md`](../../docs/privacy-policy.md).
Keep the offline app text and public HTML consistent when data behavior changes.

Build locally:

```bash
npm ci
npm run build
```

The Worker serves `/sliding-tasks/`, `/sliding-tasks/privacy/`,
`/sliding-tasks/pt-br/`, and `/sliding-tasks/pt-br/privacy/` from the static
asset directory. Its route matches only `/sliding-tasks` and descendants.

The English landing page sends first-time visitors whose browser prefers
Portuguese to the pt-BR page. The language links save an explicit choice in
browser storage. Keep both privacy pages aligned with `docs/privacy-policy.md`.
The pages link `/sliding-tasks/favicon.svg` explicitly; the domain-level
favicon remains owned by the parent site.

The video poster and iframe fill the same responsive 16:9 frame before and
after loading.

## Publication

Pull requests run `cf build` without deployment. The manual deployment workflow
requires the `CLOUDFLARE_API_TOKEN` repository secret and deploys from `main`
after the `wastingnotime.org` Cloudflare zone is active. Static asset requests
are free and unlimited under Workers Static Assets pricing.
