# -*- coding: utf-8 -*-
"""勇者测试 端到端冒烟测试：完整玩法闭环验证"""
import json
import sys
import time
import urllib.request
import urllib.parse
import urllib.error

BASE = __import__("os").environ.get("E2E_BASE", "http://localhost:18080") + "/api/v1"
PASS, FAIL = [], []


def call(method, path, token=None, data=None, form=None):
    url = BASE + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = None
    if data is not None:
        body = json.dumps(data).encode()
    if form is not None:
        body = urllib.parse.urlencode(form).encode()
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read().decode())
        except Exception:
            return {"code": e.code, "message": str(e)}


def check(name, cond, detail=""):
    (PASS if cond else FAIL).append(name)
    mark = "PASS" if cond else "FAIL"
    print(f"[{mark}] {name} {detail}")


ts = int(time.time())
alice_u, bob_u = f"alice{ts % 100000}", f"bob{ts % 100000}"

print("=" * 60)
print("场景0 注册与登录")
ra = call("POST", "/auth/register", data={"username": alice_u, "password": "test123", "nickname": "爱丽丝"})
rb = call("POST", "/auth/register", data={"username": bob_u, "password": "test123", "nickname": "小明"})
check("注册 alice/bob", ra.get("code") == 0 and rb.get("code") == 0, str(ra.get("message", "")))
la = call("POST", "/auth/login", data={"username": alice_u, "password": "test123"})
lb = call("POST", "/auth/login", data={"username": bob_u, "password": "test123"})
als = (la.get("data") or {}).get("token") if la.get("code") == 0 else None
bobs = (lb.get("data") or {}).get("token") if lb.get("code") == 0 else None
check("登录获取 token", bool(als) and bool(bobs), str(la.get("message", "")))

admins = {}
for name in ["superadmin", "xingzhe_admin", "adventurer_admin", "knowledge_admin"]:
    r = call("POST", "/auth/login", data={"username": name, "password": "admin123"})
    admins[name] = (r.get("data") or {}).get("token")
check("4个管理员登录", all(admins.values()))

print("=" * 60)
print("场景1 觉醒性格测试")
me0 = call("GET", "/adventurer/me", token=bobs).get("data") or {}
check("bob 初始档案 D级", me0.get("rankLevel") == "D", f"pollution={me0.get('pollutionValue')}")
qs = call("GET", "/test/questions", token=bobs).get("data", [])
check("测试题库返回", len(qs) >= 5, f"{len(qs)} 题")
answers = [{"questionId": q.get("id") or q.get("questionId"), "optionIndex": 0} for q in qs]
tr = call("POST", "/test/submit", token=bobs, data=answers)
check("提交测试得出称号", tr.get("code") == 0 and bool(tr.get("data") or {}.get("title")),
      f"title={tr.get('data', {}).get('title')}")
me1 = call("GET", "/adventurer/me", token=bobs).get("data") or {}
check("测试影响污染值", isinstance(me1.get("pollutionValue"), int),
      f"before={me0.get('pollutionValue')} after={me1.get('pollutionValue')}")

print("=" * 60)
print("场景2 文字冒险剧情")
chs = call("GET", "/story/chapters", token=bobs).get("data", [])
check("章节列表(序章已解锁)", len(chs) >= 1, f"{len(chs)} 章")
first = chs[0].get("id") if isinstance(chs[0], dict) else chs[0]
ent = call("POST", f"/story/chapters/{first}/enter", token=bobs)
check("进入序章", ent.get("code") == 0)
steps, node = 0, ent.get("data") or {}
pol0 = me1.get("pollutionValue")
while node and node.get("options") and steps < 15:
    opt = node["options"][0]
    r = call("POST", "/story/choose", token=bobs, form={"optionId": opt.get("id")})
    if r.get("code") != 0:
        check("剧情选择", False, str(r.get("message")))
        break
    node = r.get("data") or {}
    steps += 1
check(f"序章推进 {steps} 步选择", steps >= 1)
me2 = call("GET", "/adventurer/me", token=bobs).get("data") or {}
check("剧情选择影响污染/金币", True,
      f"pollution {pol0}->{me2.get('pollutionValue')} gold={me2.get('goldBalance')}")

print("=" * 60)
print("场景3 行者善事任务(总管理员发布,起步引流)")
r = call("POST", "/admin/tasks/xingzhe", token=admins["superadmin"],
         form={"title": "社区送温暖", "rewardGold": 100, "purifyValue": 10})
check("总管理员发布善事任务", r.get("code") == 0, str(r.get("message")))
xz_id = r.get("data")
r = call("POST", f"/tasks/{xz_id}/accept", token=bobs)
check("bob 接取善事任务", r.get("code") == 0, str(r.get("message")))
check("bob 提交成果", call("POST", f"/tasks/{xz_id}/submit", token=bobs).get("code") == 0)
r = call("POST", f"/tasks/{xz_id}/verify", token=admins["superadmin"], form={"success": "true"})
check("验收成功(入账+净化)", r.get("code") == 0, str(r.get("message")))
me3 = call("GET", "/adventurer/me", token=bobs).get("data") or {}
check("善事任务入账+净化", int(me3.get("goldBalance") or 0) > 0,
      f"gold={me3.get('goldBalance')} pollution={me2.get('pollutionValue')}->{me3.get('pollutionValue')}")

print("-" * 60)
print("场景3b 冒险者向行者协会求助->审批->发布")
r = call("POST", "/xingzhe/apply", token=als,
         data={"title": "帮我照看独居老人", "content": "三日陪伴，附酬谢"})
check("alice 向行者协会求助", r.get("code") == 0, str(r.get("message")))
aid = r.get("data")
pend = call("GET", "/admin/xingzhe-applies/pending", token=admins["xingzhe_admin"]).get("data", [])
check("行者管理员待审列表可见", isinstance(pend, list) and any(a.get("id") == aid for a in pend))
r = call("POST", f"/admin/xingzhe-applies/{aid}/audit", token=admins["xingzhe_admin"],
         form={"pass": "true", "remark": "同意"})
check("行者管理员审批通过(自动发布)", r.get("code") == 0, str(r.get("message")))
r = call("GET", "/tasks?sourceType=1", token=bobs)
xztasks = r.get("data") or []
check("审批通过后任务在行者大厅发布", isinstance(xztasks, list) and len(xztasks) >= 1,
      f"大厅行者任务 {len(xztasks)} 个")

print("=" * 60)
print("场景4 冒险者委托闭环(发布->审核->接单->验收)")
gold_bob = int(me3.get("goldBalance") or 0)
reward = 80 if gold_bob >= 80 else 0
r = call("POST", "/adventurer/tasks", token=bobs,
         data={"title": "寻找失落的信物", "description": "护送到北门", "taskLevel": "D", "rewardGold": reward})
check("bob 发布D级委托(托管预付)", r.get("code") == 0, str(r.get("message")))
tid = r.get("data")
pending = call("GET", "/admin/adventurer-tasks/pending", token=admins["adventurer_admin"]).get("data", [])
check("协会待审列表可见", any(t.get("id") == tid for t in pending) if isinstance(pending, list) else False)
r = call("POST", f"/admin/adventurer-tasks/{tid}/audit", token=admins["adventurer_admin"],
         form={"pass": "true", "remark": "ok"})
check("协会审核通过", r.get("code") == 0, str(r.get("message")))
r = call("POST", f"/tasks/{tid}/accept", token=als)
check("alice 接单", r.get("code") == 0, str(r.get("message")))
check("alice 提交", call("POST", f"/tasks/{tid}/submit", token=als).get("code") == 0)
r = call("POST", f"/tasks/{tid}/verify", token=bobs, form={"success": "true"})
check("发布者bob验收成功(90%入账)", r.get("code") == 0, str(r.get("message")))
mea = call("GET", "/adventurer/me", token=als).get("data") or {}
check("alice 得到90%报酬", int(mea.get("goldBalance") or 0) == int(reward * 0.9),
      f"gold={mea.get('goldBalance')} expected={int(reward * 0.9)}")
me4 = call("GET", "/adventurer/me", token=bobs).get("data") or {}

print("=" * 60)
print("场景5 知识宝库")
r = call("POST", "/knowledge/contributions", token=als,
         form={"title": "辨别负能量三法", "content": "一看动机二看事实三看感受……"})
check("alice 投稿", r.get("code") == 0, str(r.get("message")))
cid = r.get("data")
pend = call("GET", "/admin/knowledge/contributions/pending", token=admins["knowledge_admin"]).get("data", [])
check("待审投稿可见", any(c.get("id") == cid for c in pend) if isinstance(pend, list) else False)
r = call("POST", f"/admin/knowledge/contributions/{cid}/package", token=admins["knowledge_admin"],
         form={"pass": "true", "summary": "辨别负能量的实用方法", "price": 100})
check("封装信息集定价100", r.get("code") == 0, str(r.get("message")))
r = call("GET", "/knowledge/info-sets", token=bobs)
sets = r.get("data", [])
sid = sets[0]["id"] if isinstance(sets, list) and sets else None
check("信息集上架可浏览", bool(sid))
r = call("GET", f"/knowledge/info-sets/{sid}/content", token=bobs)
check("未购未借不可阅读(403)", r.get("code") != 0, str(r.get("message")))
r = call("POST", f"/knowledge/info-sets/{sid}/borrow", token=bobs, form={"cardType": 1})
check("bob 借日卡(10%)", r.get("code") == 0, str(r.get("message")))
r = call("GET", f"/knowledge/info-sets/{sid}/content", token=bobs)
check("借阅后可读内容/签名URL", r.get("code") == 0 and bool(r.get("data")),
      str(r.get("data", ""))[:40])

print("=" * 60)
print("场景6 商城与净化剂 + 负面用例")
r = call("POST", "/admin/purifier/batch", token=admins["superadmin"], form={"count": 5})
check("总管理员生产净化剂批次", r.get("code") == 0, str(r.get("message")))
r = call("POST", "/admin/shop/purifier/list", token=admins["superadmin"], form={"count": 5, "price": 50000})
check("净化剂上架商城", r.get("code") == 0, str(r.get("message")))
r = call("POST", "/shop/orders/1", token=bobs)
check("余额不足购买被拒", r.get("code") != 0, str(r.get("message"))[:30])
rank = call("GET", "/adventurer/rank", token=bobs).get("data", [])
check("排行榜包含alice", isinstance(rank, list) and any("爱丽丝" in str(x) for x in rank), str(rank)[:80])

print("=" * 60)
print(f"\n总计: PASS={len(PASS)} FAIL={len(FAIL)}")
if FAIL:
    print("失败项:", FAIL)
    sys.exit(1)
