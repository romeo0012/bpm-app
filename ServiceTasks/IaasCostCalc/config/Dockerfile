
FROM alpine:latest

# Install Java and other dependencies
RUN apk add --no-cache nodejs npm curl bash

# Set working directory
WORKDIR /app
RUN npm install node-fetch
RUN npm install dotenv
RUN npm install pg
RUN curl -fsSL https://letsencrypt.org/certs/staging/letsencrypt-stg-root-x1.pem -o /app/root-ca.pem

# Copy application files
COPY app.js .
COPY package.json .
COPY .env .

# Start application
ENV NODE_EXTRA_CA_CERTS=/app/root-ca.pem
CMD ["node", "/app/app.js"]
