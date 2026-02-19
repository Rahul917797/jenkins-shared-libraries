FROM openjdk:17.0.8-jdk-slim
WORKDIR /app
COPY target/my-app-1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--server.port=8081"]
