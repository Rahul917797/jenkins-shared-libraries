FROM eclipse-temurin:17.0.8_7-jdk-jammy
WORKDIR /app
COPY target/my-app-1.0-SNAPSHOT.jar /app/my-app.jar
EXPOSE 8081
CMD ["java", "-jar", "/app/my-app.jar"]
