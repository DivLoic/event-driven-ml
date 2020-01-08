#!/usr/bin/env bash

BUCKET=edml
echo $BUCKET

TRAINER_PACKAGE_PATH=edml-trainer/trainer
echo $TRAINER_PACKAGE_PATH

MAIN_TRAINER_MODULE="trainer.task"

now=$(date +"%Y%m%d_%H%M%S")
JOB_NAME=edml_trainer_$now
echo $JOB_NAME

JOB_DIR=gs://$BUCKET/ai-platform/$JOB_NAME
echo $JOB_DIR
REGION="europe-west1"

PYTHON_VERSION=3.5
RUNTIME_VERSION=1.14

OUTDIR=gs://$BUCKET/ai-platform/models
echo $OUTDIR
#gsutil -m rm -rf $OUTDIR

gcloud ai-platform jobs submit training $JOB_NAME \
    --job-dir $JOB_DIR \
    --package-path $TRAINER_PACKAGE_PATH \
    --module-name $MAIN_TRAINER_MODULE \
    --region $REGION \
    --python-version $PYTHON_VERSION \
    --runtime-version $RUNTIME_VERSION \
    -- \
    --bucket=$BUCKET \
    --output-dir=$OUTDIR \
    --pattern="*" \
    --nembeds 10 \
    --nnsize 20 10 5 \
    --batch-size=128 \
    --train-examples=174000 \
    --eval-steps=1 \
