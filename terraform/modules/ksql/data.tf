data "template_file" "ksql_server_properties" {

  template = file("${path.module}/util/ksql-server.properties")

  vars = {

    global_prefix = "${var.global_prefix}"
    broker_list = "${var.ccloud_broker_list}"
    access_key = "${var.ccloud_access_key}"
    secret_key = "${var.ccloud_secret_key}"
    sr_access_key = "${var.ccloud_sr_access_key}"
    sr_secret_key = "${var.ccloud_sr_secret_key}"
    confluent_home_value = "${var.confluent_home_value}"

    schema_registry_url = "${var.ccloud_schema_registry_url}"
    schema_registry_basic_auth = "${var.ccloud_schema_registry_basic_auth}"

  }

}

data "template_file" "ksql_server_bootstrap" {

  template = file("${path.module}/util/ksql-server.sh")

  vars = {

    confluent_platform_location = "${var.confluent_platform_location}"
    ksql_server_properties = "${data.template_file.ksql_server_properties.rendered}"
    confluent_home_value = "${var.confluent_home_value}"

  }

}