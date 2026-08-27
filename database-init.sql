-- 创建数据库
CREATE DATABASE IF NOT EXISTS collaborative_sharing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE collaborative_sharing;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    department VARCHAR(100) DEFAULT '城建档案局' COMMENT '所属部门',
    user_role VARCHAR(50) DEFAULT 'ARCHIVE_ADMIN' COMMENT '账号角色',
    created_at DATETIME,
    updated_at DATETIME
);

-- 创建数据要素表（保留旧表兼容旧数据，新业务使用新表）
CREATE TABLE IF NOT EXISTS data_elements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_name VARCHAR(200) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    data_subtype VARCHAR(100) COMMENT '数据子类',
    security_level VARCHAR(50) DEFAULT '内部' COMMENT '安全等级',
    data_version VARCHAR(50) DEFAULT 'V1.0' COMMENT '数据版本',
    source_system VARCHAR(100) DEFAULT '城建档案综合管理系统' COMMENT '来源系统',
    description TEXT,
    status ENUM('NORMAL', 'PENDING_REVIEW') DEFAULT 'NORMAL',
    uploader VARCHAR(100),
    department VARCHAR(100),
    upload_time DATETIME,
    character_count INT DEFAULT 0,
    summary TEXT,
    attachment_path VARCHAR(500),
    attachment_name VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME
);

-- 数据要素上传批次表（完整字段）
CREATE TABLE IF NOT EXISTS data_element_upload_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型',
    batch_name VARCHAR(200) COMMENT '批次名称',
    original_file_name VARCHAR(500) COMMENT '原始文件名',
    stored_file_path VARCHAR(500) COMMENT '存储路径',
    file_hash VARCHAR(128) COMMENT '文件SHA-256哈希',
    file_size BIGINT COMMENT '文件大小（字节）',
    uploader VARCHAR(100) COMMENT '上传人',
    upload_time DATETIME COMMENT '上传时间',
    total_rows INT DEFAULT 0 COMMENT '数据行数',
    business_record_count INT DEFAULT 0 COMMENT '业务记录数',
    recognized_columns TEXT COMMENT '已识别字段列表',
    unrecognized_columns LONGTEXT COMMENT '扩展字段列详情JSON',
    involved_subtypes VARCHAR(500) COMMENT '涉及的数据子类',
    duplicate_of_batch_id BIGINT NULL COMMENT '重复文件的原始批次ID',
    import_policy VARCHAR(32) DEFAULT 'skip' COMMENT '导入策略: skip/force/replace',
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据要素上传批次表';

-- 数据要素业务记录表
CREATE TABLE IF NOT EXISTS data_element_business_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT COMMENT '批次ID',
    data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型，固定为"业务数据"',
    data_subtype VARCHAR(100) COMMENT '数据子类（9类之一）',
    row_index INT COMMENT '原始Excel行号',
    project_id VARCHAR(50) COMMENT '关联工程ID（同一行拆分的记录共享）',
    display_name VARCHAR(500) COMMENT '显示标题',
    record_json LONGTEXT COMMENT '识别后的字段和值（JSON格式）',
    extra_json LONGTEXT COMMENT '未识别扩展字段（JSON格式）',
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_batch_id (batch_id),
    KEY idx_data_subtype (data_subtype),
    KEY idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据要素业务记录表';

-- 数据要素子类配置表（用于汇总表行增删和合规检查状态）
CREATE TABLE IF NOT EXISTS data_element_subtype_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型',
    data_subtype VARCHAR(100) NOT NULL COMMENT '数据子类',
    table_name VARCHAR(100) COMMENT '数据库表名',
    field_count INT DEFAULT 0 COMMENT '字段数',
    is_builtin TINYINT(1) DEFAULT 0 COMMENT '是否默认子类',
    is_visible TINYINT(1) DEFAULT 1 COMMENT '是否显示',
    sort_order INT DEFAULT 0 COMMENT '排序',
    self_check_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' COMMENT '合规自检状态',
    security_check_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' COMMENT '温州数安港检查状态',
    compliance_result TEXT COMMENT '最近合规检查结果',
    self_check_time DATETIME COMMENT '自检时间',
    security_check_time DATETIME COMMENT '数安港检查时间',
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_data_type_subtype (data_type, data_subtype)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据要素子类配置表';

-- 创建服务组件表（新增字段：服务编码、接口地址、所属部门、安全等级、授权单位、服务配置、备注、合规审查字段）
CREATE TABLE IF NOT EXISTS service_components (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(200) NOT NULL,
    service_code VARCHAR(100) COMMENT '服务编码',
    description TEXT,
    version VARCHAR(20),
    api_url VARCHAR(500) COMMENT '接口地址',
    owner_department VARCHAR(100) COMMENT '所属部门',
    security_level VARCHAR(50) COMMENT '安全等级',
    authorized_units TEXT COMMENT '授权单位',
    config_json TEXT COMMENT '服务配置',
    remark TEXT COMMENT '备注',
    status VARCHAR(50) DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿/PENDING_APPROVAL待审批/PUBLISHED已发布/AUTHORIZED已授权/OFFLINE已下架',
    created_at DATETIME,
    updated_at DATETIME,
    compliance_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' COMMENT '合规状态',
    compliance_apply_unit VARCHAR(100) COMMENT '合规审查申请单位',
    compliance_purpose TEXT COMMENT '共享用途',
    compliance_data_scope TEXT COMMENT '涉及数据范围',
    compliance_remark TEXT COMMENT '审查说明',
    compliance_submit_time DATETIME COMMENT '合规审查提交时间',
    compliance_result TEXT COMMENT '合规审查结果'
);

-- 创建待办事项表（流程审批：上传人、上传时间、上传部门、流程名称、流程用途、流程状态、上传附件）
CREATE TABLE IF NOT EXISTS todo_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader VARCHAR(100) NOT NULL,
    upload_time DATETIME NOT NULL,
    department VARCHAR(100) NOT NULL,
    process_name VARCHAR(200) NOT NULL,
    process_purpose TEXT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    attachment_path VARCHAR(500),
    attachment_name VARCHAR(200),
    reviewer VARCHAR(100),
    review_time DATETIME,
    review_comment TEXT,
    created_at DATETIME,
    updated_at DATETIME
);

-- 创建系统设置表（用于存储背景图片等设置）
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    created_at DATETIME,
    updated_at DATETIME
);

-- ====================
-- 兼容方案：针对已有旧表的字段扩展
-- 注意：以下 ALTER 语句需要 MySQL 8.0+（ADD COLUMN IF NOT EXISTS）
-- 如果使用 MySQL 5.7，请忽略以下语句的报错，
-- 应用启动时 DataInitializer 会自动处理字段补充
-- ====================
--
-- 以下 ALTER 语句已注释，由 DataInitializer 启动时自动执行
-- 如需手动执行，请根据实际 MySQL 版本调整语法
--

-- 初始化数据要素默认子类行
INSERT INTO data_element_subtype_config (data_type, data_subtype, table_name, field_count, is_builtin, is_visible, sort_order, self_check_status, security_check_status, created_at, updated_at)
VALUES
('业务数据', '工程基本信息', 'uc_project_basic', 23, 1, 1, 1, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()),
('业务数据', '单体工程信息', 'uc_building', 16, 1, 1, 2, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()),
('业务数据', '竣工图纸信息', 'uc_as_built_drawing', 19, 1, 1, 3, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()),
('业务数据', '工程合同信息', 'uc_contract', 14, 1, 1, 4, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()),
('业务数据', '竣工验收信息', 'uc_acceptance', 14, 1, 1, 5, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()),
('业务数据', '产权人信息', 'uc_property_owner', 15, 1, 1, 6, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()),
('业务数据', '标准地名信息', 'uc_standard_place_name', 15, 1, 1, 7, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()),
('业务数据', '空间坐标信息', 'uc_spatial_geometry', 17, 1, 1, 8, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()),
('业务数据', '五方主体信息', 'uc_project_party', 15, 1, 1, 9, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW())
ON DUPLICATE KEY UPDATE
table_name = VALUES(table_name),
field_count = VALUES(field_count),
is_builtin = 1,
sort_order = VALUES(sort_order),
updated_at = NOW();

-- 插入默认用户数据
INSERT INTO users (username, password, real_name, department, user_role, created_at, updated_at)
VALUES
('admin', 'admin', '系统管理员', '城建档案局', 'ARCHIVE_ADMIN', NOW(), NOW()),
('wyb', '123456', '物业办', '物业办', 'EXTERNAL_DEPARTMENT', NOW(), NOW()),
('zak', '123456', '质安科', '质安科', 'EXTERNAL_DEPARTMENT', NOW(), NOW()),
('bdc', '123456', '不动产', '不动产', 'EXTERNAL_DEPARTMENT', NOW(), NOW()),
('wygs', '123456', '物业公司', '物业公司', 'EXTERNAL_DEPARTMENT', NOW(), NOW()),
('yjb', '123456', '应急办', '应急办', 'EXTERNAL_DEPARTMENT', NOW(), NOW()),
('gaj', '123456', '公安系统', '公安系统', 'EXTERNAL_DEPARTMENT', NOW(), NOW())
ON DUPLICATE KEY UPDATE
real_name = VALUES(real_name),
department = VALUES(department),
user_role = VALUES(user_role),
updated_at = NOW();

-- 显示创建结果
SELECT 'Database and tables created successfully!' as message;

-- ====================
-- 可选：将旧版示例数据更新为城建档案数据要素格式
-- 如果数据库中仍存在旧版示例数据（如"结构、电气、管道"等旧类型），可执行以下语句进行更新
-- 注意：SELESCT old data first to confirm, then run these UPDATEs if needed
-- ====================
-- UPDATE data_elements SET data_type = '业务数据', data_subtype = '工程基本信息', security_level = '内部', source_system = '城建档案综合管理系统', data_version = 'V1.0' WHERE data_name = '建筑结构数据';
-- UPDATE data_elements SET data_type = '业务数据', data_subtype = '竣工图纸信息', security_level = '敏感', source_system = '城建档案综合管理系统', data_version = 'V1.0' WHERE data_name = '电气布线数据';
-- UPDATE data_elements SET data_type = '业务数据', data_subtype = '工程基本信息', security_level = '内部', source_system = '城建档案综合管理系统', data_version = 'V1.0' WHERE data_name = '给排水系统数据';
-- UPDATE data_elements SET data_type = '业务数据', data_subtype = '工程合同信息', security_level = '内部', source_system = '城建档案综合管理系统', data_version = 'V1.0' WHERE data_name = '管道维护数据';
-- UPDATE data_elements SET data_type = '业务数据', data_subtype = '竣工图纸信息', security_level = '内部', source_system = '城建档案综合管理系统', data_version = 'V1.0' WHERE data_name = '建筑设计数据';
