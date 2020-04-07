data "template_file" "ksql_server_properties" {

  template = file("${path.module}/util/ksql-server.properties")

  vars = {

    global_prefix        = var.global_prefix
    broker_list          = var.confluent_cloud_conf["broker_list"]
    access_key           = var.confluent_cloud_conf["access_key"]
    secret_key           = var.confluent_cloud_conf["secret_key"]
    sr_access_key        = var.confluent_cloud_conf["sr_access_key"]
    sr_secret_key        = var.confluent_cloud_conf["sr_secret_key"]
    confluent_home_value = var.confluent_home_value

    schema_registry_url        = var.confluent_cloud_conf["schema_registry_url"]
    schema_registry_basic_auth = var.confluent_cloud_conf["schema_registry_basic_auth"]
  }
}

data "template_file" "ksql_server_bootstrap" {

  template = file("${path.module}/util/ksql-server.sh")

  vars = {

    confluent_home_value        = var.confluent_home_value
    confluent_platform_location = var.confluent_platform_location
    ksql_server_properties      = data.template_file.ksql_server_properties.rendered
  }
}