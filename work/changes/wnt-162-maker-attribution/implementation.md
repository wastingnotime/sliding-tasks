# WNT-162 — public-site maker attribution

Issue: https://linear.app/wastingnotime/issue/WNT-162

All four EN/PT-BR landing/privacy footers now link the localized `made by WNT`
/ `feito por WNT` signature to https://wastingnotime.org/. Live text uses the
product font and muted `#645f5a` color (6.00:1 against cream per the kit).
The decorative 24 × 18 mark has an 8px text gap, 3px outer padding for clear
space, and a minimum 24px link target. Underlining, localized accessible names,
native keyboard behavior, visible focus and forced-colors focus are supplied.
Footer groups wrap independently. Portuguese privacy headings may break long
words to prevent overflow at 320px after the web font loads.

## Asset provenance

- Approved kit: wnt-design Forward W **v1.0.0**, WNT-160.
- Source: `assets/identity/v1.0.0/exports/mark-dark.svg` in
  https://github.com/wastingnotime/wnt-design/tree/main/assets/identity/v1.0.0
- Source SHA-256: `ded2a636f42a1a06bc7d5f76653dea7bedff4f87133aa8253becdacdd8fc6ccb`.
- Consumer: `/sliding-tasks/assets/wnt-forward-w-v1.0.0-muted.svg`.
- Only adaptation: solid fill `#101813` → product muted `#645f5a`;
  geometry, viewBox and proportions preserved.
- Consumer SHA-256: `f9676ecbfc5d94f8fb37fc2b736c05c6877ec4bb0ae91098d7db97828c2ea72a`.

## Local validation

Built the nginx image and passed the existing integration route, stylesheet/hash,
cache fallback, health and unrelated-path checks. Receipt: `site-validation.json`.
Eight Chromium scenarios cover four routes at 1280px and 320px: loaded mark,
localized accessible link name, no horizontal overflow, Tab/Shift+Tab focus,
visible outline and Enter activation to the parent URL (navigation intercepted).
Screenshots are local candidate evidence, including keyboard focus. Accessible
name checked through markup; no manual screen-reader session was performed.

## Delivery

Source PR pending review. Main-branch merge builds/publishes the immutable image
and automatically opens the infra promotion PR under `contracts/site-promotion.md`.
Production rollout requires that reviewed infra promotion. Deployed screenshots
and the deployed image digest must be appended after rollout; local screenshots
do not satisfy that remaining WNT-162 acceptance item.
