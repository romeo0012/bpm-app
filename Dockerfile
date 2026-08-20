FROM alpine:latest
ENV APP_USER=1001

# Install Java and other dependencies
RUN apk add --no-cache openjdk17-jre curl bash \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup -u $APP_USER

# Set working directory
WORKDIR /app

# Copy application files
COPY --chown=appuser:appgroup target/bpm-app.jar app.jar
COPY --chown=appuser:appgroup truststore.jks .

# Expose port
EXPOSE 9090

# Run as non-root user
USER $APP_USER

# Start application
CMD ["sh", "-c", "exec java -Xmx128m -Xms128m -XX:MaxMetaspaceSize=128m -Djavax.net.ssl.trustStore=truststore.jks -Djavax.net.ssl.trustStorePassword=\"$JAVAX_NET_SSL_TRUSTSTOREPASSWORD\" -jar app.jar"]
