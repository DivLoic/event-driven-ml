#!/usr/bin/env bash

git config --global user.name Github Action
git config --global user.email octocat@github.com

mkdir -p /tmp/repo/event-driven-ml/
cp -R /home/jupyter/event-driven-ml/ /tmp/repo/
cd /tmp/repo/event-driven-ml/
git checkout -b wip/ai-platform/backup
git add -A
git commit -a -m "AI Platform backup: $(date +"%Y-%m-%d %H:%M:%S")" --allow-empty
git remote add github https://${GITHUB_USER}:${GITHUB_TOKEN}@github.com/DivLoic/event-driven-ml.git
git push -u -f github wip/ai-platform/backup && cd ~/ && rm -rf  /tmp/repo/event-driven-ml/