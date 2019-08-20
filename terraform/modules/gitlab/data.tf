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

data "template_file" "gitlab-runner-startup" {
  template = file("${path.module}/utils/gitlab-runner-startup.sh")

  vars = {
    REGISTER_SCRIPT = data.template_file.gitlab-runner-register.rendered
  }
}

data "template_file" "gitlab-runner-register" {
  template = file("${path.module}/utils/register-all.sh")
}