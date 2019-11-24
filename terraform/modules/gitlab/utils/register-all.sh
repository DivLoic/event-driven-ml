#!/usr/bin/env bash

sudo gitlab-runner register \
  --non-interactive \
  --url "http://gitlab.europe-west1-b.c.event-driven-ml.internal:80/" \
  --clone-url "http://gitlab.europe-west1-b.c.event-driven-ml.internal:80/" \
  --registration-token ${TOKEN} \
  --executor "docker" \
  --docker-image gcr.io/event-driven-ml/edml-cicd:0.1.0 \
  --description "docker-runner" \
  --tag-list "docker,gcloud,gradle,tensorflow,dev" \
  --run-untagged="true" \
  --locked="false" \
  --access-level="not_protected"

sudo gitlab-runner register \
  --non-interactive \
  --url "http://gitlab.europe-west1-b.c.event-driven-ml.internal:80/" \
  --clone-url "http://gitlab.europe-west1-b.c.event-driven-ml.internal:80/" \
  --registration-token ${TOKEN} \
  --executor "docker" \
  --docker-image gcr.io/event-driven-ml/edml-cicd:0.1.0 \
  --description "docker-runner" \
  --tag-list "docker,gcloud,gradle,tensorflow,staging" \
  --run-untagged="true" \
  --locked="false" \
  --access-level="not_protected"

sudo gitlab-runner register \
  --non-interactive \
  --url "http://gitlab.europe-west1-b.c.event-driven-ml.internal:80/" \
  --clone-url "http://gitlab.europe-west1-b.c.event-driven-ml.internal:80/" \
  --registration-token ${TOKEN} \
  --executor "docker" \
  --docker-image gcr.io/event-driven-ml/edml-cicd:0.1.0 \
  --description "docker-runner" \
  --tag-list "docker,gcloud,gradle,tensorflow,prod" \
  --run-untagged="true" \
  --locked="false" \
  --access-level="not_protected"                                                                                                                       32,5          All