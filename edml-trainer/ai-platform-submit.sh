#!/usr/bin/env bash

SOURCE="${BASH_SOURCE[0]}"

VERSION=$1
APP="${APP:-edml-trainer}"
BUCKET="${BUCKET:-edml}"
PACKAGE="${PACKAGE:-trainer/}"
MODULE="${MODULE:-trainer.task}"
REGION="${REGION:-europe-west1}"

JOB_NAME=$(echo ${APP}-${VERSION} | tr '-' '_' | tr '.' '_')

JOB_NUM=$(gcloud ai-platform jobs list | grep ${JOB_NAME} | wc -l | bc)

JOB_ID=${JOB_NAME}_${JOB_NUM}

JOB_DIR=gs://${BUCKET}/models/${APP}/${VERSION}-${JOB_NUM}

PYTHON_VERSION="${PYTHON_VERSION:-3.5}"
RUNTIME_VERSION="${RUNTIME_VERSION:-1.15}"

OUTDIR=${JOB_DIR}/model

echo "+  SUBMIT PARAMETERS +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
echo "+  JOB_NAME = ${JOB_NAME}"
echo "+  JOB_ID = ${JOB_ID}"
echo "+  VERSION = ${VERSION}"
echo "+  PACKAGE = ${PACKAGE}"
echo "+  MODULE = ${MODULE}"
echo "+  REGION = ${REGION}"
echo "+  JOB_DIR = ${JOB_DIR}"
echo "+  PYTHON_VERSION = ${PYTHON_VERSION}"
echo "+  RUNTIME_VERSION = ${RUNTIME_VERSION}"
echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"

TRAIN_NNSIZE="${TRAIN_NNSIZE:-20 10 5}"
TRAIN_NEMBEDS="${TRAIN_NEMBEDS:-10}"
TRAIN_BATCH_SIZE="${TRAIN_BATCH_SIZE:-128}"
TRAIN_EVALSTEP="${TRAIN_EVALSTEP:-1}"
TRAIN_EXAMPLES="${TRAIN_EXAMPLES:-500}" # 174000

echo ""
echo ""
echo ""
echo ""
echo "+  TRAIN PARAMETERS ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
echo "+  BUCKET = ${BUCKET}"
echo "+  OUTDIR = ${OUTDIR}"
echo "+  VERSION = ${VERSION}"
echo "+  TRAIN_EVALSTEP = ${TRAIN_EVALSTEP}"
echo "+  TRAIN_NNSIZE = ${TRAIN_NNSIZE}"
echo "+  TRAIN_NEMBEDS = ${TRAIN_NEMBEDS}"
echo "+  TRAIN_BATCH_SIZE = ${TRAIN_BATCH_SIZE}"
echo "+  TRAIN_EXAMPLES = ${TRAIN_EXAMPLES}"
echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"

gcloud ai-platform jobs submit training ${JOB_ID} \
     --job-dir ${JOB_DIR} \
     --package-path ${PACKAGE} \
     --module-name ${MODULE} \
     --region ${REGION} \
     --python-version ${PYTHON_VERSION} \
     --runtime-version ${RUNTIME_VERSION} \
     -- \
     --bucket=${BUCKET} \
     --output-dir=${OUTDIR} \
     --nembeds ${TRAIN_NEMBEDS} \
     --nnsize ${TRAIN_NNSIZE} \
     --batch-size=${TRAIN_BATCH_SIZE} \
     --train-examples=${TRAIN_EXAMPLES} \
     --eval-steps=${TRAIN_EVALSTEP}

gcloud ai-platform jobs describe ${JOB_ID}

gcloud ai-platform jobs stream-logs ${JOB_ID}

export MODEL_ID=${JOB_ID}
export MODEL_PATH=${JOB_DIR}

STATUS=$(gcloud ai-platform jobs describe ${JOB_ID} --format="value(state)")

[[ "$STATUS" -ne "SUCCEEDED" ]] && exit 1 || echo "The training job sucessfully completed!"
