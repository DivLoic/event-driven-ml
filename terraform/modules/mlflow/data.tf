data "google_compute_lb_ip_ranges" "ranges" {}

data "google_compute_ssl_certificate" "mlflow-cert" {
  name = "mlflow-cert"
}

data "google_compute_global_address" "mlflow-ip" {
  name = "mlflow-ip"
}

data "template_file" "mlflow_startup" {
  template = file("${path.module}/utils/mlflow-startup.sh")
}