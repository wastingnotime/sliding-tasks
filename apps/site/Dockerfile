FROM nginx:1.31.3-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY sliding-tasks/ /usr/share/nginx/html/sliding-tasks/

# Bind every page to its stylesheet contents, including behind caches that ignore
# query strings. Keep styles.css available for pages cached from older releases.
RUN set -eu; \
    cd /usr/share/nginx/html/sliding-tasks; \
    stylesheet="styles.$(sha256sum styles.css | cut -d ' ' -f 1).css"; \
    cp styles.css "$stylesheet"; \
    find . -name '*.html' -exec sed -i "s|/sliding-tasks/styles.css|/sliding-tasks/$stylesheet|g" {} +

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 CMD wget -q -O - http://127.0.0.1/health/ready | grep -qx ready
