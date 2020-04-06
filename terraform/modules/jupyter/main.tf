
resource "google_compute_subnetwork" "jupyter_subnet" {
  name = "jupyter-subnet"
  project = var.gcp_project
  region = var.gcp_region
  network = var.network
  ip_cidr_range = "10.0.2.0/28"
}

resource "google_compute_instance" "jupyter" {
  project      = var.gcp_project
  name         = "${var.notebook_name}-jupyter-tf"
  machine_type = var.machine_type
  zone         = var.gcp_zone

  tags = ["deeplearning-vm", "jupyter"]

  metadata_startup_script = data.template_file.jupyter_startup.rendered

  boot_disk {
    initialize_params {
      image = "deeplearning-platform-release/tf-latest-gpu"
      size  = var.disk_size
      type  = "pd-ssd"
    }
    auto_delete = true
  }

  network_interface {
    access_config {}
    network = google_compute_subnetwork.jupyter_subnet.network
    subnetwork = google_compute_subnetwork.jupyter_subnet.self_link
  }

  metadata = {
    title                 = "TensorFlow/Keras/Horovod.CUDA10.0"
    framework             = "TensorFlow:1.14"
    proxy-mode            = "service_account"
    #shutdown-script       = data.template_file.jupyter_shutdown.rendered
    install-nvidia-driver = "True"
  }

  service_account {
    scopes = [
      "https://www.googleapis.com/auth/cloud-platform",
      "https://www.googleapis.com/auth/userinfo.email"
    ]
  }

  guest_accelerator {
    type  = "nvidia-tesla-k80"
    count = 1
  }

  scheduling {
    on_host_maintenance = "TERMINATE"
  }
}
