#!/bin/bash

# --- 配置信息 ---
TAG="1.2.0"
IMAGE="weasleyj/chinaport-data-signature"
CONTAINER_NAME="chinaport-data-signature"
BUILDER_NAME="multiarch-builder"
DOCKERFILE_PATH="./Dockerfile"

# --- 彩色输出 ---
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# --- 日志函数 ---
log_info()    { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_warn()    { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_error()   { echo -e "${RED}❌ $1${NC}"; }

# --- 错误处理 ---
set -e
trap 'log_error "脚本执行失败，请检查错误信息"; exit 1' ERR

# --- 显示配置 ---
echo -e "${BLUE}🚀 开始多架构Docker镜像构建...${NC}"
echo -e "${BLUE}   镜像: ${IMAGE}${NC}"
echo -e "${BLUE}   标签: ${TAG}, latest${NC}"
echo -e "${BLUE}   平台: linux/amd64, linux/arm64${NC}"
echo ""

# [1] 检查前置条件
log_info "检查Docker环境..."
if ! docker info &>/dev/null; then
  log_error "Docker未启动或不可用，请先启动 Docker Desktop 后再运行本脚本！"
  exit 1
fi

if ! docker buildx version &>/dev/null; then
  log_error "Docker buildx 不可用，请确认Docker版本支持buildx功能！"
  exit 1
fi

if [[ ! -f "$DOCKERFILE_PATH" ]]; then
  log_error "Dockerfile不存在: $DOCKERFILE_PATH"
  exit 1
fi

log_success "Docker环境检查通过"

# [2] 清理旧容器
if docker ps -a --format '{{.Names}}' | grep -wq "$CONTAINER_NAME"; then
  log_info "清理旧容器: $CONTAINER_NAME"
  docker stop "$CONTAINER_NAME" 2>/dev/null || true
  docker rm -f "$CONTAINER_NAME" 2>/dev/null || true
  log_success "旧容器清理完成"
fi

# [3] Docker Hub 登录检查
log_info "检查Docker Hub登录状态..."
if ! docker system info --format '{{.RegistryConfig.IndexConfigs}}' 2>/dev/null | grep -q "weasleyj"; then
  log_warn "未检测到登录状态，准备登录 Docker Hub..."
  echo -e "${YELLOW}💡 建议使用 Personal Access Token 作为密码${NC}"
  echo -e "${YELLOW}   创建Token: https://hub.docker.com/settings/security${NC}"

  if ! docker login -u weasleyj; then
    log_error "Docker Hub 登录失败！"
    exit 1
  fi
  log_success "Docker Hub 登录成功"
else
  log_success "已登录 Docker Hub"
fi

# [4] 设置多架构构建器
log_info "配置多架构构建器..."
if docker buildx ls | grep -q "$BUILDER_NAME"; then
  log_info "使用现有构建器: $BUILDER_NAME"
  docker buildx use "$BUILDER_NAME"
else
  log_info "创建新的构建器: $BUILDER_NAME"
  docker buildx create --name "$BUILDER_NAME" --driver docker-container --use --bootstrap
fi

# 检查构建器支持的平台
log_info "验证构建器平台支持..."
if ! docker buildx inspect --bootstrap | grep -E "(linux/amd64|linux/arm64)" &>/dev/null; then
  log_error "构建器不支持所需平台 (linux/amd64, linux/arm64)"
  exit 1
fi
log_success "构建器平台支持验证通过"

# [5] 构建并推送镜像
log_info "开始构建并推送多架构镜像..."
echo -e "${BLUE}构建参数:${NC}"
echo -e "  平台: linux/amd64,linux/arm64"
echo -e "  标签: ${IMAGE}:${TAG}, ${IMAGE}:latest"
echo -e "  Dockerfile: ${DOCKERFILE_PATH}"
echo ""

docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag "${IMAGE}:${TAG}" \
  --tag "${IMAGE}:latest" \
  --file "$DOCKERFILE_PATH" \
  --push \
  --progress=plain \
  .

log_success "镜像构建并推送完成！"

# [6] 验证推送结果
log_info "验证镜像推送结果..."
echo -e "${GREEN}🎉 成功推送的镜像:${NC}"
echo -e "   📦 ${IMAGE}:${TAG}"
echo -e "   📦 ${IMAGE}:latest"
echo -e "   🏗️  支持架构: linux/amd64, linux/arm64"

# [7] 清理选项提示
echo ""
log_info "构建完成！"
echo -e "${YELLOW}💡 如需清理构建器，运行: docker buildx rm $BUILDER_NAME${NC}"
echo -e "${YELLOW}💡 查看镜像详情: docker buildx imagetools inspect ${IMAGE}:${TAG}${NC}"

log_success "脚本执行完成！"

cd .. & mvn clean
