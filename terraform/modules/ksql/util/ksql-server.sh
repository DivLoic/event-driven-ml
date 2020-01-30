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
mkdir ${confluent_home_value}/data

########### Generating Props File ###########

cd ${confluent_home_value}/etc/ksql

cat > ksql-server-ccloud.properties <<- "EOF"
${ksql_server_properties}
EOF

########### Creating the Service ############

cat > /lib/systemd/system/ksql-server.service <<- "EOF"
[Unit]
Description=Confluent KSQL Server
After=network.target

[Service]
Type=simple
Restart=always
RestartSec=1
ExecStart=${confluent_home_value}/bin/ksql-server-start ${confluent_home_value}/etc/ksql/ksql-server-ccloud.properties
ExecStop=${confluent_home_value}/bin/ksql-server-stop ${confluent_home_value}/etc/ksql/ksql-server-ccloud.properties

[Install]
WantedBy=multi-user.target
EOF

########### Enable and Start ###########

########### Demo scripts ###########
yum upgrade -y && yum update -y
yum install -y python3.5 python3-dev python3-pip git curl
pip3 install doitlive

echo "alias ksql='/etc/confluent/confluent-5.3.1/bin/ksql'" >> /root/.bashrc
echo "alias predictions='doitlive play -q /root/.predictions'" >> /root/.bashrc
echo "alias corrections='doitlive play -q /root/.corrections'" >> /root/.bashrc

cat > /root/.predictions <<- "EOF"
#doitlive prompt: {dir.cyan} {hostname.green} ->
#doitlive alias: ksql="/etc/confluent/confluent-5.3.1/bin/ksql"
echo "SHOW STREAMS;" | ksql
echo "DESCRIBE PREDICTION;" | ksql
echo "SELECT SUBSTRING(ROWKEY, 0, 10), VERSION, PREDICTION FROM PREDICTION;" | ksql
EOF

cat > /root/.corrections <<- "EOF"
#doitlive prompt: {dir.cyan} {hostname.green} ->
#doitlive alias: ksql="/etc/confluent/confluent-5.3.1/bin/ksql"
echo "SELECT TIMESTAMPTOSTRING(DROPOFF_DATETIME, 'yyyy-MM-dd HH:mm:ss.SSS', 'Europe/Paris'), VERSION, TRIP_DURATION, PREDICTION FROM SCORING;" | ksql
EOF

########### Demo scripts ###########

systemctl enable ksql-server
systemctl start ksql-server
