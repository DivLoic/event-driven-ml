data "google_compute_lb_ip_ranges" "ranges" {}

data "google_compute_ssl_certificate" "tensorboard-cert" {
  name = "tensorboard-cert"
}

data "google_compute_global_address" "tensorboard-ip" {
  name = "tensorboard-ip"
}

data "template_file" "tensorboard-startup" {
  template = file("${path.module}/utils/tensorboard-startup.sh")
}