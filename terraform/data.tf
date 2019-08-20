data "google_compute_ssl_certificate" "control-center-cert" {
  name = "ccenter-cert"
}

data "google_compute_global_address" "control-center-ip" {
  name = "control-center-ip"
}

data "google_compute_lb_ip_ranges" "ranges" {}