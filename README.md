# 智慧医疗系统（SmartMedicalSystem）

基于 Spring Boot 的智慧医疗管理系统课程设计，覆盖患者挂号、医生问诊、处方开药、药房发药、缴费结算等完整就诊流程。

## 技术栈

- **后端**：Spring Boot 4.0.8 + MyBatis + MySQL 8
- **认证**：JWT Token
- **前端**：原生 HTML / CSS / JavaScript，按角色划分为患者端、医生端、管理员端、药房端等页面

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户认证 | 登录注册，JWT 鉴权，按角色（患者/医生/管理员）分配权限 |
| 挂号管理 | 患者选择科室、医生、时段进行预约挂号 |
| 问诊管理 | 医生接诊、书写病历、开具处方 |
| 处方与药房 | 处方药品明细管理，药房审核发药 |
| 缴费结算 | 就诊费用生成与支付管理 |
| 智能导诊与分诊 | 症状导诊推荐，分诊记录管理 |
| 在线咨询 | 患者提问与医生解答 |
| 基础数据 | 科室、医生排班、药品、患者档案等管理 |

## 快速开始

1. 准备 MySQL 8 数据库，导入 `src/main/resources/db/` 下的 `schema.sql` 和 `data.sql`
2. 修改 `src/main/resources/application.yml` 中的数据库连接配置
3. 启动应用

```bash
./mvnw spring-boot:run
```

4. 访问 `http://localhost:8080/login.html` 进入登录页

## 项目结构

```
src/main/java/com/example/medical/
├── auth/         # Token 存储与鉴权
├── config/       # 全局配置
├── controller/   # 接口层
├── entity/       # 实体类
├── mapper/       # MyBatis 数据访问层
├── service/      # 业务逻辑层
└── util/         # 工具类
src/main/resources/
├── db/           # 建表与初始化数据脚本
└── public/       # 前端页面
```

