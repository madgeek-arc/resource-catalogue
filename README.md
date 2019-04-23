# eInfraCentral Catalogue #
###### What is this project about?

- - -

## Local deployment:

#### Requirements:

* Java 8
* Maven
* Tomcat 8.5
* ActiveMQ 5.14.0
* Elasticsearch 5.6.9
* PostgreSQL 9.5 or greater

#### Clone
`git clone https://github.com/eInfraCentral/eic.git`

#### Build
`mvn clean package`

#### PostgreSQL - Create DB ___registry___
```sql
CREATE USER <user> WITH PASSWORD 'your-password'; -- or use an existing user

CREATE DATABASE registry WITH OWNER <user>;
```

#### Deploy
1. Ensure that PostgreSQL, ActiveMQ and Elasticsearch are up and running.

2. Create a file named `registry.properties` inside the /lib folder of your Tomcat installation and populate it with the [Application Properties Example](######Application Properties Example) (or edit the `application.properties` file of the project).

3. Deploy the webapp on Tomcat.

- - -


###### Application Properties Example
```properties
##########################
## Mandatory Properties ##
##########################
fqdn=localhost
platform.root=http://${fqdn}/
registry.host=http://${fqdn}:8080/eic-registry/


## DB Properties ##
jdbc.url=jdbc:postgresql://${fqdn}:5432/registry
jdbc.username=
jdbc.password=

## Elastisearch Properties ##
elasticsearch.url=${fqdn}
elasticsearch.port=9300
elasticsearch.cluster={{clusterName}}

## JMS Properties ##
jms.host=tcp://${fqdn}:61616
jms.prefix={{e.g. registry}}

## eic Properties ##
webapp.home=http://localhost:8080/eic-registry/openid_connect_login
webapp.front=http://localhost:3000


#########################
## Optional Properties ##
#########################

## eic Properties ##
eic.admins=test@email.com, test2@email.com
einfracentral.debug=false

## AAI Properties ##
oidc.issuer=
oidc.secret=
oidc.id=

## Mail Properties ##
mail.smtp.auth=
mail.smtp.host=
mail.smtp.user=
mail.smtp.password=
mail.smtp.port=
mail.smtp.protocol=
mail.smtp.ssl.enable=

## Matomo Properties ##
matomoToken=
```

- - -