-- ============================================
-- 城建档案数据要素管理 - 新表初始化脚本
-- 请在MySQL中执行此脚本来创建新表
-- ============================================

-- 数据要素上传批次表
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
    import_policy VARCHAR(32) DEFAULT 'skip' COMMENT '导入策略',
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

SELECT '新表创建成功！' AS message;
