FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
RUN mvn dependency:go-offline -q || true

COPY src ./src

RUN mvn clean package -DskipTests -q && \
    mv target/ai-flow-enterprise-*.jar app.jar && \
    rm -rf target ~/.m2

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S aiflow && adduser -S aiflow -G aiflow

WORKDIR /app

COPY --from=build /app/app.jar app.jar

RUN chown -R aiflow:aiflow /app

USER aiflow

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
