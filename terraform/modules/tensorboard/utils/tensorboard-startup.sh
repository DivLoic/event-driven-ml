#!/usr/bin/env bash

apt-get update -y && apt-get install -y python3.5 python3-dev python3-pip curl

pip3 install tensorflow==1.14 tensorboard==1.14

mkdir tensorboard && cd tensorboard

gsutil cp -R gs://edml/ai-platform/models/* ./

nohup tensorboard --logdir=. --port=80 &

