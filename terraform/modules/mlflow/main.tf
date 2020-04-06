
resource "google_compute_subnetwork" "mlflow_subnet" {
  name          = "mlflow-subnet"
  project       = var.gcp_project
  region        = var.gcp_region
  network       = var.network
  ip_cidr_range = "10.0.1.0/28"
}

module "mlflow_container_config" {
  source = "github.com/terraform-google-modules/terraform-google-container-vm"
  container = {
    image   = "gcr.io/event-driven-ml/mlflow-server:0.1.0"
    command = ["mlflow"]
    args    = ["server", "--host", "0.0.0.0", "--backend-store-uri", "/gce/mlruns/"]
    env = [
      {
        name  = "env"
        value = "dev"
      }
    ],
    volumeMounts = [
      {
        mountPath = "/gce/"
        name      = "mlflow-disk"
        readOnly  = false
      }
    ]
  }
  volumes = [
    {
      name = "mlflow-disk"
      gcePersistentDisk = {
        pdName = "mlflow-disk"
        fsType = "ext4"
      }
    }
  ]
}

resource "google_compute_instance" "mlflow" {

  name                      = "mlflow"
  zone                      = "europe-west2-b"
  machine_type              = "n1-standard-2"
  allow_stopping_for_update = true
  metadata_startup_script   = data.template_file.mlflow_startup.rendered
  service_account {
    scopes = ["cloud-platform"]
  }

  tags = ["mlflow"]
  network_interface {
    network    = google_compute_subnetwork.mlflow_subnet.network
    subnetwork = google_compute_subnetwork.mlflow_subnet.self_link
    access_config {}
  }

  attached_disk {
    mode        = "READ_WRITE"
    source      = "projects/event-driven-ml/zones/europe-west2-b/disks/mlflow-disk"
    device_name = "mlflow-disk"
  }

  boot_disk {

    initialize_params {
      type  = "pd-standard"
      image = module.mlflow_container_config.source_image
    }
  }

  metadata = {
    google-logging-enabled    = "true"
    gce-container-declaration = module.mlflow_container_config.metadata_value
  }

  labels = {
    container-vm = module.mlflow_container_config.vm_container_label
  }
}

resource "google_compute_instance_group" "mlflow" {
  name = "mlflow-group"
  zone = "europe-west2-b"
  instances = [
    google_compute_instance.mlflow.self_link
  ]

  named_port {
    name = "http"
    port = 5000
  }
}

resource "google_compute_health_check" "mlflow" {
  name = "mlflow-http-health-check"

  healthy_threshold   = 3
  unhealthy_threshold = 3
  check_interval_sec  = 5
  timeout_sec         = 3

  http_health_check {
    port         = "5000"
    request_path = "/"
  }
}

resource "google_compute_backend_service" "mlflow" {

  name        = "mlflow-backend-service"
  port_name   = "http"
  protocol    = "HTTP"
  timeout_sec = 120

  backend {
    group = google_compute_instance_group.mlflow.self_link
  }
  health_checks = [
    google_compute_health_check.mlflow.self_link
  ]
}

resource "google_compute_url_map" "mlflow" {
  name            = "mlflow-url-map"
  default_service = google_compute_backend_service.mlflow.self_link
}

resource "google_compute_target_https_proxy" "mlflow" {
  name    = "mlflow-http-proxy"
  url_map = google_compute_url_map.mlflow.self_link
  ssl_certificates = [
  data.google_compute_ssl_certificate.mlflow-cert.self_link]
}

resource "google_compute_global_forwarding_rule" "mlflow" {
  name       = "mlflow-frontend"
  port_range = "443"
  target     = google_compute_target_https_proxy.mlflow.self_link
  ip_address = data.google_compute_global_address.mlflow-ip.self_link
}

resource "google_compute_firewall" "mlflow" {

  name    = "mlflow-firewall"
  network = google_compute_subnetwork.mlflow_subnet.network

  allow {
    protocol = "tcp"
    ports    = ["5000"]
  }
  target_tags = ["mlflow"]
  source_ranges = concat(
    data.google_compute_lb_ip_ranges.ranges.http_ssl_tcp_internal
  )
}