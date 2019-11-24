FROM openjdk:8-jdk

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y && apt-get install -y apt-utils dialog software-properties-common
RUN apt-get update -y && apt-get install -y lsb lsb-release
RUN apt-get install -y python3-dev python3-pip

RUN if [ ! -e /usr/bin/python ]; then ln -sf python3 /usr/bin/python ; fi
RUN if [ ! -e /usr/bin/pip ]; then ln -s pip3 /usr/bin/pip ; fi

RUN echo "deb http://packages.cloud.google.com/apt cloud-sdk-$(lsb_release -c -s) main" \
  | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list

RUN cat /etc/apt/sources.list.d/google-cloud-sdk.list
RUN curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg --output - | apt-key add -
RUN apt-get update -y && apt-get install -y google-cloud-sdk kubectl


RUN pip3 install --no-cache --upgrade pip
RUN pip3 install --no-cache --upgrade setuptools wheel tensorflow

RUN mkdir -p /etc/share/edml
ADD edml-model-upload.jar /etc/share/edml/edml-model-upload.jar


