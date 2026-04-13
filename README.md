# 天天影院

一个面向影院购票场景的多端项目，包含：
- Java 微服务后端
- Vue3 管理后台
- 微信小程序用户端
- AI 影讯问答能力

项目围绕影院业务的核心链路展开，覆盖用户登录、影片管理、影院与影厅管理、排片管理、在线选座、订单流转、后台运营以及 AI 对话问答等功能。

## 项目亮点

- 基于 `Spring Boot + Spring Cloud Alibaba` 拆分认证、影片、影院排片、票务、AI 等多个后端模块
- 使用 `Spring Security + JWT` 实现认证鉴权
- 使用 `Redis + 分布式锁 + WebSocket` 处理在线选座时的状态同步与并发控制
- 使用 `Vue3 + Element Plus` 搭建后台管理系统
- 使用微信小程序实现移动端购票与订单查看流程
- 使用阿里百炼能力接入 AI 对话问答服务，支持电影相关信息查询

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- Spring Cloud 2023
- Spring Cloud Alibaba
- Spring Security
- MyBatis-Plus
- MySQL 8
- Redis
- RabbitMQ
- Nacos
- Redisson

### 前端管理端

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

### 小程序端

- 微信小程序原生开发

### 第三方能力

- 阿里百炼 DashScope
- 腾讯云 COS
- 高德地图

## 功能模块

### 后端模块

- `cinema-auth`
  负责登录注册、用户信息、JWT 鉴权、管理员与用户权限控制。
- `cinema-movie`
  负责影片、演员、广告、评论、收藏、类型等内容管理。
- `cinema-hall`
  负责影院、影厅、排片、座位、座位状态同步等业务。
- `cinema-ticketing`
  负责订单创建、订单流转、支付相关流程、票务处理。
- `cinema-ai`
  负责 AI 对话问答、电影信息查询等能力。
- `cinema-common`
  公共配置、通用实体、工具类、JWT 工具等共享模块。

### 管理后台

- 用户登录与权限控制
- 影片管理
- 演员管理
- 广告管理
- 影院管理
- 影厅管理
- 排片管理
- 订单管理
- 评论管理
- 数据看板

### 微信小程序端

- 首页与影片浏览
- 影院详情与附近影院
- 排片查看
- 在线选座
- 下单与支付流程
- 订单列表与订单详情
- 收藏与个人中心
- 登录注册
- AI 对话页面

## 目录结构

```text
天天影院
├─ cinema                    # Java 微服务后端
│  ├─ cinema-auth
│  ├─ cinema-common
│  ├─ cinema-movie
│  ├─ cinema-hall
│  ├─ cinema-ticketing
│  └─ cinema-ai
├─ vue
│  └─ cinema-admin           # Vue3 管理后台
├─ weixin                    # 微信小程序用户端
├─ .gitignore
└─ README.md
```

## 环境要求

部署或本地运行前，建议准备以下环境：

- JDK 17
- Maven 3.9+
- Node.js 18+
- npm 9+
- MySQL 8.x
- Redis 6.x 或更高
- RabbitMQ 3.x
- Nacos 2.x
- 微信开发者工具

## 端口说明

后端服务默认端口如下：

- `cinema-auth`：`8000`
- `cinema-movie`：`8001`
- `cinema-hall`：`8002`
- `cinema-ticketing`：`8003`
- `cinema-ai`：`8004`

管理后台默认通过本地环境变量访问这些服务。  
微信小程序端也默认按上述端口访问本地服务。

## 配置说明

项目中的公共后端配置位于：

`cinema/cinema-common/src/main/resources/cinema-shared.yml`
该文件已经替换为可公开上传的占位版本，实际运行前请按本地环境填写：

- MySQL
- Redis
- RabbitMQ
- Nacos
- JWT 密钥
- 内部服务 Token
- DashScope API Key
- 微信小程序配置
- 高德地图 Key
- 腾讯云 COS 配置

前端环境变量示例位于：

`vue/cinema-admin/.env.example`

如需本地启动管理后台，可复制为 `.env.development` 后再按需修改地址。

## 本地开发启动

### 1. 启动基础依赖

请先确保以下服务已启动：

- MySQL
- Redis
- RabbitMQ
- Nacos

并创建好业务数据库，例如：

- `cinema`

### 2. 配置后端公共配置

编辑文件：

`cinema/cinema-common/src/main/resources/cinema-shared.yml`

将占位值改成你本地环境中的真实配置。

### 3. 启动后端服务

进入后端目录：

```powershell
cd D:\New project\天天影院\cinema
```

先安装依赖并编译：

```powershell
mvn clean package -DskipTests
```

然后分别启动各模块：

```powershell
mvn -pl cinema-auth -am spring-boot:run
mvn -pl cinema-movie -am spring-boot:run
mvn -pl cinema-hall -am spring-boot:run
mvn -pl cinema-ticketing -am spring-boot:run
mvn -pl cinema-ai -am spring-boot:run
```

### 4. 启动管理后台

进入前端目录：

```powershell
cd D:\New project\天天影院\vue\cinema-admin
```

安装依赖：

```powershell
npm install
```

创建本地环境变量文件：

```powershell
Copy-Item .env.example .env.development
```

启动开发环境：

```powershell
npm run dev
```

### 5. 启动微信小程序

使用微信开发者工具导入：

`D:\New project\天天影院\weixin`

然后根据你的本地服务地址调整请求配置，主要参考：

`weixin/utils/request.js`

如果需要真机调试，请保证后端服务地址可被手机访问。

## 部署说明

### 后端部署

推荐将每个微服务单独打包部署。

在 `cinema` 目录执行：

```powershell
mvn clean package -DskipTests
```

打包完成后，可在各模块 `target` 目录中获取 jar 包并分别启动：

```powershell
java -jar cinema-auth\target\cinema-auth-*.jar
java -jar cinema-movie\target\cinema-movie-*.jar
java -jar cinema-hall\target\cinema-hall-*.jar
java -jar cinema-ticketing\target\cinema-ticketing-*.jar
java -jar cinema-ai\target\cinema-ai-*.jar
```

建议生产环境使用：

- Windows 服务
- NSSM
- Linux `systemd`
- Docker / Docker Compose

来托管各个 Java 服务与基础中间件。

### 管理后台部署

进入管理后台目录：

```powershell
cd D:\New project\天天影院\vue\cinema-admin
npm install
npm run build
```

构建产物位于：

`vue/cinema-admin/dist`

可将其部署到：

- Nginx
- 宝塔静态站点
- 云服务器静态资源目录

并通过反向代理将接口请求转发到对应后端服务。

### 微信小程序部署

小程序端不需要传统 Web 部署，使用微信开发者工具进行上传与发布即可。  
发布前需要确认：

- `project.config.json` 中的 `appid` 已替换为你自己的小程序 AppID
- 小程序后台已配置合法请求域名
- 后端服务已部署到公网可访问地址

## 部署建议

如果要部署到服务器，建议采用以下结构：

- Nginx：负责管理后台静态资源与接口转发
- Java 微服务：分别运行 `auth / movie / hall / ticketing / ai`
- MySQL：业务数据存储
- Redis：缓存、会话、座位状态控制
- RabbitMQ：异步消息处理
- Nacos：配置中心与服务发现

建议至少拆分出以下配置：

- 开发环境
- 测试环境
- 生产环境

避免将真实密钥直接写入仓库。

## 注意事项

- 当前仓库已移除 `resource/` 目录，媒体素材不会随仓库提交
- `.zip`、`node_modules`、`target`、`dist`、IDE 配置等文件已通过 `.gitignore` 过滤
- 若你要公开上传到 GitHub，请再次确认没有测试账号、真实业务数据或截图残留

## 后续可补充内容

如果你准备把这个仓库用于答辩、求职或展示，建议后续再补：

- 数据库建表 SQL
- 接口文档截图或在线文档地址
- 系统架构图
- 选座并发控制流程图
- 小程序页面截图
- 部署拓扑图
