# @🌏 # REST Proxy Bootstrap # @🌏 #

data "template_file" "rest_proxy_properties" {

  template = "${file("util/rest-proxy.properties")}"

  vars = {

    broker_list = "${var.ccloud_broker_list}"
    access_key = "${var.ccloud_access_key}"
    secret_key = "${var.ccloud_secret_key}"
    confluent_home_value = "${var.confluent_home_value}"

    schema_registry_url = "${var.ccloud_schema_registry_url}"
    schema_registry_basic_auth = "${var.ccloud_schema_registry_basic_auth}"

  }

}

data "template_file" "rest_proxy_bootstrap" {

  template = "${file("util/rest-proxy.sh")}"

  vars = {

    confluent_platform_location = "${var.confluent_platform_location}"
    rest_proxy_properties = "${data.template_file.rest_proxy_properties.rendered}"
    confluent_home_value = "${var.confluent_home_value}"

  }

}

# 🔗 # Kafka Connect Bootstrap # 🔗 #

data "template_file" "kafka_connect_properties" {

  template = "${file("util/kafka-connect.properties")}"

  vars = {

    global_prefix = "${var.global_prefix}"
    broker_list = "${var.ccloud_broker_list}"
    access_key = "${var.ccloud_access_key}"
    secret_key = "${var.ccloud_secret_key}"
    confluent_home_value = "${var.confluent_home_value}"

    schema_registry_url = "${var.ccloud_schema_registry_url}"
    schema_registry_basic_auth = "${var.ccloud_schema_registry_basic_auth}"

  }

}

data "template_file" "kafka_connect_bootstrap" {

  template = "${file("util/kafka-connect.sh")}"

  vars = {

    confluent_platform_location = "${var.confluent_platform_location}"
    kafka_connect_properties = "${data.template_file.kafka_connect_properties.rendered}"
    confluent_home_value = "${var.confluent_home_value}"

  }

}

# 🤖 # Control Center Bootstrap # 🤖 #

data "template_file" "control_center_properties" {

  template = "${file("util/control-center.properties")}"

  vars = {

    global_prefix = "${var.global_prefix}"
    broker_list = "${var.ccloud_broker_list}"
    access_key = "${var.ccloud_access_key}"
    secret_key = "${var.ccloud_secret_key}"
    confluent_home_value = "${var.confluent_home_value}"

    schema_registry_url = "${var.ccloud_schema_registry_url}"
    schema_registry_basic_auth = "${var.ccloud_schema_registry_basic_auth}"
    kafka_connect_url = "http://${google_compute_global_address.kafka_connect[0].address}"
    ksql_server_url = "http://module.ksql.ksql_server_address:80"
    ksql_public_url = "http://module.ksql.ksql_server_address:80"
    #ksql_server_url = "http://${module.ksql.ksql_server_address}:80"
    #ksql_public_url = "http://${module.ksql.ksql_server_address}:80"
  }
}

data "template_file" "control_center_bootstrap" {

  template = "${file("util/control-center.sh")}"

  vars = {

    confluent_platform_location = "${var.confluent_platform_location}"
    control_center_properties = "${data.template_file.control_center_properties.rendered}"
    confluent_home_value = "${var.confluent_home_value}"

  }

}