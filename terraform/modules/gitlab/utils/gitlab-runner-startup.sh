#!/usr/bin/env bash

sudo curl -L --output /usr/local/bin/gitlab-runner \
 https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-linux-amd64

sudo chmod +x /usr/local/bin/gitlab-runner
curl -sSL https://get.docker.com/ | sh
sudo useradd --comment 'GitLab Runner' --create-home gitlab-runner --shell /bin/bash

sudo gitlab-runner install --user=gitlab-runner --working-directory=/home/gitlab-runner
sudo gitlab-runner start

sudo gitlab-runner verify --delete

cat > /etc/gitlab-runner/config.toml <<- "EOF"
${RUNNER_CONFIG}
EOF

gitlab-runner verify

