FROM nginx:1.31.3-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY sliding-tasks/ /usr/share/nginx/html/sliding-tasks/

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 CMD wget -q -O - http://127.0.0.1/health/ready | grep -qx ready
