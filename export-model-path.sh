#!/usr/bin/env bash

# $1 module name (format: edml-trainer)
# $2 edml revision version (format: 1.1.1-20200414151830-97a4b33)

APP=$1

VERSION=$2

JOB_NAME=$(echo ${APP}-${VERSION} | tr '-' '_' | tr '.' '_')

JOB_NUM=$(gcloud ai-platform jobs list | grep ${JOB_NAME} | wc -l | xargs -I{} echo "{}-1" | bc)

JOB_DIR=$(gcloud ai-platform jobs describe ${JOB_NAME}_${JOB_NUM} --format="value(trainingInput.jobDir)")

echo "export MODEL_PATH=$(gsutil ls ${JOB_DIR}/model/export/exporter/ | sort | tail -1)"