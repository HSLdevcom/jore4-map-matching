FROM maven:3-eclipse-temurin-17 AS builder

# set up workdir
WORKDIR /build

# download dependencies
COPY ./pom.xml /build
RUN mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

# copy sources
COPY ./src /build/src
# package using "prod" profile
COPY ./profiles/prod /build/profiles/prod
RUN mvn -Pprod -DskipTests=true clean package spring-boot:repackage

FROM eclipse-temurin:25.0.1_8-jre

# Application Insights version
ARG APPINSIGHTS_VERSION=3.7.7

# expose server port
EXPOSE 8080

# install curl
RUN apt-get update \
 && apt-get install -y curl \
 && rm -rf /var/lib/apt/lists/*

# download script for reading Docker secrets
ADD --chmod=755 https://raw.githubusercontent.com/HSLdevcom/jore4-tools/main/docker/read-secrets.sh /tmp/read-secrets.sh

# Connection string is provided as env in Kubernetes by secrets manager
# it should not be provided for other environments (local etc)
ADD --chmod=755 https://github.com/microsoft/ApplicationInsights-Java/releases/download/${APPINSIGHTS_VERSION}/applicationinsights-agent-${APPINSIGHTS_VERSION}.jar /usr/src/jore4-map-matching/applicationinsights-agent.jar
COPY --chmod=755 ./applicationinsights.json /usr/src/jore4-map-matching/applicationinsights.json

# add helper script for constructing JDBC URL
COPY --chmod=755 ./scripts/build-jdbc-urls.sh /tmp/build-jdbc-urls.sh

# copy compiled jar from builder stage
COPY --from=builder /build/target/*.jar /usr/src/jore4-map-matching/jore4-map-matching.jar

# read Docker secrets into environment variables and run application
CMD ["/bin/bash", "-c", "source /tmp/read-secrets.sh && source /tmp/build-jdbc-urls.sh && java -javaagent:/usr/src/jore4-map-matching/applicationinsights-agent.jar -jar /usr/src/jore4-map-matching/jore4-map-matching.jar"]

HEALTHCHECK --interval=1m --timeout=5s \
  CMD curl --fail http://localhost:8080/actuator/health
