variable "gcp_credentials" {}

variable "gcp_project" {}

variable "gcp_region" {}

variable "mnt_point" {
  default = "/mnt/disks/gce-containers-mounts/tmpfss"
}

variable "network" {}