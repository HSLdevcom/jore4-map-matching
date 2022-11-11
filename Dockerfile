FROM maven:3-eclipse-temurin-17-alpine AS builder

# set up workdir
WORKDIR /build

# download dependencies
COPY ./spring-boot/pom.xml /build
RUN mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

# copy sources
COPY ./spring-boot/src /build/src
# package using "prod" profile
COPY ./spring-boot/profiles/prod /build/profiles/prod
RUN mvn -Pprod -DskipTests=true clean package spring-boot:repackage

FROM eclipse-temurin:17-jre

# expose server port
EXPOSE 8080

# install curl
RUN apt-get update \
 && apt install -y curl \
 && rm -rf /var/lib/apt/lists/*

# download script for reading Docker secrets
RUN curl -o /tmp/read-secrets.sh "https://raw.githubusercontent.com/HSLdevcom/jore4-tools/main/docker/read-secrets.sh"

# add helper script for constructing JDBC URL
COPY ./docker/build-jdbc-urls.sh /tmp/build-jdbc-urls.sh

# copy compiled jar from builder stage
COPY --from=builder /build/target/*.jar /usr/src/jore4-map-matching/jore4-map-matching-backend.jar

# read Docker secrets into environment variables and run application
CMD /bin/bash -c "source /tmp/read-secrets.sh && source /tmp/build-jdbc-urls.sh && java -jar /usr/src/jore4-map-matching/jore4-map-matching-backend.jar"

HEALTHCHECK --interval=1m --timeout=5s \
  CMD curl --fail http://localhost:8080/actuator/health
