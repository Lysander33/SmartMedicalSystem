-- ============================================================
-- 智慧医疗就诊系统 · 数据库结构
-- MySQL 8.4 / utf8mb4
--
-- 执行方式（必须带字符集参数，否则中文乱码）：
--   docker exec -i mysql mysql --default-character-set=utf8mb4 -uroot -p123456 < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS medical DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE medical;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS sys_user, payment, prescription_item, prescription, medicine,
  medical_record, message, consultation, registration, schedule, visitor,
  qa_question, triage_record, doctor, patient, department, staff;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 一、账号与基础档案
-- ============================================================

-- 科室
CREATE TABLE department (
  id    INT NOT NULL AUTO_INCREMENT COMMENT '科室ID',
  name  VARCHAR(50)  NOT NULL COMMENT '科室名称',
  intro VARCHAR(500) DEFAULT NULL COMMENT '科室简介',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dept_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科室';

-- 统一登录账号表：四个角色共用一张表，靠 role 区分。
-- 患者登录名=手机号，医生登录名=工号，药房/管理员=员工账号。
-- ref_id 指向业务表主键（patient.id / doctor.id），药房和管理员没有业务表，为 NULL。
-- 说明：ref_id 是多态的（指向不同表），因此无法建物理外键，靠 (role, ref_id) 索引 + 应用层保证。
CREATE TABLE sys_user (
  id          INT NOT NULL AUTO_INCREMENT COMMENT '账号ID',
  username    VARCHAR(50) NOT NULL COMMENT '登录账号',
  password    VARCHAR(64) NOT NULL COMMENT '登录密码（课设演示用明文）',
  role        VARCHAR(20) NOT NULL COMMENT 'patient/doctor/pharmacy/admin',
  ref_id      INT DEFAULT NULL COMMENT '关联业务主键：patient.id 或 doctor.id',
  name        VARCHAR(50) NOT NULL DEFAULT '' COMMENT '显示名称',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用（登录唯一开关）',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role_ref (role, ref_id),
  CONSTRAINT ck_sys_user_role CHECK (role IN ('patient','doctor','pharmacy','admin')),
  CONSTRAINT ck_sys_user_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一登录账号';

-- 患者档案（登录密码已迁至 sys_user）
CREATE TABLE patient (
  id          INT NOT NULL AUTO_INCREMENT COMMENT '患者ID',
  phone       VARCHAR(20) NOT NULL COMMENT '手机号（与 sys_user.username 一致）',
  name        VARCHAR(50) NOT NULL DEFAULT '' COMMENT '姓名',
  id_card     VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
  sex         VARCHAR(4)  DEFAULT NULL COMMENT '性别',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=停用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_patient_phone (phone),
  CONSTRAINT ck_patient_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者档案';

-- 医生（登录工号/密码已迁至 sys_user）
-- status：1=正常 0=停用 2=离职（软删除，历史处方/挂号/病历全部保留可追溯）
CREATE TABLE doctor (
  id            INT NOT NULL AUTO_INCREMENT COMMENT '医生ID',
  name          VARCHAR(50) NOT NULL COMMENT '姓名',
  title         VARCHAR(50) DEFAULT NULL COMMENT '职称',
  dept_id       INT NOT NULL COMMENT '所属科室ID',
  specialty     VARCHAR(255) DEFAULT NULL COMMENT '擅长领域',
  intro         VARCHAR(500) DEFAULT NULL COMMENT '医生简介',
  fee           DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '挂号费',
  score         DECIMAL(3,1)  NOT NULL DEFAULT 5.0 COMMENT '评分',
  online_status VARCHAR(10)   NOT NULL DEFAULT '在线' COMMENT '在线/忙碌/离线',
  status        TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=停用 2=离职',
  PRIMARY KEY (id),
  KEY idx_doctor_dept (dept_id),
  KEY idx_doctor_status (status),
  CONSTRAINT fk_doctor_dept FOREIGN KEY (dept_id) REFERENCES department (id),
  CONSTRAINT ck_doctor_status CHECK (status IN (0,1,2)),
  CONSTRAINT ck_doctor_fee CHECK (fee >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医生';

-- 就诊人（一个患者账号可绑定多名家属）
CREATE TABLE visitor (
  id         INT NOT NULL AUTO_INCREMENT COMMENT '就诊人ID',
  patient_id INT NOT NULL COMMENT '所属患者账号',
  name       VARCHAR(50) NOT NULL COMMENT '就诊人姓名',
  sex        VARCHAR(4)  DEFAULT NULL COMMENT '性别',
  age        INT         DEFAULT NULL COMMENT '年龄',
  id_card    VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
  phone      VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  PRIMARY KEY (id),
  KEY idx_visitor_patient (patient_id),
  CONSTRAINT fk_visitor_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
  CONSTRAINT ck_visitor_age CHECK (age IS NULL OR (age >= 0 AND age <= 150))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='就诊人';

-- ============================================================
-- 二、药品与排班
-- ============================================================

CREATE TABLE medicine (
  id             INT NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  name           VARCHAR(100) NOT NULL COMMENT '药品名称',
  generic_name   VARCHAR(100) DEFAULT NULL COMMENT '通用名',
  spec           VARCHAR(100) DEFAULT NULL COMMENT '规格',
  category       VARCHAR(50)  DEFAULT NULL COMMENT '分类',
  manufacturer   VARCHAR(100) DEFAULT NULL COMMENT '生产厂家',
  price          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
  stock          INT NOT NULL DEFAULT 0 COMMENT '库存',
  warn_stock     INT NOT NULL DEFAULT 0 COMMENT '库存预警值',
  insurance_type VARCHAR(20) DEFAULT NULL COMMENT '医保类别：甲类/乙类/丙类',
  status         VARCHAR(10) NOT NULL DEFAULT '上架' COMMENT '上架/下架',
  PRIMARY KEY (id),
  KEY idx_medicine_status (status),
  KEY idx_medicine_name (name),
  CONSTRAINT ck_medicine_stock CHECK (stock >= 0),
  CONSTRAINT ck_medicine_price CHECK (price >= 0),
  CONSTRAINT ck_medicine_status CHECK (status IN ('上架','下架'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='药品信息';

-- 医生排班号源。CHECK 兜住"取消挂号重复执行导致号源虚增超卖"。
CREATE TABLE schedule (
  id           INT NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  doctor_id    INT NOT NULL COMMENT '医生ID',
  work_date    DATE NOT NULL COMMENT '出诊日期',
  time_slot    VARCHAR(20) NOT NULL COMMENT '时段：上午/下午/晚间',
  total_count  INT NOT NULL DEFAULT 0 COMMENT '总号源',
  remain_count INT NOT NULL DEFAULT 0 COMMENT '剩余号源',
  PRIMARY KEY (id),
  UNIQUE KEY uk_schedule (doctor_id, work_date, time_slot),
  KEY idx_schedule_date (work_date),
  CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
  CONSTRAINT ck_schedule_count CHECK (total_count >= 0 AND remain_count >= 0 AND remain_count <= total_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医生排班号源';

-- ============================================================
-- 三、挂号与缴费
-- ============================================================

CREATE TABLE registration (
  id           INT NOT NULL AUTO_INCREMENT COMMENT '挂号ID',
  reg_no       VARCHAR(50) NOT NULL COMMENT '挂号单号',
  patient_id   INT NOT NULL COMMENT '患者ID',
  visitor_id   INT NOT NULL COMMENT '就诊人ID',
  visitor_name VARCHAR(50) DEFAULT NULL COMMENT '就诊人姓名（快照）',
  doctor_id    INT NOT NULL COMMENT '医生ID',
  doctor_name  VARCHAR(50) DEFAULT NULL COMMENT '医生姓名（快照）',
  dept_name    VARCHAR(50) DEFAULT NULL COMMENT '科室名称（快照）',
  work_date    DATE DEFAULT NULL COMMENT '就诊日期',
  time_slot    VARCHAR(20) DEFAULT NULL COMMENT '就诊时段',
  fee          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '挂号费',
  status       VARCHAR(10) NOT NULL DEFAULT '待支付' COMMENT '待支付/已支付/已取消/已完成',
  pay_type     VARCHAR(20) DEFAULT NULL COMMENT '微信/支付宝',
  pay_time     DATETIME DEFAULT NULL,
  finish_time  DATETIME DEFAULT NULL COMMENT '就诊完成时间（医生归档门诊病历时写入）',
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reg_no (reg_no),
  KEY idx_reg_patient (patient_id),
  KEY idx_reg_doctor_status (doctor_id, status),
  KEY idx_reg_conflict (visitor_id, doctor_id, work_date, time_slot),
  CONSTRAINT fk_reg_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
  CONSTRAINT fk_reg_visitor FOREIGN KEY (visitor_id) REFERENCES visitor (id),
  CONSTRAINT fk_reg_doctor  FOREIGN KEY (doctor_id)  REFERENCES doctor (id),
  CONSTRAINT ck_reg_status CHECK (status IN ('待支付','已支付','已取消','已完成'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='挂号记录';

-- 缴费订单。biz_type+biz_id 唯一：同一个业务单据只能有一张订单
-- （旧库没有这个唯一键，而 selectByBiz 按单条返回，重复行会抛 TooManyResultsException）。
-- biz_id 是多态的（指向 registration 或 prescription），故不建外键。
CREATE TABLE payment (
  id          INT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  order_no    VARCHAR(50) NOT NULL COMMENT '订单号',
  patient_id  INT NOT NULL COMMENT '患者ID',
  biz_type    VARCHAR(20) NOT NULL COMMENT '业务类型：挂号费/药品费',
  biz_id      INT NOT NULL COMMENT '业务单据ID',
  title       VARCHAR(200) DEFAULT NULL COMMENT '订单标题',
  amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  status      VARCHAR(10) NOT NULL DEFAULT '待支付' COMMENT '待支付/已支付/已退款/已取消',
  pay_type    VARCHAR(20) DEFAULT NULL COMMENT '微信/支付宝',
  pay_time    DATETIME DEFAULT NULL,
  ticket_no   VARCHAR(50) DEFAULT NULL COMMENT '电子票据号',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_order_no (order_no),
  UNIQUE KEY uk_payment_ticket_no (ticket_no),
  UNIQUE KEY uk_payment_biz (biz_type, biz_id),
  KEY idx_payment_patient (patient_id, status),
  CONSTRAINT fk_payment_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
  CONSTRAINT ck_payment_status CHECK (status IN ('待支付','已支付','已退款','已取消')),
  CONSTRAINT ck_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='缴费订单';

-- ============================================================
-- 四、在线问诊
-- ============================================================

CREATE TABLE consultation (
  id           INT NOT NULL AUTO_INCREMENT COMMENT '问诊ID',
  patient_id   INT NOT NULL COMMENT '患者ID',
  visitor_id   INT NOT NULL COMMENT '就诊人ID',
  visitor_name VARCHAR(50) DEFAULT NULL COMMENT '就诊人姓名（快照）',
  doctor_id    INT NOT NULL COMMENT '医生ID',
  doctor_name  VARCHAR(50) DEFAULT NULL COMMENT '医生姓名（快照）',
  type         VARCHAR(20) NOT NULL DEFAULT '图文' COMMENT '图文/语音/视频',
  description  VARCHAR(500) DEFAULT NULL COMMENT '病情描述',
  status       VARCHAR(10) NOT NULL DEFAULT '待接诊' COMMENT '待接诊/问诊中/已结束',
  summary      VARCHAR(500) DEFAULT NULL COMMENT '问诊小结',
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finish_time  DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_consult_doctor (doctor_id, status),
  KEY idx_consult_patient (patient_id),
  CONSTRAINT fk_consult_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
  CONSTRAINT fk_consult_visitor FOREIGN KEY (visitor_id) REFERENCES visitor (id),
  CONSTRAINT fk_consult_doctor  FOREIGN KEY (doctor_id)  REFERENCES doctor (id),
  CONSTRAINT ck_consult_status CHECK (status IN ('待接诊','问诊中','已结束'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线咨询（问诊）';

CREATE TABLE message (
  id              INT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  consultation_id INT NOT NULL COMMENT '问诊ID',
  sender          VARCHAR(20) NOT NULL COMMENT '发送方：patient/doctor',
  content         VARCHAR(1000) NOT NULL COMMENT '消息内容',
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_message_consultation (consultation_id),
  CONSTRAINT fk_message_consultation FOREIGN KEY (consultation_id) REFERENCES consultation (id),
  CONSTRAINT ck_message_sender CHECK (sender IN ('patient','doctor'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问诊消息';

-- ============================================================
-- 五、处方与病历
-- ============================================================

CREATE TABLE prescription (
  id              INT NOT NULL AUTO_INCREMENT COMMENT '处方ID',
  rx_no           VARCHAR(50) NOT NULL COMMENT '处方号',
  consultation_id INT DEFAULT NULL COMMENT '关联问诊ID',
  patient_id      INT NOT NULL COMMENT '患者ID',
  visitor_name    VARCHAR(50) DEFAULT NULL COMMENT '就诊人姓名（快照）',
  doctor_id       INT DEFAULT NULL COMMENT '医生ID',
  doctor_name     VARCHAR(50) DEFAULT NULL COMMENT '医生姓名（快照）',
  diagnosis       VARCHAR(200) DEFAULT NULL COMMENT '诊断结论',
  status          VARCHAR(10) NOT NULL DEFAULT '待缴费' COMMENT '待缴费/待发药/已完成/已作废',
  total_amount    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '处方总金额',
  remark          VARCHAR(255) DEFAULT NULL COMMENT '发药备注',
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finish_time     DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rx_no (rx_no),
  KEY idx_rx_patient (patient_id),
  KEY idx_rx_doctor (doctor_id),
  KEY idx_rx_status (status),
  CONSTRAINT fk_rx_consultation FOREIGN KEY (consultation_id) REFERENCES consultation (id),
  CONSTRAINT fk_rx_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
  CONSTRAINT fk_rx_doctor  FOREIGN KEY (doctor_id)  REFERENCES doctor (id),
  CONSTRAINT ck_rx_status CHECK (status IN ('待缴费','待发药','已完成','已作废'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电子处方';

CREATE TABLE prescription_item (
  id              INT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  prescription_id INT NOT NULL COMMENT '处方ID',
  medicine_id     INT NOT NULL COMMENT '药品ID',
  medicine_name   VARCHAR(100) DEFAULT NULL COMMENT '药品名称（快照）',
  spec            VARCHAR(100) DEFAULT NULL COMMENT '规格（快照）',
  quantity        INT NOT NULL DEFAULT 1 COMMENT '数量',
  dosage          VARCHAR(50) DEFAULT NULL COMMENT '单次用量',
  frequency       VARCHAR(50) DEFAULT NULL COMMENT '服用频次',
  course          VARCHAR(50) DEFAULT NULL COMMENT '疗程',
  price           DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价（快照）',
  subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '小计',
  PRIMARY KEY (id),
  KEY idx_rxitem_prescription (prescription_id),
  KEY idx_rxitem_medicine (medicine_id),
  CONSTRAINT fk_rxitem_prescription FOREIGN KEY (prescription_id) REFERENCES prescription (id),
  CONSTRAINT fk_rxitem_medicine FOREIGN KEY (medicine_id) REFERENCES medicine (id),
  CONSTRAINT ck_rxitem_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='处方明细';

-- 病历。两条来源路径，靠唯一键保证一次就诊只归档一份：
--   门诊：医生完成挂号就诊 → registration_id 唯一
--   问诊：医生结束在线咨询 → consultation_id 唯一
-- （MySQL 唯一索引允许多个 NULL，所以另一条路径留空不会互相冲突）
CREATE TABLE medical_record (
  id              INT NOT NULL AUTO_INCREMENT COMMENT '病历ID',
  patient_id      INT NOT NULL COMMENT '患者ID',
  visitor_id      INT NOT NULL COMMENT '就诊人ID',
  visitor_name    VARCHAR(50) DEFAULT NULL COMMENT '就诊人姓名（快照）',
  doctor_id       INT DEFAULT NULL COMMENT '医生ID',
  doctor_name     VARCHAR(50) DEFAULT NULL COMMENT '医生姓名（快照）',
  dept_name       VARCHAR(50) DEFAULT NULL COMMENT '科室名称（快照）',
  type            VARCHAR(10) NOT NULL DEFAULT '门诊' COMMENT '门诊(挂号就诊)/问诊(在线咨询)',
  registration_id INT DEFAULT NULL COMMENT '来源挂号单',
  consultation_id INT DEFAULT NULL COMMENT '来源问诊',
  diagnosis       VARCHAR(200) NOT NULL DEFAULT '' COMMENT '诊断结论',
  advice          VARCHAR(500) DEFAULT NULL COMMENT '医嘱',
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_record_registration (registration_id),
  UNIQUE KEY uk_record_consultation (consultation_id),
  KEY idx_record_visitor (visitor_id),
  KEY idx_record_patient (patient_id),
  CONSTRAINT fk_record_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
  CONSTRAINT fk_record_visitor FOREIGN KEY (visitor_id) REFERENCES visitor (id),
  CONSTRAINT fk_record_doctor  FOREIGN KEY (doctor_id)  REFERENCES doctor (id),
  CONSTRAINT fk_record_registration FOREIGN KEY (registration_id) REFERENCES registration (id),
  CONSTRAINT fk_record_consultation FOREIGN KEY (consultation_id) REFERENCES consultation (id),
  CONSTRAINT ck_record_type CHECK (type IN ('门诊','问诊'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='病历';

-- ============================================================
-- 六、在线问答与预分诊
-- ============================================================

CREATE TABLE qa_question (
  id           INT NOT NULL AUTO_INCREMENT COMMENT '问答ID',
  patient_id   INT NOT NULL COMMENT '患者ID',
  patient_name VARCHAR(50) DEFAULT NULL COMMENT '患者姓名（快照）',
  content      VARCHAR(500) NOT NULL COMMENT '问题内容',
  answer       VARCHAR(1000) DEFAULT NULL COMMENT '管理员解答',
  status       VARCHAR(10) NOT NULL DEFAULT '待回复' COMMENT '待回复/已回复',
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  answer_time  DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_qa_patient (patient_id),
  KEY idx_qa_status (status),
  CONSTRAINT fk_qa_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
  CONSTRAINT ck_qa_status CHECK (status IN ('待回复','已回复'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='在线问答';

CREATE TABLE triage_record (
  id             INT NOT NULL AUTO_INCREMENT COMMENT '预分诊ID',
  patient_id     INT NOT NULL COMMENT '患者ID',
  visitor_id     INT DEFAULT NULL COMMENT '就诊人ID',
  visitor_name   VARCHAR(50) DEFAULT NULL COMMENT '就诊人姓名（快照）',
  body_part      VARCHAR(50) DEFAULT NULL COMMENT '症状部位',
  symptoms       VARCHAR(500) DEFAULT NULL COMMENT '症状描述',
  duration       VARCHAR(20) DEFAULT NULL COMMENT '发病时间',
  suggested_dept VARCHAR(50) DEFAULT NULL COMMENT '建议科室',
  urgency        VARCHAR(10) NOT NULL DEFAULT '普通' COMMENT '普通/急诊',
  advice         VARCHAR(500) DEFAULT NULL COMMENT '就诊建议',
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_triage_patient (patient_id),
  CONSTRAINT fk_triage_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
  CONSTRAINT fk_triage_visitor FOREIGN KEY (visitor_id) REFERENCES visitor (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预分诊记录';

-- ============================================================
-- 说明：以下关系故意不建物理外键
--   1. sys_user.ref_id —— 多态指向 patient.id 或 doctor.id
--   2. payment.biz_id   —— 多态指向 registration.id 或 prescription.id
-- 这两处靠 (role, ref_id) / (biz_type, biz_id) 索引 + 应用层保证一致性。
-- ============================================================
