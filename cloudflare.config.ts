import { bindings, defineConfig, triggers } from "cf/config";

export default defineConfig({
  accountId: "1ae390f76f0a4b24c47f58f6e1c0e9b4",
  worker: {
    name: "sliding-tasks-public-site",
    entrypoint: "./src/index.ts",
    compatibilityDate: "2026-11-03",
    assets: {
      htmlHandling: "auto-trailing-slash",
      runWorkerFirst: true,
    },
    env: {
      ASSETS: bindings.assets(),
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
