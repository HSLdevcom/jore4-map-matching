# Builder docker image.
FROM maven:3-openjdk-11 AS builder

# Set up workdir.
WORKDIR /build

# Download dependencies.
COPY ./spring-boot/pom.xml /build
RUN mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

# Build with "prod" profile.
COPY ./spring-boot/src /build/src
COPY ./spring-boot/profiles/prod /build/profiles/prod
RUN mvn -Pprod clean package spring-boot:repackage

# Distributed docker image
FROM openjdk:11-jre

# Expose server port.
EXPOSE 8080

# download script for reading docker secrets
RUN curl -o /tmp/read-secrets.sh "https://raw.githubusercontent.com/HSLdevcom/jore4-tools/main/docker/read-secrets.sh"

# Copy over jdbc url helper script
COPY build-jdbc-urls.sh /tmp/build-jdbc-urls.sh

# Copy over compiled jar.
COPY --from=builder /build/target/*.jar /usr/src/jore4-map-matching/jore4-map-matching-backend.jar

# Read docker secrets into environment variables and run application
CMD /bin/bash -c "source /tmp/read-secrets.sh && source /tmp/build-jdbc-urls.sh && java -jar /usr/src/jore4-map-matching/jore4-map-matching-backend.jar"

HEALTHCHECK --interval=1m --timeout=5s \
  CMD curl --fail http://localhost:8080/actuator/health
