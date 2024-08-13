FROM tomcat:8.5-jre11-openjdk
MAINTAINER "***REMOVED***"
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/resource-catalogue-service.war /usr/local/tomcat/webapps/resource-catalogue-service.war
COPY ./src/main/resources/application.properties /usr/local/tomcat/lib/registry.properties
COPY ./server.xml /usr/local/tomcat/conf/server.xml
RUN ["cat", "/usr/local/tomcat/lib/registry.properties"]
CMD ["catalina.sh", "run"]
