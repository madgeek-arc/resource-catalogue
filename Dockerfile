### Build using Maven ###
FROM maven:3.9 AS maven
ARG profile

WORKDIR /build

# Copy parent pom.xml first
COPY pom.xml .

# Copy module poms for caching
COPY resource-catalogue-api/pom.xml ./resource-catalogue-api/
COPY resource-catalogue-elastic/pom.xml ./resource-catalogue-elastic/
COPY resource-catalogue-jms/pom.xml ./resource-catalogue-jms/
COPY resource-catalogue-model/pom.xml ./resource-catalogue-model/
COPY resource-catalogue-model-lot1/pom.xml ./resource-catalogue-model-lot1/
COPY resource-catalogue-rest/pom.xml ./resource-catalogue-rest/
COPY resource-catalogue-service/pom.xml ./resource-catalogue-service/
COPY matomo/pom.xml ./matomo/

## Go offline to cache dependencies
RUN mvn dependency:go-offline -B

# Copy the full source
COPY . .

## For debugging reasons  ##
RUN if [ -z "$profile" ] ; then echo "Building without profile"; sleep 2 ; else echo "Building using profile: '$profile'"; sleep 2 ; fi
RUN mvn help:effective-pom

## Run maven based on given profile ##
RUN if [ -z "$profile" ] ; then mvn package ; else mvn package -P $profile ; fi


### Create Docker Image ###
FROM openjdk:21-jdk-slim

RUN apt update && apt install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=maven /build/resource-catalogue-service/target/*.jar /app/resource-catalogue.jar

RUN groupadd -g 10001 catalogue && \
       useradd -u 10000 -g catalogue catalogue \
       && chown -R catalogue:catalogue /app

USER catalogue
ENTRYPOINT ["java", "-jar", "/app/resource-catalogue.jar"]
