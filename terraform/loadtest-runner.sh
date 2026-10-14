#!/bin/bash
set -euo pipefail

dnf install -y jq python3-pip

K6_VERSION=$(curl -fsSL https://api.github.com/repos/grafana/k6/releases/latest | jq -r .tag_name)
curl -fsSL "https://github.com/grafana/k6/releases/download/$K6_VERSION/k6-$K6_VERSION-linux-arm64.tar.gz" \
  | tar -xz -C /tmp
install "/tmp/k6-$K6_VERSION-linux-arm64/k6" /usr/local/bin/k6

pip3 install boto3 pymysql

curl -fsSL -o /opt/rds-ca.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem

cat > /etc/security/limits.d/loadtest.conf <<'EOF'
* soft nofile 250000
* hard nofile 250000
EOF

cat > /etc/sysctl.d/99-loadtest.conf <<'EOF'
net.ipv4.ip_local_port_range = 1024 65535
net.ipv4.tcp_tw_reuse = 1
EOF
sysctl --system