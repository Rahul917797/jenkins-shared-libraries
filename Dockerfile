FROM openjdk:17-alpine

# Install nginx
RUN apt-get update && apt-get install -y nginx && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy JAR and supporting files
COPY target/my-app-1.0-SNAPSHOT.jar /app/my-app.jar
COPY nginx.conf /etc/nginx/nginx.conf
COPY start.sh /start.sh

# Make script executable
RUN chmod +x /start.sh

# Expose both app and nginx ports
EXPOSE 8080
EXPOSE 80

# Start Java and nginx
CMD ["/start.sh"]

