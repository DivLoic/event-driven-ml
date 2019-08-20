###########################################
################# Outputs #################
###########################################

output "rest-proxy" {

  value = "${var.instance_count["rest_proxy"] >= 1
           ? "${join(",", formatlist("http://%s", google_compute_global_address.rest_proxy.*.address))}"
           : "REST Proxy has been disabled"}"

}

output "kafka-connect" {

  value = "${var.instance_count["kafka_connect"] >= 1
           ? "${join(",", formatlist("http://%s", google_compute_global_address.kafka_connect.*.address))}"
           : "Kafka Connect has been disabled"}"

}

output "ksql-server" {

  value = "${var.instance_count["ksql_server"] >= 1
           ? "${join(",", formatlist("http://%s", google_compute_global_address.ksql_server.*.address))}"
           : "KSQL Server has been disabled"}"

}

/*
output "control-center" {

  value = "https://${data.google_compute_global_address.control-center-ip.self_link}"

}*/
