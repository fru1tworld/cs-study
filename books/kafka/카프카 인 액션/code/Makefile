# Makefile for Confluent Platform 7.9.0 smoke test

# Define colors
GREEN := \033[0;32m
YELLOW := \033[0;33m
BLUE := \033[0;34m
MAGENTA := \033[0;35m
CYAN := \033[0;36m
RED := \033[0;31m
NC := \033[0m # No Color

.PHONY: smoke-test setup test teardown

# Run the full smoke test (setup, test, teardown)
smoke-test: setup test teardown
	@echo "$(GREEN)✅ Smoke test completed successfully!$(NC)"
	@echo "$(MAGENTA)🎉 All Confluent Platform 7.9.0 components are working properly!$(NC)"

# Start the Docker Compose environment
setup:
	@echo "$(BLUE)🚀 Starting Docker Compose environment...$(NC)"
	@docker-compose up -d
	@echo "$(YELLOW)⏳ Waiting for services to be ready with exponential retry...$(NC)"
	@./wait-for-services.sh

# Run the smoke test
test:
	@echo "$(CYAN)🦓 Checking Zookeeper...$(NC)"
	@docker exec zookeeper bash -c "echo ruok | nc localhost 2181"

	@echo "$(CYAN)🔍 Checking Kafka brokers...$(NC)"
	@docker exec broker1 bash -c "kafka-topics --bootstrap-server broker1:29092 --list"

	@echo "$(CYAN)📝 Creating test topic...$(NC)"
	@docker exec broker1 bash -c "kafka-topics --bootstrap-server broker1:29092 --create --topic smoke-test --partitions 3 --replication-factor 3"

	@echo "$(CYAN)📤 Producing test message...$(NC)"
	@docker exec broker1 bash -c "echo 'Smoke test message' | kafka-console-producer --bootstrap-server broker1:29092 --topic smoke-test"

	@echo "$(CYAN)📥 Consuming test message...$(NC)"
	@docker exec broker1 bash -c "kafka-console-consumer --bootstrap-server broker1:29092 --topic smoke-test --from-beginning --max-messages 1 --timeout-ms 10000"

	@echo "$(CYAN)📋 Checking Schema Registry...$(NC)"
	@docker exec schema-registry bash -c "curl -s http://schema-registry:8081/subjects"

	@echo "$(CYAN)🔎 Checking ksqlDB...$(NC)"
	@docker exec ksqldb-server bash -c "curl -s http://ksqldb-server:8088/info"

	@echo "$(CYAN)🧹 Cleaning up...$(NC)"
	@docker exec broker1 bash -c "kafka-topics --bootstrap-server broker1:29092 --delete --topic smoke-test"

# Stop and remove the Docker Compose environment
teardown:
	@echo "$(RED)🛑 Shutting down Docker Compose environment...$(NC)"
	@docker-compose down
