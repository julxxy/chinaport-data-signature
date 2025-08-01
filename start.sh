#!/bin/bash

export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8

UKEY_HOST="127.0.0.1"    # UKEY 服务地址
UKEY_PASSWORD="88888888" # 电子口岸 UKEY 密码

# SPRING_ARGS 中有些参数要根据实际情况修改
APP_NAME="chinaport-data-signature-webapp"
JVM_ARGS="-Xms1g -Xmx1g"
SPRING_ARGS="--server.port=8080 \
--spring.profiles.active=prod \
--spring.mail.enable=false \
--spring.mail.to=abc@qq.com \
--spring.mail.cc=abc@outlook.com,abc@qq.com \
--spring.mail.host=smtp.189.cn \
--spring.mail.port=465 \
--spring.mail.username=xxx@189.cn \
--spring.mail.password= \
--spring.mail.protocol=smtp \
--spring.mail.properties.mail.smtp.ssl.enable=true \
--spring.mail.properties.mail.debug=false \
--eport.signature.ukey.ws-url=ws://$UKEY_HOST:61232 \
--eport.signature.ukey.password=$UKEY_PASSWORD \
--eport.signature.auth.enable=off \
--eport.signature.auth.token=DefaultAuthToken \
--eport.signature.report.ceb-message.cop-code=4601630008 \
--eport.signature.report.ceb-message.cop-name=杭州人喝杭水进出口贸易有限公司 \
--eport.signature.report.ceb-message.dxp-id=DXPENT0000530816 \
--eport.signature.report.customs179.ebp-code=48016602UV"

# 判断操作系统
OS="$(uname | tr '[:upper:]' '[:lower:]')"

if [[ "$OS" == *"mingw"* || "$OS" == *"msys"* || "$OS" == *"cygwin"* ]]; then
  # Windows（Git Bash、MSYS、Cygwin）
  PID=$(netstat -ano | grep ':8080' | grep LISTENING | awk '{print $NF}' | head -n 1)
  if [ -n "$PID" ]; then
    echo "Windows: Killing process on port 8080, PID: $PID"
    taskkill //F //PID $PID
  fi
else
  # Linux/macOS
  PID=$(lsof -ti tcp:8080)
  if [ -n "$PID" ]; then
    echo "Unix: Killing process on port 8080, PID: $PID"
    kill -9 $PID
  fi
fi

# 启动应用，不展示控制台输出
nohup java --add-opens java.base/sun.nio.cs=ALL-UNNAMED -Dfile.encoding=UTF-8 $JVM_ARGS -jar $APP_NAME.jar $SPRING_ARGS >/dev/null 2>&1 &

# 查找最新的日志文件: 请根据你的日志配置修改日志文件路径，下面以 logs/app.log 为例
LOGFILE=$(ls -Art logs/app.log 2>/dev/null | tail -n 1)

if [ -n "$LOGFILE" ]; then
  echo "tail -f $LOGFILE"
  tail -f "$LOGFILE"
else
  echo "日志文件未生成，请检查日志目录配置！"
fi
