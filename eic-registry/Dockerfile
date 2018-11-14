FROM tomcat:8.5-jre8-alpine
MAINTAINER "***REMOVED***"
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/eic-registry.war /usr/local/tomcat/webapps/eic-registry.war
COPY ./src/main/resources/application.properties /usr/local/tomcat/lib/registry.properties
COPY ./server.xml /usr/local/tomcat/conf/server.xml
RUN ["cat", "/usr/local/tomcat/lib/registry.properties"]
CMD ["catalina.sh", "run"]
