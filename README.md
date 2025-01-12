<div align="center">
  <img src='https://eosc.eu/wp-content/uploads/2024/02/EOSC-Beyond-logo.png'></img>
</div>

# Resource Catalogue [v5.0.0]

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

---

## Description
The Resource Catalogue is a Java-based platform designed to manage and organize a diverse range of resources. 
It provides a comprehensive collection of research services developed collaboratively by various research communities 
and IT service providers. The project operates under the EOSC Beyond initiative, which aims to promote Open Science 
and foster innovation within the framework of the European Open Science Cloud (EOSC).

---

## Getting Started:

Follow these steps to set up a development environment for Resource Catalogue:

### Prerequisites:

* Java 21
* Maven
* ActiveMQ 5.14.0
* Elasticsearch 7.17.x
* PostgreSQL 9.5 or greater

### Installation

1. **Create Database and necessary extension**
   ```sql
   USER <user> WITH PASSWORD 'your-password'; -- or use an existing user
   CREATE DATABASE db WITH OWNER <user>;
   ```
   ```bash
   sudo -u postgres psql db
   ```
   ```sql
   CREATE EXTENSION tablefunc;
   ```
2. **Clone the repository**:
   ```bash
   git clone https://github.com/madgeek-arc/resource-catalogue.git
   ```
3. **Create a file named `secret.properties` and populate it to resolve `application.properties` placeholders.
   You can view an example at [Secret Properties Example](#Secret-Properties-Example).**
4. **Build**
   ```bash
   cd resource-catalogue
   mvn clean package -Dspring.config.additional-location=file:/path/to/secret.properties -P beyond
   ```

### Deployment

1. Ensure that PostgreSQL, ActiveMQ and Elasticsearch services are up and running.
2. Deploy.

---

## Test execution:
```bash
  mvn clean verify -Dspring.config.additional-location=file:/path/to/secret.properties
```
Test results will be displayed in the terminal.

---

## Documentation Links
For extensive and detailed documentation, please refer to
[Resource Catalogue Documentation](https://github.com/madgeek-arc/resource-catalogue-docs).

---

## Versioning:
This project adheres to [Semantic Versioning](https://semver.org/). For the available versions, see the 
[tags](https://github.com/madgeek-arc/resource-catalogue/tags).

---

## Authors
- **Konstantinos Spyrou** - Development - [GitHub](https://github.com/spyroukostas)
- **Michael Zouros** - Development - [GitHub](https://github.com/mzouros)

See the [contributors list](https://github.com/madgeek-arc/resource-catalogue/graphs/contributors) 
for a full list of contributors.

---

## Acknowledgements

- **Java**: The programming language used for building the application.
- **Maven**: Dependency management and build automation tool.
- **ActiveMQ**: Message broker facilitating asynchronous communication.
- **Elasticsearch**: Search and analytics engine powering our search functionality.
- **PostgreSQL**: Relational database management system for data storage.
- **Spring Boot**: Java framework used for programming standalone, production-grade Spring-based applications.
- Special thanks to all contributors, testers and the open-source community for their invaluable support and resources.

---

##### Secret Properties Example

```properties
TBD
```