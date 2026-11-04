import { defineConfig, triggers } from "cf/config";

export default defineConfig({
  accountId: "1ae390f76f0a4b24c47f58f6e1c0e9b4",
  worker: {
    name: "sliding-tasks-public-site",
    compatibilityDate: "2026-11-03",
    assets: {
      basePath: "/sliding-tasks",
      htmlHandling: "auto-trailing-slash",
    },
    triggers: [
      triggers.fetch({
        pattern: "wastingnotime.org/sliding-tasks",
        zone: "wastingnotime.org",
      }),
      triggers.fetch({
        pattern: "wastingnotime.org/sliding-tasks/*",
        zone: "wastingnotime.org",
      }),
    ],
  },
});
