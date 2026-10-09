# Site image validation

`validate_site.py` checks an already running site container and writes
`site-validation.json`. Set `SITE_BASE_URL` (default localhost:18085),
`GITHUB_SHA`, and `IMAGE_REFERENCE` when validating a published candidate.
The site-image workflow runs it before publication and again against the exact
published digest before generating the promotion handoff.
