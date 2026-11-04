interface Env {
  ASSETS: Fetcher;
}

const legacyStylesheetPath =
  /^\/sliding-tasks\/styles\.[0-9a-f]{64}\.css$/;

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (legacyStylesheetPath.test(url.pathname)) {
      url.pathname = "/sliding-tasks/styles.css";
      return env.ASSETS.fetch(new Request(url, request));
    }

    return env.ASSETS.fetch(request);
  },
} satisfies ExportedHandler<Env>;
