variable "gcp_credentials" {}

variable "gcp_project" {}

# ~ 😸 ~ # Gitlab Vars # ~ 😸 ~ #

# ~ 🧠 ~ # TensorBoard Vars # ~ 🧠 ~ #

# ~ ⛅ ~ # Confluent Cloud Vars # ~ ⛅ ~ #

variable "confluent_cloud_conf" {
  type = "map"

  default = {
    ccloud_broker_list                = null
    ccloud_access_key                 = null
    ccloud_secret_key                 = null
    ccloud_sr_access_key              = null
    ccloud_sr_secret_key              = null
    ccloud_schema_registry_url        = null
    ccloud_schema_registry_basic_auth = null
  }
}

variable "global_prefix" {
  default = "edml"
}

variable "gcp_region" {
  default = "europe-west2"
}

variable "gcp_availability_zones" {
  type    = "list"
  default = ["europe-west2-b"]
}

variable "instance_count" {
  type = "map"
  default = {
    "ksql_server" = 1
  }
}

variable "confluent_platform_location" {
  default = "http://packages.confluent.io/archive/5.3/confluent-5.3.1-2.12.zip"
}

variable "confluent_home_value" {
  default = "/etc/confluent/confluent-5.3.1"
}

variable "github_user" {}

variable "github_token" {}

variable "ai_platform_gitbranch" {
  default = ""
}