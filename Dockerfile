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
COPY --chown=appuser:appgroup docker-entrypoint.sh .

RUN chmod 755 /app/docker-entrypoint.sh

# Run as non-root user
USER $APP_USER

ENTRYPOINT ["/app/docker-entrypoint.sh"]
