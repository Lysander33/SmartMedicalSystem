-- ============================================================
-- 智慧医疗就诊系统 · 演示数据
-- 执行前请先执行 schema.sql
--
-- 执行方式（必须带字符集参数，否则中文乱码）：
--   docker exec -i mysql mysql --default-character-set=utf8mb4 -uroot -p123456 < data.sql
--
-- 演示账号（密码统一 123456）：
--   患者   13800000001 / 13800000002
--   医生   D1001（王建国·心血管内科，演示主账号）/ D1002 ~ D1010
--   药房   yf001
--   管理员 admin
-- ============================================================

USE medical;
SET NAMES utf8mb4;

-- ============================================================
-- 1. 科室（TriageController 规则里的全部建议科室 + 兜底"全科医学科"）
-- ============================================================
INSERT INTO department(id, name, intro) VALUES
(1, '心血管内科', '诊治高血压、冠心病、心律失常、心力衰竭等心血管疾病'),
(2, '呼吸内科',   '诊治感冒发热、咳嗽、哮喘、肺炎、慢性阻塞性肺疾病等'),
(3, '消化内科',   '诊治胃炎、胃溃疡、腹痛腹泻、肝胆疾病等消化系统疾病'),
(4, '神经内科',   '诊治头痛头晕、失眠、脑血管病、周围神经病变等'),
(5, '口腔科',     '诊治龋齿、牙痛、牙周炎、口腔溃疡等口腔疾病'),
(6, '皮肤科',     '诊治湿疹、荨麻疹、痤疮、皮肤瘙痒等皮肤疾病'),
(7, '骨科',       '诊治骨折、关节扭伤、颈腰椎病、腰腿疼痛等'),
(8, '儿科',       '诊治儿童常见病、多发病，提供生长发育指导'),
(9, '全科医学科', '症状不明确时的首诊科室，提供综合诊疗与转诊建议');

-- ============================================================
-- 2. 患者档案 + 就诊人
-- ============================================================
INSERT INTO patient(id, phone, name, id_card, sex) VALUES
(1, '13800000001', '张伟', '110101199001011234', '男'),
(2, '13800000002', '李娜', '310101199203054567', '女');

INSERT INTO visitor(id, patient_id, name, sex, age, id_card, phone) VALUES
(1, 1, '张伟',   '男', 36, '110101199001011234', '13800000001'),
(2, 1, '张小雨', '女',  8, NULL,                 '13800000001'),
(3, 2, '李娜',   '女', 34, '310101199203054567', '13800000002');

-- ============================================================
-- 3. 医生（10 名，覆盖 9 个科室）
-- ============================================================
INSERT INTO doctor(id, name, title, dept_id, specialty, intro, fee, score, online_status, status) VALUES
(1,  '王建国', '主任医师',   1, '高血压、冠心病、心律失常', '从事心血管内科临床工作 30 年，擅长高血压与冠心病的规范化治疗。', 50.00, 4.9, '在线', 1),
(2,  '刘敏',   '副主任医师', 1, '心力衰竭、心律失常',       '心血管内科副主任医师，长期从事心力衰竭的诊疗与随访管理。',     30.00, 4.7, '在线', 1),
(3,  '陈志强', '主任医师',   2, '哮喘、慢性咳嗽、肺炎',     '呼吸内科主任医师，擅长支气管哮喘与慢性咳嗽的诊治。',           50.00, 4.8, '在线', 1),
(4,  '赵雅琴', '副主任医师', 3, '胃炎、胃溃疡、功能性消化不良', '消化内科副主任医师，擅长胃肠疾病的规范化诊疗。',           30.00, 4.6, '在线', 1),
(5,  '孙立军', '主任医师',   4, '头痛、失眠、脑血管病',     '神经内科主任医师，擅长头痛、失眠及脑血管疾病的诊治。',         50.00, 4.8, '忙碌', 1),
(6,  '周晓峰', '主治医师',   5, '龋齿、牙周炎、根管治疗',   '口腔科主治医师，擅长龋齿充填与根管治疗。',                     20.00, 4.5, '在线', 1),
(7,  '吴丽华', '副主任医师', 6, '湿疹、荨麻疹、痤疮',       '皮肤科副主任医师，擅长过敏性皮肤病与痤疮的综合治疗。',         30.00, 4.7, '在线', 1),
(8,  '郑海涛', '主任医师',   7, '骨折、关节扭伤、颈腰椎病', '骨科主任医师，擅长创伤骨折与颈腰椎疾病的诊治。',               50.00, 4.9, '在线', 1),
(9,  '冯雪梅', '副主任医师', 8, '儿童呼吸道感染、腹泻',     '儿科副主任医师，擅长儿童常见病与多发病的诊治。',               30.00, 4.8, '在线', 1),
(10, '韩明',   '主治医师',   9, '全科诊疗、慢病管理',       '全科医学科主治医师，提供综合诊疗与转诊建议。',                 20.00, 4.4, '在线', 1);

-- ============================================================
-- 4. 统一账号（四个角色共用 sys_user 表，密码演示用明文 123456）
--    ref_id：患者指向 patient.id，医生指向 doctor.id，药房/管理员为 NULL
-- ============================================================
INSERT INTO sys_user(username, password, role, ref_id, name, status) VALUES
('admin',      '123456', 'admin',    NULL, '系统管理员', 1),
('yf001',      '123456', 'pharmacy', NULL, '药房管理员', 1),
('13800000001','123456', 'patient',  1,    '张伟',       1),
('13800000002','123456', 'patient',  2,    '李娜',       1),
('D1001',      '123456', 'doctor',   1,    '王建国',     1),
('D1002',      '123456', 'doctor',   2,    '刘敏',       1),
('D1003',      '123456', 'doctor',   3,    '陈志强',     1),
('D1004',      '123456', 'doctor',   4,    '赵雅琴',     1),
('D1005',      '123456', 'doctor',   5,    '孙立军',     1),
('D1006',      '123456', 'doctor',   6,    '周晓峰',     1),
('D1007',      '123456', 'doctor',   7,    '吴丽华',     1),
('D1008',      '123456', 'doctor',   8,    '郑海涛',     1),
('D1009',      '123456', 'doctor',   9,    '冯雪梅',     1),
('D1010',      '123456', 'doctor',   10,   '韩明',       1);

-- ============================================================
-- 5. 药品（含 1 种低库存药，用于演示"缺药待补"）
-- ============================================================
INSERT INTO medicine(id, name, generic_name, spec, category, manufacturer, price, stock, warn_stock, insurance_type, status) VALUES
(1, '阿莫西林胶囊',   '阿莫西林',    '0.5g*24粒',  '抗感染药', '华北制药股份有限公司', 20.00, 200,  30, '甲类', '上架'),
(2, '头孢克肟胶囊',   '头孢克肟',    '0.1g*12粒',  '抗感染药', '广州白云山制药股份有限公司', 35.50, 5, 20, '甲类', '上架'),
(3, '布洛芬缓释胶囊', '布洛芬',      '0.3g*20粒',  '解热镇痛药', '中美天津史克制药有限公司', 18.00, 300, 50, '甲类', '上架'),
(4, '阿司匹林肠溶片', '阿司匹林',    '100mg*30片', '心脑血管药', '拜耳医药保健有限公司',   15.00, 150, 30, '甲类', '上架'),
(5, '硝苯地平缓释片', '硝苯地平',    '20mg*30片',  '心脑血管药', '青岛黄海制药有限责任公司', 28.00, 120, 30, '甲类', '上架'),
(6, '奥美拉唑肠溶胶囊','奥美拉唑',   '20mg*14粒',  '消化系统药', '阿斯利康制药有限公司',   32.00, 180, 40, '甲类', '上架'),
(7, '氯雷他定片',     '氯雷他定',    '10mg*6片',   '抗过敏药', '上海先灵葆雅制药有限公司', 22.00, 90,  20, '乙类', '上架'),
(8, '云南白药气雾剂', '云南白药',    '85g',        '外用中成药', '云南白药集团股份有限公司', 45.00, 60,  15, '乙类', '上架'),
(9, '复方甘草片',     '复方甘草',    '100片',      '呼吸系统药', '太极集团重庆桐君阁药厂', 12.00, 80,  20, '乙类', '上架');

-- ============================================================
-- 6. 排班：每位医生未来 3 天、上午/下午各 20 个号源
--    用 CURDATE() 生成，演示数据永远不会过期
-- ============================================================
INSERT INTO schedule(doctor_id, work_date, time_slot, total_count, remain_count)
SELECT d.id, DATE_ADD(CURDATE(), INTERVAL s.day_offset DAY), t.time_slot, 20, 20
FROM doctor d
CROSS JOIN (SELECT 0 AS day_offset UNION ALL SELECT 1 UNION ALL SELECT 2) s
CROSS JOIN (SELECT '上午' AS time_slot UNION ALL SELECT '下午') t
WHERE d.id BETWEEN 1 AND 10;

-- ============================================================
-- 7. 挂号单 + 对应缴费订单
--    reg 1：张伟 挂 王建国 今日上午 → 待支付（演示在线缴费）
--    reg 2：李娜 挂 王建国 今日下午 → 已支付（医生端"挂号患者队列"首屏即有数据）
-- ============================================================
INSERT INTO registration(id, reg_no, patient_id, visitor_id, visitor_name, doctor_id, doctor_name, dept_name,
                         work_date, time_slot, fee, status, pay_type, pay_time, finish_time) VALUES
(1, 'REG20260920000001', 1, 1, '张伟', 1, '王建国', '心血管内科', CURDATE(), '上午', 50.00, '待支付', NULL, NULL, NULL),
(2, 'REG20260920000002', 2, 3, '李娜', 1, '王建国', '心血管内科', CURDATE(), '下午', 50.00, '已支付', '微信', NOW(), NULL);

INSERT INTO payment(id, order_no, patient_id, biz_type, biz_id, title, amount, status, pay_type, pay_time, ticket_no) VALUES
(1, 'PAY20260920000001', 1, '挂号费', 1, '心血管内科-王建国-挂号费', 50.00, '待支付', NULL, NULL, NULL),
(2, 'PAY20260920000002', 2, '挂号费', 2, '心血管内科-王建国-挂号费', 50.00, '已支付', '微信', NOW(), 'TICKET20260920000002');

-- 上面两张挂号单占用了号源，同步扣减剩余号源
UPDATE schedule SET remain_count = remain_count - 1
WHERE doctor_id = 1 AND work_date = CURDATE() AND time_slot IN ('上午', '下午');

-- ============================================================
-- 8. 在线问诊 + 消息
--    consult 1：待接诊（张小雨 发热咳嗽 -> 陈志强）医生端首屏即有排队患者
--    consult 2：已结束（张伟 腹痛 -> 赵雅琴）用于挂出下面那张处方
-- ============================================================
INSERT INTO consultation(id, patient_id, visitor_id, visitor_name, doctor_id, doctor_name, type, description,
                         status, summary, create_time, finish_time) VALUES
(1, 1, 2, '张小雨', 3, '陈志强', '图文', '孩子昨晚开始发烧 38.5 度，伴有咳嗽、流鼻涕，请问需要立刻来医院吗？',
 '待接诊', NULL, NOW(), NULL),
(2, 1, 1, '张伟', 4, '赵雅琴', '图文', '饭后上腹部隐痛两周，偶有反酸、嗳气。',
 '已结束', '考虑慢性胃炎，建议规律饮食、忌辛辣，已开具抑酸与胃黏膜保护用药，两周后复查。', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO message(consultation_id, sender, content, create_time) VALUES
(1, 'patient', '医生您好，孩子昨晚开始发烧，最高 38.5 度，需要马上来医院吗？', NOW()),
(1, 'doctor',  '先物理降温并多喝水，我正在查看您的描述，请稍等。', NOW()),
(2, 'patient', '医生，我饭后总是上腹隐痛，已经两周了。', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'doctor',  '症状考虑慢性胃炎可能性大，我先为您开一周的药，注意清淡饮食。', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- 9. 处方（来源：已结束的问诊 2）+ 明细 + 已支付药品费订单
--    状态"待发药" → 药房端首屏即有可发药处方
-- ============================================================
INSERT INTO prescription(id, rx_no, consultation_id, patient_id, visitor_name, doctor_id, doctor_name,
                         diagnosis, status, total_amount, remark, create_time, finish_time) VALUES
(1, 'RX20260920000001', 2, 1, '张伟', 4, '赵雅琴', '慢性胃炎',
 '待发药', 58.00, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL);

INSERT INTO prescription_item(prescription_id, medicine_id, medicine_name, spec, quantity, dosage, frequency, course, price, subtotal) VALUES
(1, 1, '阿莫西林胶囊',   '0.5g*24粒', 2, '0.5g', '每日3次', '7天', 20.00, 40.00),
(1, 3, '布洛芬缓释胶囊', '0.3g*20粒', 1, '0.3g', '每日2次', '5天', 18.00, 18.00);

INSERT INTO payment(id, order_no, patient_id, biz_type, biz_id, title, amount, status, pay_type, pay_time, ticket_no) VALUES
(3, 'PAY20260920000003', 1, '药品费', 1, '电子处方 RX20260920000001 药品费', 58.00, '已支付', '支付宝', DATE_SUB(NOW(), INTERVAL 2 DAY), 'TICKET20260920000003');

-- ============================================================
-- 10. 病历（问诊来源）——患者端"病历记录"首屏即有数据
-- ============================================================
INSERT INTO medical_record(patient_id, visitor_id, visitor_name, doctor_id, doctor_name, dept_name, type,
                           registration_id, consultation_id, diagnosis, advice, create_time) VALUES
(1, 1, '张伟', 4, '赵雅琴', '消化内科', '问诊', NULL, 2, '慢性胃炎',
 '规律三餐、忌辛辣刺激与浓茶咖啡；按时服药，两周后复诊；若出现黑便、呕血请立即就诊。',
 DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- 11. 在线问答（1 条待回复 → 管理端首屏即有工单）
-- ============================================================
INSERT INTO qa_question(patient_id, patient_name, content, answer, status, create_time, answer_time) VALUES
(1, '张伟', '感冒期间可以和阿莫西林一起服用吗？会不会有冲突？', NULL, '待回复', NOW(), NULL),
(2, '李娜', '网上挂号后当天没去就诊，挂号费可以退吗？',
 '就诊当天未到诊的，请在挂号时段结束前于"我的挂号"中申请退号，费用将原路退回；超时未退号的视为爽约，费用不予退还。',
 '已回复', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 20 HOUR));

-- ============================================================
-- 12. 预分诊历史
-- ============================================================
INSERT INTO triage_record(patient_id, visitor_id, visitor_name, body_part, symptoms, duration, suggested_dept, urgency, advice, create_time) VALUES
(1, 1, '张伟', '胸部', '胸痛、心悸', '1天',
 '心血管内科', '急诊',
 '检测到高危症状【胸痛】，请尽快前往线下急诊就医！（本结果由AI预分诊模型给出，仅供参考，不构成诊断）',
 DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ============================================================
-- 数据自检
-- ============================================================
SELECT '科室'   AS 表, COUNT(*) AS 行数 FROM department
UNION ALL SELECT '医生',   COUNT(*) FROM doctor
UNION ALL SELECT '账号',   COUNT(*) FROM sys_user
UNION ALL SELECT '药品',   COUNT(*) FROM medicine
UNION ALL SELECT '排班',   COUNT(*) FROM schedule
UNION ALL SELECT '挂号',   COUNT(*) FROM registration
UNION ALL SELECT '订单',   COUNT(*) FROM payment
UNION ALL SELECT '问诊',   COUNT(*) FROM consultation
UNION ALL SELECT '处方',   COUNT(*) FROM prescription
UNION ALL SELECT '病历',   COUNT(*) FROM medical_record
UNION ALL SELECT '问答',   COUNT(*) FROM qa_question;
