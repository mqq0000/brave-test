# -*- coding: utf-8 -*-
"""Generate MyBatis-Plus entity & mapper classes for brave-test-backend."""
import os

BASE = r"C:\Users\31201\WorkBuddy\2026-09-12-17-46-34\brave-test-backend\src\main\java\com\bravetest"

# name -> (table, [(field, java_type, comment)])
ENTITIES = {
    "User": ("user", [
        ("id", "Long", "雪花ID"),
        ("username", "String", "登录名"),
        ("password", "String", "BCrypt密文"),
        ("nickname", "String", "昵称"),
        ("avatarUrl", "String", "头像(MinIO)"),
        ("role", "String", "ADVENTURER/XINGZHE_ADMIN/ADVENTURER_ADMIN/KNOWLEDGE_ADMIN/SUPER_ADMIN"),
        ("status", "Integer", "0正常 1禁用"),
        ("createdAt", "LocalDateTime", ""),
        ("updatedAt", "LocalDateTime", ""),
    ]),
    "Adventurer": ("adventurer", [
        ("id", "Long", ""),
        ("userId", "Long", "1:1关联user"),
        ("rankLevel", "String", "D/C/B/A/S"),
        ("completedCount", "Integer", "累计完成任务数"),
        ("pollutionValue", "Integer", "污染值0~100"),
        ("sproutStatus", "String", "NORMAL/GRAY/RED"),
        ("goldBalance", "Long", "金币余额"),
        ("bannedUntil", "LocalDateTime", "协会封禁截止，NULL=正常"),
        ("knowledgeBanned", "Integer", "知识宝库封禁 0否 1是"),
        ("createdAt", "LocalDateTime", ""),
        ("updatedAt", "LocalDateTime", ""),
    ]),
    "Task": ("task", [
        ("id", "Long", ""),
        ("sourceType", "Integer", "1行者协会 2冒险者协会"),
        ("publisherId", "Long", "发布者userId"),
        ("title", "String", ""),
        ("description", "String", ""),
        ("taskLevel", "String", "D/C/B/A/S"),
        ("rewardGold", "Long", "报酬(发布者托管金额)"),
        ("acceptorGold", "Long", "接取者所得(=90%)"),
        ("purifyValue", "Integer", "净化值"),
        ("status", "Integer", "0待审核 1发布中 2进行中 3待验收 4完成 5失败 6驳回 7下架"),
        ("acceptorId", "Long", "接取者userId"),
        ("acceptDeadline", "LocalDateTime", ""),
        ("finishDeadline", "LocalDateTime", ""),
        ("auditId", "Long", "审核人userId"),
        ("auditRemark", "String", ""),
        ("createdAt", "LocalDateTime", ""),
        ("updatedAt", "LocalDateTime", ""),
    ]),
    "TaskAcceptRecord": ("task_accept_record", [
        ("id", "Long", ""),
        ("taskId", "Long", ""),
        ("adventurerId", "Long", "冒险者档案id"),
        ("adventurerUserId", "Long", ""),
        ("isCrossLevel", "Integer", "是否越级接单"),
        ("status", "Integer", "0进行中 1成功 2失败"),
        ("goldPaid", "Long", "结算金币快照"),
        ("purifyGained", "Integer", "净化值快照"),
        ("startedAt", "LocalDateTime", ""),
        ("finishedAt", "LocalDateTime", ""),
    ]),
    "PurificationRecord": ("purification_record", [
        ("id", "Long", ""),
        ("adventurerId", "Long", ""),
        ("source", "String", "XINGZHE_TASK/PURIFIER/KNOWLEDGE"),
        ("valueBefore", "Integer", ""),
        ("valueAfter", "Integer", ""),
        ("refId", "Long", "关联来源id"),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "XingzheApply": ("xingzhe_apply", [
        ("id", "Long", ""),
        ("applicantId", "Long", "申请人userId"),
        ("title", "String", ""),
        ("content", "String", ""),
        ("auditStatus", "Integer", "0待审 1通过 2驳回"),
        ("auditorId", "Long", ""),
        ("auditRemark", "String", ""),
        ("publishedTaskId", "Long", "通过后生成的行者任务id"),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "ShopItem": ("shop_item", [
        ("id", "Long", ""),
        ("name", "String", ""),
        ("itemType", "Integer", "1普通 2净化剂"),
        ("price", "Long", ""),
        ("stock", "Integer", ""),
        ("sellerId", "Long", "上架管理员userId"),
        ("status", "Integer", "0下架 1在售"),
        ("imageUrl", "String", ""),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "PurificationBatch": ("purification_batch", [
        ("id", "Long", ""),
        ("batchNo", "String", "批次号"),
        ("produceMonth", "LocalDate", "生产月份(每月限100支)"),
        ("unitPrice", "Long", "定价50000"),
        ("producedBy", "Long", "总管理员userId"),
        ("remaining", "Integer", "本批剩余"),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "TradeOrder": ("trade_order", [
        ("id", "Long", ""),
        ("itemId", "Long", ""),
        ("buyerId", "Long", ""),
        ("sellerId", "Long", ""),
        ("price", "Long", ""),
        ("type", "Integer", "1普通购买 2净化剂"),
        ("status", "Integer", "0待支付 1已完成 2已取消"),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "KnowledgeContribution": ("knowledge_contribution", [
        ("id", "Long", ""),
        ("contributorId", "Long", ""),
        ("title", "String", ""),
        ("content", "String", ""),
        ("privacyDeclared", "Integer", "隐私声明 0无 1已声明不侵犯隐私"),
        ("auditStatus", "Integer", "0待审 1通过 2驳回"),
        ("auditorId", "Long", ""),
        ("auditRemark", "String", ""),
        ("infoSetId", "Long", "封装后回填"),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "InfoSet": ("info_set", [
        ("id", "Long", ""),
        ("contributionId", "Long", ""),
        ("contributorId", "Long", ""),
        ("title", "String", ""),
        ("summary", "String", ""),
        ("contentUrl", "String", "MinIO对象key"),
        ("price", "Long", "估价定价"),
        ("status", "Integer", "0下架 1在售"),
        ("soldCount", "Integer", ""),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "BorrowRecord": ("borrow_record", [
        ("id", "Long", ""),
        ("infoSetId", "Long", ""),
        ("userId", "Long", ""),
        ("cardType", "Integer", "1日卡 2周卡 3月卡"),
        ("price", "Long", ""),
        ("startAt", "LocalDateTime", ""),
        ("expireAt", "LocalDateTime", ""),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "PurchaseRecord": ("purchase_record", [
        ("id", "Long", ""),
        ("infoSetId", "Long", ""),
        ("buyerId", "Long", ""),
        ("price", "Long", ""),
        ("purchasedAt", "LocalDateTime", ""),
    ]),
    "ViolationRecord": ("violation_record", [
        ("id", "Long", ""),
        ("userId", "Long", ""),
        ("type", "Integer", "1二次售卖/传播牟利"),
        ("evidenceUrl", "String", ""),
        ("penalty", "Integer", "1踢出知识宝库"),
        ("handlerId", "Long", ""),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "GoldFlow": ("gold_flow", [
        ("id", "Long", ""),
        ("userId", "Long", ""),
        ("bizType", "String", "TASK_REWARD/COMMISSION/ESCROW/LEVEL_UP_REWARD/SHOP_BUY/PURIFIER/REFUND/TRANSFER_UP/GRANT"),
        ("amount", "Long", "正收入 负支出"),
        ("balanceAfter", "Long", ""),
        ("refType", "String", ""),
        ("refId", "Long", ""),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "AssociationLedger": ("association_ledger", [
        ("id", "Long", ""),
        ("association", "Integer", "1行者 2冒险者 3知识宝库 4总管理员"),
        ("month", "String", "yyyy-MM"),
        ("income", "Long", ""),
        ("expense", "Long", ""),
        ("balance", "Long", ""),
        ("updatedAt", "LocalDateTime", ""),
    ]),
    "SettlementLog": ("settlement_log", [
        ("id", "Long", ""),
        ("month", "String", "结算月份yyyy-MM"),
        ("type", "Integer", "1佣金上缴60% 2知识宝库拨款40% 3净化剂生产"),
        ("amount", "Long", ""),
        ("fromAccount", "Integer", ""),
        ("toAccount", "Integer", ""),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "StoryChapter": ("story_chapter", [
        ("id", "Long", ""),
        ("seq", "Integer", "章节序号(解锁链)"),
        ("title", "String", "章节标题"),
        ("createdAt", "LocalDateTime", ""),
    ]),
    "StoryNode": ("story_node", [
        ("id", "Long", ""),
        ("chapterId", "Long", ""),
        ("nodeType", "String", "NARRATE叙述/CHOICE抉择/END终章"),
        ("content", "String", "剧情文本"),
        ("seq", "Integer", "章内顺序"),
    ]),
    "StoryOption": ("story_option", [
        ("id", "Long", ""),
        ("nodeId", "Long", "所属节点"),
        ("text", "String", "选项文本"),
        ("pollutionDelta", "Integer", "污染变化 正=染污 负=净化"),
        ("goldDelta", "Long", "金币变化"),
        ("nextNodeId", "Long", "下一节点 NULL=本章完结"),
        ("seq", "Integer", "展示顺序"),
    ]),
    "PlayerStoryProgress": ("player_story_progress", [
        ("id", "Long", ""),
        ("adventurerId", "Long", ""),
        ("userId", "Long", ""),
        ("chapterId", "Long", ""),
        ("currentNodeId", "Long", ""),
        ("status", "Integer", "0进行中 1已完成"),
        ("choicesLog", "String", "选择记录JSON"),
        ("createdAt", "LocalDateTime", ""),
        ("updatedAt", "LocalDateTime", ""),
    ]),
    "TestQuestion": ("test_question", [
        ("id", "Long", ""),
        ("seq", "Integer", "题号"),
        ("content", "String", "题干"),
        ("options", "String", "选项JSON [{text,courage,rationality,kindness,pollution}]"),
        ("status", "Integer", "0下线 1启用"),
    ]),
    "TestResult": ("test_result", [
        ("id", "Long", ""),
        ("userId", "Long", ""),
        ("adventurerId", "Long", ""),
        ("answers", "String", "作答JSON"),
        ("traitCourage", "Integer", "勇气分"),
        ("traitRationality", "Integer", "理性分"),
        ("traitKindness", "Integer", "仁善分"),
        ("pollutionDelta", "Integer", "负面选择累计的初始污染"),
        ("title", "String", "觉醒称号"),
        ("createdAt", "LocalDateTime", ""),
    ]),
}

IMPORTS = {
    "String": [],
    "Integer": [],
    "Long": [],
    "LocalDateTime": ["java.time.LocalDateTime"],
    "LocalDate": ["java.time.LocalDate"],
}

ENTITY_TEMPLATE = """package com.bravetest.entity;

{imports}

/**
 * {comment}
 */
@Data
@TableName("{table}")
public class {name} {{

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
{fields}
}}
"""

MAPPER_TEMPLATE = """package com.bravetest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bravetest.entity.{name};
import org.apache.ibatis.annotations.Mapper;

/** {comment} Mapper */
@Mapper
public interface {name}Mapper extends BaseMapper<{name}> {{
}}
"""

for name, (table, fields) in ENTITIES.items():
    imports = set()
    for _, t, _ in fields:
        imports.update(IMPORTS.get(t, []))
    imports.add("lombok.Data")
    imports.add("com.baomidou.mybatisplus.annotation.IdType")
    imports.add("com.baomidou.mybatisplus.annotation.TableId")
    imports.add("com.baomidou.mybatisplus.annotation.TableName")
    import_lines = "\n".join("import " + i + ";" for i in sorted(imports))

    field_lines = []
    for fname, ftype, fcomment in fields:
        if fname == "id":
            continue
        if fcomment:
            field_lines.append("    /** %s */" % fcomment)
        field_lines.append("    private %s %s;" % (ftype, fname))
        field_lines.append("")

    entity = ENTITY_TEMPLATE.format(
        imports=import_lines, comment=table, table=table, name=name,
        fields="\n".join(field_lines).rstrip())

    with open(os.path.join(BASE, "entity", name + ".java"), "w", encoding="utf-8") as f:
        f.write(entity)

    mapper = MAPPER_TEMPLATE.format(name=name, comment=table)
    with open(os.path.join(BASE, "mapper", name + "Mapper.java"), "w", encoding="utf-8") as f:
        f.write(mapper)

print("Generated %d entities and %d mappers" % (len(ENTITIES), len(ENTITIES)))
