data "template_file" "jupyter-github-backup" {
  template = file("${path.module}/utils/jupyter-github-backup.sh")

  vars = {
    GITHUB_USER = var.github_user
    GITHUB_TOKEN = var.github_token
  }
}

data "template_file" "jupyter_startup" {
  template = file("${path.module}/utils/jupyter-startup.sh")

  vars = {
    ZONE = var.gcp_zone
    NAME = var.notebook_name
    BRANCH = var.git_branch
    JUPYTER_GITHUB_BACKUP = data.template_file.jupyter-github-backup.rendered
  }
}