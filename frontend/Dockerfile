FROM node:22-alpine AS build
WORKDIR /app
RUN corepack enable
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint-hardened.sh /docker-entrypoint-hardened.sh
RUN sed -i 's/\r$//' /docker-entrypoint-hardened.sh \
    && chown -R nginx:nginx /usr/share/nginx/html /etc/nginx \
    && chmod +x /docker-entrypoint-hardened.sh
USER nginx
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint-hardened.sh"]
