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

module "gitlab" {
  source = "./modules/gitlab"
  gcp_credentials =  var.gcp_project
  gcp_project = var.gcp_project
  gcp_region = var.gcp_region
}

resource "google_container_cluster" "apps-kube" {

  name = "edml-apps-kube"
  location = "europe-west1-b"

  initial_node_count = 5
}

module "tensorboard" {
  source = "./modules/tensorboard"
  gcp_credentials =  var.gcp_project
  gcp_project = var.gcp_project
  gcp_region = var.gcp_region
}

module "ksql" {
  source = "./modules/ksql"
  gcp_project = var.gcp_project
  gcp_region = var.gcp_region
  global_prefix = var.global_prefix
  gcp_credentials =  var.gcp_project
  gcp_availability_zones = var.gcp_availability_zones
  ccloud_access_key = var.ccloud_access_key
  ccloud_broker_list = var.ccloud_broker_list
  ccloud_secret_key = var.ccloud_secret_key
  ccloud_sr_access_key = var.ccloud_sr_access_key
  ccloud_sr_secret_key = var.ccloud_sr_secret_key
  ccloud_schema_registry_url = var.ccloud_schema_registry_url
  ccloud_schema_registry_basic_auth = var.ccloud_schema_registry_basic_auth
  confluent_home_value = var.confluent_home_value
  confluent_platform_location = var.confluent_platform_location
  private_subnet = google_compute_subnetwork.private_subnet
}

output "ksql-server" {
  value = "http://${module.ksql.ksql_server_address}"
}

