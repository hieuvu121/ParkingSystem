FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/parkingSystem
ENV PGDB_USERNAME=postgres
ENV PGDB_PASSWORD=postgres
ENV MAIL_HOST=smtp.gmail.com
ENV MAIL_USERNAME=
ENV MAIL_PASSWORD=
ENV JWT_SECRET=
ENV OPEN_AI_KEY=
ENV OPENWEATHER_API_KEY=
ENV APP_BASE_URL=http://localhost:8080
ENV APP_WEBSOCKET_ALLOWED_ORIGINS=http://localhost:5175

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
