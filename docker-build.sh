#!/bin/bash

# Build script for Docker Compose setup
# This script builds the application JAR and then starts the containers

set -e

echo "Building application..."
mvn clean package -DskipTests

echo "Building Docker image and starting containers..."
docker-compose up --build -d

echo "Waiting for services to be ready..."
sleep 10

echo "Checking service status..."
docker-compose ps

echo ""
echo "Application should be available at http://localhost:8080"
echo "PostgreSQL is available at localhost:5432"
echo ""
echo "To view logs: docker-compose logs -f app"
echo "To stop: docker-compose down"


