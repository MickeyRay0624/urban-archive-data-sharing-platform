package com.collaborative.sharing.util;

import java.util.*;

/**
 * 数据要素字段字典。
 * 支持业务数据、业务元数据、服务实例数据、日志数据四类一级类型。
 */
public class DataElementFieldDict {

    private DataElementFieldDict() {}

    public static final String TYPE_BUSINESS = "业务数据";
    public static final String TYPE_METADATA = "业务元数据";
    public static final String TYPE_SERVICE_INSTANCE = "服务实例数据";
    public static final String TYPE_LOG = "日志数据";

    public static final String KEY_BUSINESS = "business";
    public static final String KEY_METADATA = "metadata";
    public static final String KEY_SERVICE_INSTANCE = "service-instance";
    public static final String KEY_LOG = "log";

    /** 9类业务子类列表（按固定顺序），保留旧代码直接引用。 */
    public static final String[] SUBTYPE_LIST = {
        "工程基本信息",
        "单体工程信息",
        "竣工图纸信息",
        "工程合同信息",
        "竣工验收信息",
        "产权人信息",
        "标准地名信息",
        "空间坐标信息",
        "五方主体信息"
    };

    /** 9类业务子类对应的建议数据库表名，保留旧代码直接引用。 */
    public static final Map<String, String> SUBTYPE_TABLE_MAP = new LinkedHashMap<>();

    public static class DataTypeConfig {
        public final String typeKey;
        public final String dataType;
        public final String pageTitle;
        public final String recordLabel;

        public DataTypeConfig(String typeKey, String dataType, String pageTitle, String recordLabel) {
            this.typeKey = typeKey;
            this.dataType = dataType;
            this.pageTitle = pageTitle;
            this.recordLabel = recordLabel;
        }
    }

    /**
     * 字段条目：一级类型、中文名、英文名、所属子类。
     */
    public static class FieldEntry {
        public final String dataType;
        public final String cnName;
        public final String enName;
        public final String subtype;

        public FieldEntry(String cnName, String enName, String subtype) {
            this(TYPE_BUSINESS, cnName, enName, subtype);
        }

        public FieldEntry(String dataType, String cnName, String enName, String subtype) {
            this.dataType = dataType;
            this.cnName = cnName;
            this.enName = enName;
            this.subtype = subtype;
        }
    }

    private static final Map<String, DataTypeConfig> DATA_TYPE_CONFIGS_BY_KEY = new LinkedHashMap<>();
    private static final Map<String, DataTypeConfig> DATA_TYPE_CONFIGS_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, String[]> SUBTYPE_LIST_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> SUBTYPE_TABLE_MAP_BY_TYPE = new LinkedHashMap<>();

    /** 所有字段列表 */
    private static final List<FieldEntry> ALL_FIELDS = new ArrayList<>();

    /** 旧业务数据中文字段名 -> FieldEntry，保留旧代码兼容。 */
    private static final Map<String, FieldEntry> CN_MAP = new LinkedHashMap<>();

    /** 旧业务数据英文字段名 -> FieldEntry，保留旧代码兼容。 */
    private static final Map<String, FieldEntry> EN_MAP = new LinkedHashMap<>();

    private static final Map<String, Map<String, FieldEntry>> CN_MAP_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, Map<String, FieldEntry>> EN_MAP_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, Map<String, FieldEntry>> CN_LOWER_MAP_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, Map<String, FieldEntry>> EN_LOWER_MAP_BY_TYPE = new LinkedHashMap<>();

    /** 字段别名映射：仅业务数据保留历史别名。 */
    private static final Map<String, String> FIELD_ALIAS_MAP = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS_MAP_LOWER = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> ALIAS_MAP_LOWER_BY_TYPE = new LinkedHashMap<>();

    /** 指定为扩展字段（不识别为标准元数据，但保存到 extra_json）的字段名集合 */
    private static final Set<String> EXTRA_ONLY_FIELDS = new LinkedHashSet<>();

    private static Map<String, List<String>> subtypeFieldCache = null;
    private static final Map<String, Map<String, List<String>>> subtypeFieldCacheByType = new LinkedHashMap<>();

    static {
        initDataTypes();
        initSubtypeConfigs();
        initFields();
        initBusinessAliases();
        initExtraOnlyFields();
        rebuildIndexes();
    }

    private static void initDataTypes() {
        addDataTypeConfig(KEY_BUSINESS, TYPE_BUSINESS, "城建档案业务数据管理", "业务数据记录");
        addDataTypeConfig(KEY_METADATA, TYPE_METADATA, "城建档案业务元数据管理", "元数据记录");
        addDataTypeConfig(KEY_SERVICE_INSTANCE, TYPE_SERVICE_INSTANCE, "城建档案服务实例数据管理", "服务实例记录");
        addDataTypeConfig(KEY_LOG, TYPE_LOG, "城建档案日志数据管理", "日志记录");
    }

    private static void addDataTypeConfig(String typeKey, String dataType, String pageTitle, String recordLabel) {
        DataTypeConfig config = new DataTypeConfig(typeKey, dataType, pageTitle, recordLabel);
        DATA_TYPE_CONFIGS_BY_KEY.put(typeKey, config);
        DATA_TYPE_CONFIGS_BY_TYPE.put(dataType, config);
    }

    private static void initSubtypeConfigs() {
        SUBTYPE_TABLE_MAP.put("工程基本信息", "uc_project_basic");
        SUBTYPE_TABLE_MAP.put("单体工程信息", "uc_building");
        SUBTYPE_TABLE_MAP.put("竣工图纸信息", "uc_as_built_drawing");
        SUBTYPE_TABLE_MAP.put("工程合同信息", "uc_contract");
        SUBTYPE_TABLE_MAP.put("竣工验收信息", "uc_acceptance");
        SUBTYPE_TABLE_MAP.put("产权人信息", "uc_property_owner");
        SUBTYPE_TABLE_MAP.put("标准地名信息", "uc_standard_place_name");
        SUBTYPE_TABLE_MAP.put("空间坐标信息", "uc_spatial_geometry");
        SUBTYPE_TABLE_MAP.put("五方主体信息", "uc_project_party");
        registerSubtypeConfig(TYPE_BUSINESS, SUBTYPE_LIST, SUBTYPE_TABLE_MAP);

        String[] metadataSubtypes = {
            "数据定义元数据",
            "数据结构元数据",
            "数据约束条件元数据",
            "数据关系元数据",
            "数据质量元数据",
            "数据安全权限元数据"
        };
        Map<String, String> metadataTables = new LinkedHashMap<>();
        metadataTables.put("数据定义元数据", "metadata_data_definition");
        metadataTables.put("数据结构元数据", "metadata_data_schema");
        metadataTables.put("数据约束条件元数据", "metadata_data_constraint");
        metadataTables.put("数据关系元数据", "metadata_data_relation");
        metadataTables.put("数据质量元数据", "metadata_data_quality");
        metadataTables.put("数据安全权限元数据", "metadata_security_permission");
        registerSubtypeConfig(TYPE_METADATA, metadataSubtypes, metadataTables);

        String[] serviceSubtypes = {
            "服务注册信息",
            "服务标识版本信息",
            "服务安全认证信息",
            "服务授权配置",
            "用户单位接入信息",
            "服务路由配置",
            "服务上下架状态"
        };
        Map<String, String> serviceTables = new LinkedHashMap<>();
        serviceTables.put("服务注册信息", "service_registry");
        serviceTables.put("服务标识版本信息", "service_identifier_version");
        serviceTables.put("服务安全认证信息", "service_security_auth");
        serviceTables.put("服务授权配置", "service_authorization_config");
        serviceTables.put("用户单位接入信息", "service_unit_access");
        serviceTables.put("服务路由配置", "service_route_config");
        serviceTables.put("服务上下架状态", "service_lifecycle_status");
        registerSubtypeConfig(TYPE_SERVICE_INSTANCE, serviceSubtypes, serviceTables);

        String[] logSubtypes = {
            "系统运行日志",
            "安全审计日志",
            "访问调用日志",
            "用户行为日志",
            "服务健康监控日志",
            "异常告警日志"
        };
        Map<String, String> logTables = new LinkedHashMap<>();
        logTables.put("系统运行日志", "log_system_running");
        logTables.put("安全审计日志", "log_security_audit");
        logTables.put("访问调用日志", "log_access_call");
        logTables.put("用户行为日志", "log_user_behavior");
        logTables.put("服务健康监控日志", "log_service_health");
        logTables.put("异常告警日志", "log_exception_alert");
        registerSubtypeConfig(TYPE_LOG, logSubtypes, logTables);
    }

    private static void registerSubtypeConfig(String dataType, String[] subtypeList, Map<String, String> tableMap) {
        SUBTYPE_LIST_BY_TYPE.put(dataType, subtypeList);
        SUBTYPE_TABLE_MAP_BY_TYPE.put(dataType, tableMap);
    }

    private static void addField(String cn, String en, String subtype) {
        addField(TYPE_BUSINESS, cn, en, subtype);
    }

    private static void addField(String dataType, String cn, String en, String subtype) {
        ALL_FIELDS.add(new FieldEntry(dataType, cn, en, subtype));
    }

    private static void initFields() {
        initBusinessFields();
        initMetadataFields();
        initServiceInstanceFields();
        initLogFields();
    }

    private static void initBusinessFields() {
        String s1 = "工程基本信息";
        addField("工程ID", "project_id", s1);
        addField("工程名称", "project_name", s1);
        addField("工程地址", "project_address", s1);
        addField("用地规划许可证", "land_planning_permit_no", s1);
        addField("工程规划许可证", "construction_planning_permit_no", s1);
        addField("施工许可证", "construction_permit_no", s1);
        addField("建设单位", "developer_name", s1);
        addField("施工单位", "construction_org_name", s1);
        addField("监理单位", "supervision_org_name", s1);
        addField("勘察单位", "survey_org_name", s1);
        addField("设计单位", "design_org_name", s1);
        addField("案卷名称", "volume_title", s1);
        addField("文件名称", "file_title", s1);
        addField("分户名称", "household_name", s1);
        addField("栋标识码", "building_identifier", s1);
        addField("户标志码", "household_identifier", s1);
        addField("电子文件关联", "digital_file_relation", s1);
        addField("开始日期", "start_date", s1);
        addField("结束日期", "end_date", s1);
        addField("档案项目号", "archive_project_code", s1);
        addField("项目类型", "project_type", s1);
        addField("归档状态", "archive_status", s1);
        addField("保管期限", "retention_period", s1);

        String s2 = "单体工程信息";
        addField("单体工程ID", "building_id", s2);
        addField("所属工程ID", "project_id", s2);
        addField("单体工程名称", "building_name", s2);
        addField("楼栋号/栋号", "building_no", s2);
        addField("栋标识码", "building_identifier", s2);
        addField("单位工程名称", "unit_project_name", s2);
        addField("结构类型", "structure_type", s2);
        addField("建筑功能", "building_function", s2);
        addField("地上层数", "floors_above_ground", s2);
        addField("地下层数", "floors_under_ground", s2);
        addField("建筑高度", "building_height", s2);
        addField("建筑面积", "building_area", s2);
        addField("占地面积", "footprint_area", s2);
        addField("开工日期", "building_start_date", s2);
        addField("竣工日期", "building_completion_date", s2);
        addField("质量等级", "quality_grade", s2);

        String s3 = "竣工图纸信息";
        addField("图纸ID", "drawing_id", s3);
        addField("所属工程ID", "project_id", s3);
        addField("关联单体工程ID", "building_id", s3);
        addField("图纸名称", "drawing_title", s3);
        addField("图纸类别", "drawing_category", s3);
        addField("专业代码", "discipline_code", s3);
        addField("图号", "drawing_no", s3);
        addField("图纸版本", "drawing_version", s3);
        addField("图纸目录序号", "drawing_index_no", s3);
        addField("竣工图章信息", "as_built_stamp_info", s3);
        addField("编制单位", "prepared_by_org", s3);
        addField("编制人", "prepared_by_person", s3);
        addField("审核人", "reviewed_by_person", s3);
        addField("技术负责人", "technical_leader", s3);
        addField("监理工程师", "supervision_engineer", s3);
        addField("编制日期", "prepared_date", s3);
        addField("分户名称", "household_name", s3);
        addField("电子文件关联", "digital_file_relation", s3);
        addField("文件格式", "file_format", s3);

        String s4 = "工程合同信息";
        addField("合同ID", "contract_id", s4);
        addField("所属工程ID", "project_id", s4);
        addField("合同名称", "contract_title", s4);
        addField("合同类型", "contract_type", s4);
        addField("合同编号", "contract_no", s4);
        addField("发包人/甲方", "contract_employer", s4);
        addField("承包人/乙方", "contract_contractor", s4);
        addField("签订日期", "sign_date", s4);
        addField("合同金额", "contract_amount", s4);
        addField("合同工期", "contract_duration", s4);
        addField("工程范围", "contract_scope", s4);
        addField("质量标准", "quality_standard", s4);
        addField("履约状态", "performance_status", s4);
        addField("电子文件关联", "digital_file_relation", s4);

        String s5 = "竣工验收信息";
        addField("验收ID", "acceptance_id", s5);
        addField("所属工程ID", "project_id", s5);
        addField("关联单体工程ID", "building_id", s5);
        addField("验收文件名称", "acceptance_file_title", s5);
        addField("验收类别", "acceptance_type", s5);
        addField("验收日期", "acceptance_date", s5);
        addField("验收组织单位", "organizer_org", s5);
        addField("验收结论", "acceptance_conclusion", s5);
        addField("质量评定结果", "quality_evaluation_result", s5);
        addField("竣工报告日期", "completion_report_date", s5);
        addField("参验单位", "participating_orgs", s5);
        addField("备案编号", "completion_record_no", s5);
        addField("整改意见", "rectification_opinion", s5);
        addField("电子文件关联", "digital_file_relation", s5);

        String s6 = "产权人信息";
        addField("产权记录ID", "owner_record_id", s6);
        addField("所属工程ID", "project_id", s6);
        addField("户标志码", "household_identifier", s6);
        addField("产权人名称", "owner_name", s6);
        addField("产权人类型", "owner_type", s6);
        addField("证件类型", "id_doc_type", s6);
        addField("证件号码", "id_doc_no", s6);
        addField("共有情况", "co_ownership_status", s6);
        addField("不动产单元号", "real_estate_unit_no", s6);
        addField("权利类型", "right_type", s6);
        addField("权利性质", "right_nature", s6);
        addField("坐落", "real_estate_location", s6);
        addField("建筑面积", "property_building_area", s6);
        addField("登记时间", "registration_time", s6);
        addField("证书编号", "certificate_no", s6);

        String s7 = "标准地名信息";
        addField("地名记录ID", "place_name_id", s7);
        addField("所属工程ID", "project_id", s7);
        addField("标准地名", "standard_place_name", s7);
        addField("罗马字母拼写", "romanized_name", s7);
        addField("地名类别", "place_name_type", s7);
        addField("所属政区", "administrative_area", s7);
        addField("位置描述", "location_description", s7);
        addField("批准机关", "approval_authority", s7);
        addField("批准时间", "approval_date", s7);
        addField("门牌/门楼牌号", "house_number", s7);
        addField("道路/街巷名称", "road_street_name", s7);
        addField("小区/建筑物名称", "building_complex_name", s7);
        addField("地址全称", "full_standard_address", s7);
        addField("地名状态", "place_name_status", s7);
        addField("来源文件名称", "source_file_title", s7);

        String s8 = "空间坐标信息";
        addField("空间记录ID", "spatial_id", s8);
        addField("所属工程ID", "project_id", s8);
        addField("关联单体工程ID", "building_id", s8);
        addField("空间对象类型", "geometry_object_type", s8);
        addField("坐标系统", "coordinate_system", s8);
        addField("高程基准", "vertical_datum", s8);
        addField("几何类型", "geometry_type", s8);
        addField("经度/横坐标", "x_coordinate", s8);
        addField("纬度/纵坐标", "y_coordinate", s8);
        addField("高程", "z_coordinate", s8);
        addField("界址点编号", "boundary_point_no", s8);
        addField("界址线编号", "boundary_line_no", s8);
        addField("宗地/用地面积", "land_area", s8);
        addField("测绘成果名称", "survey_result_title", s8);
        addField("测绘单位", "surveying_org", s8);
        addField("测绘日期", "survey_date", s8);
        addField("空间数据文件", "geometry_file", s8);

        String s9 = "五方主体信息";
        addField("主体记录ID", "party_record_id", s9);
        addField("所属工程ID", "project_id", s9);
        addField("主体类型", "party_type", s9);
        addField("单位名称", "party_org_name", s9);
        addField("统一社会信用代码", "unified_social_credit_code", s9);
        addField("资质证书编号", "qualification_cert_no", s9);
        addField("资质等级", "qualification_grade", s9);
        addField("项目负责人姓名", "project_leader_name", s9);
        addField("项目负责人证件号", "project_leader_id_no", s9);
        addField("注册执业资格", "professional_qualification", s9);
        addField("执业证书编号", "professional_cert_no", s9);
        addField("联系电话", "contact_phone", s9);
        addField("责任范围", "responsibility_scope", s9);
        addField("人员登记表来源", "personnel_register_file", s9);
        addField("电子文件关联", "digital_file_relation", s9);
    }

    private static void initMetadataFields() {
        String s1 = "数据定义元数据";
        addField(TYPE_METADATA, "元数据ID", "metadata_id", s1);
        addField(TYPE_METADATA, "数据名称", "data_name", s1);
        addField(TYPE_METADATA, "数据描述", "data_description", s1);
        addField(TYPE_METADATA, "数据域", "data_domain", s1);
        addField(TYPE_METADATA, "业务含义", "business_meaning", s1);
        addField(TYPE_METADATA, "来源系统", "source_system", s1);
        addField(TYPE_METADATA, "责任部门", "owner_department", s1);
        addField(TYPE_METADATA, "数据一级类型", "data_type_name", s1);
        addField(TYPE_METADATA, "数据子类", "data_subtype_name", s1);
        addField(TYPE_METADATA, "状态", "status", s1);

        String s2 = "数据结构元数据";
        addField(TYPE_METADATA, "结构ID", "schema_id", s2);
        addField(TYPE_METADATA, "表名", "table_name", s2);
        addField(TYPE_METADATA, "表中文名", "table_cn_name", s2);
        addField(TYPE_METADATA, "字段名", "field_name", s2);
        addField(TYPE_METADATA, "字段中文名", "field_cn_name", s2);
        addField(TYPE_METADATA, "字段类型", "field_type", s2);
        addField(TYPE_METADATA, "字段长度", "field_length", s2);
        addField(TYPE_METADATA, "字段格式", "field_format", s2);
        addField(TYPE_METADATA, "是否可为空", "nullable_flag", s2);
        addField(TYPE_METADATA, "是否主键", "primary_key_flag", s2);

        String s3 = "数据约束条件元数据";
        addField(TYPE_METADATA, "约束ID", "constraint_id", s3);
        addField(TYPE_METADATA, "约束对象", "object_name", s3);
        addField(TYPE_METADATA, "约束类型", "constraint_type", s3);
        addField(TYPE_METADATA, "约束规则", "constraint_rule", s3);
        addField(TYPE_METADATA, "取值范围", "value_range", s3);
        addField(TYPE_METADATA, "是否必填", "required_flag", s3);
        addField(TYPE_METADATA, "是否唯一", "unique_flag", s3);
        addField(TYPE_METADATA, "校验提示", "validation_message", s3);

        String s4 = "数据关系元数据";
        addField(TYPE_METADATA, "关系ID", "relation_id", s4);
        addField(TYPE_METADATA, "源对象", "source_object", s4);
        addField(TYPE_METADATA, "源字段", "source_field", s4);
        addField(TYPE_METADATA, "目标对象", "target_object", s4);
        addField(TYPE_METADATA, "目标字段", "target_field", s4);
        addField(TYPE_METADATA, "关系类型", "relation_type", s4);
        addField(TYPE_METADATA, "基数关系", "cardinality", s4);
        addField(TYPE_METADATA, "关系说明", "relation_description", s4);

        String s5 = "数据质量元数据";
        addField(TYPE_METADATA, "质量规则ID", "quality_id", s5);
        addField(TYPE_METADATA, "质量规则名称", "quality_rule_name", s5);
        addField(TYPE_METADATA, "质量维度", "quality_dimension", s5);
        addField(TYPE_METADATA, "检查频率", "check_frequency", s5);
        addField(TYPE_METADATA, "阈值", "threshold_value", s5);
        addField(TYPE_METADATA, "质量评分", "quality_score", s5);
        addField(TYPE_METADATA, "问题数量", "issue_count", s5);
        addField(TYPE_METADATA, "最近检查时间", "last_check_time", s5);

        String s6 = "数据安全权限元数据";
        addField(TYPE_METADATA, "安全规则ID", "security_id", s6);
        addField(TYPE_METADATA, "安全等级", "security_level", s6);
        addField(TYPE_METADATA, "权限范围", "permission_scope", s6);
        addField(TYPE_METADATA, "脱敏规则", "desensitization_rule", s6);
        addField(TYPE_METADATA, "是否加密", "encryption_flag", s6);
        addField(TYPE_METADATA, "访问角色", "access_role", s6);
        addField(TYPE_METADATA, "是否需要审批", "approval_required", s6);
    }

    private static void initServiceInstanceFields() {
        String s1 = "服务注册信息";
        addField(TYPE_SERVICE_INSTANCE, "服务ID", "service_id", s1);
        addField(TYPE_SERVICE_INSTANCE, "服务名称", "service_name", s1);
        addField(TYPE_SERVICE_INSTANCE, "服务类型", "service_type", s1);
        addField(TYPE_SERVICE_INSTANCE, "服务提供方", "service_provider", s1);
        addField(TYPE_SERVICE_INSTANCE, "服务描述", "service_desc", s1);
        addField(TYPE_SERVICE_INSTANCE, "注册时间", "register_time", s1);
        addField(TYPE_SERVICE_INSTANCE, "服务状态", "status", s1);

        String s2 = "服务标识版本信息";
        addField(TYPE_SERVICE_INSTANCE, "实例ID", "instance_id", s2);
        addField(TYPE_SERVICE_INSTANCE, "服务标识符", "service_identifier", s2);
        addField(TYPE_SERVICE_INSTANCE, "服务版本", "service_version", s2);
        addField(TYPE_SERVICE_INSTANCE, "接口版本", "api_version", s2);
        addField(TYPE_SERVICE_INSTANCE, "发布时间", "release_time", s2);
        addField(TYPE_SERVICE_INSTANCE, "兼容性说明", "compatibility_desc", s2);

        String s3 = "服务安全认证信息";
        addField(TYPE_SERVICE_INSTANCE, "认证ID", "auth_id", s3);
        addField(TYPE_SERVICE_INSTANCE, "认证方式", "auth_method", s3);
        addField(TYPE_SERVICE_INSTANCE, "证书编号", "certificate_no", s3);
        addField(TYPE_SERVICE_INSTANCE, "Token有效期", "token_validity", s3);
        addField(TYPE_SERVICE_INSTANCE, "加密算法", "encryption_algorithm", s3);
        addField(TYPE_SERVICE_INSTANCE, "安全等级", "security_level", s3);
        addField(TYPE_SERVICE_INSTANCE, "认证状态", "auth_status", s3);

        String s4 = "服务授权配置";
        addField(TYPE_SERVICE_INSTANCE, "授权ID", "grant_id", s4);
        addField(TYPE_SERVICE_INSTANCE, "服务ID", "service_id", s4);
        addField(TYPE_SERVICE_INSTANCE, "授权单位", "authorized_unit", s4);
        addField(TYPE_SERVICE_INSTANCE, "授权角色", "authorized_role", s4);
        addField(TYPE_SERVICE_INSTANCE, "访问范围", "access_scope", s4);
        addField(TYPE_SERVICE_INSTANCE, "授权开始时间", "start_time", s4);
        addField(TYPE_SERVICE_INSTANCE, "授权结束时间", "end_time", s4);
        addField(TYPE_SERVICE_INSTANCE, "审批状态", "approval_status", s4);

        String s5 = "用户单位接入信息";
        addField(TYPE_SERVICE_INSTANCE, "接入ID", "unit_access_id", s5);
        addField(TYPE_SERVICE_INSTANCE, "用户单位名称", "unit_name", s5);
        addField(TYPE_SERVICE_INSTANCE, "统一社会信用代码", "unit_code", s5);
        addField(TYPE_SERVICE_INSTANCE, "联系人", "contact_person", s5);
        addField(TYPE_SERVICE_INSTANCE, "联系电话", "contact_phone", s5);
        addField(TYPE_SERVICE_INSTANCE, "接入网络", "access_network", s5);
        addField(TYPE_SERVICE_INSTANCE, "接入IP", "access_ip", s5);
        addField(TYPE_SERVICE_INSTANCE, "接入状态", "access_status", s5);

        String s6 = "服务路由配置";
        addField(TYPE_SERVICE_INSTANCE, "路由ID", "route_id", s6);
        addField(TYPE_SERVICE_INSTANCE, "网关路径", "gateway_path", s6);
        addField(TYPE_SERVICE_INSTANCE, "目标地址", "target_url", s6);
        addField(TYPE_SERVICE_INSTANCE, "请求方法", "request_method", s6);
        addField(TYPE_SERVICE_INSTANCE, "超时时间", "timeout_seconds", s6);
        addField(TYPE_SERVICE_INSTANCE, "重试策略", "retry_policy", s6);
        addField(TYPE_SERVICE_INSTANCE, "路由状态", "route_status", s6);

        String s7 = "服务上下架状态";
        addField(TYPE_SERVICE_INSTANCE, "生命周期ID", "lifecycle_id", s7);
        addField(TYPE_SERVICE_INSTANCE, "服务ID", "service_id", s7);
        addField(TYPE_SERVICE_INSTANCE, "操作类型", "operation_type", s7);
        addField(TYPE_SERVICE_INSTANCE, "操作时间", "operation_time", s7);
        addField(TYPE_SERVICE_INSTANCE, "操作人", "operator", s7);
        addField(TYPE_SERVICE_INSTANCE, "审核结果", "review_result", s7);
        addField(TYPE_SERVICE_INSTANCE, "备注", "remark", s7);
    }

    private static void initLogFields() {
        String s1 = "系统运行日志";
        addField(TYPE_LOG, "日志ID", "log_id", s1);
        addField(TYPE_LOG, "日志时间", "log_time", s1);
        addField(TYPE_LOG, "模块名称", "module_name", s1);
        addField(TYPE_LOG, "日志级别", "log_level", s1);
        addField(TYPE_LOG, "事件类型", "event_type", s1);
        addField(TYPE_LOG, "事件内容", "event_content", s1);
        addField(TYPE_LOG, "服务器IP", "server_ip", s1);
        addField(TYPE_LOG, "追踪ID", "trace_id", s1);

        String s2 = "安全审计日志";
        addField(TYPE_LOG, "审计ID", "audit_id", s2);
        addField(TYPE_LOG, "审计时间", "audit_time", s2);
        addField(TYPE_LOG, "用户名", "user_name", s2);
        addField(TYPE_LOG, "用户角色", "user_role", s2);
        addField(TYPE_LOG, "操作类型", "operation_type", s2);
        addField(TYPE_LOG, "目标资源", "target_resource", s2);
        addField(TYPE_LOG, "认证结果", "auth_result", s2);
        addField(TYPE_LOG, "风险等级", "risk_level", s2);
        addField(TYPE_LOG, "客户端IP", "client_ip", s2);

        String s3 = "访问调用日志";
        addField(TYPE_LOG, "访问ID", "access_id", s3);
        addField(TYPE_LOG, "调用时间", "call_time", s3);
        addField(TYPE_LOG, "服务名称", "service_name", s3);
        addField(TYPE_LOG, "服务标识符", "service_identifier", s3);
        addField(TYPE_LOG, "调用单位", "caller_unit", s3);
        addField(TYPE_LOG, "请求方法", "request_method", s3);
        addField(TYPE_LOG, "请求路径", "request_path", s3);
        addField(TYPE_LOG, "响应状态", "response_status", s3);
        addField(TYPE_LOG, "耗时毫秒", "duration_ms", s3);

        String s4 = "用户行为日志";
        addField(TYPE_LOG, "行为ID", "behavior_id", s4);
        addField(TYPE_LOG, "用户名", "user_name", s4);
        addField(TYPE_LOG, "所属部门", "department", s4);
        addField(TYPE_LOG, "行为类型", "behavior_type", s4);
        addField(TYPE_LOG, "页面名称", "page_name", s4);
        addField(TYPE_LOG, "操作名称", "action_name", s4);
        addField(TYPE_LOG, "操作时间", "action_time", s4);
        addField(TYPE_LOG, "执行结果", "result", s4);

        String s5 = "服务健康监控日志";
        addField(TYPE_LOG, "监控ID", "health_id", s5);
        addField(TYPE_LOG, "监控时间", "monitor_time", s5);
        addField(TYPE_LOG, "服务名称", "service_name", s5);
        addField(TYPE_LOG, "实例ID", "instance_id", s5);
        addField(TYPE_LOG, "CPU使用率", "cpu_usage", s5);
        addField(TYPE_LOG, "内存使用率", "memory_usage", s5);
        addField(TYPE_LOG, "每秒请求数", "qps", s5);
        addField(TYPE_LOG, "错误率", "error_rate", s5);
        addField(TYPE_LOG, "健康状态", "health_status", s5);

        String s6 = "异常告警日志";
        addField(TYPE_LOG, "告警ID", "alert_id", s6);
        addField(TYPE_LOG, "告警时间", "alert_time", s6);
        addField(TYPE_LOG, "告警类型", "alert_type", s6);
        addField(TYPE_LOG, "告警级别", "alert_level", s6);
        addField(TYPE_LOG, "告警内容", "alert_message", s6);
        addField(TYPE_LOG, "影响服务", "affected_service", s6);
        addField(TYPE_LOG, "处理状态", "handle_status", s6);
        addField(TYPE_LOG, "处理人", "handler", s6);
        addField(TYPE_LOG, "处理时间", "handle_time", s6);
    }

    private static void initBusinessAliases() {
        FIELD_ALIAS_MAP.put("项目名称", "project_name");
        FIELD_ALIAS_MAP.put("工程项目名称", "project_name");
        FIELD_ALIAS_MAP.put("建设项目名称", "project_name");
        FIELD_ALIAS_MAP.put("地址", "project_address");
        FIELD_ALIAS_MAP.put("工程地点", "project_address");
        FIELD_ALIAS_MAP.put("建设地点", "project_address");
        FIELD_ALIAS_MAP.put("项目地址", "project_address");
        FIELD_ALIAS_MAP.put("甲方", "contract_employer");
        FIELD_ALIAS_MAP.put("发包方", "contract_employer");
        FIELD_ALIAS_MAP.put("发包人", "contract_employer");
        FIELD_ALIAS_MAP.put("合同甲方", "contract_employer");
        FIELD_ALIAS_MAP.put("乙方", "contract_contractor");
        FIELD_ALIAS_MAP.put("承包方", "contract_contractor");
        FIELD_ALIAS_MAP.put("承包人", "contract_contractor");
        FIELD_ALIAS_MAP.put("合同乙方", "contract_contractor");
        FIELD_ALIAS_MAP.put("附件链接", "digital_file_relation");
        FIELD_ALIAS_MAP.put("附件地址", "digital_file_relation");
        FIELD_ALIAS_MAP.put("文件链接", "digital_file_relation");
        FIELD_ALIAS_MAP.put("文件地址", "digital_file_relation");
        FIELD_ALIAS_MAP.put("电子文件链接", "digital_file_relation");
        FIELD_ALIAS_MAP.put("电子文件地址", "digital_file_relation");
        FIELD_ALIAS_MAP.put("extra_attachment_url", "digital_file_relation");
        FIELD_ALIAS_MAP.put("attachment_url", "digital_file_relation");
        FIELD_ALIAS_MAP.put("file_url", "digital_file_relation");
        FIELD_ALIAS_MAP.put("经度", "x_coordinate");
        FIELD_ALIAS_MAP.put("纬度", "y_coordinate");
        FIELD_ALIAS_MAP.put("X坐标", "x_coordinate");
        FIELD_ALIAS_MAP.put("Y坐标", "y_coordinate");
        FIELD_ALIAS_MAP.put("横坐标", "x_coordinate");
        FIELD_ALIAS_MAP.put("纵坐标", "y_coordinate");
        FIELD_ALIAS_MAP.put("高程", "z_coordinate");
        FIELD_ALIAS_MAP.put("质量评分", "quality_evaluation_result");
        FIELD_ALIAS_MAP.put("质量评价", "quality_evaluation_result");
        FIELD_ALIAS_MAP.put("质量评定", "quality_evaluation_result");
        FIELD_ALIAS_MAP.put("验收质量评价", "quality_evaluation_result");
        FIELD_ALIAS_MAP.put("坐落位置", "real_estate_location");
        FIELD_ALIAS_MAP.put("project name", "project_name");
        FIELD_ALIAS_MAP.put("project address", "project_address");
        FIELD_ALIAS_MAP.put("building name", "building_name");
        FIELD_ALIAS_MAP.put("contract name", "contract_title");
        FIELD_ALIAS_MAP.put("contract no", "contract_no");
        FIELD_ALIAS_MAP.put("sign date", "sign_date");
        FIELD_ALIAS_MAP.put("acceptance date", "acceptance_date");
    }

    private static void initExtraOnlyFields() {
        EXTRA_ONLY_FIELDS.add("备注");
        EXTRA_ONLY_FIELDS.add("说明");
        EXTRA_ONLY_FIELDS.add("备注说明");
    }

    private static void rebuildIndexes() {
        CN_MAP.clear();
        EN_MAP.clear();
        CN_MAP_BY_TYPE.clear();
        EN_MAP_BY_TYPE.clear();
        CN_LOWER_MAP_BY_TYPE.clear();
        EN_LOWER_MAP_BY_TYPE.clear();
        ALIAS_MAP_LOWER.clear();
        ALIAS_MAP_LOWER_BY_TYPE.clear();

        for (FieldEntry fe : ALL_FIELDS) {
            putFieldIndex(CN_MAP_BY_TYPE, fe.dataType, fe.cnName, fe);
            putFieldIndex(EN_MAP_BY_TYPE, fe.dataType, fe.enName, fe);
            putFieldIndex(CN_LOWER_MAP_BY_TYPE, fe.dataType, lowerKey(fe.cnName), fe);
            putFieldIndex(EN_LOWER_MAP_BY_TYPE, fe.dataType, lowerKey(fe.enName), fe);
            putFieldIndex(CN_LOWER_MAP_BY_TYPE, fe.dataType, noSpaceLowerKey(fe.cnName), fe);
            putFieldIndex(EN_LOWER_MAP_BY_TYPE, fe.dataType, noSpaceLowerKey(fe.enName), fe);

            if (TYPE_BUSINESS.equals(fe.dataType)) {
                if (fe.cnName != null && !fe.cnName.isEmpty()) CN_MAP.put(fe.cnName, fe);
                if (fe.enName != null && !fe.enName.isEmpty()) EN_MAP.put(fe.enName, fe);
            }
        }

        Map<String, String> businessAlias = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : FIELD_ALIAS_MAP.entrySet()) {
            String lower = lowerKey(e.getKey());
            String noSpace = noSpaceLowerKey(e.getKey());
            businessAlias.put(lower, e.getValue());
            ALIAS_MAP_LOWER.put(lower, e.getValue());
            businessAlias.put(noSpace, e.getValue());
            ALIAS_MAP_LOWER.put(noSpace, e.getValue());
        }
        ALIAS_MAP_LOWER_BY_TYPE.put(TYPE_BUSINESS, businessAlias);
    }

    private static void putFieldIndex(Map<String, Map<String, FieldEntry>> outer,
                                      String dataType,
                                      String key,
                                      FieldEntry fe) {
        if (key == null || key.isEmpty()) return;
        Map<String, FieldEntry> inner = outer.get(dataType);
        if (inner == null) {
            inner = new LinkedHashMap<>();
            outer.put(dataType, inner);
        }
        inner.put(key, fe);
    }

    private static String lowerKey(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private static String noSpaceLowerKey(String s) {
        return lowerKey(s).replaceAll("\\s+", "");
    }

    private static String normalizeDataType(String dataType) {
        if (dataType == null || dataType.trim().isEmpty()) return TYPE_BUSINESS;
        String trimmed = dataType.trim();
        DataTypeConfig byType = DATA_TYPE_CONFIGS_BY_TYPE.get(trimmed);
        if (byType != null) return byType.dataType;
        DataTypeConfig byKey = DATA_TYPE_CONFIGS_BY_KEY.get(trimmed);
        return byKey != null ? byKey.dataType : TYPE_BUSINESS;
    }

    public static boolean isValidTypeKey(String typeKey) {
        return typeKey != null && DATA_TYPE_CONFIGS_BY_KEY.containsKey(typeKey.trim());
    }

    public static String getDataTypeByKey(String typeKey) {
        if (typeKey == null || typeKey.trim().isEmpty()) return TYPE_BUSINESS;
        DataTypeConfig config = DATA_TYPE_CONFIGS_BY_KEY.get(typeKey.trim());
        return config != null ? config.dataType : TYPE_BUSINESS;
    }

    public static String getTypeKeyByDataType(String dataType) {
        DataTypeConfig config = DATA_TYPE_CONFIGS_BY_TYPE.get(normalizeDataType(dataType));
        return config != null ? config.typeKey : KEY_BUSINESS;
    }

    public static String getTypeTitle(String dataType) {
        DataTypeConfig config = DATA_TYPE_CONFIGS_BY_TYPE.get(normalizeDataType(dataType));
        return config != null ? config.pageTitle : DATA_TYPE_CONFIGS_BY_KEY.get(KEY_BUSINESS).pageTitle;
    }

    public static String getPageTitleByKey(String typeKey) {
        return getTypeTitle(getDataTypeByKey(typeKey));
    }

    public static String getRecordLabel(String dataType) {
        DataTypeConfig config = DATA_TYPE_CONFIGS_BY_TYPE.get(normalizeDataType(dataType));
        return config != null ? config.recordLabel : "数据记录";
    }

    public static String getBasePath(String typeKey) {
        String key = isValidTypeKey(typeKey) ? typeKey.trim() : KEY_BUSINESS;
        return "/data-elements/" + key;
    }

    public static List<DataTypeConfig> getDataTypeConfigs() {
        return new ArrayList<DataTypeConfig>(DATA_TYPE_CONFIGS_BY_KEY.values());
    }

    public static String[] getAllDataTypes() {
        List<String> types = new ArrayList<String>();
        for (DataTypeConfig config : DATA_TYPE_CONFIGS_BY_KEY.values()) {
            types.add(config.dataType);
        }
        return types.toArray(new String[types.size()]);
    }

    public static String[] getSubtypeList(String dataType) {
        String[] subtypes = SUBTYPE_LIST_BY_TYPE.get(normalizeDataType(dataType));
        if (subtypes == null) subtypes = SUBTYPE_LIST;
        return subtypes.clone();
    }

    public static Map<String, String> getSubtypeTableMap(String dataType) {
        Map<String, String> map = SUBTYPE_TABLE_MAP_BY_TYPE.get(normalizeDataType(dataType));
        if (map == null) map = SUBTYPE_TABLE_MAP;
        return new LinkedHashMap<String, String>(map);
    }

    public static String getDefaultSubtype(String dataType) {
        String[] subtypes = getSubtypeList(dataType);
        return subtypes.length > 0 ? subtypes[0] : "";
    }

    public static Map<String, List<String>> getSubtypeFieldsMap() {
        return getSubtypeFieldsMap(TYPE_BUSINESS);
    }

    public static Map<String, List<String>> getSubtypeFieldsMap(String dataType) {
        String normalizedDataType = normalizeDataType(dataType);
        Map<String, List<String>> cached = subtypeFieldCacheByType.get(normalizedDataType);
        if (cached != null) return cached;

        Map<String, List<String>> result = new LinkedHashMap<>();
        String[] subtypes = getSubtypeList(normalizedDataType);
        for (String st : subtypes) {
            result.put(st, new ArrayList<String>());
        }
        for (FieldEntry fe : ALL_FIELDS) {
            if (!normalizedDataType.equals(fe.dataType)) continue;
            List<String> list = result.get(fe.subtype);
            if (list != null && fe.enName != null && !fe.enName.isEmpty()
                    && !list.contains(fe.enName)) {
                list.add(fe.enName);
            }
        }
        subtypeFieldCacheByType.put(normalizedDataType, result);
        if (TYPE_BUSINESS.equals(normalizedDataType)) subtypeFieldCache = result;
        return result;
    }

    public static Map<String, List<String[]>> getSubtypeFieldDisplay() {
        return getSubtypeFieldDisplay(TYPE_BUSINESS);
    }

    public static Map<String, List<String[]>> getSubtypeFieldDisplay(String dataType) {
        String normalizedDataType = normalizeDataType(dataType);
        Map<String, List<String[]>> result = new LinkedHashMap<>();
        String[] subtypes = getSubtypeList(normalizedDataType);
        for (String st : subtypes) {
            result.put(st, new ArrayList<String[]>());
        }
        for (FieldEntry fe : ALL_FIELDS) {
            if (!normalizedDataType.equals(fe.dataType)) continue;
            List<String[]> list = result.get(fe.subtype);
            if (list != null) {
                list.add(new String[]{fe.cnName, fe.enName});
            }
        }
        return result;
    }

    public static String normalizeHeader(String header) {
        if (header == null) return "";
        String s = header.trim();
        s = s.replaceAll("[（(][^）)]*[）)]", "").trim();
        s = s.replaceAll("[:：]", "").trim();
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    public static String normalizeHeaderForMatch(String header) {
        String s = normalizeHeader(header);
        return s.toLowerCase();
    }

    public static boolean isExtraOnlyField(String normalizedHeader) {
        if (normalizedHeader == null) return false;
        return EXTRA_ONLY_FIELDS.contains(normalizedHeader);
    }

    public static FieldEntry matchField(String header) {
        return matchField(header, TYPE_BUSINESS);
    }

    public static FieldEntry matchField(String header, String dataType) {
        if (header == null || header.trim().isEmpty()) return null;

        String normalized = normalizeHeader(header);
        if (normalized.isEmpty()) return null;

        String normalizedDataType = normalizeDataType(dataType);
        String lower = normalized.toLowerCase();

        FieldEntry fe = tryMatch(normalized, normalizedDataType);
        if (fe != null) return fe;

        fe = tryMatch(lower, normalizedDataType);
        if (fe != null) return fe;

        Map<String, String> aliasMap = ALIAS_MAP_LOWER_BY_TYPE.get(normalizedDataType);
        if (aliasMap != null) {
            String aliasTarget = aliasMap.get(lower);
            if (aliasTarget != null) {
                fe = tryMatch(aliasTarget, normalizedDataType);
                if (fe != null) return fe;
            }
        }

        String noSpace = lower.replaceAll("\\s+", "");
        fe = tryMatch(noSpace, normalizedDataType);
        if (fe != null) return fe;

        if (aliasMap != null) {
            String aliasTarget = aliasMap.get(noSpace);
            if (aliasTarget != null) {
                fe = tryMatch(aliasTarget, normalizedDataType);
                if (fe != null) return fe;
            }
        }

        return null;
    }

    private static FieldEntry tryMatch(String name, String dataType) {
        if (name == null || name.isEmpty()) return null;
        String normalizedDataType = normalizeDataType(dataType);
        FieldEntry fe = lookup(CN_MAP_BY_TYPE, normalizedDataType, name);
        if (fe != null) return fe;
        fe = lookup(EN_MAP_BY_TYPE, normalizedDataType, name);
        if (fe != null) return fe;
        fe = lookup(CN_LOWER_MAP_BY_TYPE, normalizedDataType, name);
        if (fe != null) return fe;
        return lookup(EN_LOWER_MAP_BY_TYPE, normalizedDataType, name);
    }

    private static FieldEntry lookup(Map<String, Map<String, FieldEntry>> outer, String dataType, String key) {
        Map<String, FieldEntry> inner = outer.get(dataType);
        return inner != null ? inner.get(key) : null;
    }

    public static List<String> getFieldsBySubtype(String subtype) {
        return getFieldsBySubtype(TYPE_BUSINESS, subtype);
    }

    public static List<String> getFieldsBySubtype(String dataType, String subtype) {
        String normalizedDataType = normalizeDataType(dataType);
        List<String> result = new ArrayList<>();
        for (FieldEntry fe : ALL_FIELDS) {
            if (normalizedDataType.equals(fe.dataType) && fe.subtype.equals(subtype)
                    && fe.enName != null && !fe.enName.isEmpty()
                    && !result.contains(fe.enName)) {
                result.add(fe.enName);
            }
        }
        return result;
    }

    public static List<String[]> getFieldDisplayBySubtype(String subtype) {
        return getFieldDisplayBySubtype(TYPE_BUSINESS, subtype);
    }

    public static List<String[]> getFieldDisplayBySubtype(String dataType, String subtype) {
        String normalizedDataType = normalizeDataType(dataType);
        List<String[]> result = new ArrayList<>();
        for (FieldEntry fe : ALL_FIELDS) {
            if (normalizedDataType.equals(fe.dataType) && fe.subtype.equals(subtype)) {
                result.add(new String[]{fe.cnName, fe.enName});
            }
        }
        return result;
    }

    public static String getCnName(String enName) {
        return getCnName(TYPE_BUSINESS, enName);
    }

    public static String getCnName(String dataType, String enName) {
        if (enName == null) return enName;
        FieldEntry fe = tryMatch(enName, dataType);
        return fe != null ? fe.cnName : enName;
    }

    public static final String[] PARTY_TYPES = {
        "建设单位", "施工单位", "监理单位", "勘察单位", "设计单位"
    };

    private static final Map<String, String> PARTY_FIELD_MAP = new LinkedHashMap<>();
    static {
        PARTY_FIELD_MAP.put("建设单位", "建设单位");
        PARTY_FIELD_MAP.put("施工单位", "施工单位");
        PARTY_FIELD_MAP.put("监理单位", "监理单位");
        PARTY_FIELD_MAP.put("勘察单位", "勘察单位");
        PARTY_FIELD_MAP.put("设计单位", "设计单位");
        PARTY_FIELD_MAP.put("developer_name", "建设单位");
        PARTY_FIELD_MAP.put("construction_org_name", "施工单位");
        PARTY_FIELD_MAP.put("supervision_org_name", "监理单位");
        PARTY_FIELD_MAP.put("survey_org_name", "勘察单位");
        PARTY_FIELD_MAP.put("design_org_name", "设计单位");
    }

    public static Map<String, String> getPartyFieldMap() {
        return PARTY_FIELD_MAP;
    }

    public static boolean isPartyField(String fieldName) {
        return PARTY_FIELD_MAP.containsKey(fieldName);
    }
}
