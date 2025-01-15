<div align="center">
  <img src='https://eosc.eu/wp-content/uploads/2024/02/EOSC-Beyond-logo.png'></img>
</div>

# Resource Catalogue

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

---

## Description
The Resource Catalogue is a Java-based platform designed to manage and organize a diverse range of resources. 
It provides a comprehensive collection of research services developed collaboratively by various research communities 
and IT service providers. 

The project operates under the EOSC Beyond initiative, which aims to promote Open Science 
and foster innovation within the framework of the European Open Science Cloud (EOSC).
EOSC Beyond overall objective is to advance Open Science and innovation in research in the context of the European Open Science Cloud (EOSC) by providing new EOSC Core capabilities allowing scientific applications to find, compose and access multiple Open Science resources and offer them as integrated capabilities to researchers.

---

## Getting Started:

Follow these steps to set up a development environment for Resource Catalogue:

### Prerequisites:

* Java 21
* Maven 3.9+
* ActiveMQ 5.x.x
* Elasticsearch 7.17.x
* PostgreSQL 9.5+

### Installation

1. **Create Database and necessary extension**
   ```sql
   USER <user> WITH PASSWORD 'your-password'; -- or use an existing user
   CREATE DATABASE <db> WITH OWNER <user>;
   ```
2. **Clone the repository**:
   ```bash
   git clone https://github.com/madgeek-arc/resource-catalogue.git
   ```
3. **Create a file named `secret.properties` and populate it to resolve `application.properties` placeholders.
   You can view an example at [Secret Properties Example](#Secret-Properties-Example).**
4. **Build and Package**  
   To build the project and package the code into an executable .jar file with an embedded Tomcat server:
   1. _Navigate_ to the project directory
   2. _Execute_ the following Maven command
   ```bash
   mvn clean package
   ```

5. **Run**  
   ```bash
   java -jar resource-catalogue-service/target/resource-catalogue-service-X.X.X-SNAPSHOT.jar \
   --spring.config.additional-location=file:/path/to/secret.properties
   ```

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

Special thanks to all contributors, testers and the open-source community for their invaluable support and resources.

---

##### Secret Properties Example

```properties
TBD
```