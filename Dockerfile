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
FROM openjdk:21

WORKDIR /beyond
COPY --from=maven /tmp/resource-catalogue-service/target/*.jar /beyond/resource-catalogue.jar
# COPY application.yml /etc/intelcomp/application.yml

RUN groupadd -g 10001 eosc && \
       useradd -u 10000 -g eosc eosc \
       && chown -R eosc:eosc /beyond

USER eosc
ENTRYPOINT ["java", "-jar", "/beyond/resource-catalogue.jar"]