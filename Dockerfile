FROM maven:3-openjdk-11-slim AS builder

# set up workdir
WORKDIR /build

# download dependencies
COPY ./spring-boot/pom.xml /build
RUN mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

# copy sources
COPY ./spring-boot/src /build/src
# package using "prod" profile
COPY ./spring-boot/profiles/prod /build/profiles/prod
RUN mvn -Pprod clean package spring-boot:repackage


# slim image for distribution
FROM eclipse-temurin:11-jre-alpine

# expose server port
EXPOSE 8080

# install curl
RUN apk --no-cache add curl

# download script for reading Docker secrets
RUN curl -o /tmp/read-secrets.sh "https://raw.githubusercontent.com/HSLdevcom/jore4-tools/main/docker/read-secrets.sh"

# add helper script for constructing JDBC URL
COPY ./docker/build-jdbc-urls.sh /tmp/build-jdbc-urls.sh

# copy compiled jar from builder stage
COPY --from=builder /build/target/*.jar /usr/src/jore4-map-matching/jore4-map-matching-backend.jar

# read Docker secrets into environment variables and run application
CMD /bin/sh -c "source /tmp/read-secrets.sh && source /tmp/build-jdbc-urls.sh && java -jar /usr/src/jore4-map-matching/jore4-map-matching-backend.jar"

HEALTHCHECK --interval=1m --timeout=5s \
  CMD curl --fail http://localhost:8080/actuator/health
