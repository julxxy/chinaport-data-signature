#!/bin/bash

TAG="1.2.0"
IMAGE="weasleyj/chinaport-data-signature"

# 可选：先移除旧容器和镜像
docker stop chinaport-data-signature 2>/dev/null && docker rm -f chinaport-data-signature 2>/dev/null

# 登录一次即可
docker login -u weasleyj

# 多平台构建并推送（arm64 + amd64），如只需 arm64 可调整
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ${IMAGE}:${TAG} \
  --push \
  -f ./Dockerfile .
