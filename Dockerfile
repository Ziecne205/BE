# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Tai dependency truoc de tan dung cache Docker layer
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render inject PORT; Spring doc PORT tu application.yml
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
