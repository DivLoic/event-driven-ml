
resource "google_compute_network" "gitbal" {
  name = "gitlab-network"
  auto_create_subnetworks = false
}

# Subnet
resource "google_compute_subnetwork" "private-gitlab-subnet" {
  name = "private-gitlab-subnet"
  project = "${var.gcp_project}"
  region = "${var.gcp_region}"
  network = "${google_compute_network.gitbal.id}"
  ip_cidr_range = "10.0.1.0/24"
}

resource "google_compute_subnetwork" "public-gitlab-subnet" {

  name = "public-gitlab-subnet"
  project = "${var.gcp_project}"
  region = "${var.gcp_region}"
  network = "${google_compute_network.gitbal.id}"
  ip_cidr_range = "10.0.2.0/24"
}

resource "google_compute_instance_from_template" "gitlab" {

  name = "gitlab"
  zone = "europe-west1-b"
  machine_type = "n1-standard-1"
  network_interface {
    network = google_compute_network.gitbal.self_link
    subnetwork = google_compute_subnetwork.private-gitlab-subnet.self_link
    access_config {
      nat_ip = data.google_compute_address.gitlab-scm-ip.address
    }
  }
  attached_disk {
    source = "projects/event-driven-ml/zones/europe-west1-b/disks/gitlab-disk"
    mode = "READ_WRITE"
    device_name = "gitlab-disk"
  }
  source_instance_template = "projects/event-driven-ml/global/instanceTemplates/gitlab-template-8"

  tags = ["gitlab"]
}

resource "google_compute_instance" "gitlab-runner" {

  name = "gitlab-runner"
  zone = "europe-west1-b"
  machine_type = "n1-standard-2"
  allow_stopping_for_update = true
  metadata_startup_script = data.template_file.gitlab-runner-startup.rendered
  service_account {
    scopes = ["cloud-platform"]
  }
  tags = ["gitlab"]
  network_interface {
    network = google_compute_network.gitbal.self_link
    subnetwork = google_compute_subnetwork.private-gitlab-subnet.self_link
    access_config {}
  }
  boot_disk {
    initialize_params {
      size = 50
      image = "ubuntu-1804-bionic-v20191002"
    }
  }
}

resource "google_compute_instance_group" "gitlab" {
  name = "gitlab-group"
  zone = "europe-west1-b"
  instances = [
    "${google_compute_instance_from_template.gitlab.self_link}"
  ]

  # startup script
  # sudo mkdir -p /mnt/disks/gitlab-disk
  # sudo chmod a+w /mnt/disks/gitlab-disk
  # sudo mkfs.ext4 -m 0 -F -E lazy_itable_init=0,lazy_journal_init=0,discard /dev/sdb
  # sudo mount -o discard,defaults /dev/sdb /mnt/disks/gitlab-disk
}

resource "google_compute_health_check" "gitlab" {
  name = "gitlab-http-health-check"

  healthy_threshold = 1
  timeout_sec        = 120
  check_interval_sec = 300
  unhealthy_threshold = 10


  http_health_check {
    port = "80"
    request_path = "/users/sign_in/"
  }
}

resource "google_compute_backend_service" "gitlab" {

  name = "gitlab-backend-service"
  port_name = "http"
  protocol = "HTTP"
  timeout_sec = 120

  backend {
    group = "${google_compute_instance_group.gitlab.self_link}"
  }
  health_checks = ["${google_compute_health_check.gitlab.self_link}"]
}

resource "google_compute_url_map" "gitlab" {
  name = "gitlab-url-map"
  default_service = "${google_compute_backend_service.gitlab.self_link}"
}

resource "google_compute_target_https_proxy" "gitlab" {
  name = "gitlab-http-proxy"
  url_map = "${google_compute_url_map.gitlab.self_link}"

  ssl_certificates = ["${data.google_compute_ssl_certificate.gitlab-cert.self_link}"]
}

resource "google_compute_global_forwarding_rule" "gitlab" {

  name = "gitlab-frontend"
  port_range = "443"
  target = "${google_compute_target_https_proxy.gitlab.self_link}"
  ip_address = "${data.google_compute_global_address.gitlab-ip.self_link}"
}

resource "google_compute_firewall" "gitlab" {

  name = "gitlab-firewall"
  network = google_compute_network.gitbal.name
  allow {

    protocol = "tcp"
    ports = ["80"]
  }
  target_tags = ["gitlab"]
  source_ranges = concat(
    data.google_compute_lb_ip_ranges.ranges.http_ssl_tcp_internal,
    [google_compute_subnetwork.private-gitlab-subnet.ip_cidr_range]
  )
}


resource "google_compute_firewall" "gitlab-ssh" {

  name = "gitlab-ssh-firewall"
  network = google_compute_network.gitbal.name
  allow {
    protocol = "tcp"
    ports = ["22", "2222", "9418"]
  }
  source_ranges = ["0.0.0.0/0"]
  target_tags = ["gitlab"]
}