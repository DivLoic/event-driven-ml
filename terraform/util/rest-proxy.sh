#!/bin/bash

########### Update and Install ###########

yum update -y
yum install wget -y
yum install unzip -y
yum install java-1.8.0-openjdk-devel.x86_64 -y

########### Initial Bootstrap ###########

cd /tmp
wget ${confluent_platform_location}
unzip confluent-5.3.1-2.12.zip
mkdir /etc/confluent
mv confluent-5.3.1 /etc/confluent

########### Generating Props File ###########

cd ${confluent_home_value}/etc/kafka-rest

cat > kafka-rest-ccloud.properties <<- "EOF"
${rest_proxy_properties}
EOF

########### Creating the Service ############

cat > /lib/systemd/system/kafka-rest.service <<- "EOF"
[Unit]
Description=Confluent Kafka REST
After=network.target

[Service]
Type=simple
Restart=always
RestartSec=1
ExecStart=${confluent_home_value}/bin/kafka-rest-start ${confluent_home_value}/etc/kafka-rest/kafka-rest-ccloud.properties
ExecStop=${confluent_home_value}/bin/kafka-rest-stop ${confluent_home_value}/etc/kafka-rest/kafka-rest-ccloud.properties

[Install]
WantedBy=multi-user.target
EOF

############# Enable and Start ############

systemctl enable kafka-rest
systemctl start kafka-rest
