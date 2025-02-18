### Build using Maven ###
FROM maven:3.9 AS maven
ARG profile

COPY pom.xml /tmp/
COPY . /tmp/

WORKDIR /tmp/

## For debugging reasons  ##
RUN if [ -z "$profile" ] ; then echo "Building without profile"; sleep 2 ; else echo "Building using profile: '$profile'"; sleep 2 ; fi

## run maven based of profile given ##
RUN if [ -z "$profile" ] ; then mvn package -U ; else mvn package -U -P $profile ; fi


### Create Docker Image ###
FROM openjdk:21-jdk-slim

RUN apt update && apt install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=maven /tmp/resource-catalogue-service/target/*.jar /app/resource-catalogue.jar

RUN groupadd -g 10001 catalogue && \
       useradd -u 10000 -g catalogue catalogue \
       && chown -R catalogue:catalogue /app

USER catalogue
ENTRYPOINT ["java", "-jar", "/app/resource-catalogue.jar"]
