# **China E-Port Data Signature**

中国电子口岸（海口海关）报文加签推送应用  
_A free, open-source end-to-end signature solution for Chinese E-Port CEBXxxMessage and Customs 179 messages.
Plug-and-play, no middleware required. Suitable for enterprise-grade e-commerce and trade._

---

[![GitHub Release](https://img.shields.io/github/v/release/julxxy/chinaport-data-signature?style=flat-square&logo=github&label=Release)](https://github.com/julxxy/chinaport-data-signature/releases)
[![GitHub Stars](https://img.shields.io/github/stars/julxxy/chinaport-data-signature?style=flat-square&logo=github&label=Stars)](https://github.com/julxxy/chinaport-data-signature/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/julxxy/chinaport-data-signature?style=flat-square&logo=github&label=Forks)](https://github.com/julxxy/chinaport-data-signature/network/members)
[![GitHub Issues](https://img.shields.io/github/issues/julxxy/chinaport-data-signature?style=flat-square&logo=github&label=Issues)](https://github.com/julxxy/chinaport-data-signature/issues)
[![License](https://img.shields.io/github/license/julxxy/chinaport-data-signature?style=flat-square&logo=gnu&label=License)](https://github.com/julxxy/chinaport-data-signature/blob/main/LICENSE)

[![Docker Pulls](https://img.shields.io/docker/pulls/weasleyj/chinaport-data-signature?style=flat-square&logo=docker&label=Docker%20Pulls&color=2496ED)](https://hub.docker.com/r/weasleyj/chinaport-data-signature)
[![Docker Image Size](https://img.shields.io/docker/image-size/weasleyj/chinaport-data-signature/latest?style=flat-square&logo=docker&label=Image%20Size&color=2496ED)](https://hub.docker.com/r/weasleyj/chinaport-data-signature)
[![Docker Image Version](https://img.shields.io/docker/v/weasleyj/chinaport-data-signature?style=flat-square&logo=docker&label=Docker%20Version&color=2496ED&sort=semver)](https://hub.docker.com/r/weasleyj/chinaport-data-signature/tags)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.weasley-j/chinaport-data-signature-data-model?style=flat-square&logo=apache-maven&label=Maven%20Central&color=C71A36)](https://search.maven.org/artifact/io.github.weasley-j/chinaport-data-signature-data-model)

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![WebSocket](https://img.shields.io/badge/WebSocket-Support-010101?style=flat-square&logo=socket.io&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)

[![Download Latest](https://img.shields.io/badge/Download-Latest%20Release-brightgreen?style=for-the-badge&logo=download)](https://github.com/julxxy/chinaport-data-signature/releases/latest)
[![Docker Run](https://img.shields.io/badge/Docker-Quick%20Start-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://hub.docker.com/r/weasleyj/chinaport-data-signature)
[![View Documentation](https://img.shields.io/badge/Documentation-View%20Docs-blue?style=for-the-badge&logo=gitbook)](https://github.com/julxxy/chinaport-data-signature/wiki/%E5%BC%80%E5%8F%91%E8%80%85%E4%B8%AD%E5%BF%83)

---

## Foreword | 前言

> After nearly two years of open-source code maintenance and exploration, we have developed an all-in-one solution offering free, open and universal access for a wide range of businesses. The aim is to digitally sign and forward XML messages to the General Administration of Customs via China E-Port (HaiKou Customs), facilitating efficient cross-border e-commerce and import-export business.
> 
> 经过近两年的开源代码维护和探索，我们终于开发出一站式免费解决方案，为更多企业提供快速接入和使用能力。项目旨在实现中国电子口岸（海口海关）总署XML 报文加签推送，助力跨境电商与进出口业务发展。
> 
> _2023-07_

本项目遵循 `GNU 3.0` 协议，测试用例中企业信息已获授权，企业调试时应将报文数据中涉及到的`报文传输编码（DXPID）`、`copCode`、`copName`、`电商平台代码ebpCode`等主体信息替换成企业在中国电子口岸后台注册的有效信息。

---

## Features | 功能亮点

- 支持中国电子口岸 CEBXxxMessage 及 179 数据报文加签
- Plug-and-play，开箱即用，无需中间件，无需二次编译
- 支持 JSON 报文直推
- 加签失败邮件通知、支持企业微信、钉钉、飞书、Discord 等扩展告警
- U-Key 健康状态异常自动重启 WebSocket 客户端
- 支持 Docker 镜像部署 & Maven 数据模型集成

---

## Usage Scenarios | 业务场景

- 进出口贸易、跨境电商、物流、外贸等企业数据上报
- 所有需向中国电子口岸或海关报送签名报文的企业系统

---

## Quick Start | 快速使用

- 单体应用：直接下载 [Releases](https://github.com/julxxy/chinaport-data-signature/releases)
- 微服务：切换 [feature_microservice](https://github.com/julxxy/chinaport-data-signature/tree/feature_microservice) 分支
- Docker 镜像：[Docker Hub](https://hub.docker.com/repository/docker/weasleyj/chinaport-data-signature)
- Java项目集成：使用 [Maven 数据模型](https://central.sonatype.com/artifact/io.github.weasley-j/chinaport-data-signature-data-model), Maven Integration
```xml
<dependency>
    <groupId>io.github.weasley-j</groupId>
    <artifactId>chinaport-data-signature-data-model</artifactId>
    <version>Latest Version</version>
</dependency>
```

> 建议 Windows 平台用 [Git Bash](https://gitforwindows.org/) 启动（终端日志不乱码）

---

## 功能概述

![中国电子口岸（海口海关）报文加签推送应用](https://weasley.oss-cn-shanghai.aliyuncs.com/Photos/%E4%B8%AD%E5%9B%BD%E7%94%B5%E5%AD%90%E5%8F%A3%E5%B2%B8%EF%BC%88%E6%B5%B7%E5%8F%A3%E6%B5%B7%E5%85%B3%EF%BC%89%E6%8A%A5%E6%96%87%E5%8A%A0%E7%AD%BE%E6%8E%A8%E9%80%81%E5%BA%94%E7%94%A8_20250730211142.jpg)

---

## 系统架构

![电子口岸报文加签拓补图](https://weasley.oss-cn-shanghai.aliyuncs.com/Photos/%E7%94%B5%E5%AD%90%E5%8F%A3%E5%B2%B8%E6%8A%A5%E6%96%87%E5%8A%A0%E7%AD%BE%E6%8B%93%E8%A1%A5%E5%9B%BE_20250730210810.jpg)

---

## 报文加签推送流程

![ChinaEportSignatureApp加签流程](https://weasley.oss-cn-shanghai.aliyuncs.com/Photos/ChinaEportSignatureApp%E5%8A%A0%E7%AD%BE%E6%B5%81%E7%A8%8B_20250730211035.jpg)

---

> 2022年2月14日之前互联网无相关资料，本项目填补空白。对比市场付费服务多数隐藏加签算法，完全开源，欢迎反馈贡献！

---

## 电子口岸支持覆盖范围

理论支持中国大陆各大地方电子口岸，表中为已验证/待验证端口：

| 电子口岸   | 是否支持 | 是否验证 |
|--------|------|------|
| 海口电子口岸 | ✅    | ✅    |
| 广州电子口岸 | ✅    | ✅    |
| 郑州电子口岸 | ✅    | 待验证  |
| 上海电子口岸 | ✅    | 待验证  |
| 杭州电子口岸 | ✅    | 待验证  |
| ...    |      |      |

---

## 软件运行环境/开发环境

| 类目   | 版本                                                                                                         | 备注                           |
|------|------------------------------------------------------------------------------------------------------------|------------------------------|
| JDK  | [Java SE Development Kit 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) | Java SE Development Kit 17 + |
| 硬件   | Windows10/11/Server, Linux                                                                                 | u-key 仅支持插在 Windows 电脑       |
| 性能   | ≥1核CPU+2G内存                                                                                                | 最低配置                         |
| 技术要求 | 基本 shell 操作                                                                                                | Windows 需会安装 U-Key 并设置脚本     |

- _参见[中国电子口岸C卡/Key客户端下载页面：卡介质登录](https://app.singlewindow.cn/cas/login?service=http%3A%2F%2Fwww.singlewindow.cn%2Fsinglewindow%2Flogin.jspx)_
- _电子口岸的操作员 Ukey 图片资料_ <img src="https://alphahub-test-bucket.oss-cn-shanghai.aliyuncs.com/image/IMG_0401.jpg" alt="u-key图片资料" style="zoom:50%;" />

---

## 项目运行配置

本项目为 Spring Boot 应用，所有参数均支持yaml/properties配置，详见 [配置样例](https://github.com/julxxy/chinaport-data-signature/blob/main/chinaport-data-signature-webapp/src/main/resources/application-dev.yml)

**主要配置参数一览：**

| 配置参数                                             | 必须 | 说明                                                                                                                                                                                                                                              |
|--------------------------------------------------|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| eport.signature.algorithm                        | N  | [XML签名的算法类型](https://github.com/julxxy/chinaport-data-signature/blob/main/chinaport-data-signature-webapp/src/main/java/cn/alphahub/eport/signature/config/SignatureAlgorithmProperties.java#L20)，不指定时使用程序自动推断的算法, 推荐使用程序自动推断的算法                |
| eport.signature.ukey.ws-url                      | Y  | `u-key`所连接的`Windows`电脑的`socket`链接`url`,如: `ws://127.0.0.1:61232`,下载`release`直接运行的修改全局配置的`UKEY_HOST`即可                                                                                                                                           |
| eport.signature.ukey.password                    | N  | u-key密码的密码，默认: `88888888`, 如果密码改过，需要指定下，下载`release`直接运行的修改全局配置的`UKEY_PASSWORD`即可                                                                                                                                                                |
| eport.signature.ukey.health.endpoint.client-name | N  | Windows上重启的ukey可执行文件全限定文件名称，不指定将自动查找（将程序安装到Windows平台上会生效，Linux、Uni环境无效）                                                                                                                                                                         |
| eport.signature.auth.enable                      | N  | [是否启用token鉴权](https://github.com/julxxy/chinaport-data-signature/blob/main/chinaport-data-signature-webapp/src/main/java/cn/alphahub/eport/signature/config/AuthenticationProperties.java#L34)，取值：<u>on/off</u>，生产环境建议开启                        |
| eport.signature.auth.token                       | N  | 客户端请求鉴权token, 默认值: DefaultAuthToken, [请求头](https://github.com/julxxy/chinaport-data-signature/blob/main/chinaport-data-signature-webapp/src/main/java/cn/alphahub/eport/signature/config/AuthenticationProperties.java#L24)，<u>生产环境不建议使用默认值</u> |
| eport.signature.report.ceb-message.cop-code      | Y  | [电子口岸XML报文](https://github.com/julxxy/chinaport-data-signature/blob/main/chinaport-data-signature-webapp/src/main/java/cn/alphahub/eport/signature/config/ChinaEportProperties.java#L20)传输企业代码                                                  |
| eport.signature.report.ceb-message.cop-name      | Y  | 传输企业名称，报文传输的企业名称                                                                                                                                                                                                                                |
| eport.signature.report.ceb-message.dxp-id        | Y  | 文传输编号，向中国电子口岸数据中心申请数据交换平台的用户编号                                                                                                                                                                                                                  |
| eport.signature.report.ceb-message.server        | N  | [海关服务器地址](https://github.com/julxxy/chinaport-data-signature/blob/main/chinaport-data-signature-webapp/src/main/java/cn/alphahub/eport/signature/config/ChinaEportProperties.java#L43)，<u>缺省则采用Client中的密文作文默认Server URL</u>                     |
| eport.signature.report.customs179.ebp-code       | Y  | [海关 179 数据上报](https://github.com/julxxy/chinaport-data-signature/blob/main/chinaport-data-signature-webapp/src/main/java/cn/alphahub/eport/signature/config/Customs179Properties.java#L26)的电商平台代码                                               |
| eport.signature.report.customs179.server         | N  | 数据上报服务器URL地址，链接格式: https://域名联系海关/ceb2grab/grab/realTimeDataUpload，<u>没有配置的话采用项目内置的URL密文地址</u>                                                                                                                                                  |

---

### 跨平台启动

#### Windows

- [Git Bash 右键快捷启动脚本指南](https://github.com/julxxy/chinaport-data-signature/blob/main/使用GitBash右键运行启动脚本.md)

#### Linux/MacOS

```bash
sh start.sh
```
_参见[start.sh](https://github.com/julxxy/chinaport-data-signature/blob/main/start.sh)_



---

## 邮件通知/故障自愈

- [配置通知示例](https://github.com/julxxy/chinaport-data-signature/blob/main/chinaport-data-signature-webapp/src/main/resources/application-dev.yml#L26-L43)
- 支持异常自动重启、U-Key 自动健康检测
- 可拓展企业微信、钉钉、飞书、Discord等

---

## 接口调试及响应示例

- 启动项目并访问 `http://localhost:8080`
- 在线 H5 文档调试及参数说明，支持直接发起签名请求（注意请求参数的**字符串转义**）

**XML加签响应示例：**

```json
{
  "message": "操作成功",
  "success": true,
  "timestamp": "2023-07-15 12:15:47",
  "code": 200,
  "data": {
    "success": true,
    "certNo": "03000000000cde6f",
    "x509Certificate": "MIIEoDCCBESgAwIBAgIIAwAAAAAM3m8wDAYIKoEcz1UBg3UFADCBmDELMAkGA1UEBhMCQ04xDzANBgNVBAgMBuWMl+S6rDEPMA0GA1UEBwwG5YyX5LqsMRswGQYDVQQKDBLkuK3lm73nlLXlrZDlj6PlsrgxGzAZBgNVBAsMEuivgeS5pueuoeeQhuS4reW/gzEtMCsGA1UEAwwk5Lit5Zu955S15a2Q5Lia5Yqh6K+B5Lmm566h55CG5Lit5b+DMB4XDTIzMDMyOTAwMDAwMFoXDTMzMDMyOTAwMDAwMFowVjELMAkGA1UEBhMCQ04xMzAxBgNVBAsMKua1t+WNl+ecgeiNo+iqiei/m+WHuuWPo+i0uOaYk+aciemZkOWFrOWPuDESMBAGA1UEAwwJ5p2o5aaC6YeRMFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAE0vOQmplAr9igPZrA8F1msqnFd0U++6G6NhG5rNuIUWft0BwQn7eSJkt5/fvSSoe7pUg2/awHUWPnzkeeQc7oVqOCArUwggKxMBEGCWCGSAGG+EIBAQQEAwIFoDAOBgNVHQ8BAf8EBAMCBsAwCQYDVR0TBAIwADApBgNVHSUEIjAgBggrBgEFBQcDAgYIKwYBBQUHAwQGCisGAQQBgjcUAgIwHwYDVR0jBBgwFoAURCQxt0wEvoAVXmuo4N1bjKXTh0UwHQYDVR0OBBYEFAytGob5L0WqhOCZ5l6Lf2jUdNrAMGgGA1UdIARhMF8wXQYEVR0gADBVMFMGCCsGAQUFBwIBFkdodHRwczovL3d3dy5jaGluYXBvcnQuZ292LmNuL3RjbXNmaWxlL3UvY21zL3d3dy8yMDIyMDQvMTIxMzI5NDh4dDZwLnBkZjB/BgNVHR8EeDB2MHSgcqBwhm5sZGFwOi8vbGRhcC5jaGluYXBvcnQuZ292LmNuOjM4OS9jbj1jcmwwMzAwMDAsb3U9Y3JsMDAsb3U9Y3JsLGM9Y24/Y2VydGlmaWNhdGVSZXZvY2F0aW9uTGlzdD9iYXNlP2NuPWNybDAzMDAwMDA+BggrBgEFBQcBAQQyMDAwLgYIKwYBBQUHMAGGImh0dHA6Ly9vY3NwLmNoaW5hcG9ydC5nb3YuY246ODgwMC8wOgYKKwYBBAGpQ2QFAQQsDCrmtbfljZfnnIHojaPoqonov5vlh7rlj6PotLjmmJPmnInpmZDlhazlj7gwEgYKKwYBBAGpQ2QFAwQEDAIwMTAiBgorBgEEAalDZAUIBBQMEjUxMjMyNDE5NjQxMDE3Mjk3WDAgBgorBgEEAalDZAUJBBIMEDAzLUpKMEc5MDAyMjA3NTIwGQYKKwYBBAGpQ2QFCwQLDAlNQTVUTkZHWTkwEgYKKwYBBAGpQ2QFDAQEDAIwMDASBgorBgEEAalDZAIBBAQMAjEyMBIGCisGAQQBqUNkAgQEBAwCMTQwDAYIKoEcz1UBg3UFAANIADBFAiBM4OVAc8aaCZU4XFfcVMkC7bWIIenRnPLxrnwVeYO3CQIhANQ767YIurkJCoLtwyqQPbUZe/+3BjGZcIWqB1mAl9T+",
    "digestValue": "b2FtM/jHvW4fnJpMMPWk/177RwA=",
    "signatureValue": "Pa2Ge3A1iH9e+85jIraVccvf2MB4ykjgC1MjObNd/MbPOSZRWTaizlnfReH3ErRMH5Wc3m60ZM6h56v7t7lIWg==",
    "signatureNode": "<ds:SignedInfo xmlns:ceb=\"http://www.chinaport.gov.cn/ceb\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sm2-sm3\"></ds:SignatureMethod><ds:Reference URI=\"\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>b2FtM/jHvW4fnJpMMPWk/177RwA=</ds:DigestValue></ds:Reference></ds:SignedInfo>"
  }
}
```

**海关 179 加签响应示例：**

```json
{
  "message": "操作成功",
  "success": true,
  "timestamp": "2023-07-15 12:16:36",
  "code": 200,
  "data": {
    "success": true,
    "certNo": "03000000000cde6f",
    "signatureValue": "pVWbjCXqCy2zk0RRqbw16hWZozjTSP444fdT2k5MjqigJDsLZ/Vfgout3o/Gg0WrPbJnH/2wlI8I0n3niqYiqw=="
  }
}
```

---

## 支持我们的项目

### 为什么赞助

- 帮助企业节约采购费用，使用行业通用开源加签
- 推动技术开放与协作，促进行业共享
- 支持项目持续改进和功能更新

### 为什么开源

- 市面绝大多数电子口岸加签解决方案为商业付费且封闭，企业投入成本高，透明度低。
- 我们坚信报文加签属于数字贸易的基础设施，应惠及更多企业与开发者。
- 开源不仅有助于提升行业信任度，也能集结社区力量持续改进和快速适配各地业务需求。
- 欢迎各类企业和个人贡献代码、文档、案例或提出宝贵建议！

### 如何赞助

- [PayPal](https://www.paypal.me/shiwenjinga)

| ![AliPay](https://weasley.oss-cn-shanghai.aliyuncs.com/Photos/image-20230713012837745.png) | ![WeChatPay](https://weasley.oss-cn-shanghai.aliyuncs.com/Photos/image-20230713013148459.png) |
|:------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------:|

---

## 感谢/接入企业名录

排名不分先后，自2023-07-11登记：

- 海南省荣誉进出口贸易有限公司 [官网](http://www.hnzrjck.com/)
- 广州市汇客物流有限公司
- 海南嗨亿购科技有限公司
- 小红书
- 广东铭鸿数据有限公司
- ...

---

## License

[GNU 3.0](LICENSE)

---
**任何问题欢迎提 issue 或发邮件至 julxxy@outlook.com 交流！**
