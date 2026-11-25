-- =============================================================
-- 智培云 WisTrain-Cloud 数据库初始化脚本
-- 基于权限联动的企业培训与第三方智能体集成系统
-- 版本：v1.0    日期：2026-11-22    环境：MySQL 8.0
-- =============================================================

CREATE DATABASE IF NOT EXISTS wistrain_cloud
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE wistrain_cloud;

-- -------------------------------------------------------------
-- 1. 部门表 tb_department
-- -------------------------------------------------------------
CREATE TABLE tb_department (
  DEPT_ID      INT AUTO_INCREMENT COMMENT '部门ID' PRIMARY KEY,
  DEPT_NAME    VARCHAR(100) NOT NULL COMMENT '部门名称',
  DEPT_CODE    VARCHAR(50)  NOT NULL COMMENT '部门编码',
  DEPT_STATE   TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  CREATE_TIME  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_dept_code (DEPT_CODE)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- -------------------------------------------------------------
-- 2. 用户表 tb_user（部门管理员 / 企业员工）
-- -------------------------------------------------------------
CREATE TABLE tb_user (
  USER_ID       INT AUTO_INCREMENT COMMENT '用户ID' PRIMARY KEY,
  USER_LOGNAME  VARCHAR(50)  NOT NULL COMMENT '登录名（工号/邮箱）',
  USER_PWD      VARCHAR(100) NOT NULL COMMENT '密码密文（BCrypt）',
  USER_REALNAME VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  USER_EMAIL    VARCHAR(50)  NOT NULL COMMENT '邮箱',
  USER_ROLE     TINYINT NOT NULL COMMENT '角色：1部门管理员 2企业员工',
  USER_STATE    TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  DEPT_ID       INT NOT NULL COMMENT '所属部门',
  CREATE_TIME   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_user_logname (USER_LOGNAME),
  KEY idx_user_dept (DEPT_ID),
  CONSTRAINT fk_user_dept FOREIGN KEY (DEPT_ID) REFERENCES tb_department (DEPT_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -------------------------------------------------------------
-- 3. 培训项目表 tb_training_project
-- -------------------------------------------------------------
CREATE TABLE tb_training_project (
  PROJECT_ID     INT AUTO_INCREMENT COMMENT '项目ID' PRIMARY KEY,
  PROJECT_NAME   VARCHAR(100)  NOT NULL COMMENT '项目名称',
  PROJECT_DESC   VARCHAR(2000) NULL COMMENT '项目描述',
  PROJECT_STATE  TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1进行中 2已结束',
  PROJECT_START  DATETIME NULL COMMENT '开始时间',
  PROJECT_END    DATETIME NULL COMMENT '结束时间',
  DEPT_ID        INT NOT NULL COMMENT '所属部门',
  CREATE_USER_ID INT NOT NULL COMMENT '创建人',
  CREATE_TIME    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_project_dept_state (DEPT_ID, PROJECT_STATE),
  CONSTRAINT fk_project_dept FOREIGN KEY (DEPT_ID) REFERENCES tb_department (DEPT_ID),
  CONSTRAINT fk_project_creator FOREIGN KEY (CREATE_USER_ID) REFERENCES tb_user (USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训项目表';

-- -------------------------------------------------------------
-- 4. 培训资料表 tb_training_material
-- -------------------------------------------------------------
CREATE TABLE tb_training_material (
  MATERIAL_ID    INT AUTO_INCREMENT COMMENT '资料ID' PRIMARY KEY,
  PROJECT_ID     INT NOT NULL COMMENT '所属项目',
  MATERIAL_NAME  VARCHAR(200) NOT NULL COMMENT '资料名称',
  MATERIAL_TYPE  TINYINT NOT NULL COMMENT '类型：1文档 2视频 3外链',
  MATERIAL_URL   VARCHAR(500) NOT NULL COMMENT '资料地址（文件路径或链接）',
  MATERIAL_ORDER INT NOT NULL DEFAULT 0 COMMENT '学习顺序',
  CREATE_TIME    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_material_project (PROJECT_ID),
  CONSTRAINT fk_material_project FOREIGN KEY (PROJECT_ID) REFERENCES tb_training_project (PROJECT_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训资料表';

-- -------------------------------------------------------------
-- 5. 项目成员表 tb_project_member（学习进度与解锁的核心载体）
-- -------------------------------------------------------------
CREATE TABLE tb_project_member (
  MEMBER_ID    INT AUTO_INCREMENT COMMENT '记录ID' PRIMARY KEY,
  PROJECT_ID   INT NOT NULL COMMENT '培训项目',
  USER_ID      INT NOT NULL COMMENT '员工',
  JOIN_TIME    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  LEARN_STATE  TINYINT NOT NULL DEFAULT 0 COMMENT '学习状态：0未开始 1学习中 2已完成',
  PROGRESS     TINYINT NOT NULL DEFAULT 0 COMMENT '学习进度（0-100）',
  UNLOCK_TIME  DATETIME NULL COMMENT '智能体解锁时间（冗余展示用，判定以实时计算为准）',
  UNIQUE KEY uk_project_user (PROJECT_ID, USER_ID),
  KEY idx_member_user (USER_ID),
  CONSTRAINT fk_member_project FOREIGN KEY (PROJECT_ID) REFERENCES tb_training_project (PROJECT_ID),
  CONSTRAINT fk_member_user FOREIGN KEY (USER_ID) REFERENCES tb_user (USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员表';

-- -------------------------------------------------------------
-- 6. 智能体表 tb_agent
-- -------------------------------------------------------------
CREATE TABLE tb_agent (
  AGENT_ID        INT AUTO_INCREMENT COMMENT '智能体ID' PRIMARY KEY,
  AGENT_NAME      VARCHAR(100) NOT NULL COMMENT '智能体名称',
  AGENT_PLATFORM  TINYINT NOT NULL COMMENT '平台：1Coze 2Dify 3OpenAI兼容 4其他',
  AGENT_API_KEY   VARCHAR(500) NOT NULL COMMENT 'API Key（AES加密存储，不回显明文）',
  AGENT_ENDPOINT  VARCHAR(500) NOT NULL COMMENT '接口地址',
  AGENT_MODEL     VARCHAR(100) NULL COMMENT '模型标识',
  AGENT_DESC      VARCHAR(1000) NULL COMMENT '智能体简介与适用场景',
  AGENT_STATE     TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  DEPT_ID         INT NOT NULL COMMENT '所属部门',
  CREATE_USER_ID  INT NOT NULL COMMENT '创建人',
  CREATE_TIME     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_agent_dept_state (DEPT_ID, AGENT_STATE),
  CONSTRAINT fk_agent_dept FOREIGN KEY (DEPT_ID) REFERENCES tb_department (DEPT_ID),
  CONSTRAINT fk_agent_creator FOREIGN KEY (CREATE_USER_ID) REFERENCES tb_user (USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体表';

-- -------------------------------------------------------------
-- 7. 项目—智能体权限关联表 tb_project_agent（权限联动规则配置）
-- -------------------------------------------------------------
CREATE TABLE tb_project_agent (
  ID                INT AUTO_INCREMENT COMMENT '记录ID' PRIMARY KEY,
  PROJECT_ID        INT NOT NULL COMMENT '培训项目',
  AGENT_ID          INT NOT NULL COMMENT '智能体',
  UNLOCK_RULE       TINYINT NOT NULL DEFAULT 1 COMMENT '解锁规则：1完成全部资料 2达到进度阈值',
  UNLOCK_THRESHOLD  TINYINT NULL COMMENT '进度阈值（百分比，规则2时必填）',
  CREATE_TIME       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_project_agent (PROJECT_ID, AGENT_ID),
  KEY idx_pa_agent (AGENT_ID),
  CONSTRAINT fk_pa_project FOREIGN KEY (PROJECT_ID) REFERENCES tb_training_project (PROJECT_ID),
  CONSTRAINT fk_pa_agent FOREIGN KEY (AGENT_ID) REFERENCES tb_agent (AGENT_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-智能体权限关联表';

-- -------------------------------------------------------------
-- 8. 智能体会话表 tb_agent_session（选做）
-- -------------------------------------------------------------
CREATE TABLE tb_agent_session (
  SESSION_ID    INT AUTO_INCREMENT COMMENT '会话ID' PRIMARY KEY,
  AGENT_ID      INT NOT NULL COMMENT '智能体',
  USER_ID       INT NOT NULL COMMENT '员工',
  SESSION_TITLE VARCHAR(200) NULL COMMENT '会话标题',
  CREATE_TIME   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  LAST_TIME     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近消息时间',
  KEY idx_session_user_agent (USER_ID, AGENT_ID),
  CONSTRAINT fk_session_agent FOREIGN KEY (AGENT_ID) REFERENCES tb_agent (AGENT_ID),
  CONSTRAINT fk_session_user FOREIGN KEY (USER_ID) REFERENCES tb_user (USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体会话表';

-- -------------------------------------------------------------
-- 9. 学习记录表 tb_learn_record（设计补充：按资料记录「已学」）
--    原数据字典仅有 PROGRESS 汇总字段，补齐「已学资料数」明细
-- -------------------------------------------------------------
CREATE TABLE tb_learn_record (
  RECORD_ID    INT AUTO_INCREMENT COMMENT '记录ID' PRIMARY KEY,
  PROJECT_ID   INT NOT NULL COMMENT '培训项目',
  USER_ID      INT NOT NULL COMMENT '员工',
  MATERIAL_ID  INT NOT NULL COMMENT '资料',
  LEARN_TIME   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '学习时间',
  UNIQUE KEY uk_project_user_material (PROJECT_ID, USER_ID, MATERIAL_ID),
  KEY idx_record_user (USER_ID),
  CONSTRAINT fk_record_project FOREIGN KEY (PROJECT_ID) REFERENCES tb_training_project (PROJECT_ID),
  CONSTRAINT fk_record_material FOREIGN KEY (MATERIAL_ID) REFERENCES tb_training_material (MATERIAL_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';

-- -------------------------------------------------------------
-- 初始化示例数据（教学演示用）
-- -------------------------------------------------------------
INSERT INTO tb_department (DEPT_NAME, DEPT_CODE) VALUES ('软件研发部', 'RD'), ('产品运营部', 'PO');
INSERT INTO tb_user (USER_LOGNAME, USER_PWD, USER_REALNAME, USER_EMAIL, USER_ROLE, DEPT_ID)
VALUES ('admin', '{BCrypt占位-部署时替换}', '张经理', 'admin@wistrain.com', 1, 1);
