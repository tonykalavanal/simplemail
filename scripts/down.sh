#!/usr/bin/env bash
# Delete the cluster (and everything in it) with Terraform.
set -euo pipefail
cd "$(dirname "$0")/.."
terraform -chdir=terraform destroy -auto-approve -input=false
