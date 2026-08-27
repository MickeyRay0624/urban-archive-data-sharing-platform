package com.collaborative.sharing.config;

import com.collaborative.sharing.entity.DataElement;
import com.collaborative.sharing.entity.ServiceComponent;
import com.collaborative.sharing.entity.TodoItem;
import com.collaborative.sharing.mapper.DataElementMapper;
import com.collaborative.sharing.mapper.ServiceComponentMapper;
import com.collaborative.sharing.mapper.TodoItemMapper;
import com.collaborative.sharing.util.DataElementFieldDict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DataElementMapper dataElementMapper;
    
    @Autowired
    private ServiceComponentMapper serviceComponentMapper;
    
    @Autowired
    private TodoItemMapper todoItemMapper;

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        // 自动创建新表（如果不存在）
        autoCreateNewTables();
        autoEnsureUserAccounts();

        // 只在数据为空时才初始化示例数据
        try {
            // 检查是否已有数据，如果有则跳过初始化
            if (!dataElementMapper.findAll().isEmpty()
                    || !todoItemMapper.findAll().isEmpty()
                    || !serviceComponentMapper.findAll().isEmpty()) {
                refreshDemoDisplayData();
                System.out.println("数据库已有数据，已刷新页面演示服务组件和审批事项");
                return;
            }
            
            System.out.println("数据库为空，开始初始化示例数据...");
            
            // ======== 初始化服务组件 ========
            // 先清空旧的服务组件数据
            serviceComponentMapper.findAll().forEach(service -> serviceComponentMapper.deleteById(service.getId()));

            ServiceComponent service1 = new ServiceComponent();
            service1.setServiceName("房屋建筑档案查询服务");
            service1.setServiceCode("ARCHIVE_QUERY_SERVICE");
            service1.setDescription("提供房屋建筑城建档案数据的查询和检索功能，支持按工程编号、产权人、地址等多维度检索");
            service1.setVersion("2.1.0");
            service1.setApiUrl("/api/archive/query");
            service1.setOwnerDepartment("温州市城市建设档案馆");
            service1.setSecurityLevel("内部");
            service1.setAuthorizedUnits("不动产登记服务中心、住建局");
            service1.setStatus(ServiceComponent.ServiceStatus.AUTHORIZED);
            service1.setCreatedAt(LocalDateTime.now().minusDays(25));
            service1.setUpdatedAt(LocalDateTime.now().minusDays(25));
            serviceComponentMapper.insert(service1);

            ServiceComponent service2 = new ServiceComponent();
            service2.setServiceName("竣工图纸调阅服务");
            service2.setServiceCode("DRAWING_ACCESS_SERVICE");
            service2.setDescription("支持在线调阅建筑工程竣工图纸，提供图纸预览、下载、打印等功能");
            service2.setVersion("1.5.2");
            service2.setApiUrl("/api/archive/drawing");
            service2.setOwnerDepartment("温州市城市建设档案馆");
            service2.setSecurityLevel("敏感");
            service2.setAuthorizedUnits("住建局、应急管理局");
            service2.setStatus(ServiceComponent.ServiceStatus.PUBLISHED);
            service2.setCreatedAt(LocalDateTime.now().minusDays(15));
            service2.setUpdatedAt(LocalDateTime.now().minusDays(15));
            serviceComponentMapper.insert(service2);

            ServiceComponent service3 = new ServiceComponent();
            service3.setServiceName("产权建筑档案核验服务");
            service3.setServiceCode("PROPERTY_VERIFY_SERVICE");
            service3.setDescription("对产权建筑的档案数据进行核验比对，确保档案与实物一致性");
            service3.setVersion("3.0.1");
            service3.setApiUrl("/api/archive/property/verify");
            service3.setOwnerDepartment("温州市城市建设档案馆");
            service3.setSecurityLevel("重要");
            service3.setAuthorizedUnits("不动产登记服务中心");
            service3.setStatus(ServiceComponent.ServiceStatus.PENDING_APPROVAL);
            service3.setCreatedAt(LocalDateTime.now().minusDays(10));
            service3.setUpdatedAt(LocalDateTime.now().minusDays(10));
            serviceComponentMapper.insert(service3);

            // ======== 初始化数据要素 ========
            DataElement data1 = new DataElement();
            data1.setDataName("房屋建筑工程基本信息");
            data1.setDataType("业务数据");
            data1.setDataSubtype("工程基本信息");
            data1.setSecurityLevel("内部");
            data1.setDataVersion("V1.0");
            data1.setSourceSystem("城建档案综合管理系统");
            data1.setDescription("包含房屋建筑工程的立项、规划、施工许可等基本信息");
            data1.setStatus(DataElement.DataStatus.NORMAL);
            data1.setUploader("admin");
            data1.setDepartment("温州市城市建设档案馆");
            data1.setUploadTime(LocalDateTime.now().minusDays(30));
            data1.setSummary("用于建筑工程基本信息查询和管理");
            data1.setCharacterCount(50);
            data1.setCreatedAt(LocalDateTime.now().minusDays(30));
            data1.setUpdatedAt(LocalDateTime.now().minusDays(30));
            dataElementMapper.insert(data1);

            DataElement data2 = new DataElement();
            data2.setDataName("竣工图纸目录信息");
            data2.setDataType("业务数据");
            data2.setDataSubtype("竣工图纸信息");
            data2.setSecurityLevel("敏感");
            data2.setDataVersion("V1.0");
            data2.setSourceSystem("城建档案综合管理系统");
            data2.setDescription("建筑工程竣工图纸的目录索引信息，包括图纸编号、图名、比例尺等");
            data2.setStatus(DataElement.DataStatus.NORMAL);
            data2.setUploader("admin");
            data2.setDepartment("温州市城市建设档案馆");
            data2.setUploadTime(LocalDateTime.now().minusDays(20));
            data2.setSummary("竣工图纸目录查询和调阅");
            data2.setCharacterCount(45);
            data2.setCreatedAt(LocalDateTime.now().minusDays(20));
            data2.setUpdatedAt(LocalDateTime.now().minusDays(20));
            dataElementMapper.insert(data2);

            DataElement data3 = new DataElement();
            data3.setDataName("城建档案字段元数据");
            data3.setDataType("业务元数据");
            data3.setDataSubtype("字段说明");
            data3.setSecurityLevel("内部");
            data3.setDataVersion("V1.0");
            data3.setSourceSystem("城建档案数据要素资源库");
            data3.setDescription("城建档案各数据字段的定义、格式、约束条件的详细说明文档");
            data3.setStatus(DataElement.DataStatus.NORMAL);
            data3.setUploader("admin");
            data3.setDepartment("温州市城市建设档案馆");
            data3.setUploadTime(LocalDateTime.now().minusDays(15));
            data3.setSummary("用于数据标准化管理和字段映射");
            data3.setCharacterCount(42);
            data3.setCreatedAt(LocalDateTime.now().minusDays(15));
            data3.setUpdatedAt(LocalDateTime.now().minusDays(15));
            dataElementMapper.insert(data3);

            DataElement data4 = new DataElement();
            data4.setDataName("服务组件调用实例信息");
            data4.setDataType("服务实例数据");
            data4.setDataSubtype("服务标识符");
            data4.setSecurityLevel("内部");
            data4.setDataVersion("V1.0");
            data4.setSourceSystem("协同共享服务组件系统");
            data4.setDescription("各服务组件在线调用实例的标识符与配置信息记录");
            data4.setStatus(DataElement.DataStatus.NORMAL);
            data4.setUploader("admin");
            data4.setDepartment("温州市城市建设档案馆");
            data4.setUploadTime(LocalDateTime.now().minusDays(10));
            data4.setSummary("服务调用实例的标识符查询");
            data4.setCharacterCount(38);
            data4.setCreatedAt(LocalDateTime.now().minusDays(10));
            data4.setUpdatedAt(LocalDateTime.now().minusDays(10));
            dataElementMapper.insert(data4);

            DataElement data5 = new DataElement();
            data5.setDataName("服务访问日志");
            data5.setDataType("日志数据");
            data5.setDataSubtype("访问日志");
            data5.setSecurityLevel("重要");
            data5.setDataVersion("V1.0");
            data5.setSourceSystem("协同共享服务组件系统");
            data5.setDescription("记录所有服务组件的访问请求、响应时间、操作人等信息");
            data5.setStatus(DataElement.DataStatus.PENDING_REVIEW);
            data5.setUploader("admin");
            data5.setDepartment("温州市城市建设档案馆");
            data5.setUploadTime(LocalDateTime.now().minusDays(5));
            data5.setSummary("用于审计和安全监控");
            data5.setCharacterCount(40);
            data5.setCreatedAt(LocalDateTime.now().minusDays(5));
            data5.setUpdatedAt(LocalDateTime.now().minusDays(5));
            dataElementMapper.insert(data5);

            // ======== 初始化待办事项 ========
            todoItemMapper.findAll().forEach(todo -> todoItemMapper.deleteById(todo.getId()));
            TodoItem todo1 = new TodoItem();
            todo1.setUploader("物业办");
            todo1.setUploadTime(LocalDateTime.now().minusDays(2));
            todo1.setDepartment("物业办");
            todo1.setProcessName("房屋建筑档案查询服务调用申请");
            todo1.setProcessPurpose("申请调用房屋建筑档案查询服务，用于物业管理事项核验。");
            todo1.setStatus(TodoItem.TodoStatus.PENDING);
            todo1.setCreatedAt(LocalDateTime.now().minusDays(2));
            todo1.setUpdatedAt(LocalDateTime.now().minusDays(2));
            todoItemMapper.insert(todo1);

            TodoItem todo2 = new TodoItem();
            todo2.setUploader("质安科");
            todo2.setUploadTime(LocalDateTime.now().minusDays(3));
            todo2.setDepartment("质安科");
            todo2.setProcessName("竣工图纸调阅服务授权申请");
            todo2.setProcessPurpose("申请调阅竣工图纸目录及图纸预览服务，用于质量安全监管。");
            todo2.setStatus(TodoItem.TodoStatus.APPROVED);
            todo2.setReviewer("admin");
            todo2.setReviewTime(LocalDateTime.now().minusDays(2));
            todo2.setReviewComment("审批通过，同意开通竣工图纸调阅服务。");
            todo2.setCreatedAt(LocalDateTime.now().minusDays(3));
            todo2.setUpdatedAt(LocalDateTime.now().minusDays(2));
            todoItemMapper.insert(todo2);

            TodoItem todo3 = new TodoItem();
            todo3.setUploader("不动产");
            todo3.setUploadTime(LocalDateTime.now().minusDays(1));
            todo3.setDepartment("不动产");
            todo3.setProcessName("产权建筑档案核验服务授权申请");
            todo3.setProcessPurpose("申请调用产权建筑档案核验服务，用于不动产登记业务比对。");
            todo3.setStatus(TodoItem.TodoStatus.PENDING);
            todo3.setCreatedAt(LocalDateTime.now().minusDays(1));
            todo3.setUpdatedAt(LocalDateTime.now().minusDays(1));
            todoItemMapper.insert(todo3);

            TodoItem todo4 = new TodoItem();
            todo4.setUploader("物业公司");
            todo4.setUploadTime(LocalDateTime.now().minusDays(4));
            todo4.setDepartment("物业公司");
            todo4.setProcessName("物业维修档案查询服务申请");
            todo4.setProcessPurpose("申请查询小区维修相关档案信息，用于物业维修工单核验。");
            todo4.setStatus(TodoItem.TodoStatus.APPROVED);
            todo4.setReviewer("admin");
            todo4.setReviewTime(LocalDateTime.now().minusDays(3));
            todo4.setReviewComment("审批通过，同意开通物业维修档案查询服务。");
            todo4.setCreatedAt(LocalDateTime.now().minusDays(4));
            todo4.setUpdatedAt(LocalDateTime.now().minusDays(3));
            todoItemMapper.insert(todo4);

            TodoItem todo5 = new TodoItem();
            todo5.setUploader("应急办");
            todo5.setUploadTime(LocalDateTime.now().minusHours(12));
            todo5.setDepartment("应急办");
            todo5.setProcessName("应急处置建筑图纸调阅申请");
            todo5.setProcessPurpose("申请在应急处置场景下调阅重点建筑图纸及基础档案。");
            todo5.setStatus(TodoItem.TodoStatus.PENDING);
            todo5.setCreatedAt(LocalDateTime.now().minusHours(12));
            todo5.setUpdatedAt(LocalDateTime.now().minusHours(12));
            todoItemMapper.insert(todo5);

            TodoItem todo6 = new TodoItem();
            todo6.setUploader("公安系统");
            todo6.setUploadTime(LocalDateTime.now().minusHours(6));
            todo6.setDepartment("公安系统");
            todo6.setProcessName("重点场所档案核验接口申请");
            todo6.setProcessPurpose("申请调用重点场所档案核验接口，用于安全管理场景。");
            todo6.setStatus(TodoItem.TodoStatus.REJECTED);
            todo6.setReviewer("admin");
            todo6.setReviewTime(LocalDateTime.now().minusHours(2));
            todo6.setReviewComment("申请材料中的调用范围不够明确，请补充后重新提交。");
            todo6.setCreatedAt(LocalDateTime.now().minusHours(6));
            todo6.setUpdatedAt(LocalDateTime.now().minusHours(2));
            todoItemMapper.insert(todo6);

            refreshDemoDisplayData();
            
            System.out.println("示例数据初始化成功！");
        } catch (Exception e) {
            // 忽略初始化错误，继续启动应用
            System.err.println("数据初始化失败: " + e.getMessage());
        }
    }

    private void refreshDemoDisplayData() {
        try {
            refreshDemoServiceComponents();
            refreshDemoTodoItems();
        } catch (Exception e) {
            System.err.println("刷新演示服务组件和审批事项失败: " + e.getMessage());
        }
    }

    private void refreshDemoServiceComponents() {
        Set<String> demoCodes = new HashSet<>(Arrays.asList(
                "ARCHIVE_QUERY_SERVICE",
                "DRAWING_ACCESS_SERVICE",
                "PROPERTY_VERIFY_SERVICE",
                "PROPERTY_MAINTENANCE_SERVICE",
                "EMERGENCY_DRAWING_SERVICE",
                "PUBLIC_SECURITY_VERIFY_SERVICE"
        ));
        Set<String> demoNames = new HashSet<>(Arrays.asList(
                "房屋建筑档案查询服务",
                "竣工图纸调阅服务",
                "产权建筑档案核验服务",
                "物业维修档案查询服务",
                "应急处置图纸调阅服务",
                "重点场所档案核验接口",
                "数据查询服务",
                "数据分析服务",
                "数据导入服务"
        ));

        for (ServiceComponent service : serviceComponentMapper.findAll()) {
            String code = service.getServiceCode() != null ? service.getServiceCode().trim().toUpperCase() : "";
            String name = service.getServiceName() != null ? service.getServiceName().trim() : "";
            String description = service.getDescription() != null ? service.getDescription() : "";
            if (demoCodes.contains(code)
                    || demoNames.contains(name)
                    || code.contains("IMPORT")
                    || name.contains("数据导入")
                    || description.contains("数据导入")) {
                serviceComponentMapper.deleteById(service.getId());
            }
        }

        insertDemoService(
                "房屋建筑档案查询服务",
                "ARCHIVE_QUERY_SERVICE",
                "面向物业办、物业公司、不动产等部门提供房屋建筑档案查询与核验能力。",
                "2.1.0",
                "/api/archive/query",
                "物业办",
                "内部",
                "物业办、物业公司、不动产",
                ServiceComponent.ServiceStatus.AUTHORIZED,
                "用于物业管理、产权登记等场景的档案查询。",
                18
        );
        insertDemoService(
                "竣工图纸调阅服务",
                "DRAWING_ACCESS_SERVICE",
                "面向质安科、应急办、公安系统提供竣工图纸目录、预览和调阅能力。",
                "1.5.2",
                "/api/archive/drawing",
                "质安科",
                "敏感",
                "质安科、应急办、公安系统",
                ServiceComponent.ServiceStatus.PUBLISHED,
                "用于质量安全监管、应急处置和重点场所核验。",
                15
        );
        insertDemoService(
                "产权建筑档案核验服务",
                "PROPERTY_VERIFY_SERVICE",
                "面向不动产部门提供产权建筑档案核验和登记信息比对能力。",
                "3.0.1",
                "/api/archive/property/verify",
                "不动产",
                "重要",
                "不动产、公安系统",
                ServiceComponent.ServiceStatus.AUTHORIZED,
                "用于不动产登记业务中的档案一致性核验。",
                11
        );
        insertDemoService(
                "物业维修档案查询服务",
                "PROPERTY_MAINTENANCE_SERVICE",
                "面向物业公司提供小区维修相关档案查询能力，辅助维修工单核验。",
                "1.0.0",
                "/api/archive/property-maintenance/query",
                "物业公司",
                "内部",
                "物业公司、物业办",
                ServiceComponent.ServiceStatus.PENDING_APPROVAL,
                "物业公司提交的服务申请，等待城建档案局审批。",
                8
        );
        insertDemoService(
                "应急处置图纸调阅服务",
                "EMERGENCY_DRAWING_SERVICE",
                "面向应急办提供重点建筑图纸和基础档案快速调阅能力。",
                "1.2.0",
                "/api/archive/emergency/drawing",
                "应急办",
                "重要",
                "应急办、公安系统",
                ServiceComponent.ServiceStatus.PUBLISHED,
                "用于应急处置场景下的建筑空间信息调阅。",
                6
        );
        insertDemoService(
                "重点场所档案核验接口",
                "PUBLIC_SECURITY_VERIFY_SERVICE",
                "面向公安系统提供重点场所建筑档案核验接口。",
                "1.1.0",
                "/api/archive/security-place/verify",
                "公安系统",
                "敏感",
                "公安系统",
                ServiceComponent.ServiceStatus.DRAFT,
                "公安系统草稿申请，可提交给城建档案局审批。",
                3
        );
    }

    private void insertDemoService(String serviceName, String serviceCode, String description,
                                   String version, String apiUrl, String ownerDepartment,
                                   String securityLevel, String authorizedUnits,
                                   ServiceComponent.ServiceStatus status, String remark, int daysAgo) {
        LocalDateTime time = LocalDateTime.now().minusDays(daysAgo);
        ServiceComponent service = new ServiceComponent();
        service.setServiceName(serviceName);
        service.setServiceCode(serviceCode);
        service.setDescription(description);
        service.setVersion(version);
        service.setApiUrl(apiUrl);
        service.setOwnerDepartment(ownerDepartment);
        service.setSecurityLevel(securityLevel);
        service.setAuthorizedUnits(authorizedUnits);
        service.setStatus(status);
        service.setRemark(remark);
        service.setCreatedAt(time);
        service.setUpdatedAt(time);
        serviceComponentMapper.insert(service);
    }

    private void refreshDemoTodoItems() {
        Set<String> demoProcessNames = new HashSet<>(Arrays.asList(
                "房屋建筑档案查询服务调用申请",
                "竣工图纸调阅服务授权申请",
                "产权建筑档案核验服务授权申请",
                "物业维修档案查询服务申请",
                "应急处置建筑图纸调阅申请",
                "重点场所档案核验接口申请",
                "建筑结构审查流程",
                "电气系统改造申请",
                "管道维修申请",
                "档案数据整理流程",
                "配电设施检修",
                "排水系统升级"
        ));

        for (TodoItem todo : todoItemMapper.findAll()) {
            if (demoProcessNames.contains(todo.getProcessName())) {
                todoItemMapper.deleteById(todo.getId());
            }
        }

        insertDemoTodo("物业办", "物业办", "房屋建筑档案查询服务调用申请",
                "申请调用房屋建筑档案查询服务，用于物业管理事项核验。",
                TodoItem.TodoStatus.PENDING, null, null, 2);
        insertDemoTodo("质安科", "质安科", "竣工图纸调阅服务授权申请",
                "申请调阅竣工图纸目录及图纸预览服务，用于质量安全监管。",
                TodoItem.TodoStatus.APPROVED, "admin", "审批通过，同意开通竣工图纸调阅服务。", 3);
        insertDemoTodo("不动产", "不动产", "产权建筑档案核验服务授权申请",
                "申请调用产权建筑档案核验服务，用于不动产登记业务比对。",
                TodoItem.TodoStatus.PENDING, null, null, 1);
        insertDemoTodo("物业公司", "物业公司", "物业维修档案查询服务申请",
                "申请查询小区维修相关档案信息，用于物业维修工单核验。",
                TodoItem.TodoStatus.APPROVED, "admin", "审批通过，同意开通物业维修档案查询服务。", 4);
        insertDemoTodo("应急办", "应急办", "应急处置建筑图纸调阅申请",
                "申请在应急处置场景下调阅重点建筑图纸及基础档案。",
                TodoItem.TodoStatus.PENDING, null, null, 0);
        insertDemoTodo("公安系统", "公安系统", "重点场所档案核验接口申请",
                "申请调用重点场所档案核验接口，用于安全管理场景。",
                TodoItem.TodoStatus.REJECTED, "admin", "申请材料中的调用范围不够明确，请补充后重新提交。", 0);
    }

    private void insertDemoTodo(String uploader, String department, String processName,
                                String processPurpose, TodoItem.TodoStatus status,
                                String reviewer, String reviewComment, int daysAgo) {
        LocalDateTime uploadTime = daysAgo > 0 ? LocalDateTime.now().minusDays(daysAgo) : LocalDateTime.now().minusHours(6);
        TodoItem todo = new TodoItem();
        todo.setUploader(uploader);
        todo.setUploadTime(uploadTime);
        todo.setDepartment(department);
        todo.setProcessName(processName);
        todo.setProcessPurpose(processPurpose);
        todo.setStatus(status);
        todo.setReviewer(reviewer);
        if (reviewer != null) {
            todo.setReviewTime(uploadTime.plusHours(6));
            todo.setReviewComment(reviewComment);
        }
        todo.setCreatedAt(uploadTime);
        todo.setUpdatedAt(reviewer != null ? uploadTime.plusHours(6) : uploadTime);
        todoItemMapper.insert(todo);
    }

    private void autoEnsureUserAccounts() {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "username VARCHAR(50) NOT NULL UNIQUE,"
                    + "password VARCHAR(255) NOT NULL,"
                    + "real_name VARCHAR(100),"
                    + "department VARCHAR(100) DEFAULT '城建档案局' COMMENT '所属部门',"
                    + "user_role VARCHAR(50) DEFAULT 'ARCHIVE_ADMIN' COMMENT '账号角色',"
                    + "created_at DATETIME,"
                    + "updated_at DATETIME"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表'");

            String[] userAlterStatements = {
                "ALTER TABLE users ADD COLUMN department VARCHAR(100) DEFAULT '城建档案局' COMMENT '所属部门'",
                "ALTER TABLE users ADD COLUMN user_role VARCHAR(50) DEFAULT 'ARCHIVE_ADMIN' COMMENT '账号角色'"
            };
            for (String alterSql : userAlterStatements) {
                try {
                    jdbc.execute(alterSql);
                } catch (Exception ignored) {
                    // 列已存在时忽略
                }
            }

            jdbc.update("INSERT INTO users (username, password, real_name, department, user_role, created_at, updated_at) "
                    + "VALUES ('admin', 'admin', '系统管理员', '城建档案局', 'ARCHIVE_ADMIN', NOW(), NOW()) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "real_name = VALUES(real_name), department = VALUES(department), user_role = VALUES(user_role), updated_at = NOW()");

            seedExternalDepartmentUser(jdbc, "wyb", "物业办");
            seedExternalDepartmentUser(jdbc, "zak", "质安科");
            seedExternalDepartmentUser(jdbc, "bdc", "不动产");
            seedExternalDepartmentUser(jdbc, "wygs", "物业公司");
            seedExternalDepartmentUser(jdbc, "yjb", "应急办");
            seedExternalDepartmentUser(jdbc, "gaj", "公安系统");

            System.out.println("用户账号角色自动创建/检查完成");
        } catch (Exception e) {
            System.err.println("自动创建用户账号角色失败: " + e.getMessage());
        }
    }

    private void seedExternalDepartmentUser(JdbcTemplate jdbc, String username, String department) {
        jdbc.update("INSERT INTO users (username, password, real_name, department, user_role, created_at, updated_at) "
                        + "VALUES (?, '123456', ?, ?, 'EXTERNAL_DEPARTMENT', NOW(), NOW()) "
                        + "ON DUPLICATE KEY UPDATE "
                        + "real_name = VALUES(real_name), department = VALUES(department), user_role = VALUES(user_role), updated_at = NOW()",
                username, department, department);
    }

    /**
     * 自动创建数据要素管理的新表（data_element_upload_batch 和 data_element_business_record）
     * 如果表已存在则跳过，不会影响现有数据。
     */
    private void autoCreateNewTables() {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

            // 创建上传批次表（完整字段）
            jdbc.execute("CREATE TABLE IF NOT EXISTS data_element_upload_batch ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型',"
                    + "batch_name VARCHAR(200) COMMENT '批次名称',"
                    + "original_file_name VARCHAR(500) COMMENT '原始文件名',"
                    + "stored_file_path VARCHAR(500) COMMENT '存储路径',"
                    + "file_hash VARCHAR(128) COMMENT '文件SHA-256哈希',"
                    + "file_size BIGINT COMMENT '文件大小（字节）',"
                    + "uploader VARCHAR(100) COMMENT '上传人',"
                    + "upload_time DATETIME COMMENT '上传时间',"
                    + "total_rows INT DEFAULT 0 COMMENT '数据行数',"
                    + "business_record_count INT DEFAULT 0 COMMENT '业务记录数',"
                    + "recognized_columns TEXT COMMENT '已识别字段列表',"
                    + "unrecognized_columns LONGTEXT COMMENT '扩展字段列详情JSON',"
                    + "involved_subtypes VARCHAR(500) COMMENT '涉及的数据子类',"
                    + "duplicate_of_batch_id BIGINT NULL COMMENT '重复文件的原始批次ID',"
                    + "import_policy VARCHAR(32) DEFAULT 'skip' COMMENT '导入策略: skip/force/replace',"
                    + "created_at DATETIME,"
                    + "updated_at DATETIME"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据要素上传批次表'");

            // 兼容已有表：添加缺失的新字段（用ALTER TABLE ADD COLUMN，捕获异常跳过已存在的列）
            String[] alterStatements = {
                "ALTER TABLE data_element_upload_batch ADD COLUMN data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型'",
                "ALTER TABLE data_element_upload_batch ADD COLUMN file_hash VARCHAR(128) COMMENT '文件SHA-256哈希'",
                "ALTER TABLE data_element_upload_batch ADD COLUMN file_size BIGINT COMMENT '文件大小'",
                "ALTER TABLE data_element_upload_batch ADD COLUMN business_record_count INT DEFAULT 0 COMMENT '业务记录数'",
                "ALTER TABLE data_element_upload_batch ADD COLUMN duplicate_of_batch_id BIGINT NULL COMMENT '重复文件的原始批次ID'",
                "ALTER TABLE data_element_upload_batch ADD COLUMN import_policy VARCHAR(32) DEFAULT 'skip' COMMENT '导入策略'",
                "ALTER TABLE data_element_upload_batch MODIFY COLUMN unrecognized_columns LONGTEXT COMMENT '扩展字段列详情JSON'"
            };
            for (String alterSql : alterStatements) {
                try {
                    jdbc.execute(alterSql);
                } catch (Exception ignored) {
                    // 列已存在或无法修改时忽略
                }
            }
            try {
                jdbc.update("UPDATE data_element_upload_batch SET data_type = '业务数据' WHERE data_type IS NULL OR data_type = ''");
            } catch (Exception ignored) {
                // 兼容旧库失败时继续启动
            }

            // 创建业务记录表
            jdbc.execute("CREATE TABLE IF NOT EXISTS data_element_business_record ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "batch_id BIGINT COMMENT '批次ID',"
                    + "data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型',"
                    + "data_subtype VARCHAR(100) COMMENT '数据子类（9类之一）',"
                    + "row_index INT COMMENT '原始Excel行号',"
                    + "project_id VARCHAR(50) COMMENT '关联工程ID',"
                    + "display_name VARCHAR(500) COMMENT '显示标题',"
                    + "record_json LONGTEXT COMMENT '识别后的字段和值',"
                    + "extra_json LONGTEXT COMMENT '未识别扩展字段',"
                    + "created_at DATETIME,"
                    + "updated_at DATETIME,"
                    + "KEY idx_batch_id (batch_id),"
                    + "KEY idx_data_type (data_type),"
                    + "KEY idx_data_type_subtype (data_type, data_subtype),"
                    + "KEY idx_data_subtype (data_subtype),"
                    + "KEY idx_project_id (project_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据要素业务记录表'");

            String[] recordAlterStatements = {
                "ALTER TABLE data_element_business_record ADD COLUMN data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型'",
                "ALTER TABLE data_element_business_record ADD INDEX idx_data_type (data_type)",
                "ALTER TABLE data_element_business_record ADD INDEX idx_data_type_subtype (data_type, data_subtype)"
            };
            for (String alterSql : recordAlterStatements) {
                try {
                    jdbc.execute(alterSql);
                } catch (Exception ignored) {
                    // 列或索引已存在时忽略
                }
            }
            try {
                jdbc.update("UPDATE data_element_business_record SET data_type = '业务数据' WHERE data_type IS NULL OR data_type = ''");
            } catch (Exception ignored) {
                // 兼容旧库失败时继续启动
            }

            // 创建数据子类配置表：用于主表行的增删、显示顺序与合规检查状态
            jdbc.execute("CREATE TABLE IF NOT EXISTS data_element_subtype_config ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型',"
                    + "data_subtype VARCHAR(100) NOT NULL COMMENT '数据子类',"
                    + "table_name VARCHAR(100) COMMENT '数据库表名',"
                    + "field_count INT DEFAULT 0 COMMENT '字段数',"
                    + "is_builtin TINYINT(1) DEFAULT 0 COMMENT '是否默认子类',"
                    + "is_visible TINYINT(1) DEFAULT 1 COMMENT '是否显示',"
                    + "sort_order INT DEFAULT 0 COMMENT '排序',"
                    + "self_check_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' COMMENT '合规自检状态',"
                    + "security_check_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' COMMENT '温州数安港检查状态',"
                    + "compliance_result TEXT COMMENT '最近合规检查结果',"
                    + "self_check_time DATETIME COMMENT '自检时间',"
                    + "security_check_time DATETIME COMMENT '数安港检查时间',"
                    + "created_at DATETIME,"
                    + "updated_at DATETIME,"
                    + "UNIQUE KEY uk_data_type_subtype (data_type, data_subtype)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据要素子类配置表'");

            String[] subtypeAlterStatements = {
                "ALTER TABLE data_element_subtype_config ADD COLUMN data_type VARCHAR(50) DEFAULT '业务数据' COMMENT '数据一级类型'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN table_name VARCHAR(100) COMMENT '数据库表名'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN field_count INT DEFAULT 0 COMMENT '字段数'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN is_builtin TINYINT(1) DEFAULT 0 COMMENT '是否默认子类'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN is_visible TINYINT(1) DEFAULT 1 COMMENT '是否显示'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN sort_order INT DEFAULT 0 COMMENT '排序'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN self_check_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' COMMENT '合规自检状态'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN security_check_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' COMMENT '温州数安港检查状态'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN compliance_result TEXT COMMENT '最近合规检查结果'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN self_check_time DATETIME COMMENT '自检时间'",
                "ALTER TABLE data_element_subtype_config ADD COLUMN security_check_time DATETIME COMMENT '数安港检查时间'"
            };
            for (String alterSql : subtypeAlterStatements) {
                try {
                    jdbc.execute(alterSql);
                } catch (Exception ignored) {
                    // 列或索引已存在时忽略
                }
            }
            try {
                jdbc.update("UPDATE data_element_subtype_config SET data_type = '业务数据' WHERE data_type IS NULL OR data_type = ''");
            } catch (Exception ignored) {
                // 兼容旧库失败时继续启动
            }
            try {
                jdbc.execute("ALTER TABLE data_element_subtype_config DROP INDEX uk_data_subtype");
            } catch (Exception ignored) {
                // 旧索引不存在时忽略
            }
            try {
                jdbc.execute("ALTER TABLE data_element_subtype_config ADD UNIQUE KEY uk_data_type_subtype (data_type, data_subtype)");
            } catch (Exception ignored) {
                // 联合索引已存在时忽略
            }

            seedDefaultSubtypeConfigs(jdbc);

            System.out.println("数据要素新表自动创建/检查完成");
        } catch (Exception e) {
            System.err.println("自动创建新表失败: " + e.getMessage() + "，请手动执行 init-new-tables.sql");
        }
    }

    private void seedDefaultSubtypeConfigs(JdbcTemplate jdbc) {
        String[] dataTypes = DataElementFieldDict.getAllDataTypes();
        for (String dataType : dataTypes) {
            Map<String, String> tableMap = DataElementFieldDict.getSubtypeTableMap(dataType);
            Map<String, List<String>> fieldsMap = DataElementFieldDict.getSubtypeFieldsMap(dataType);
            String[] subtypeList = DataElementFieldDict.getSubtypeList(dataType);
            for (int i = 0; i < subtypeList.length; i++) {
                String subtype = subtypeList[i];
                String tableName = tableMap.get(subtype);
                List<String> fields = fieldsMap.get(subtype);
                Integer fieldCount = fields != null ? fields.size() : 0;
                jdbc.update(
                        "INSERT INTO data_element_subtype_config "
                                + "(data_type, data_subtype, table_name, field_count, is_builtin, is_visible, sort_order, "
                                + "self_check_status, security_check_status, created_at, updated_at) "
                                + "VALUES (?, ?, ?, ?, 1, 1, ?, 'NOT_SUBMITTED', 'NOT_SUBMITTED', NOW(), NOW()) "
                                + "ON DUPLICATE KEY UPDATE "
                                + "table_name = VALUES(table_name), "
                                + "field_count = VALUES(field_count), "
                                + "is_builtin = 1, "
                                + "sort_order = VALUES(sort_order), "
                                + "updated_at = NOW()",
                        dataType, subtype, tableName, fieldCount, i + 1
                );
            }
        }
    }
}
