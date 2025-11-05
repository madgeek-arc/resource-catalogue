[![EOSC Beyond Logo][eosc-logo]]()

# Resource Catalogue

[![License][license-badge]][license-link]
[![Contributor Covenant][coc-badge]][coc-link]
[![SQAaaS badge shields.io][sqaaas-badge]][sqaaas-link]

---

## Description

The Resource Catalogue is a Java-based platform designed to manage and organize
a diverse range of resources. It provides a comprehensive collection of research
services developed collaboratively by various research communities and IT
service providers.

The project operates under the EOSC Beyond initiative, which aims to promote
Open Science and foster innovation within the framework of the European Open
Science Cloud (EOSC). EOSC Beyond overall objective is to advance Open Science
and innovation in research in the context of the European Open Science Cloud
(EOSC) by providing new EOSC Core capabilities allowing scientific applications
to find, compose and access multiple Open Science resources and offer them as
integrated capabilities to researchers.

---

## Getting Started

Follow these steps to set up a development environment for Resource Catalogue:

### Prerequisites

* Java 21
* Maven 3.9+
* ActiveMQ 5.x.x
* Elasticsearch 7.17.x
* PostgreSQL 9.5+

### Installation

1. Create Database and necessary extension

   ```sql
   USER <user> WITH PASSWORD 'your-password'; -- or use an existing user
   CREATE DATABASE <db> WITH OWNER <user>;
   ```

2. Clone the repository:

   ```bash
   git clone https://github.com/madgeek-arc/resource-catalogue.git
   ```

3. Create the necessary configuration files
    1. `application.properties` – Add the required settings.
       See the [Application Properties Example](#Application-Properties-Example)
       for reference.
    2. `pid.yml` – Create this file only if you plan to use the PID Service.
       See the [PID Properties Example](#PID-Properties-Example) for details.
4. Build and Package
    To build the project and package the code into an executable .jar file with
    an embedded Tomcat server:
    1. Navigate to the project directory
    2. Execute the following Maven command

   ```bash
   mvn clean package
   ```

5. Run
    1. without PID Service

   ```bash
   java -jar \
   resource-catalogue-service/target/resource-catalogue-service-X.X.X.jar \
   --spring.config.additional-location=file:/path/to/application.properties
   ```

    2. with PID Service

   ```bash
   java -jar \
   resource-catalogue-service/target/resource-catalogue-service-X.X.X.jar \
   --spring.config.additional-location= \
   file:/path/to/application.properties,file:/path/to/pid.yml
   ```

---

### Initial Setup Wizard

Before using the application, you **must run the Setup Wizard** once.  
This step populates the database with essential resources required for the
smooth operation of the service.

#### Purpose

* Loads necessary controlled vocabularies.
* Loads required resource models.
* Creates the default Catalogue.

The Setup Wizard should be executed **only once**, immediately after the first
successful startup.

#### How to Run

Once the application is up and running, open the following URL in your browser
or via an API client:

```
http://localhost:8080/api/wizard/step1
```

The Setup Wizard will guide you through each step of the initialization process.

---

## Test execution

```bash
  mvn clean verify
```

Test results will be displayed in the terminal.

---

## Documentation Links

For extensive and detailed documentation, please refer to
[Documentation](https://madgeek-arc.github.io/resource-catalogue-docs/).

---

## Versioning

This project adheres to [Semantic Versioning](https://semver.org/).
For the available versions, see the
[tags](https://github.com/madgeek-arc/resource-catalogue/tags).

---

## Authors

* Konstantinos Spyrou - Development - [GitHub](https://github.com/spyroukostas)
* Michael Zouros - Development - [GitHub](https://github.com/mzouros)

See the [contributors list][contrib] for a full list of contributors.

---

## Acknowledgements

Special thanks to all contributors, testers and the open-source community for
their invaluable support and resources.

---

### Application Properties Example

Refer to [application.properties][prop] for the complete set of
configuration options.

```properties
#########################
##  Server Properties  ##
#########################

dynamic.properties.path=

## Server Configuration ##
server.port=8080
server.servlet.context-path=/api

## Logging Configuration ##
logging.level.root=INFO


#########################
##  Spring Properties  ##
#########################

## Profiles ##
spring.profiles.active=beyond

## Servlet ##
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

## Bean Configuration ##
spring.jpa.open-in-view=false
spring.main.allow-bean-definition-overriding=true

## Redis Properties ##
spring.data.redis.host=
spring.data.redis.port=
spring.data.redis.password=

## Security Properties ##
### EOSC AAI Authentication ###
spring.security.oauth2.client.provider.eosc.issuer-uri=
spring.security.oauth2.client.registration.eosc.client-id=
spring.security.oauth2.client.registration.eosc.client-name=
spring.security.oauth2.client.registration.eosc.client-secret=
spring.security.oauth2.client.registration.eosc.scope=
spring.security.oauth2.client.registration.eosc.redirect-uri=
spring.security.oauth2.resourceserver.jwt.issuer-uri=

## Springdoc Configuration ##
springdoc.api-docs.path=/api-docs
### Controllers ###
springdoc.group-configs[0].group=resource-catalogue
springdoc.group-configs[0].display-name=1.Resource Catalogue
springdoc.group-configs[0].packages-to-scan=gr.uoa.di.madgik.resourcecatalogue
springdoc.group-configs[1].group=dynamic-catalogue
springdoc.group-configs[1].display-name=2.Dynamic Catalogue
springdoc.group-configs[1].packages-to-scan=gr.uoa.di.madgik.catalogue
springdoc.group-configs[2].group=registry
springdoc.group-configs[2].display-name=3.Registry
springdoc.group-configs[2].packages-to-scan=gr.uoa.di.madgik.registry
### Swagger UI ###
springdoc.swagger-ui.docExpansion=none
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.syntaxHighlight.activated=false
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true


###########################
##  Registry Properties  ##
###########################

## Platform Settings ##
fqdn=

## Registry Configuration ##
### Host ###
registry.host=http://localhost:${server.port}${server.servlet.context-path}
### DB - Datasource Properties ###
registry.datasource.configuration.maximum-pool-size=
registry.datasource.driver-class-name=org.postgresql.Driver
registry.datasource.username=
registry.datasource.password=
registry.datasource.url=
### DB - JPA Properties ###
registry.jpa.properties.hibernate.allow_update_outside_transaction=true
registry.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
registry.jpa.properties.hibernate.enable_lazy_load_no_trans=true
registry.jpa.properties.hibernate.format_sql=false
registry.jpa.properties.hibernate.hbm2ddl.auto=
registry.jpa.properties.hibernate.show_sql=false
### Elastic Properties ###
registry.elasticsearch.uris=
registry.elasticsearch.username=
registry.elasticsearch.password=
### JMS Properties ###
#### ActiveMQ ####
registry.jms.host=
registry.jms.prefix=
registry.jms.username=
registry.jms.password=
#### Catalogue JMS Prefix ###
catalogue.jms.prefix=
#### AMS ####
catalogue.jms.ams.enabled=true
catalogue.jms.ams.host=
catalogue.jms.ams.key=
catalogue.jms.ams.project=


############################
##  Catalogue Properties  ##
############################

## Basic Info ##
catalogue.id=resource-catalogue
catalogue.name=Resource Catalogue
catalogue.homepage=
catalogue.version=@project.version@
## Admins / Onboarding Team ##
catalogue.admins=
catalogue.onboarding-team=
## Redirect URLs ##
catalogue.login-redirect=
catalogue.logout-redirect=
## Resource ID Prefixes ##
catalogue.resources.adapter.id-prefix=adapter
catalogue.resources.configuration-template.id-prefix=configuration_template
catalogue.resources.configuration-template-instance.id-prefix=configuration_template_instance
catalogue.resources.datasource.id-prefix=datasource
catalogue.resources.deployable-service.id-prefix=deployable_service
catalogue.resources.helpdesk.id-prefix=helpdesk
catalogue.resources.interoperability-record.id-prefix=interoperability_record
catalogue.resources.monitoring.id-prefix=monitoring
catalogue.resources.provider.id-prefix=provider
catalogue.resources.resource-interoperability-record.id-prefix=resource_interoperability_record
catalogue.resources.service.id-prefix=service
catalogue.resources.tool.id-prefix=tool
catalogue.resources.training_resource.id-prefix=training_resource
catalogue.resources.vocabulary-curation.id-prefix=vocabulary_curation
## Email Notification Properties ##
catalogue.emails.enabled=false
catalogue.emails.admin-notifications=false
catalogue.emails.provider-notifications=false
catalogue.emails.registration-emails.to=
catalogue.emails.helpdesk-emails.to=
catalogue.emails.helpdesk-emails.cc=
catalogue.emails.monitoring-emails.to=
catalogue.emails.resource-consistency-notifications=false
catalogue.emails.resource-consistency-emails.to=
catalogue.emails.resource-consistency-emails.cc=
catalogue.emails.support=
## Mailer Properties ##
catalogue.mailer.host=
catalogue.mailer.from=
catalogue.mailer.username=
catalogue.mailer.password=
catalogue.mailer.port=
catalogue.mailer.protocol=
catalogue.mailer.auth=
catalogue.mailer.ssl=


#########################
##  External Services  ##
#########################

## PID Service ##
pid.service.enabled=false

## OpenAIRE Datasource Properties ##
openaire.ds.api=https://beta.services.openaire.eu/
```

### PID Properties Example

Refer to [pid.yml](resource-catalogue-service/src/main/resources/pid.yml)
for the complete set of configuration options.

```yaml
## PID Properties ##
catalogue:
  resources:
    provider:
      resolve-endpoints:
      pid-issuer:
        url:
        user:
        user-index:
        password:
        auth:
          self-signed-cert:
          client-key:
          client-cert:
    service:
      resolve-endpoints:
      pid-issuer:
        url:
        user:
        user-index:
        password:
        auth:
          self-signed-cert:
          client-key:
          client-cert:
    training-resource:
      resolve-endpoints:
      pid-issuer:
        url:
        user:
        user-index:
        password:
        auth:
          self-signed-cert:
          client-key:
          client-cert:
    interoperability-record:
      resolve-endpoints:
      pid-issuer:
        url:
        user:
        user-index:
        password:
        auth:
          self-signed-cert:
          client-key:
          client-cert:
    adapter:
      resolve-endpoints:
      pid-issuer:
        url:
        user:
        user-index:
        password:
        auth:
          self-signed-cert:
          client-key:
          client-cert:
    deployable-service:
      resolve-endpoints:
      pid-issuer:
        url:
        user:
        user-index:
        password:
        auth:
          self-signed-cert:
          client-key:
          client-cert:
```

#### Important note

Resource Catalogue supports both Mutual TLS (mTLS) and Basic Authentication.
For Basic Authentication, provide a password; otherwise, provide all necessary
configurations under the auth block.

[eosc-logo]: https://eosc.eu/wp-content/uploads/2024/02/EOSC-Beyond-logo.png
[license-badge]: https://img.shields.io/badge/license-Apache%202.0-blue.svg
[license-link]: LICENSE
[coc-badge]: https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg
[coc-link]: CODE_OF_CONDUCT.md
[sqaaas-badge]: https://img.shields.io/badge/sqaaas%20software-bronze-e6ae77
[sqaaas-link]: https://api.eu.badgr.io/public/assertions/odpAhWUqTNCyZpOYupboVg
[contrib]: https://github.com/madgeek-arc/resource-catalogue/graphs/contributors
[prop]:resource-catalogue-service/src/main/resources/application.properties
