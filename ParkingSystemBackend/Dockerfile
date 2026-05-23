FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV APP_BASE_URL=http://localhost:8080
ENV APP_WEBSOCKET_ALLOWED_ORIGINS=http://localhost:5175

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]