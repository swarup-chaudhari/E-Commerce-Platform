#!/usr/bin/env bash
# Builds one Docker image per service. For Minikube run first:  eval $(minikube docker-env)
set -euo pipefail
cd "$(dirname "$0")/.."
for m in service-registry config-server api-gateway order-service inventory-service payment-service notification-service; do
  echo ">>> building ecommerce/$m:1.0.0"
  docker build --build-arg MODULE="$m" -t "ecommerce/$m:1.0.0" ./backend
done
echo ">>> building ecommerce/frontend:1.0.0"
docker build -t ecommerce/frontend:1.0.0 ./frontend
