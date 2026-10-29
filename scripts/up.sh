#!/usr/bin/env bash
# Build everything and deploy to a local kind cluster created by Terraform.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> 1/4 Creating the cluster and namespace with Terraform"
terraform -chdir=terraform init -input=false
terraform -chdir=terraform apply -auto-approve -input=false

echo "==> 2/4 Building the Docker image"
docker build -t simplemail:dev .

echo "==> 3/4 Loading the image into the kind cluster"
kind load docker-image simplemail:dev --name simplemail

echo "==> 4/4 Deploying to Kubernetes"
kubectl config use-context kind-simplemail
kubectl apply -f k8s/
kubectl -n simplemail rollout status deployment/mailpit --timeout=120s
kubectl -n simplemail rollout status deployment/simplemail --timeout=180s

kubectl -n simplemail get pods,svc
echo
echo "Done. Open the app and the inbox with:"
echo "  kubectl -n simplemail port-forward svc/simplemail 8080:80"
echo "  kubectl -n simplemail port-forward svc/mailpit 8025:8025"
