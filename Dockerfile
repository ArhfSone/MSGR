# Unified image for all messenger microservices (gateway, auth, user, chat, file).
# Runtime service is selected via MSGR_SERVICE env var (see docker/entrypoint.sh).

FROM maven:3.9-eclipse-temurin-21 AS build-gateway
WORKDIR /build/msgr-gateway
COPY msgr-gateway/pom.xml .
COPY msgr-gateway/src ./src
RUN mvn -B clean package -DskipTests

FROM maven:3.9-eclipse-temurin-21 AS build-auth
WORKDIR /build/msgr-auth
COPY msgr-auth/pom.xml .
COPY msgr-auth/src ./src
RUN mvn -B clean package -DskipTests

FROM maven:3.9-eclipse-temurin-21 AS build-user
WORKDIR /build/msgr-user
COPY msgr-user/pom.xml .
COPY msgr-user/src ./src
RUN mvn -B clean package -DskipTests

FROM maven:3.9-eclipse-temurin-21 AS build-chat
WORKDIR /build/msgr-chat
COPY msgr-chat/pom.xml .
COPY msgr-chat/src ./src
RUN mvn -B clean package -DskipTests

FROM maven:3.9-eclipse-temurin-21 AS build-file
WORKDIR /build/msgr-file
COPY msgr-file/pom.xml .
COPY msgr-file/src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build-gateway /build/msgr-gateway/target/*.jar /app/gateway.jar
COPY --from=build-auth /build/msgr-auth/target/*.jar /app/auth.jar
COPY --from=build-user /build/msgr-user/target/*.jar /app/user.jar
COPY --from=build-chat /build/msgr-chat/target/*.jar /app/chat.jar
COPY --from=build-file /build/msgr-file/target/*.jar /app/file.jar

COPY docker/entrypoint.sh /entrypoint.sh
RUN sed -i 's/\r$//' /entrypoint.sh && chmod +x /entrypoint.sh && mkdir -p /app/uploads

EXPOSE 8080 8081 8082 8083 8084

ENTRYPOINT ["sh", "/entrypoint.sh"]
