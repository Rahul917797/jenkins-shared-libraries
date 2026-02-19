FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY target/my-app-1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--server.port=8081"]
