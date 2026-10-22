FROM node:22-bookworm-slim AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ENV VITE_API_BASE_URL=/api
RUN npm run build

FROM eclipse-temurin:21-jdk-jammy AS backend
WORKDIR /build/backend
COPY backend/ ./
RUN chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends nginx gettext-base tini \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 10001 --create-home app \
    && mkdir -p /app /tmp/nginx/client /tmp/nginx/proxy \
    && chown -R app:app /app /tmp/nginx
WORKDIR /app
COPY --from=backend /build/backend/target/lessonpt-0.0.1-SNAPSHOT.jar /app/lessonpt.jar
COPY --from=frontend /build/frontend/dist/ /app/public/
COPY deploy/render/nginx.conf.template /app/nginx.conf.template
COPY deploy/render/proxy.conf /app/proxy.conf
COPY deploy/render/start.sh /app/start.sh
RUN chmod 755 /app/start.sh
ENV PORT=10000 \
    LESSONPT_PROFILE=prod,render \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"
USER app
EXPOSE 10000
ENTRYPOINT ["/usr/bin/tini", "--", "/app/start.sh"]
