terraform {
  required_version = ">= 1.6"

  required_providers {
    # Creates a local Kubernetes cluster that runs inside Docker
    kind = {
      source  = "tehcyx/kind"
      version = ">= 0.6"
    }
    # Talks to the cluster's API to create Kubernetes objects
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.30"
    }
  }
}
