REGISTRY   := docker.madgik.di.uoa.gr
IMAGE_NAME := $(REGISTRY)/resource-catalogue
TAG         = $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
CONFIG     ?= file:./compose/config/application.properties,file:./compose/config/pid.yaml

.PHONY: build run docker-build docker-push compose compose-down

build:
	mvn clean package -DskipTests

####
# Sources compose/.env (if present) so Spring can resolve placeholder vars (e.g. ${DB_NAME}).
# Override CONFIG to point at a local properties file with localhost hostnames:
#   make run CONFIG=file:./local.properties
#   make debug CONFIG=file:./local.properties
run:
	@jar=$$(find resource-catalogue-service/target -maxdepth 1 \
	    -name "resource-catalogue-service-*.jar" ! -name "*.original" 2>/dev/null | head -1); \
	[ -n "$$jar" ] || { echo "No JAR found. Run 'make build' first."; exit 1; }; \
	[ -f ./compose/.env ] && { set -a; . ./compose/.env; set +a; }; \
	trap 'exit 0;' INT; java -jar "$$jar" --spring.config.additional-location=$(CONFIG)

debug:
	@jar=$$(find resource-catalogue-service/target -maxdepth 1 \
	    -name "resource-catalogue-service-*.jar" ! -name "*.original" 2>/dev/null | head -1); \
	[ -n "$$jar" ] || { echo "No JAR found. Run 'make build' first."; exit 1; }; \
	[ -f ./compose/.env ] && { set -a; . ./compose/.env; set +a; }; \
	trap 'exit 0;' INT; java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar "$$jar" --spring.config.additional-location=$(CONFIG)
####

docker-build:
	mvn clean package -DskipTests
	docker build -t $(IMAGE_NAME):$(TAG) .

docker-push:
	docker image push $(IMAGE_NAME):$(TAG)

compose:
	TAG=$(TAG) docker compose -f compose/compose.yaml up $(SERVICE)

compose-down:
	TAG=$(TAG) docker compose -f compose/compose.yaml down $(SERVICE)

.DEFAULT_GOAL := docker-build
