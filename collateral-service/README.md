# collateral-service

## 项目概述

**法币抵押型稳定币模拟系统 - 课题2：抵押物管理与风险控制**

技术栈：Spring Boot 3.2 + PostgreSQL + RabbitMQ

---

## 项目结构

```
collateral-service/
├── src/main/java/com/stablecoin/collateral/
│   ├── CollateralServiceApplication.java   # 启动入口
│   ├── config/
│   │   ├── AppConfig.java                  # RestTemplate 等 Bean 配置
│   │   └── RabbitMQConfig.java             # Exchange、Queue、Binding 配置
│   ├── controller/
│   │   ├── CollateralController.java       # 抵押物存入接口
│   │   ├── ReserveController.java          # 储备验证/仪表盘接口
│   │   ├── RiskAlertController.java        # 风险警报接口
│   │   └── AuditReportController.java      # 审计报告接口
│   ├── service/
│   │   ├── CollateralService.java          # 抵押物管理接口
│   │   ├── ReserveService.java             # 储备验证接口
│   │   ├── RiskAlertService.java           # 风险警报接口
│   │   ├── AuditReportService.java         # 审计报告接口
│   │   └── impl/                          # 以上接口的实现类
│   ├── entity/
│   │   ├── CollateralDeposit.java          # 抵押物存入记录
│   │   ├── ReservePool.java                # 储备池（单行聚合）
│   │   ├── ReserveSnapshot.java            # 储备率快照（时序）
│   │   ├── RiskAlert.java                  # 风险警报
│   │   ├── AuditReport.java                # 审计报告
│   │   └── ExchangeRate.java               # 汇率
│   ├── repository/                         # Spring Data JPA Repositories
│   ├── dto/                                # 请求/响应 DTO
│   ├── mq/
│   │   ├── CollateralEventPublisher.java   # 向课题1发布事件
│   │   └── StablecoinEventConsumer.java    # 消费课题1事件
│   ├── scheduler/
│   │   └── ReserveCheckScheduler.java      # 定时储备验证（每60秒）
│   └── exception/
│       ├── BusinessException.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml                     # 主配置文件
│   └── db/init.sql                         # 数据库初始化脚本
└── pom.xml
```

---

## 快速启动

### 1. 前置要求

- JDK 17+
- Maven 3.8+
- PostgreSQL 14+（本地运行，端口 5432）
- RabbitMQ 3.12+（本地运行，端口 5672）

### 2. 初始化数据库

```bash
psql -U postgres -f src/main/resources/db/init.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`，修改以下内容：

```yaml
spring:
  datasource:
    password: your_password   # 改为你的 PostgreSQL 密码
```

### 4. 启动服务

```bash
mvn spring-boot:run
```

服务启动在 **http://localhost:8081**

### 5. 查看 API 文档

访问 Swagger UI：**http://localhost:8081/swagger-ui/index.html**

---

## 核心 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/collateral/deposit | 抵押物存入 |
| GET  | /api/collateral/deposits | 查询存入记录 |
| GET  | /api/reserve/dashboard | 仪表盘数据 |
| POST | /api/reserve/snapshot | 手动触发储备验证 |
| GET  | /api/reserve/history | 储备率历史（折线图） |
| POST | /api/reserve/check | 验证储备（供课题1调用）|
| GET  | /api/alerts/active | 活跃风险警报 |
| PUT  | /api/alerts/{id}/resolve | 解决警报 |
| POST | /api/reports/generate | 生成审计报告 |
| GET  | /api/reports | 查询所有报告 |

---

## RabbitMQ 事件协议

### 接收（来自课题1）

| Exchange | Routing Key | 说明 |
|----------|-------------|------|
| stablecoin.events | issuance.completed | 稳定币发行完成 |
| stablecoin.events | redemption.completed | 稳定币赎回完成 |

### 发送（到课题1）

| Exchange | Routing Key | 说明 |
|----------|-------------|------|
| collateral.events | reserve.risk.alert | 储备风险警报 |
| collateral.events | reserve.healthy | 储备恢复健康 |

---

## 风险等级说明

| 储备率 | 等级 | 说明 |
|--------|------|------|
| >= 110% | HEALTHY | 健康 |
| 100%~110% | NORMAL | 正常 |
| 90%~100% | WARNING | 预警 |
| < 90% | CRITICAL | 危急，触发警报 |
