FROM google/cloud-sdk:alpine

RUN apk --update add openjdk8
RUN apk add --no-cache python3

RUN if [ ! -e /usr/bin/python ]; then ln -sf python3 /usr/bin/python ; fi
RUN if [ ! -e /usr/bin/pip ]; then ln -s pip3 /usr/bin/pip ; fi

RUN python3 -m ensurepip && rm -r /usr/lib/python*/ensurepip

RUN pip3 install --no-cache --upgrade pip setuptools wheel tensorflow


