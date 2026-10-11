#!/usr/bin/env python3
"""为 fin2(memberId=12) 灌入真实感演示数据：账户/成员/流水/保单/资产/提醒快照"""
import subprocess, random
from datetime import datetime, timedelta
from decimal import Decimal

MYSQL = ["docker", "exec", "-i", "star-mysql", "mysql", "--default-character-set=utf8mb4",
         "-uroot", "-proot123", "star_site"]

def sql(statements):
    for s in statements:
        subprocess.run(MYSQL, input=s.encode(), check=True)

random.seed(42)
ME, FAMILY = 12, 8

# ---------- 账户（重置为真实感组合） ----------
sql([f"""
DELETE FROM finance_transaction WHERE owner_member_id={ME};
DELETE FROM finance_account WHERE owner_member_id={ME};
INSERT INTO finance_account (id, family_id, owner_member_id, name, account_type, balance, initial_balance, liability_flag, institution) VALUES
(9001,{FAMILY},{ME},'招行工资卡','bank',38620.55,0,0,'招商银行'),
(9002,{FAMILY},{ME},'微信钱包','wechat',2356.80,0,0,'微信支付'),
(9003,{FAMILY},{ME},'支付宝','alipay',4180.00,0,0,'支付宝'),
(9004,{FAMILY},{ME},'公积金','fund',186000.00,0,0,'市公积金中心'),
(9005,{FAMILY},{ME},'招行信用卡','credit',-6320.00,0,1,'招商银行'),
(9006,{FAMILY},{ME},'房贷','loan',1268000.00,0,1,'工商银行');
"""])

# ---------- 流水：近 3 个月，真实感 ----------
CATS_EXP = {"餐饮": (15, 80), "购物": (50, 600), "用车": (30, 400), "日用": (20, 200),
            "医疗": (80, 500), "教育": (100, 800), "娱乐": (30, 200), "话费": (50, 150)}
today = datetime.now()
txs = []
txid = 9100000
# 每月工资 28600（发到招行卡）
for m in range(3):
    month_day = today - timedelta(days=30 * m)
    pay_day = month_day.replace(day=10)
    txid += 1
    txs.append((txid, 9001, 1, 28600.00, "SALARY", "工资", pay_day))
# 房贷每月 8600（支出，招行卡）
    txid += 1
    txs.append((txid, 9001, 2, 8600.00, "MORTGAGE", "房贷", pay_day.replace(day=15)))

# 日常支出：微信/支付宝
for m in range(3):
    base = today - timedelta(days=30 * m)
    for _ in range(18):
        day = base - timedelta(days=random.randint(0, 27))
        cat = random.choice(list(CATS_EXP))
        lo, hi = CATS_EXP[cat]
        amt = round(random.uniform(lo, hi), 2)
        acc = random.choice([9002, 9003])
        txid += 1
        txs.append((txid, acc, 2, amt, cat if cat in ("MORTGAGE", "CAR", "MEDICAL", "EDUCATION") else
                    random.choice(["FOOD", "SHOPPING", "OTHER_EXPENSE"]), cat, day))

# 信用卡消费几笔 + 还款
for _ in range(5):
    day = today - timedelta(days=random.randint(1, 60))
    txid += 1
    txs.append((txid, 9005, 2, round(random.uniform(100, 2000), 2), "SHOPPING", "信用卡消费", day))

rows = []
for tid, acc, d, amt, cat, cname, day in txs:
    t = day.strftime("%Y-%m-%d") + f" {random.randint(8,21):02d}:{random.randint(0,59):02d}:00"
    rows.append(f"({tid},{FAMILY},{ME},{acc},'账户{acc}',{d},{amt},'{cat}','{cname}','{t}',1)")

sql([f"""INSERT INTO finance_transaction
(id, family_id, owner_member_id, account_id, account_name, direction, amount, category, category_name, trans_time, source)
VALUES {','.join(rows)};"""])

# 账户余额校准（期初+流水轧差后手动校准到真实感数字）
sql([f"""UPDATE finance_account SET balance=38620.55 WHERE id=9001;
UPDATE finance_account SET balance=2356.80 WHERE id=9002;
UPDATE finance_account SET balance=4180.00 WHERE id=9003;
UPDATE finance_account SET balance=-6320.00 WHERE id=9005;"""])

# ---------- 家庭成员（补全 4-2-1 结构） ----------
sql([f"""DELETE FROM family_member WHERE family_id={FAMILY};
INSERT INTO family_member (id, family_id, name, relation, gender, birth_date) VALUES
(101,{FAMILY},'钱七','self',1,'1992-03-15'),
(102,{FAMILY},'林晓','spouse',2,'1994-07-22'),
(103,{FAMILY},'钱多多','child',1,'2021-11-03'),
(104,{FAMILY},'钱父','father',1,'1965-02-18'),
(105,{FAMILY},'钱母','mother',2,'1967-09-05');"""])

# ---------- 保单 ----------
sql([f"""DELETE FROM insurance_policy WHERE owner_member_id={ME};
INSERT INTO insurance_policy (id, family_id, owner_member_id, company, product_name, policy_no, category, insured_member_id, insured_member_name, start_date, maturity_date, premium, payment_freq, next_payment_date, coverage_amount, status) VALUES
(301,{FAMILY},{ME},'平安人寿','平安福重疾2021','PA2021BL001','critical_illness',101,'钱七','2021-03-20','2046-03-20',8600.00,'annual','2027-03-20',500000,1),
(302,{FAMILY},{ME},'泰康在线','泰康百万医疗','TK2022MD002','major_medical',101,'钱七','2022-04-01','2042-04-01',620.00,'annual','2027-04-01',2000000,1),
(303,{FAMILY},{ME},'太平洋人寿','金诺优享','CPIC2023L003','life',102,'林晓','2023-06-15','2053-06-15',4200.00,'annual','2027-06-15',1000000,1),
(304,{FAMILY},{ME},'人保健康','好医保长期医疗','PICC2024M004','major_medical',102,'林晓','2024-01-10','2044-01-10',258.00,'annual','2027-01-10',2000000,1),
(305,{FAMILY},{ME},'平安产险','家庭综合意外险','PA2025AC005','accident',103,'钱多多','2025-02-01','2027-01-31',198.00,'annual','2027-02-01',200000,1);"""])

# ---------- 档案资产（房产+车） ----------
sql([f"""DELETE FROM archive_asset_valuation WHERE asset_id IN (SELECT id FROM archive_asset WHERE owner_member_id={ME});
DELETE FROM archive_asset WHERE owner_member_id={ME};
INSERT INTO archive_asset (id, family_id, owner_member_id, name, asset_type, holder_member_id, holder_member_name, acquire_date, current_value, valued_at, next_inspect_date, remark) VALUES
(401,{FAMILY},{ME},'XX花园 89平两居','real_estate',101,'钱七','2021-06-01',2180000.00,'2026-09-01',NULL,'房贷月供 8600'),
(402,{FAMILY},{ME},'特斯拉 Model Y','vehicle',101,'钱七','2024-03-18',189000.00,'2026-09-20','2027-03-10',''),
(403,{FAMILY},{ME},'定期存款','deposit',NULL,'','2023-01-01',200000.00,'2026-10-01',NULL,'');
INSERT INTO archive_asset_valuation (asset_id, value, valued_at) VALUES
(401,2250000.00,'2026-04-01'),(401,2210000.00,'2026-07-01'),(401,2180000.00,'2026-10-01'),
(402,196000.00,'2026-04-01'),(402,192000.00,'2026-07-01'),(402,189000.00,'2026-10-01'),
(403,200000.00,'2026-04-01'),(403,200000.00,'2026-07-01'),(403,200000.00,'2026-10-01');"""])

# ---------- 提醒（不同日期状态，含逾期/今天/未来） ----------
sql([f"""DELETE FROM reminder_rule WHERE owner_member_id={ME};
DELETE FROM reminder_occurrence WHERE owner_member_id={ME};
DELETE FROM notification WHERE owner_member_id={ME};
INSERT INTO reminder_rule (id, family_id, owner_member_id, source, biz_type, biz_id, title, first_remind_at, repeat_type, repeat_until, advance_days, status) VALUES
(501,{FAMILY},{ME},'star-site-biz-insurance','policy_payment',301,'平安福重疾 缴费','2027-03-13','ANNUAL','2046-03-20',7,1),
(502,{FAMILY},{ME},'star-site-biz-archive','vehicle_inspect',402,'特斯拉 Model Y 年检','2027-02-23','ANNUAL',NULL,15,1),
(503,{FAMILY},{ME},'star-site-biz-insurance','document_expiry',0,'钱母 身份证 到期','2026-11-20','NONE',NULL,30,1);"""])

print("mock 数据灌入完成")
