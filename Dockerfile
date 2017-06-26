FROM tomcat:7-jre8
MAINTAINER "stevengatsios@gmail.com"

RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/omtd-registry.war /usr/local/tomcat/webapps/omtd-registry.war
#COPY ./src/main/resources/eu/openminted/registry/domain/application.properties /usr/local/tomcat/lib/application.properties
COPY ./src/main/resources/eu/openminted/registry/domain/application.properties /usr/local/tomcat/lib/registry.properties


#ENV LOG4J_URL http://central.maven.org/maven2/log4j/log4j/1.2.17/log4j-1.2.17.jar

#RUN wget -o /usr/local/tomcat/lib/log4j-1.2.17.jar "$LOG4J_URL"

RUN ["cat", "/usr/local/tomcat/lib/registry.properties"]
CMD ["catalina.sh", "run"]