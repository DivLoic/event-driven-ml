#!/usr/bin/env bash

sudo curl -L --output /usr/local/bin/gitlab-runner \
 https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-linux-amd64

sudo chmod +x /usr/local/bin/gitlab-runner
curl -sSL https://get.docker.com/ | sh
sudo useradd --comment 'GitLab Runner' --create-home gitlab-runner --shell /bin/bash

sudo gitlab-runner install --user=gitlab-runner --working-directory=/home/gitlab-runner
sudo gitlab-runner start

cat > /home/gitlab-runner/register-all.sh <<- "EOF"
${REGISTER_SCRIPT}
EOF