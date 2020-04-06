resource "google_compute_network" "main" {
  name                    = var.global_prefix
  auto_create_subnetworks = false
}

resource "google_compute_firewall" "main-ssh-access" {

  name    = "main-ssh-access"
  network = google_compute_network.main.name
  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
  source_ranges = ["0.0.0.0/0"]
  target_tags = [
    "gitlab",
    //"ksql"
    //"mlflow"
    //"jupyter"
    //"tensorboard"
  ]
}