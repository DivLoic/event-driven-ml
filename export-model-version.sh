#!/usr/bin/env bash

# $1 module name (format: edml-trainer)
# $2 edml revision version (format: 1.1.1-20200414151830-97a4b33)

APP=$1

VERSION=$2

JOB_NAME=$(echo ${APP}-${VERSION} | tr '-' '_' | tr '.' '_')

JOB_NUM=$(gcloud ai-platform jobs list | grep ${JOB_NAME} | wc -l | xargs -I{} echo "{}-1" | bc)

echo "export MODEL_VERSION=${VERSION}-${JOB_NUM}"