# Imagem unica: a API serve tambem o front compilado (mesma origem, sem CORS).
# O contexto do build e a pasta pai, com trainingapp-api/ e trainingapp-web/ lado a lado:
#   docker compose -f compose.prod.yaml up -d --build   (rodar dentro de trainingapp-api/)

FROM node:22-slim AS web
WORKDIR /web
COPY trainingapp-web/package.json trainingapp-web/package-lock.json ./
RUN npm ci
COPY trainingapp-web/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /api
COPY trainingapp-api/pom.xml ./
COPY trainingapp-api/src src
# Os testes rodam na maquina de desenvolvimento, nao no servidor.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=api /api/target/*.jar app.jar
COPY --from=web /web/dist/trainingapp-web/browser web
ENV TRAININGAPP_WEB_PASTA=/app/web \
    JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70
USER 1000
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
