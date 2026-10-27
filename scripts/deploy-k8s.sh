#!/usr/bin/env bash
# Deploys the whole platform to the current kubectl context (Minikube / Docker Desktop).
set -euo pipefail
cd "$(dirname "$0")/.."
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-config.yaml
kubectl apply -f k8s/infra/
echo ">>> waiting for infrastructure"
for d in postgres mongo kafka schema-registry zipkin prometheus; do
  kubectl -n ecommerce rollout status deploy/$d --timeout=240s
done
kubectl apply -f k8s/apps/service-registry.yaml -f k8s/apps/config-server.yaml
kubectl -n ecommerce rollout status deploy/service-registry --timeout=240s
kubectl apply -f k8s/apps/
for d in api-gateway order-service inventory-service payment-service notification-service frontend; do
  kubectl -n ecommerce rollout status deploy/$d --timeout=300s
done
echo
echo "Deployed. Open the web app with ONE of:"
echo "  minikube service frontend -n ecommerce --url"
echo "  kubectl -n ecommerce port-forward svc/frontend 3000:80   # then http://localhost:3000"
echo
echo "Raw API gateway:"
echo "  minikube service api-gateway -n ecommerce --url"
echo "  kubectl -n ecommerce port-forward svc/api-gateway 8080:8080"
echo "Zipkin:     kubectl -n ecommerce port-forward svc/zipkin 9411:9411"
echo "Prometheus: kubectl -n ecommerce port-forward svc/prometheus 9090:9090"
