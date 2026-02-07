.PHONY: help setup contracts dev test test-cov build docker compose down clean lint format

APP_NAME=contract-first-integrations
MAVEN=mvn

help:
	@echo "Available commands:"
	@echo "  make setup       - Install dependencies and generate sources"
	@echo "  make contracts   - Generate Java classes from Avro schemas"
	@echo "  make dev         - Run application locally (requires PostgreSQL, Kafka)"
	@echo "  make test        - Run tests"
	@echo "  make test-cov    - Run tests with coverage report"
	@echo "  make build       - Build JAR file"
	@echo "  make docker      - Build Docker image"
	@echo "  make compose     - Start full stack with docker-compose"
	@echo "  make down        - Stop docker-compose stack"
	@echo "  make clean       - Clean build artifacts"
	@echo "  make lint        - Check code quality"
	@echo "  make format      - Format code"

setup:
	$(MAVEN) clean install -DskipTests

contracts:
	$(MAVEN) avro:schema

dev:
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=dev

test:
	$(MAVEN) -B test

test-cov:
	$(MAVEN) -B test jacoco:report
	@echo "Coverage report: target/site/jacoco/index.html"

build:
	$(MAVEN) -B clean package

docker:
	docker build -t $(APP_NAME):latest .

compose:
	docker compose up --build

down:
	docker compose down -v

clean:
	$(MAVEN) clean
	docker compose down -v
	rm -rf target/

lint:
	$(MAVEN) checkstyle:check

format:
	$(MAVEN) formatter:format

# Development helpers
logs:
	docker compose logs -f app

kafka-topics:
	docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

kafka-consume:
	docker compose exec kafka kafka-console-consumer --topic orders.order-created.v1 --from-beginning --bootstrap-server localhost:9092

db-connect:
	docker compose exec postgres psql -U postgres -d orders

db-migrate:
	$(MAVEN) flyway:migrate

db-info:
	$(MAVEN) flyway:info
