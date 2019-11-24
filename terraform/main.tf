terraform {
  backend "gcs" {
    bucket = "edml"
    prefix = "/metadata"
    credentials = ".terraform/edml-gcp-access-key.json"
  }
}

provider "google" {
  credentials = file(var.gcp_credentials)
  project = var.gcp_project
  region = "europe-west1"
}

resource "google_container_cluster" "apps-kube" {

  name = "edml-apps-kube"
  location = "europe-west1-b"

  initial_node_count = 5
}

module "gitlab" {
  source = "./modules/gitlab"
  gcp_credentials =  var.gcp_project
  gcp_project = var.gcp_project
  gcp_region = var.gcp_region
}

module "tensorboard" {
  source = "./modules/tensorboard"
  gcp_credentials =  var.gcp_project
  gcp_project = var.gcp_project
  gcp_region = var.gcp_region
}
