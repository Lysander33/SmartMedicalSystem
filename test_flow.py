# -*- coding: utf-8 -*-
"""智慧医疗就诊系统 全流程闭环验证脚本"""
import json, urllib.request, urllib.parse

B = "http://localhost:8080"

def post_json(url, obj):
    req = urllib.request.Request(B + url, data=json.dumps(obj).encode("utf-8"),
                                 headers={"Content-Type": "application/json"}, method="POST")
    return json.loads(urllib.request.urlopen(req).read().decode("utf-8"))

def post(url, params=None):
    if params:
        url += "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(B + url, method="POST")
    return json.loads(urllib.request.urlopen(req).read().decode("utf-8"))

def get(url, params=None):
    if params:
        url += "?" + urllib.parse.urlencode(params)
    return json.loads(urllib.request.urlopen(B + url).read().decode("utf-8"))

def show(step, r):
    ok = r.get("code") == 200
    print(("✅" if ok else "❌", step, "->", json.dumps(r, ensure_ascii=False)[:220]))
    return r

print("===== 患者端 =====")
show("1.患者登录", post_json("/api/patient/login", {"phone": "13800000001", "password": "123456"}))
show("2.就诊人列表", get("/api/patient/visitor/list", {"patientId": 1}))
r = show("3.AI预分诊(咳嗽发热)", post_json("/api/triage/submit",
    {"patientId": 1, "visitorId": 1, "visitorName": "王大明", "bodyPart": "咽喉",
     "symptoms": "咳嗽、发烧、嗓子疼", "duration": "1-3天"}))
r = show("3b.AI预分诊(胸痛-急诊预警)", post_json("/api/triage/submit",
    {"patientId": 1, "visitorId": 1, "visitorName": "王大明", "bodyPart": "胸部",
     "symptoms": "胸痛、胸闷、心慌", "duration": "今天"}))
show("4.科室列表", get("/api/guide/depts"))
show("5.呼吸内科医生", get("/api/guide/doctors", {"deptId": 1}))
sch = get("/api/guide/schedules", {"doctorId": 1})
show("6.张志强排班", sch)
schedule_id = sch["data"][0]["id"]
r = show("7.挂号下单", post("/api/reg/create", {"scheduleId": schedule_id, "visitorId": 1, "patientId": 1}))
reg_id = r["data"]["id"]
show("7b.重复挂号拦截", post("/api/reg/create", {"scheduleId": schedule_id, "visitorId": 1, "patientId": 1}))
pend = get("/api/pay/pending", {"patientId": 1})
show("8.待缴清单", pend)
pay_id = pend["data"][0]["id"]
show("9.微信支付挂号费", post("/api/pay/pay", {"paymentId": pay_id, "payType": "微信"}))
r = show("10.发起咨询", post_json("/api/consult/start",
    {"patientId": 1, "visitorId": 1, "doctorId": 1, "type": "图文",
     "description": "咳嗽两天，伴发烧38度，嗓子疼"}))
cid = r["data"]["id"]

print("===== 医生端 =====")
show("11.医生待接诊列表", get("/api/doctorwork/pending", {"doctorId": 1}))
show("12.医生接诊", post("/api/doctorwork/accept", {"consultationId": cid}))
show("13.医生回复消息", post_json("/api/doctorwork/send",
    {"consultationId": cid, "content": "建议多喝水，先给您开点药"}))
show("14.查看对话", get("/api/consult/messages", {"consultationId": cid}))
show("15.开具电子处方", post_json("/api/doctorwork/rx/create",
    {"consultationId": cid, "diagnosis": "急性上呼吸道感染",
     "items": [
         {"medicineId": 3, "quantity": 2, "dosage": "4粒", "frequency": "每日3次", "course": "3天"},
         {"medicineId": 2, "quantity": 1, "dosage": "1粒", "frequency": "每日2次", "course": "发烧时服用"}]}))
show("15b.库存校验(超量拦截)", post_json("/api/doctorwork/rx/create",
    {"consultationId": cid, "diagnosis": "测试", "items": [{"medicineId": 3, "quantity": 99999}]}))
show("16.结束问诊", post("/api/doctorwork/finish", {"consultationId": cid, "summary": "上呼吸道感染，按时服药，多休息，三天后复诊"}))
show("17.归档病历", post_json("/api/doctorwork/record/add",
    {"patientId": 1, "visitorId": 1, "visitorName": "王大明", "doctorId": 1,
     "doctorName": "张志强", "deptName": "呼吸内科", "diagnosis": "急性上呼吸道感染",
     "advice": "按时服药，多休息，三天后复诊"}))

print("===== 患者缴费药品费 =====")
pend2 = get("/api/pay/pending", {"patientId": 1})
show("18.待缴清单(药品费)", pend2)
rx_pay = [p for p in pend2["data"] if p["bizType"] == "药品费"][0]
show("19.支付宝支付药品费", post("/api/pay/pay", {"paymentId": rx_pay["id"], "payType": "支付宝"}))

print("===== 药房 =====")
rxlist = get("/api/pharmacy/rx/list", {"status": "待发药"})
show("20.药房待发药列表", rxlist)
rx_id = rxlist["data"][0]["id"]
show("21.处方核对详情", get("/api/pharmacy/rx/detail", {"id": rx_id}))
show("22.发药并备注完成", post("/api/pharmacy/rx/dispense", {"rxId": rx_id, "remark": "已发药，患者已取走"}))

print("===== 记录查询 / 管理平台 =====")
show("23.我的处方(患者端)", get("/api/records/prescriptions", {"patientId": 1}))
show("24.我的病历", get("/api/records/medical", {"patientId": 1}))
show("25.电子票夹", get("/api/pay/tickets", {"patientId": 1}))
show("26.问答提问", post_json("/api/qa/ask", {"patientId": 1, "patientName": "王大明", "content": "取药需要带什么证件？"}))
show("27.管理员解答", post("/api/admin/qa/answer", {"id": 3, "answer": "取药请携带就诊人身份证或医保电子凭证。"}))
show("28.运营统计报表", get("/api/admin/order/stats"))
show("29.药品低库存预警检查", get("/api/admin/medicine/list"))
show("30.参数校验拦截(id=-1)", get("/api/guide/doctors", {"deptId": -1}))

import urllib.error
try:
    urllib.request.urlopen(B + "/v3/api-docs").read()
    print("✅ 31./v3/api-docs 可访问")
except Exception as e:
    print("❌ 31./v3/api-docs", e)
try:
    code = urllib.request.urlopen(B + "/swagger-ui.html").getcode()
    print("✅ 32./swagger-ui.html ->", code)
except Exception as e:
    print("❌ 32./swagger-ui.html", e)

print("===== 全流程闭环验证完成 =====")
