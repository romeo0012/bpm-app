FROM alpine:latest

# Install Java and other dependencies
RUN apk add --no-cache openjdk17-jre curl bash \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup -u 1001

# Set working directory
WORKDIR /app

# Copy application files
ARG JAVAX_NET_SSL_TRUSTSTORE
COPY --chown=appuser:appgroup target/bpm-app.jar app.jar
COPY --chown=appuser:appgroup ${JAVAX_NET_SSL_TRUSTSTORE} .

# Expose port
EXPOSE 9090

# Health check
#HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
#    CMD curl -f http://localhost:9090/health || exit 1

# Run as non-root user
USER 1001

# Start application
CMD ["sh", "-c", "exec java -Xmx128m -Xms128m -XX:MaxMetaspaceSize=128m -Djavax.net.ssl.trustStore=\"$JAVAX_NET_SSL_TRUSTSTORE\" -Djavax.net.ssl.trustStorePassword=\"$JAVAX_NET_SSL_TRUSTSTOREPASSWORD\" -jar app.jar"]
