#!/usr/bin/env bash

cat > /usr/sbin/jupyter-backup.sh <<- "EOF"
${JUPYTER_GITHUB_BACKUP}
EOF
chmod a+x /usr/sbin/jupyter-backup.sh

echo "* * * * * /usr/sbin/jupyter-backup.sh" > /tmp/crontab-root
crontab -u root /tmp/crontab-root

gcloud compute instances add-metadata \
 --zone ${ZONE} ${NAME}-notebook-tf --metadata start=$(date +%Y%m%d_%H%M%S)

export WORKSPACE=/home/jupyter/event-driven-ml

git clone https://github.com/DivLoic/event-driven-ml.git $WORKSPACE && cd $WORKSPACE

[[ ! -z "${BRANCH}" ]] && git checkout -b ${BRANCH} origin/${BRANCH}
