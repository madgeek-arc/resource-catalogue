# eInfraCentral Catalogue #
###### What is this project about?

eInfraCentral platform, developed in the context of [eInfracentral EU project](http://einfracentral.eu/) is open sourced SW used for building and synchronizing electronic service catalogues from research infrastructures. It can be used by any research organization and research Infrastructure to publish, maintain a catalogue of services and offer added value functionality on top of service offerings.

eInfracentral technology has been used for building the eInfraCentral catalogue (https://www.einfracentral.eu), the Catalogue of the European Open Science Cloud Portal (https://catalogue.eosc-portal.eu),  the catalogue of services of OpenAire infrastructure (http://catalogue.openaire.eu) and the catalogue of services of Research Infrastructres (http://www.catris.eu). 

*Detailed specifications are available also at the developer section of the portal https://www.einfracentral.eu/developers*

The eInfraCentral software is distributed under the [GPL 3.0](https://opensource.org/licenses/GPL-3.0) license.


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

#### PostgreSQL - Create DB
```sql
CREATE USER <user> WITH PASSWORD 'your-password'; -- or use an existing user

CREATE DATABASE db WITH OWNER <user>;
```
1. Log in to the created db using: `sudo -u postgres psql  db`
2. Execute the following command: `CREATE EXTENSION tablefunc;`

#### Deploy
1. Ensure that PostgreSQL, ActiveMQ and Elasticsearch are up and running.

2. Create a file named `registry.properties` inside the /lib folder of your Tomcat installation and populate it with the [Application Properties Example](#Application-Properties-Example) (or edit the `application.properties` file of the project before you [Build](#Build) it).

3. Deploy the webapp on Tomcat.

4. *Before you begin using it for the __first time__, you must [add the resourceTypes](#Add-resourceTypes).*

- - -

###### Add resourceTypes (only the first time you deploy the project)
1. Navigate to eic/eic-registry/src/main/resources/resourceTypes

2. Execute `bash loadResourceTypes.sh localhost` (replace localhost with your host)

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
jdbc.url=jdbc:postgresql://${fqdn}:5432/db
jdbc.username=<user>
jdbc.password=<your-password>

## Elasticsearch Properties ##
elasticsearch.url=${fqdn}
elasticsearch.port=9300
elasticsearch.cluster=<clusterName>

## JMS Properties ##
jms.host=tcp://${fqdn}:61616
jms.prefix=<local>

## eic Login Properties ##
webapp.homepage=http://localhost:3000
webapp.oidc.login.redirectUris=http://localhost:8080/eic-registry/openid_connect_login

## Openid Connect Properties ##
oidc.issuer=
oidc.authorization=
oidc.token=
oidc.userinfo=
oidc.revocation=
oidc.jwk=

oidc.clientId=
oidc.clientSecret=
oidc.scopes=openid, profile, email


#########################
## Optional Properties ##
#########################

## eic Properties ##
eic.admins=test@email.com, test2@email.com
einfracentral.debug=false

## sync ##
sync.host=
sync.token.filepath=

## Mail Properties ##
mail.smtp.auth=
mail.smtp.host=
mail.smtp.user=
mail.smtp.password=
mail.smtp.port=
mail.smtp.protocol=
mail.smtp.ssl.enable=

## Matomo Properties ##
matomoHost=
matomoToken=
matomoSiteId=
matomoAuthorizationHeader=
```

- - -
