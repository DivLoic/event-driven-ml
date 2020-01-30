resource "google_compute_network" "default" {

  name = "${var.global_prefix}"
  auto_create_subnetworks = false

}

resource "google_compute_subnetwork" "private_subnet" {

  name = "private-subnet-${var.global_prefix}"
  project = "${var.gcp_project}"
  region = "${var.gcp_region}"
  network = "${google_compute_network.default.id}"
  ip_cidr_range = "10.0.1.0/24"

}

resource "google_compute_subnetwork" "public_subnet" {

  name = "public-subnet-${var.global_prefix}"
  project = "${var.gcp_project}"
  region = "${var.gcp_region}"
  network = "${google_compute_network.default.id}"
  ip_cidr_range = "10.0.2.0/24"

}

resource "google_compute_firewall" "rest_proxy" {

  count = "${var.instance_count["rest_proxy"] >= 1 ? 1 : 0}"
  
  name = "rest-proxy-${var.global_prefix}"
  network = "${google_compute_network.default.name}"

  allow {

    protocol = "tcp"
    ports = ["22", "8082"]

  }

  source_ranges = ["0.0.0.0/0"]
  target_tags = ["rest-proxy-${var.global_prefix}"]

}

resource "google_compute_firewall" "kafka_connect" {

  count = "${var.instance_count["kafka_connect"] >= 1 ? 1 : 0}"

  name = "kafka-connect-${var.global_prefix}"
  network = "${google_compute_network.default.name}"

  allow {

    protocol = "tcp"
    ports = ["22", "8083"]

  }

  source_ranges = ["0.0.0.0/0"]
  target_tags = ["kafka-connect-${var.global_prefix}"]

}

resource "google_compute_firewall" "control_center" {

  count = "${var.instance_count["control_center"] >= 1 ? 1 : 0}"

  name = "control-center-${var.global_prefix}"
  network = google_compute_subnetwork.private_subnet.network#"${google_compute_network.default.name}"
  allow {
    protocol = "tcp"
    ports = ["9021"]
  }
  source_ranges = concat("${data.google_compute_lb_ip_ranges.ranges.http_ssl_tcp_internal}")
  target_tags = ["control-center-${var.global_prefix}"]
}

resource "google_compute_firewall" "control-ssh-center" {

  count = "${var.instance_count["control_center"] >= 1 ? 1 : 0}"

  name = "control-center-ssh-${var.global_prefix}"
  network = "${google_compute_network.default.name}"
  allow {
    protocol = "tcp"
    ports = ["22"]
  }
  source_ranges = ["0.0.0.0/0"]
  target_tags = ["control-center-${var.global_prefix}"]
}