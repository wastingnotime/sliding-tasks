import { cloudflare } from "@cloudflare/vite-plugin";
import { defineConfig } from "vite";

export default defineConfig({
  publicDir: "apps/site/sliding-tasks",
  plugins: [cloudflare()],
});
