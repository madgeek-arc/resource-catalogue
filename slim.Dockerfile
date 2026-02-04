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
#RUN mvn dependency:go-offline -B

# Copy the full source
COPY . .

## For debugging reasons  ##
RUN if [ -z "$profile" ] ; then echo "Building without profile"; sleep 2 ; else echo "Building using profile: '$profile'"; sleep 2 ; fi
RUN mvn help:effective-pom

## Run maven based on given profile ##
RUN if [ -z "$profile" ] ; then mvn package ; else mvn package -P $profile ; fi

RUN jdeps -q --ignore-missing-deps --recursive --multi-release 21 \
    --print-module-deps --class-path BOOT-INF/lib/* \
    resource-catalogue-service/target/resource-catalogue-service-*.jar > modules.txt

RUN $JAVA_HOME/bin/jlink \
    --verbose \
    --add-modules $(cat modules.txt) \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /slim-jdk

### Create Docker Image ###
FROM alpine:latest
ENV JAVA_HOME=/opt/jdk/jdk-21
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app

COPY --from=maven /build/resource-catalogue-service/target/*.jar /app/resource-catalogue.jar
COPY --from=maven /slim-jdk $JAVA_HOME

RUN addgroup --gid 10001 catalogue && \
       adduser --uid 10000 --ingroup catalogue --disabled-password catalogue \
       && chown -R catalogue:catalogue /app

USER catalogue
#ENTRYPOINT ["java", "-jar", "/app/resource-catalogue.jar"]
CMD ["sh"]