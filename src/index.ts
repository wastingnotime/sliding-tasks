interface Env {
  ASSETS: Fetcher;
}

const legacyStylesheetPath =
  /^\/sliding-tasks\/styles\.[0-9a-f]{64}\.css$/;
const sitePrefix = "/sliding-tasks";

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (legacyStylesheetPath.test(url.pathname)) {
      url.pathname = "/styles.css";
    } else if (url.pathname === sitePrefix || url.pathname.startsWith(`${sitePrefix}/`)) {
      url.pathname = url.pathname.slice(sitePrefix.length) || "/";
    } else {
      return new Response("Not Found", { status: 404 });
    }

    return env.ASSETS.fetch(new Request(url, request));
  },
} satisfies ExportedHandler<Env>;
