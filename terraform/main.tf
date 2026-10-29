# ---------- The cluster ----------
# One control-plane node running in a Docker container on your Mac.
# Terraform also adds a "kind-simplemail" context to ~/.kube/config so kubectl can reach it.
resource "kind_cluster" "this" {
  name           = var.cluster_name
  wait_for_ready = true

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"
    }
  }
}

# ---------- Connect the kubernetes provider to the new cluster ----------
# The credentials come straight from the cluster resource above, so there is nothing to configure by hand.
provider "kubernetes" {
  host                   = kind_cluster.this.endpoint
  client_certificate     = kind_cluster.this.client_certificate
  client_key             = kind_cluster.this.client_key
  cluster_ca_certificate = kind_cluster.this.cluster_ca_certificate
}

# ---------- The app's namespace ----------
resource "kubernetes_namespace_v1" "app" {
  metadata {
    name = var.namespace
    labels = {
      app          = "simplemail"
      "managed-by" = "terraform"
    }
  }
}
