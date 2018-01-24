FROM tomcat:7-jre8
MAINTAINER "pgl@otenel.gr"
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/eic-registry.war /usr/local/tomcat/webapps/eic-registry.war
COPY ./src/main/resources/application.properties /usr/local/tomcat/lib/registry.properties
RUN ["cat", "/usr/local/tomcat/lib/registry.properties"]
CMD ["catalina.sh", "run"]
