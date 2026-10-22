# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 lessonpt \
    && useradd --system --uid 10001 --gid lessonpt --home-dir /var/lessonpt --create-home lessonpt
WORKDIR /app
COPY --from=build /src/target/lessonpt-0.0.1-SNAPSHOT.jar /app/app.jar
COPY scripts/render-entrypoint.sh /app/render-entrypoint.sh
RUN sed -i 's/\r$//' /app/render-entrypoint.sh \
    && chmod 755 /app/render-entrypoint.sh \
    && chown -R lessonpt:lessonpt /app /var/lessonpt
USER lessonpt
ENV LESSONPT_PROFILE=prod \
    LESSONPT_RUNTIME_DIR=/var/lessonpt \
    PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/render-entrypoint.sh"]
