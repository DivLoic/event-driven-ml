terraform {
  backend "gcs" {
    bucket      = "edml"
    prefix      = "/metadata"
    credentials = ".terraform/edml-gcp-access-key.json"
  }
}

provider "google" {
  credentials = file(var.gcp_credentials)
  project     = var.gcp_project
  region      = "europe-west2"
}

resource "google_container_cluster" "apps-kube" {
  name               = "edml-apps-kube"
  location           = "europe-west2-b"
  initial_node_count = 5
}

module "gitlab" {
  source          = "./modules/gitlab"
  gcp_credentials = var.gcp_project
  gcp_project     = var.gcp_project
  gcp_region      = "europe-west2"
  network         = google_compute_network.main.id
}

module "tensorboard" {
  source          = "./modules/tensorboard"
  gcp_credentials = var.gcp_project
  gcp_project     = var.gcp_project
  gcp_region      = var.gcp_region
  network         = google_compute_network.main.id
}

module "mlflow" {
  source          = "./modules/mlflow"
  gcp_credentials = var.gcp_project
  gcp_project     = var.gcp_project
  gcp_region      = "europe-west2"
  network         = google_compute_network.main.id
}

module "jupyter" {
  source        = "./modules/jupyter"
  gcp_project   = var.gcp_project
  gcp_region    = "europe-west1"
  gcp_zone      = "europe-west1-b"
  network       = google_compute_network.main.id
  notebook_name = "edml-jupyter"
  machine_type  = "n1-standard-8"
  disk_size     = "500"
  github_user   = var.github_user
  github_token  = var.github_token
  git_branch    = var.ai_platform_gitbranch
}

module "ksql" {
  source                      = "./modules/ksql"
  gcp_project                 = var.gcp_project
  gcp_region                  = var.gcp_region
  global_prefix               = var.global_prefix
  gcp_credentials             = var.gcp_project
  gcp_availability_zones      = var.gcp_availability_zones
  confluent_cloud_conf        = var.confluent_cloud_conf
  confluent_home_value        = var.confluent_home_value
  confluent_platform_location = var.confluent_platform_location
  network                     = google_compute_network.main.id
}