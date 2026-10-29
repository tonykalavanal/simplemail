variable "cluster_name" {
  description = "Name of the local kind cluster"
  type        = string
  default     = "simplemail"
}

variable "namespace" {
  description = "Kubernetes namespace for the app"
  type        = string
  default     = "simplemail"
}
