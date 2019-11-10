#!/usr/bin/env bash

BUCKET=edml
echo $BUCKET

TRAINER_PACKAGE_PATH=gs://$BUCKET/data/taxi-trips/sources
echo $TRAINER_PACKAGE_PATH

MAIN_TRAINER_MODULE="trainer.task"

PACKAGE_STAGING_PATH=gs://$BUCKET/data/taxi-trips/staging
echo $PACKAGE_STAGING_PATH

now=$(date +"%Y%m%d")
JOB_NAME=edml_trainer_$now
echo $JOB_NAME

JOB_DIR=gs://$BUCKET/data/taxi-trips/$JOB_NAME
REGION="europe-west1-b"

OUTDIR=gs://$BUCKET/data/taxi-trips/model_test
gsutil -m rm -rf $OUTDIR

gcloud ai-platform jobs submit training $JOB_NAME \
    --staging-bucket $PACKAGE_STAGING_PATH \
    --job-dir $JOB_DIR \
    --package-path $TRAINER_PACKAGE_PATH \
    --module-name $MAIN_TRAINER_MODULE \
    --region $REGION \
    -- \
    --bucket=$BUCKET \
    --output-dir=$OUTDIR \
    --pattern="*" \
    --train-examples=500 \
    --eval-steps=1