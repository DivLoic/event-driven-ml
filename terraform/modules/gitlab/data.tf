data "google_compute_ssl_certificate" "gitlab-cert" {
  name = "gitlab-cert"
}

data "google_compute_global_address" "gitlab-ip" {
  name = "gitlab-ip"
}

data "google_compute_address" "gitlab-scm-ip" {
  name = "gitlab-scm-ip"
}

data "google_compute_lb_ip_ranges" "ranges" {}

data "template_file" "gitlab-runner-config" {
  template = file("${path.module}/utils/config.toml")
}

data "template_file" "gitlab-runner-startup" {
  template = file("${path.module}/utils/gitlab-runner-startup.sh")

  vars = {
    # VERSION="1.5.0"
    # OS="linux"  # or "darwin" for OSX, "windows" for Windows.
    # ARCH="amd64"  # or "386" for 32-bit OSs
    # DL_LINK="https://github.com/GoogleCloudPlatform/docker-credential-gcr/releases/download/"
    # ACCESS_KEY = data.template_file.access-key.rendered
    RUNNER_CONFIG = data.template_file.gitlab-runner-config.rendered
  }
}