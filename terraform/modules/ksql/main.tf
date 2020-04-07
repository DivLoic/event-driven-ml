
resource "google_compute_subnetwork" "ksql_subnet" {
  name          = "ksql-subnet"
  project       = var.gcp_project
  region        = var.gcp_region
  network       = var.network
  ip_cidr_range = "10.0.0.0/28"
}

resource "google_compute_firewall" "ksql_server" {

  name    = "ksql-server-${var.global_prefix}"
  network = google_compute_subnetwork.ksql_subnet.network

  allow {

    protocol = "tcp"
    ports    = ["22", "8088"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["ksql-server-${var.global_prefix}"]
}


resource "google_compute_global_address" "ksql_server" {
  name = "ksql-server-global-address-${var.global_prefix}"
}

resource "google_compute_global_forwarding_rule" "ksql_server" {
  name       = "ksql-server-global-forwarding-rule-${var.global_prefix}"
  target     = google_compute_target_http_proxy.ksql_server.self_link
  ip_address = google_compute_global_address.ksql_server.self_link
  port_range = "80"
}

resource "google_compute_target_http_proxy" "ksql_server" {
  name    = "ksql-server-http-proxy-${var.global_prefix}"
  url_map = google_compute_url_map.ksql_server.self_link

}

resource "google_compute_url_map" "ksql_server" {
  name            = "ksql-server-url-map-${var.global_prefix}"
  default_service = google_compute_backend_service.ksql_server.self_link

}

resource "google_compute_backend_service" "ksql_server" {
  name        = "ksql-server-backend-service-${var.global_prefix}"
  port_name   = "http"
  protocol    = "HTTP"
  timeout_sec = 5

  backend {

    group = google_compute_region_instance_group_manager.ksql_server.instance_group
  }

  health_checks = [
    google_compute_http_health_check.ksql_server.self_link
  ]
}

resource "google_compute_region_instance_group_manager" "ksql_server" {
  target_size               = 1
  name                      = "ksql-server-instance-group-${var.global_prefix}"
  instance_template         = google_compute_instance_template.ksql_server.self_link
  base_instance_name        = "ksql-server"
  region                    = var.gcp_region
  distribution_policy_zones = var.gcp_availability_zones

  named_port {

    name = "http"
    port = 8088
  }
}

resource "google_compute_http_health_check" "ksql_server" {
  name                = "ksql-server-http-health-check-${var.global_prefix}"
  request_path        = "/info"
  port                = "8088"
  healthy_threshold   = 3
  unhealthy_threshold = 3
  check_interval_sec  = 5
  timeout_sec         = 3

}

resource "google_compute_instance_template" "ksql_server" {
  name         = "ksql-server-${var.global_prefix}-template"
  machine_type = "n1-standard-8"

  metadata_startup_script = data.template_file.ksql_server_bootstrap.rendered

  disk {
    source_image = "centos-7"
    disk_size_gb = 300
  }

  network_interface {
    access_config {}
    subnetwork = google_compute_subnetwork.ksql_subnet.self_link
  }

  tags = ["ksql"]
}