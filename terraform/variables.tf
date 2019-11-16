variable "gcp_credentials" {}

variable "gcp_project" {}

# ~ 😸 ~ # Gitlab Vars # ~ 😸 ~ #

# ~ 🧠 ~ # TensorBoard Vars # ~ 🧠 ~ #

# ~ ⛅ ~ # Confluent Cloud Vars # ~ ⛅ ~ #

variable "ccloud_broker_list" {}

variable "ccloud_access_key" {}

variable "ccloud_secret_key" {}

variable "ccloud_sr_access_key" {}

variable "ccloud_sr_secret_key" {}

variable "ccloud_schema_registry_url" {}

variable "ccloud_schema_registry_basic_auth" {}

variable "global_prefix" {
  default = "ccloud-tools"
}

variable "gcp_region" {
  default = "europe-west1"
}

variable "gcp_availability_zones" {
  type = "list"
  default = ["europe-west1-b"]
}

variable "instance_count" {
  type = "map"
  default = {
    "rest_proxy"       =  0
    "kafka_connect"    =  1
    "ksql_server"      =  1
    "control_center"   =  1
  }
}

variable "confluent_platform_location" {
  default = "http://packages.confluent.io/archive/5.3/confluent-5.3.1-2.12.zip"
}

variable "confluent_home_value" {
  default = "/etc/confluent/confluent-5.3.1"
}