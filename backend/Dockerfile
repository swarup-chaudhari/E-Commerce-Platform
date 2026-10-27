# Build any service:  docker build --build-arg MODULE=order-service -t ecommerce/order-service:1.0.0 .
ARG MODULE=order-service

FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY service-registry/pom.xml service-registry/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -pl ${MODULE} -am dependency:go-offline || true
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:17-jre
ARG MODULE
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
