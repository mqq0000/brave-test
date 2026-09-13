-- ============================================================
-- 勇者测试 数据库初始化脚本 (MySQL 8)
-- 数据库: brave_test
-- 字符集: utf8mb4
-- ============================================================
CREATE DATABASE IF NOT EXISTS brave_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE brave_test;
-- 关键：声明本会话字符集，防止 mysql 客户端默认 latin1 导致中文种子数据乱码
SET NAMES utf8mb4;

-- 1. 用户账号表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL COMMENT '雪花ID',
    `username`    VARCHAR(32)  NOT NULL COMMENT '登录名',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt密文',
    `nickname`    VARCHAR(32)  NOT NULL COMMENT '昵称',
    `avatar_url`  VARCHAR(255) NULL COMMENT '头像(MinIO)',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'ADVENTURER' COMMENT 'ADVENTURER/XINGZHE_ADMIN/ADVENTURER_ADMIN/KNOWLEDGE_ADMIN/SUPER_ADMIN',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1禁用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`)
) ENGINE = InnoDB COMMENT ='用户账号表';

-- 2. 冒险者档案表
CREATE TABLE IF NOT EXISTS `adventurer` (
    `id`               BIGINT   NOT NULL,
    `user_id`          BIGINT   NOT NULL COMMENT '1:1关联user',
    `rank_level`       CHAR(1)  NOT NULL DEFAULT 'D' COMMENT 'D/C/B/A/S',
    `completed_count`  INT      NOT NULL DEFAULT 0 COMMENT '累计完成任务数',
    `pollution_value`  TINYINT  NOT NULL DEFAULT 0 COMMENT '污染值0~100',
    `sprout_status`    VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/GRAY/RED',
    `gold_balance`     BIGINT   NOT NULL DEFAULT 0 COMMENT '金币余额',
    `banned_until`     DATETIME NULL COMMENT '协会封禁截止 NULL=正常',
    `knowledge_banned` TINYINT  NOT NULL DEFAULT 0 COMMENT '知识宝库封禁 0否 1是',
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_rank` (`rank_level`)
) ENGINE = InnoDB COMMENT ='冒险者档案表';

-- 3. 任务表(行者/冒险者共用)
CREATE TABLE IF NOT EXISTS `task` (
    `id`              BIGINT   NOT NULL,
    `source_type`     TINYINT  NOT NULL COMMENT '1行者协会 2冒险者协会',
    `publisher_id`    BIGINT   NOT NULL COMMENT '发布者userId',
    `title`           VARCHAR(100)  NOT NULL,
    `description`     TEXT     NULL,
    `task_level`      CHAR(1)  NOT NULL COMMENT 'D/C/B/A/S',
    `reward_gold`     BIGINT   NOT NULL COMMENT '报酬(发布者托管金额)',
    `acceptor_gold`   BIGINT   NOT NULL COMMENT '接取者所得(=90%)',
    `purify_value`    TINYINT  NOT NULL DEFAULT 0 COMMENT '净化值(善事任务>0)',
    `status`          TINYINT  NOT NULL DEFAULT 0 COMMENT '0待审核 1发布中 2进行中 3待验收 4完成 5失败 6驳回 7下架',
    `acceptor_id`     BIGINT   NULL COMMENT '接取者userId',
    `accept_deadline`  DATETIME NULL,
    `finish_deadline` DATETIME NULL,
    `audit_id`        BIGINT   NULL COMMENT '审核人userId',
    `audit_remark`    VARCHAR(255) NULL,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_source_level` (`status`, `source_type`, `task_level`),
    KEY `idx_publisher` (`publisher_id`),
    KEY `idx_acceptor` (`acceptor_id`)
) ENGINE = InnoDB COMMENT ='任务表';

-- 4. 任务接取记录表(升降级判定依据)
CREATE TABLE IF NOT EXISTS `task_accept_record` (
    `id`               BIGINT   NOT NULL,
    `task_id`          BIGINT   NOT NULL,
    `adventurer_id`    BIGINT   NOT NULL COMMENT '冒险者档案id',
    `adventurer_user_id` BIGINT NOT NULL,
    `is_cross_level`   TINYINT  NOT NULL DEFAULT 0 COMMENT '是否越级接单',
    `status`           TINYINT  NOT NULL DEFAULT 0 COMMENT '0进行中 1成功 2失败',
    `gold_paid`        BIGINT   NULL COMMENT '结算金币快照',
    `purify_gained`    INT      NULL COMMENT '净化值快照',
    `started_at`       DATETIME NOT NULL,
    `finished_at`      DATETIME NULL,
    PRIMARY KEY (`id`),
    KEY `idx_adventurer_status` (`adventurer_id`, `status`),
    KEY `idx_task` (`task_id`)
) ENGINE = InnoDB COMMENT ='任务接取记录表';

-- 5. 净化记录表
CREATE TABLE IF NOT EXISTS `purification_record` (
    `id`           BIGINT   NOT NULL,
    `adventurer_id` BIGINT  NOT NULL,
    `source`       VARCHAR(20) NOT NULL COMMENT 'XINGZHE_TASK/PURIFIER/KNOWLEDGE',
    `value_before` INT      NOT NULL,
    `value_after`  INT      NOT NULL,
    `ref_id`       BIGINT   NULL COMMENT '关联来源id',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_adventurer` (`adventurer_id`, `created_at`)
) ENGINE = InnoDB COMMENT ='净化记录表';

-- 6. 行者协会任务帮助申请表
CREATE TABLE IF NOT EXISTS `xingzhe_apply` (
    `id`                BIGINT   NOT NULL,
    `applicant_id`      BIGINT   NOT NULL COMMENT '申请人userId',
    `title`             VARCHAR(100) NOT NULL,
    `content`           TEXT     NULL,
    `audit_status`      TINYINT  NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
    `auditor_id`        BIGINT   NULL,
    `audit_remark`      VARCHAR(255) NULL,
    `published_task_id` BIGINT   NULL COMMENT '通过后生成的行者任务id',
    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_applicant` (`applicant_id`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE = InnoDB COMMENT ='行者协会任务帮助申请表';

-- 7. 商城商品表
CREATE TABLE IF NOT EXISTS `shop_item` (
    `id`         BIGINT   NOT NULL,
    `name`       VARCHAR(64) NOT NULL,
    `item_type`  TINYINT  NOT NULL DEFAULT 1 COMMENT '1普通 2净化剂',
    `price`      BIGINT   NOT NULL,
    `stock`      INT      NOT NULL DEFAULT 0,
    `seller_id`  BIGINT   NOT NULL COMMENT '上架管理员userId',
    `status`     TINYINT  NOT NULL DEFAULT 1 COMMENT '0下架 1在售',
    `image_url`  VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`item_type`, `status`)
) ENGINE = InnoDB COMMENT ='商城商品表';

-- 8. 净化剂批次表(总管理员每月限产100支)
CREATE TABLE IF NOT EXISTS `purification_batch` (
    `id`            BIGINT   NOT NULL,
    `batch_no`      VARCHAR(32) NOT NULL,
    `produce_month` DATE     NOT NULL COMMENT '生产月份',
    `unit_price`    BIGINT   NOT NULL DEFAULT 50000,
    `produced_by`   BIGINT   NOT NULL COMMENT '总管理员userId',
    `remaining`     INT      NOT NULL COMMENT '本批剩余',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_no` (`batch_no`),
    KEY `idx_month` (`produce_month`)
) ENGINE = InnoDB COMMENT ='净化剂批次表';

-- 9. 交易订单表
CREATE TABLE IF NOT EXISTS `trade_order` (
    `id`         BIGINT   NOT NULL,
    `item_id`    BIGINT   NOT NULL,
    `buyer_id`   BIGINT   NOT NULL,
    `seller_id`  BIGINT   NOT NULL,
    `price`      BIGINT   NOT NULL,
    `type`       TINYINT  NOT NULL DEFAULT 1 COMMENT '1普通购买 2净化剂',
    `status`     TINYINT  NOT NULL DEFAULT 0 COMMENT '0待支付 1已完成 2已取消',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_buyer` (`buyer_id`, `created_at`)
) ENGINE = InnoDB COMMENT ='交易订单表';

-- 10. 知识投稿表
CREATE TABLE IF NOT EXISTS `knowledge_contribution` (
    `id`               BIGINT   NOT NULL,
    `contributor_id`   BIGINT   NOT NULL COMMENT '投稿人userId',
    `title`            VARCHAR(100) NOT NULL,
    `content`          TEXT     NOT NULL,
    `privacy_declared` TINYINT  NOT NULL DEFAULT 0 COMMENT '0无 1已声明不侵犯隐私',
    `audit_status`     TINYINT  NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
    `auditor_id`       BIGINT   NULL,
    `audit_remark`     VARCHAR(255) NULL,
    `info_set_id`      BIGINT   NULL COMMENT '封装后回填',
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_contributor` (`contributor_id`),
    KEY `idx_audit` (`audit_status`)
) ENGINE = InnoDB COMMENT ='知识投稿表';

-- 11. 信息集表
CREATE TABLE IF NOT EXISTS `info_set` (
    `id`             BIGINT   NOT NULL,
    `contribution_id` BIGINT  NOT NULL,
    `contributor_id` BIGINT   NOT NULL,
    `title`          VARCHAR(100) NOT NULL,
    `summary`        VARCHAR(500) NULL,
    `content_url`    VARCHAR(255) NULL COMMENT 'MinIO对象key(借阅用临时签名URL)',
    `price`          BIGINT   NOT NULL COMMENT '估价定价',
    `status`         TINYINT  NOT NULL DEFAULT 1 COMMENT '0下架 1在售',
    `sold_count`     INT      NOT NULL DEFAULT 0,
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_contributor` (`contributor_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='信息集表';

-- 12. 借阅记录表(1日/周/月借阅卡)
CREATE TABLE IF NOT EXISTS `borrow_record` (
    `id`         BIGINT   NOT NULL,
    `info_set_id` BIGINT  NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `card_type`  TINYINT  NOT NULL COMMENT '1日卡 2周卡 3月卡',
    `price`      BIGINT   NOT NULL,
    `start_at`   DATETIME NOT NULL,
    `expire_at`  DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_infoset` (`user_id`, `info_set_id`, `expire_at`)
) ENGINE = InnoDB COMMENT ='借阅记录表';

-- 13. 购买记录表
CREATE TABLE IF NOT EXISTS `purchase_record` (
    `id`           BIGINT   NOT NULL,
    `info_set_id`  BIGINT   NOT NULL,
    `buyer_id`     BIGINT   NOT NULL,
    `price`        BIGINT   NOT NULL,
    `purchased_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_buyer_infoset` (`buyer_id`, `info_set_id`),
    KEY `idx_infoset` (`info_set_id`)
) ENGINE = InnoDB COMMENT ='购买记录表';

-- 14. 违规记录表(二次售卖→踢出知识宝库)
CREATE TABLE IF NOT EXISTS `violation_record` (
    `id`           BIGINT   NOT NULL,
    `user_id`      BIGINT   NOT NULL,
    `type`         TINYINT  NOT NULL DEFAULT 1 COMMENT '1二次售卖/传播牟利',
    `evidence_url` VARCHAR(255) NULL,
    `penalty`      TINYINT  NOT NULL DEFAULT 1 COMMENT '1踢出知识宝库',
    `handler_id`   BIGINT   NOT NULL COMMENT '处置管理员userId',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='违规记录表';

CREATE TABLE IF NOT EXISTS `violation_report` (
    `id`            BIGINT       NOT NULL,
    `reporter_id`   BIGINT       NOT NULL COMMENT '举报人userId',
    `offender_id`   BIGINT       NOT NULL COMMENT '被举报人userId',
    `info_set_id`   BIGINT       NULL COMMENT '关联信息集',
    `description`   VARCHAR(500) NOT NULL COMMENT '举报说明',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已处罚 2已驳回',
    `handler_id`    BIGINT       NULL COMMENT '处理管理员userId',
    `handle_remark` VARCHAR(255) NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='知识宝库举报表';

CREATE TABLE IF NOT EXISTS `knowledge_favorite` (
    `id`          BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `info_set_id` BIGINT   NOT NULL,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_set` (`user_id`, `info_set_id`)
) ENGINE = InnoDB COMMENT ='知识宝库收藏表';

-- ============================================================
-- 修身·自我激励系统（自我任务 / 技能树 / 自我奖励商城）
-- ============================================================
CREATE TABLE IF NOT EXISTS `self_task` (
    `id`                BIGINT       NOT NULL,
    `user_id`           BIGINT       NOT NULL,
    `title`             VARCHAR(100) NOT NULL,
    `description`       VARCHAR(300) NULL,
    `coin_reward`       INT          NOT NULL DEFAULT 10,
    `skill_id`          BIGINT       NULL COMMENT '关联技能，完成涨经验',
    `repeat_type`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0一次性 1每日',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '1进行中 2已完成 3已放弃',
    `last_completed_at` DATETIME     NULL,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`, `status`)
) ENGINE = InnoDB COMMENT ='自我任务表（修身·自我激励）';

CREATE TABLE IF NOT EXISTS `skill` (
    `id`         BIGINT      NOT NULL,
    `user_id`    BIGINT      NOT NULL,
    `parent_id`  BIGINT      NULL COMMENT '父技能，NULL为根',
    `name`       VARCHAR(50) NOT NULL,
    `exp`        INT         NOT NULL DEFAULT 0 COMMENT '经验，每100升1级',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='技能树';

CREATE TABLE IF NOT EXISTS `self_shop_item` (
    `id`          BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL,
    `name`        VARCHAR(100) NOT NULL,
    `description` VARCHAR(300) NULL,
    `cost`        INT          NOT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 2下架',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`, `status`)
) ENGINE = InnoDB COMMENT ='自我奖励商城商品';

CREATE TABLE IF NOT EXISTS `self_redemption` (
    `id`          BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL,
    `item_id`     BIGINT       NOT NULL,
    `item_name`   VARCHAR(100) NOT NULL,
    `cost`        INT          NOT NULL,
    `assoc_fee`   INT          NOT NULL DEFAULT 0 COMMENT '入冒险者协会的会费(10%)',
    `redeemed_at` DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`, `redeemed_at`)
) ENGINE = InnoDB COMMENT ='自我商城兑换记录';

-- 15. 金币流水表
CREATE TABLE IF NOT EXISTS `gold_flow` (
    `id`            BIGINT   NOT NULL,
    `user_id`       BIGINT   NOT NULL,
    `biz_type`      VARCHAR(24) NOT NULL COMMENT 'TASK_REWARD/COMMISSION/ESCROW/LEVEL_UP_REWARD/SHOP_BUY/PURIFIER/REFUND/TRANSFER_UP/GRANT',
    `amount`        BIGINT   NOT NULL COMMENT '正收入 负支出',
    `balance_after` BIGINT   NOT NULL,
    `ref_type`      VARCHAR(24) NULL,
    `ref_id`        BIGINT   NULL,
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT ='金币流水表';

-- 16. 协会账本表
CREATE TABLE IF NOT EXISTS `association_ledger` (
    `id`          BIGINT   NOT NULL,
    `association` TINYINT  NOT NULL COMMENT '1行者 2冒险者 3知识宝库 4总管理员',
    `month`       CHAR(7)  NOT NULL COMMENT 'yyyy-MM',
    `income`      BIGINT   NOT NULL DEFAULT 0,
    `expense`     BIGINT   NOT NULL DEFAULT 0,
    `balance`     BIGINT   NOT NULL DEFAULT 0,
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_assoc_month` (`association`, `month`)
) ENGINE = InnoDB COMMENT ='协会账本表';

-- 17. 月度结算流水表
CREATE TABLE IF NOT EXISTS `settlement_log` (
    `id`          BIGINT   NOT NULL,
    `month`       CHAR(7)  NOT NULL COMMENT '结算月份yyyy-MM',
    `type`        TINYINT  NOT NULL COMMENT '1佣金上缴60% 2知识宝库拨款40% 3净化剂生产',
    `amount`      BIGINT   NOT NULL,
    `from_account` TINYINT NOT NULL,
    `to_account`  TINYINT  NOT NULL,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_month_type` (`month`, `type`)
) ENGINE = InnoDB COMMENT ='月度结算流水表';

-- ============================================================
-- 剧情引擎（文字冒险）与觉醒性格测试
-- ============================================================

-- 18. 剧情章节表
CREATE TABLE IF NOT EXISTS `story_chapter` (
    `id`         BIGINT   NOT NULL,
    `seq`        INT      NOT NULL COMMENT '章节序号(解锁链)',
    `title`      VARCHAR(100) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seq` (`seq`)
) ENGINE = InnoDB COMMENT ='剧情章节表';

-- 19. 剧情节点表
CREATE TABLE IF NOT EXISTS `story_node` (
    `id`         BIGINT   NOT NULL,
    `chapter_id` BIGINT   NOT NULL,
    `node_type`  VARCHAR(10) NOT NULL COMMENT 'NARRATE/CHOICE/END',
    `content`    TEXT     NOT NULL,
    `seq`        INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_chapter` (`chapter_id`, `seq`)
) ENGINE = InnoDB COMMENT ='剧情节点表';

-- 20. 剧情选项表
CREATE TABLE IF NOT EXISTS `story_option` (
    `id`             BIGINT  NOT NULL,
    `node_id`        BIGINT  NOT NULL,
    `text`           VARCHAR(200) NOT NULL,
    `pollution_delta` INT    NOT NULL DEFAULT 0 COMMENT '正=染污 负=净化',
    `gold_delta`     BIGINT  NOT NULL DEFAULT 0,
    `next_node_id`   BIGINT  NULL COMMENT 'NULL=本章完结',
    `seq`            INT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_node` (`node_id`)
) ENGINE = InnoDB COMMENT ='剧情选项表';

-- 21. 玩家剧情进度表
CREATE TABLE IF NOT EXISTS `player_story_progress` (
    `id`              BIGINT   NOT NULL,
    `adventurer_id`   BIGINT   NOT NULL,
    `user_id`         BIGINT   NOT NULL,
    `chapter_id`      BIGINT   NOT NULL,
    `current_node_id` BIGINT   NOT NULL,
    `status`          TINYINT  NOT NULL DEFAULT 0 COMMENT '0进行中 1已完成',
    `choices_log`     TEXT     NULL,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_chapter` (`user_id`, `chapter_id`)
) ENGINE = InnoDB COMMENT ='玩家剧情进度表';

-- 22. 觉醒测试题目表
CREATE TABLE IF NOT EXISTS `test_question` (
    `id`      BIGINT  NOT NULL,
    `seq`     INT     NOT NULL,
    `content` VARCHAR(300) NOT NULL,
    `options` TEXT    NOT NULL COMMENT '[{text,courage,rationality,kindness,pollution}]',
    `status`  TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='觉醒测试题目表';

-- 23. 觉醒测试结果表
CREATE TABLE IF NOT EXISTS `test_result` (
    `id`               BIGINT   NOT NULL,
    `user_id`          BIGINT   NOT NULL,
    `adventurer_id`    BIGINT   NOT NULL,
    `answers`          TEXT     NULL,
    `trait_courage`    INT      NOT NULL DEFAULT 0,
    `trait_rationality` INT     NOT NULL DEFAULT 0,
    `trait_kindness`   INT      NOT NULL DEFAULT 0,
    `pollution_delta`  INT      NOT NULL DEFAULT 0,
    `title`            VARCHAR(50) NOT NULL,
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`, `created_at`)
) ENGINE = InnoDB COMMENT ='觉醒测试结果表';

-- ============================================================
-- 种子数据：第一章《灰色的城》
-- ============================================================
INSERT INTO `story_chapter` (`id`, `seq`, `title`) VALUES (1001, 1, '第一章 · 灰色的城');

INSERT INTO `story_node` (`id`, `chapter_id`, `node_type`, `content`, `seq`) VALUES
(2001, 1001, 'NARRATE', '污染降临后的第三十七年。你走出住所，天是灰的，人的脸也是灰的。\n街角的公告栏上贴着行者协会的新告示：「善事即净化」。你摸了摸胸口——那里，你的勇者萌芽还在微微发光。\n可你分明感觉到，有什么沉重的东西，正顺着呼吸渗进来。', 1),
(2002, 1001, 'CHOICE', '巷口，一个浑身污渍的老人蜷缩着。他抬起头，眼睛是浊黄的，声音嘶哑：「水……给口干净水……」\n你注意到他的萌芽早已灰得发黑，但你身上只有半壶水，还得走三里路去协会。', 2),
(2003, 1001, 'NARRATE', '老人双手捧着水，浑浊的眼睛里映出你胸口那一点微光。他忽然笑了：「萌芽还亮着的人……替我看看晴天吧。」\n那一刻你胸口的沉重似乎松了一分。原来善意的流动，就是净化的开始。', 3),
(2004, 1001, 'NARRATE', '你从老人身边绕了过去。走出十几步，你听见身后传来一声几乎听不见的「谢谢」——他并没有怪你。\n可你胸口越来越沉。灰色的天，灰色的脸，你忽然分不清，那份灰是不是也开始渗进你心里。', 4),
(2005, 1001, 'CHOICE', '冒险者协会门口，两个冒险者正在争吵，其中一人的萌芽泛着刺目的红光，拳头已经攥紧。\n红光像火苗一样舔向围观人群的萌芽，有几个已经开始发灰。空气里的压迫感越来越重。', 5),
(2006, 1001, 'NARRATE', '你站了出来。你的声音不大，但胸口那点微光在发烫。\n红光的冒险者怔住了，拳头慢慢松开。围观的人悄悄舒了口气——他们胸腔里刚刚发芽的灰，也停了下来。\n「原来守住自己的人，真的能守住别人。」', 6),
(2007, 1001, 'NARRATE', '你低下头快步离开。可那团红光和渐渐蔓延的灰，一直在你眼前晃。\n你告诉自己这与你无关。可胸口那点微光，好像暗了一分。\n世界在往哪里走，你就往哪里走——这或许就是「污染」最安静的形状。', 7),
(2008, 1001, 'END', '夜幕降临，你在行者协会的灯下停住脚步。\n告示写着：「每日一善，萌芽不灭。」\n你推开协会的门——属于你的冒险，正式开始了。', 8);

INSERT INTO `story_option` (`id`, `node_id`, `text`, `pollution_delta`, `gold_delta`, `next_node_id`, `seq`) VALUES
(3001, 2001, '整理行装，前往冒险者协会', 0, 0, 2002, 1),
(3002, 2002, '把半壶水都给他', -10, 0, 2003, 1),
(3003, 2002, '冷冷地绕开，赶自己的路', 10, 0, 2004, 2),
(3004, 2002, '只分给他几口，然后记录他的污染特征', -2, 50, 2003, 3),
(3005, 2003, '向他许下承诺：一定替他看到晴天', -5, 0, 2005, 1),
(3006, 2004, '强行不去回想', 5, 0, 2005, 1),
(3007, 2004, '回到巷口留下一张字条和一块干粮', -5, 0, 2005, 2),
(3008, 2005, '挺身而出，用萌芽的光劝住他', -5, 0, 2006, 1),
(3009, 2005, '低头快步离开，事不关己', 10, 0, 2007, 2),
(3010, 2005, '吹响协会的警示哨，叫管理员来', 0, 0, 2006, 3),
(3011, 2006, '与他同行，护送他回协会', -5, 100, 2008, 1),
(3012, 2007, '在夜色中质问自己的懦弱', 0, 0, 2008, 1),
(3013, 2008, '推开门，开始冒险', 0, 0, NULL, 1);

-- ============================================================
-- 种子数据：第二章《红与灰》（完成第一章后解锁）
-- ============================================================
INSERT INTO `story_chapter` (`id`, `seq`, `title`) VALUES (1002, 2, '第二章 · 红与灰');

INSERT INTO `story_node` (`id`, `chapter_id`, `node_type`, `content`, `seq`) VALUES
(2101, 1002, 'NARRATE', '你在协会的日子里渐渐有了名气。人们叫你「萌芽还亮着的人」。\n可名气是把双刃剑——暗处有目光盯着你，那是被污染到深处的人，他们的萌芽红得像炭、灰得像烬。\n这一天，协会的新人训练场里，一个叫小栗的少年在众人哄笑声中低着头。他的萌芽，已经灰了一半。', 1),
(2102, 1002, 'CHOICE', '有人撞了小栗一下，他携带的补给散了一地，管理员的目光恰好扫过来。\n哄笑声更大了。你看见小栗的萌芽，正在以一种你熟悉的方式，一点点变暗。', 2),
(2103, 1002, 'NARRATE', '你走过去，蹲下来帮他一件件捡起补给。\n「我以前也这样被笑过。」你轻声说。小栗猛地抬头——那一刻，他萌芽上灰色褪了一线。\n管理员看在眼里，没有追究。散场的铃声响起来的时候，小栗小声问你：「明天……还能一起训练吗？」', 3),
(2104, 1002, 'NARRATE', '你没有停下脚步。\n可那一夜你睡得很坏。梦里全是散落一地的补给和那半边发灰的萌芽。\n第二天路过训练场，小栗没有来。听说他病了——也听说，灰化的萌芽会先让人病倒。', 4),
(2105, 1002, 'CHOICE', '周末，污染警报响起。城东的净化屏障出现裂口，红色污染雾正往外渗。\n行者协会召集人手。你刚结束三天的任务，污染值比平时高，胸口隐隐发沉。', 5),
(2106, 1002, 'NARRATE', '你出现在了屏障裂口前。不知是谁先看见你的萌芽在发光，人群里响起低低的欢呼。\n那一夜，你和许多人肩并肩把光举过头顶。红色污染雾退去的时候，天边竟透出一线极淡的青色。\n「看，晴天。」不知谁说了一句。你想起很久以前，你答应过一个老人，要替他看看晴天。', 6),
(2107, 1002, 'END', '协会的嘉奖令贴了出来。但你只是静静看着训练场的方向——\n明天，去问问小栗的病好些了吗。\n守护世界，先从看见具体的人开始。', 7);

INSERT INTO `story_option` (`id`, `node_id`, `text`, `pollution_delta`, `gold_delta`, `next_node_id`, `seq`) VALUES
(3101, 2101, '注意到角落里的小栗', 0, 0, 2102, 1),
(3102, 2102, '当众替他作证，是别人撞的', -5, 0, 2103, 1),
(3103, 2102, '假装没看见，毕竟你还背着任务', 8, 0, 2104, 2),
(3104, 2102, '默默走过去帮他捡补给', -8, 0, 2103, 3),
(3105, 2103, '答应他，并约定每天一起训练', -5, 0, 2105, 1),
(3106, 2104, '托人给小栗带一封信，承认自己的逃避', -5, 0, 2105, 1),
(3107, 2104, '告诉自己：我连自己都顾不上', 8, 0, 2105, 2),
(3108, 2105, '隐瞒疲惫，第一时间报名支援', -3, 0, 2106, 1),
(3109, 2105, '如实上报状态，请求担任后勤支援', 0, 200, 2106, 2),
(3110, 2105, '趁乱躲回住所，反正不缺我一个', 15, 0, 2107, 3),
(3111, 2106, '在庆功宴上把荣誉分给并肩的每个人', -5, 100, 2107, 1),
(3112, 2107, '去探望小栗', -5, 0, NULL, 1);

-- ============================================================
-- 种子数据：第三章《长夜将尽》（完成第二章后解锁）
-- ============================================================
INSERT INTO `story_chapter` (`id`, `seq`, `title`) VALUES (1003, 3, '第三章 · 长夜将尽');

INSERT INTO `story_node` (`id`, `chapter_id`, `node_type`, `content`, `seq`) VALUES
(2201, 1003, 'NARRATE', '入冬后，污染进入高发期。城的夜晚比以往更长，行人的萌芽十有八九覆着灰。\n小栗的伤好了，如今跟在你身后跑任务，胸口的萌芽一天比一天亮。\n这天夜里，行者协会的灯下贴出一张字迹颤抖的求助：城西独居的烬叔——曾经的红光冒险者——污染已入深境，拒绝一切帮助。', 1),
(2202, 1003, 'CHOICE', '你记得烬叔。当年他是最耀眼的 A 级冒险者，你的萌芽第一次发光，就是在他故事里听的。\n去，还是不去？深境污染的红雾会反噬靠近者的萌芽，协会明令禁止独行接触。', 2),
(2203, 1003, 'NARRATE', '你和行者协会的管理员一同敲开了烬叔的门。\n老人背对着你们，声音沙哑：「我都这样了，救我做什么……让这世界看看红透的人是什么下场也好。」\n你没有争辩。你只是坐在门边，把你这些年里见过的晴天，一件一件讲给他听。', 3),
(2204, 1003, 'NARRATE', '你把求助单还给了协会，附了一句：先尊重他的意愿。\n可那晚你久久没有睡。你想起老人的萌芽也曾照亮半座城——如今却没有人肯坐下来听他说话。\n第二天，你匿名捐了一支净化剂给城西救助站，注明「留给愿意伸手的人」。', 5),
(2205, 1003, 'CHOICE', '第七天夜里，烬叔的萌芽红光大盛，污染雾开始从他的小屋往外渗。救助站的人手全部顶在了屏障上。\n协会征召敢死小队进入屋内，把净化剂送到老人手中。这一次，是九死一生的任务。', 6),
(2206, 1003, 'NARRATE', '你冲进了红雾。净化剂推进老人胸口的那一刻，红光像退潮一样从他身上撤下。\n烬叔在光里睁开了眼，看见了你胸口的萌芽，忽然老泪纵横：「原来……真的有人能亮到最后。」\n你和队友们互相搀扶着走出来。天边，青色正一点点碾过夜幕。', 7),
(2207, 1003, 'NARRATE', '你在屏障后协助疏导了一整夜。老人最终被救了下来，却被这一夜的红雾彻底击穿——他的萌芽熄灭了。\n可他活着。他在救助站的窗边种下了一株芽，逢人便说：「有个亮着的人，替我看过晴天了。」\n不是每一次奔赴都有奇迹。但每一次看见，都是救赎的开始。', 8),
(2208, 1003, 'END', '长夜将尽时，协会的钟声响起——污染监测仪上，全城的污染均值第一次出现了下降。\n不是一个人的功劳。是每一盏不肯熄灭的萌芽，连成了黎明。\n小栗在你身边轻声说：「总有一天，我也要像你一样。」你笑了。火种传下去，长夜就有尽头。', 9);

INSERT INTO `story_option` (`id`, `node_id`, `text`, `pollution_delta`, `gold_delta`, `next_node_id`, `seq`) VALUES
(3201, 2201, '记下求助单的位置', 0, 0, 2202, 1),
(3202, 2202, '联络行者协会，依规共同上门', -5, 0, 2203, 1),
(3203, 2202, '把求助单上交协会，不再过问', 5, 0, 2204, 2),
(3204, 2203, '把讲晴天的每一夜都记进日志', -5, 0, 2205, 1),
(3205, 2204, '坚持每天托人给救助站带一句问候', -3, 0, 2205, 1),
(3206, 2205, '报名敢死小队，冲进去救他', -5, 0, 2206, 1),
(3207, 2205, '留在屏障后做后勤疏导', 0, 100, 2207, 2),
(3208, 2205, '拉上小栗一起报名，教他什么是责任', -8, 0, 2206, 3),
(3209, 2206, '背着老人走出红雾', -5, 0, 2208, 1),
(3210, 2207, '去窗边看那株新芽，郑重地鞠一躬', -5, 0, 2208, 1),
(3211, 2208, '对小栗说：你会做得比我更好', -5, 0, NULL, 1);

-- ============================================================
-- 种子数据：觉醒测试题目（10题）
-- ============================================================
INSERT INTO `test_question` (`id`, `seq`, `content`, `options`, `status`) VALUES
(4001, 1, '看到有人被网络恶评围攻，你通常会：', '[{"text":"立刻挺身而出，为对方发声","courage":2},{"text":"先核实真相，再决定怎么做","rationality":2},{"text":"悄悄举报恶意评论，并私信安慰对方","kindness":2}]', 1),
(4002, 2, '遭遇一次重大的失败之后，你：', '[{"text":"抹掉眼泪，第二天继续冲","courage":2},{"text":"复盘每一个环节，制定新计划","rationality":2},{"text":"先痛哭一场，然后找朋友吃顿饭","kindness":2,"pollution":3},{"text":"把责任推给环境，是这个世界太糟了","pollution":8}]', 1),
(4003, 3, '面对一个几乎不可能实现的梦想，你：', '[{"text":"明知不可为而为之，先冲再说","courage":2},{"text":"评估风险，把它拆成一百个小台阶","rationality":2},{"text":"召集更多同行者，一起把它扛起来","kindness":2}]', 1),
(4004, 4, '朋友持续向你倾倒负能量时，你：', '[{"text":"直接点醒他：不能这样沉下去","courage":1,"kindness":1},{"text":"帮他理清问题到底出在哪","rationality":2},{"text":"什么都不评判，只是安静地陪着他","kindness":2,"pollution":2}]', 1),
(4005, 5, '在被污染笼罩的世界里，你更愿意相信：', '[{"text":"胸口的火种，足以照亮黑暗","courage":2},{"text":"秩序与智慧，终会驱散迷雾","rationality":2},{"text":"人与人之间的温度，就是解药","kindness":2}]', 1),
(4006, 6, '队伍里有人偷了公共补给，证据确凿但他家里确实揭不开锅，你：', '[{"text":"按规矩上报，但私下帮他筹一份工","rationality":1,"kindness":1},{"text":"当众揭穿，规矩就是规矩","courage":2},{"text":"先弄清他家的处境，再和协会商量两全的办法","rationality":2},{"text":"睁一只眼闭一只眼，多一事不如少一事","pollution":6}]', 1),
(4007, 7, '你辛苦多日的成果被人抢了功劳，你：', '[{"text":"当面摆事实，不吵不闹但寸步不让","courage":2},{"text":"反思自己哪里没留好痕迹，下次更有章法","rationality":2},{"text":"算了，只要事情做成了就好","kindness":1,"pollution":3},{"text":"记在心里，找机会让他也难堪一次","pollution":10}]', 1),
(4008, 8, '最信任的伙伴开始变得暴躁易怒、萌芽泛红，你：', '[{"text":"直接指出他的变化，哪怕他会恼怒","courage":1,"kindness":1},{"text":"查资料弄清红化的规律，陪他一起净化","rationality":1,"kindness":1},{"text":"默默守着他，等他自己好起来","kindness":1,"pollution":2},{"text":"渐渐疏远他，免得被牵连","pollution":8}]', 1),
(4009, 9, '世界越来越糟的时候，你内心最深的声音是：', '[{"text":"越是这样，越要有人点亮自己","courage":2},{"text":"糟是有原因的，找到原因就能扭转","rationality":2},{"text":"抱紧身边的人，人心不散世界就不会散","kindness":2},{"text":"反正努力也没用","pollution":10}]', 1),
(4010, 10, '如果净化剂只剩余一支，而你和陌生孩子都需要它，你：', '[{"text":"让给孩子——明天他要替我看晴天","kindness":2,"courage":1},{"text":"分析谁的污染更深、治愈率更高，交给协会决定","rationality":2},{"text":"留给自己，活着才有资格谈其他","courage":1,"pollution":5}]', 1);

-- ============================================================
-- 种子数据：行者协会新手善事任务（开荒可接，发布者为总管理员 id=1）
-- ============================================================
INSERT INTO `task` (`id`, `source_type`, `publisher_id`, `title`, `description`, `task_level`, `reward_gold`, `acceptor_gold`, `purify_value`, `status`) VALUES
(5001, 1, 1, '社区送温暖', '为城南灰区的独居老人送去御寒衣物与干净饮水，陪伴聊天一小时。', 'D', 100, 90, 10, 1),
(5002, 1, 1, '护送药品进灰区', '将净化药草护送至城西救助站，途中注意避开污染浓雾，同行者可互相照应。', 'D', 120, 108, 10, 1),
(5003, 1, 1, '清理公园负能量残渣', '带着扫帚和好心情，清理中央公园的负能量沉积物，孩子们明天还要来玩。', 'D', 80, 72, 8, 1);

-- ============================================================
-- 初始化管理员账号（密码均为 admin123 的 BCrypt 密文，上线前修改）
-- ============================================================
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`) VALUES
(1, 'superadmin', '$2a$10$tBN6f4BsdQ56PpFE.UcsYuS2BD7wH4R9dGuUDt/bQGw.uj8HAVBBC', '总管理员', 'SUPER_ADMIN'),
(2, 'xingzhe_admin', '$2a$10$tBN6f4BsdQ56PpFE.UcsYuS2BD7wH4R9dGuUDt/bQGw.uj8HAVBBC', '行者协会管理员', 'XINGZHE_ADMIN'),
(3, 'adventurer_admin', '$2a$10$tBN6f4BsdQ56PpFE.UcsYuS2BD7wH4R9dGuUDt/bQGw.uj8HAVBBC', '冒险者协会管理员', 'ADVENTURER_ADMIN'),
(4, 'knowledge_admin', '$2a$10$tBN6f4BsdQ56PpFE.UcsYuS2BD7wH4R9dGuUDt/bQGw.uj8HAVBBC', '知识宝库管理员', 'KNOWLEDGE_ADMIN');
