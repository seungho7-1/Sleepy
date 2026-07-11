#!/bin/bash
# ====================================================
# Sleepy EC2 원클릭 배포 스크립트
# ====================================================
set -e

echo "========================================"
echo "  Sleepy 서버 자동 배포 스크립트 시작"
echo "========================================"

read -p "AWS Access Key 입력: " AWS_ACCESS_KEY
read -sp "AWS Secret Key 입력: " AWS_SECRET_KEY
echo ""
read -p "S3 버킷 이름 (기본값: sleepy-media): " S3_BUCKET
S3_BUCKET=${S3_BUCKET:-sleepy-media}

DOMAIN="sleepyslime.p-e.kr"
DB_PASSWORD="Ap513147!"
JAR_NAME="sleepy-backend-0.0.1-SNAPSHOT.jar"

echo ""
echo "[1/7] 패키지 업데이트 및 기본 도구 설치..."
sudo apt update -y
sudo apt install -y openjdk-17-jre-headless mysql-server nginx git unzip curl

echo "[2/7] Node.js 20 설치..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

echo "[3/7] MySQL 데이터베이스 설정..."
sudo systemctl start mysql
sudo mysql -e "CREATE DATABASE IF NOT EXISTS sleepy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';"
sudo mysql -e "GRANT ALL PRIVILEGES ON sleepy.* TO 'root'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"

echo "[4/7] 백엔드 클론 및 빌드..."
cd /home/ubuntu
rm -rf Sleepy
git clone https://github.com/seungho7-1/Sleepy.git
cd Sleepy

cat > src/main/resources/application.properties << PROPS
spring.application.name=sleepy-backend
server.port=8383

spring.datasource.url=jdbc:mysql://localhost:3306/sleepy?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

jwt.SECRET_KEY=sleepy-project-jwt-secret-key-1234567890
jwt.expiration=604800000

spring.data.redis.host=localhost
spring.data.redis.port=6379

spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

aws.s3.bucket=${S3_BUCKET}
aws.s3.region=ap-northeast-2
spring.cloud.aws.credentials.access-key=${AWS_ACCESS_KEY}
spring.cloud.aws.credentials.secret-key=${AWS_SECRET_KEY}
PROPS

chmod +x gradlew
./gradlew bootJar -x test
echo "백엔드 빌드 완료!"

echo "[5/7] 백엔드 systemd 서비스 등록..."
sudo tee /etc/systemd/system/sleepy.service > /dev/null << SVC
[Unit]
Description=Sleepy Spring Boot Application
After=network.target mysql.service

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/Sleepy
ExecStart=/usr/bin/java -jar /home/ubuntu/Sleepy/build/libs/${JAR_NAME}
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SVC

sudo systemctl daemon-reload
sudo systemctl enable sleepy
sudo systemctl start sleepy
echo "백엔드 서비스 시작 완료!"

echo "[6/7] 프론트엔드 클론 및 빌드..."
cd /home/ubuntu
rm -rf sleepy-frontend
git clone https://github.com/seungho7-1/sleepy-frontend.git
cd sleepy-frontend
sed -i "s|http://localhost:8383/api|https://${DOMAIN}/api|g" src/api/index.js
npm install
npm run build
sudo mkdir -p /var/www/sleepy-frontend
sudo cp -r dist/* /var/www/sleepy-frontend/
sudo chown -R www-data:www-data /var/www/sleepy-frontend

echo "[7/7] Nginx 설정..."
sudo tee /etc/nginx/sites-available/sleepy > /dev/null << NGINX
server {
    listen 80;
    server_name ${DOMAIN};

    root /var/www/sleepy-frontend;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8383/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 60s;
    }

    client_max_body_size 50M;
}
NGINX

sudo ln -sf /etc/nginx/sites-available/sleepy /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl restart nginx

echo ""
echo "========================================"
echo "  HTTP 배포 완료!"
echo "  다음 명령어로 HTTPS 인증서를 발급하세요:"
echo ""
echo "  sudo apt install -y certbot python3-certbot-nginx"
echo "  sudo certbot --nginx -d sleepyslime.kro.kr"
echo "========================================"
