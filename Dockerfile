
FROM alpine:latest

# Install Java and other dependencies
RUN apk add --no-cache openjdk17-jre curl bash

# Set working directory
WORKDIR /app

# Copy application files
COPY target/bpm-app.jar .
COPY truststore.jks .

# Expose port
EXPOSE 9090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:9090/health || exit 1

# Start application
CMD ["java", "-Djavax.net.ssl.trustStore=/app/truststore.jks", "-Djavax.net.ssl.trustStorePassword=BezpecneHeslo.123!", "-jar", "/app/bpm-app.jar"]
