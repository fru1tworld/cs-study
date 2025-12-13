#!/bin/bash
set -e

echo "🔄 Waiting for services to be ready..."

# Function to check if Zookeeper is ready
check_zookeeper() {
  echo "🦓 Checking Zookeeper..."
  docker exec zookeeper bash -c "echo ruok | nc localhost 2181" | grep -q "imok"
  return $?
}

# Function to check if Kafka brokers are ready
check_kafka() {
  echo "🔍 Checking Kafka brokers..."
  docker exec broker1 bash -c "kafka-topics --bootstrap-server broker1:29092 --list" &> /dev/null
  return $?
}

# Function to wait for a service with exponential backoff
wait_for_service() {
  local service_name=$1
  local check_function=$2
  local max_attempts=10
  local timeout=1
  local attempt=1
  local exitCode=1

  while [[ $attempt -le $max_attempts ]]
  do
    echo "⏳ Attempt $attempt/$max_attempts: Waiting for $service_name..."
    
    if $check_function; then
      echo "✅ $service_name is ready!"
      return 0
    fi

    echo "⏳ $service_name is not ready yet. Retrying in $timeout seconds..."
    sleep $timeout
    
    # Exponential backoff with a maximum of 30 seconds
    timeout=$(( timeout * 2 ))
    if [[ $timeout -gt 30 ]]; then
      timeout=30
    fi
    
    attempt=$(( attempt + 1 ))
  done

  echo "❌ Failed to connect to $service_name after $max_attempts attempts."
  return 1
}

# Wait for Zookeeper
wait_for_service "Zookeeper" check_zookeeper

# Wait for Kafka
wait_for_service "Kafka" check_kafka

echo "✅ All services are ready!"