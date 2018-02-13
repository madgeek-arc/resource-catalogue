FROM tomcat:8.5-jre8-alpine
MAINTAINER "pgl@otenel.gr"
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/eic-registry.war /usr/local/tomcat/webapps/eic-registry.war
COPY ./src/main/resources/application.properties /usr/local/tomcat/lib/registry.properties
RUN ["cat", "/usr/local/tomcat/lib/registry.properties"]
CMD ["catalina.sh", "run"]
