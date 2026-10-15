# Production image for LINDA BINTI backend + NGO dashboard (no Android SDK required).
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build
RUN apk add --no-cache bash
COPY deploy/settings.gradle.kts ./settings.gradle.kts
COPY gradle ./gradle
COPY gradlew .
COPY gradle.properties .
COPY backend ./backend
RUN chmod +x gradlew && ./gradlew :backend:installDist --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache sqlite-libs wget && mkdir -p /var/data
COPY --from=build /build/backend/build/install/backend /app
ENV LINDA_HOST=0.0.0.0
ENV LINDA_DB_URL=jdbc:sqlite:/var/data/linda_binti_backend.db
ENV LINDA_PUBLIC_URL=https://lindabinti.com
ENV LINDA_ENV=production
ENV LINDA_DEMO_SEED=0
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT:-8080}/v1/health" | grep -q '"status":"ok"' || exit 1
CMD ["/app/bin/backend"]
