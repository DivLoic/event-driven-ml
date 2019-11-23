#!/usr/bin/env bash

BUCKET=edml
echo $BUCKET

TRAINER_PACKAGE_PATH=edml-trainer/trainer
echo $TRAINER_PACKAGE_PATH

MAIN_TRAINER_MODULE="trainer.task"

PACKAGE_STAGING_PATH=gs://$BUCKET
echo $PACKAGE_STAGING_PATH

now=$(date +"%y%m%d_%H%M%S")
JOB_NAME=edml_trainer_$now
echo $JOB_NAME

JOB_DIR=gs://$BUCKET/$JOB_NAME/job_dir
REGION="europe-west1"

PYTHON_VERSION=3.5
RUNTIME_VERSION=1.14

OUTDIR=gs://$BUCKET/$JOB_NAME/model
echo $OUTDIR
#gsutil -m rm -rf $OUTDIR

gcloud ai-platform jobs submit training $JOB_NAME \
    --staging-bucket $PACKAGE_STAGING_PATH \
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
    --nnsize 10 5 \
    --train-examples=174000 \
    --eval-steps=1