#!/bin/bash

source ~/.zshrc
if command -v sptn >/dev/null 2>&1; then
  echo "检测到 sptn 命令，正在执行..."
  sptn
else
  echo "未检测到 sptn 命令，跳过执行"
fi

SETTINGS="/Users/$USER/Development/program/apache-maven/conf/settings-maven-central.xml"
MODULE="chinaport-data-signature-data-model"

clear && mvn deploy -pl :$MODULE -am --settings $SETTINGS
