package com.collaborative.sharing.service;

import com.collaborative.sharing.entity.DataElementBusinessRecord;
import com.collaborative.sharing.entity.DataElementSubtypeConfig;
import com.collaborative.sharing.entity.DataElementUploadBatch;
import com.collaborative.sharing.mapper.DataElementBusinessRecordMapper;
import com.collaborative.sharing.mapper.DataElementSubtypeConfigMapper;
import com.collaborative.sharing.mapper.DataElementUploadBatchMapper;
import com.collaborative.sharing.mapper.SubtypeSummary;
import com.collaborative.sharing.util.DataElementFieldDict;
import com.collaborative.sharing.util.FileUploadUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataElementService {

    @Autowired
    private DataElementBusinessRecordMapper businessRecordMapper;

    @Autowired
    private DataElementUploadBatchMapper uploadBatchMapper;

    @Autowired
    private DataElementSubtypeConfigMapper subtypeConfigMapper;

    private String normalizeDataType(String dataType) {
        return DataElementFieldDict.getDataTypeByKey(DataElementFieldDict.getTypeKeyByDataType(dataType));
    }

    // ==================== 数据子类主表行 ====================

    public List<DataElementSubtypeConfig> findVisibleSubtypeConfigs() {
        return findVisibleSubtypeConfigs(DataElementFieldDict.TYPE_BUSINESS);
    }

    public List<DataElementSubtypeConfig> findVisibleSubtypeConfigs(String dataType) {
        return subtypeConfigMapper.findVisibleByType(normalizeDataType(dataType));
    }

    public DataElementSubtypeConfig findSubtypeConfig(String dataSubtype) {
        return findSubtypeConfig(DataElementFieldDict.TYPE_BUSINESS, dataSubtype);
    }

    public DataElementSubtypeConfig findSubtypeConfig(String dataType, String dataSubtype) {
        return subtypeConfigMapper.findByTypeAndSubtype(normalizeDataType(dataType), dataSubtype);
    }

    @Transactional
    public DataElementSubtypeConfig createSubtypeRow(String dataSubtype, String tableName, Integer fieldCount) {
        return createSubtypeRow(DataElementFieldDict.TYPE_BUSINESS, dataSubtype, tableName, fieldCount);
    }

    @Transactional
    public DataElementSubtypeConfig createSubtypeRow(String dataType, String dataSubtype, String tableName, Integer fieldCount) {
        String normalizedDataType = normalizeDataType(dataType);
        String normalizedSubtype = dataSubtype != null ? dataSubtype.trim() : "";
        if (normalizedSubtype.isEmpty()) {
            throw new RuntimeException("数据子类不能为空");
        }

        String normalizedTableName = tableName != null ? tableName.trim() : "";
        if (normalizedTableName.isEmpty()) {
            normalizedTableName = "uc_custom_" + System.currentTimeMillis();
        }

        int safeFieldCount = fieldCount != null ? Math.max(fieldCount, 0) : 0;
        LocalDateTime now = LocalDateTime.now();
        DataElementSubtypeConfig existing = subtypeConfigMapper.findByTypeAndSubtype(normalizedDataType, normalizedSubtype);
        if (existing != null) {
            existing.setDataType(normalizedDataType);
            existing.setTableName(normalizedTableName);
            existing.setFieldCount(safeFieldCount);
            existing.setVisible(true);
            existing.setUpdatedAt(now);
            if (existing.getSelfCheckStatus() == null) {
                existing.setSelfCheckStatus("NOT_SUBMITTED");
            }
            if (existing.getSecurityCheckStatus() == null) {
                existing.setSecurityCheckStatus("NOT_SUBMITTED");
            }
            subtypeConfigMapper.update(existing);
            return existing;
        }

        Integer maxSortOrder = subtypeConfigMapper.findMaxSortOrderByType(normalizedDataType);
        DataElementSubtypeConfig config = new DataElementSubtypeConfig();
        config.setDataType(normalizedDataType);
        config.setDataSubtype(normalizedSubtype);
        config.setTableName(normalizedTableName);
        config.setFieldCount(safeFieldCount);
        config.setBuiltin(false);
        config.setVisible(true);
        config.setSortOrder((maxSortOrder != null ? maxSortOrder : 0) + 1);
        config.setSelfCheckStatus("NOT_SUBMITTED");
        config.setSecurityCheckStatus("NOT_SUBMITTED");
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        subtypeConfigMapper.insert(config);
        return config;
    }

    @Transactional
    public void deleteSubtypeRow(Long id) {
        deleteSubtypeRow(DataElementFieldDict.TYPE_BUSINESS, id);
    }

    @Transactional
    public void deleteSubtypeRow(String dataType, Long id) {
        String normalizedDataType = normalizeDataType(dataType);
        DataElementSubtypeConfig config = subtypeConfigMapper.findById(id);
        if (config == null) {
            throw new RuntimeException("数据子类行不存在");
        }
        if (config.getDataType() != null && !normalizedDataType.equals(config.getDataType())) {
            throw new RuntimeException("数据子类不属于当前数据一级类型");
        }
        if (Boolean.TRUE.equals(config.getBuiltin())) {
            subtypeConfigMapper.hideById(id, LocalDateTime.now());
        } else {
            subtypeConfigMapper.deleteById(id);
        }
    }

    @Transactional
    public Map<String, Object> selfCheckSubtype(String dataSubtype) {
        return selfCheckSubtype(DataElementFieldDict.TYPE_BUSINESS, dataSubtype);
    }

    @Transactional
    public Map<String, Object> selfCheckSubtype(String dataType, String dataSubtype) {
        String normalizedDataType = normalizeDataType(dataType);
        DataElementSubtypeConfig config = requireSubtypeConfig(normalizedDataType, dataSubtype);
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (config.getDataSubtype() == null || config.getDataSubtype().trim().isEmpty()) {
            issues.add("数据子类名称不能为空");
        }
        if (config.getTableName() == null || config.getTableName().trim().isEmpty()) {
            issues.add("数据库表名不能为空");
        }
        if (config.getFieldCount() == null || config.getFieldCount() <= 0) {
            issues.add("字段数需要大于 0");
        }

        int recordCount = businessRecordMapper.countBySubtypeAndDataType(normalizedDataType, config.getDataSubtype());
        if (recordCount == 0) {
            warnings.add("当前子类暂无导入记录，后续导入数据后建议重新检查");
        }

        boolean passed = issues.isEmpty();
        String status = passed ? "SELF_CHECK_PASSED" : "SELF_CHECK_FAILED";
        String message = passed ? "合规自检通过" : "合规自检未通过";
        String resultText = buildComplianceResultText(message, issues, warnings);
        LocalDateTime now = LocalDateTime.now();
        subtypeConfigMapper.updateSelfCheckStatusByType(normalizedDataType, config.getDataSubtype(), status, resultText, now, now);

        Map<String, Object> result = new HashMap<>();
        result.put("passed", passed);
        result.put("message", message);
        result.put("issues", issues);
        result.put("warnings", warnings);
        return result;
    }

    @Transactional
    public Map<String, Object> securityCheckSubtype(String dataSubtype) {
        return securityCheckSubtype(DataElementFieldDict.TYPE_BUSINESS, dataSubtype);
    }

    @Transactional
    public Map<String, Object> securityCheckSubtype(String dataType, String dataSubtype) {
        String normalizedDataType = normalizeDataType(dataType);
        DataElementSubtypeConfig config = requireSubtypeConfig(normalizedDataType, dataSubtype);
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!"SELF_CHECK_PASSED".equals(config.getSelfCheckStatus())) {
            issues.add("请先完成并通过合规自检");
        }
        if (config.getTableName() == null || config.getTableName().trim().isEmpty()) {
            issues.add("数据库表名不能为空");
        }
        if (config.getFieldCount() == null || config.getFieldCount() <= 0) {
            issues.add("字段数需要大于 0");
        }

        int recordCount = businessRecordMapper.countBySubtypeAndDataType(normalizedDataType, config.getDataSubtype());
        if (recordCount == 0) {
            warnings.add("当前子类暂无导入记录，本次检查仅覆盖元数据配置");
        }

        boolean passed = issues.isEmpty();
        String status = passed ? "SECURITY_CHECK_PASSED" : "SECURITY_CHECK_FAILED";
        String message = passed ? "温州数安港合规检查通过" : "温州数安港合规检查未通过";
        String resultText = buildComplianceResultText(message, issues, warnings);
        LocalDateTime now = LocalDateTime.now();
        subtypeConfigMapper.updateSecurityCheckStatusByType(normalizedDataType, config.getDataSubtype(), status, resultText, now, now);

        Map<String, Object> result = new HashMap<>();
        result.put("passed", passed);
        result.put("message", message);
        result.put("issues", issues);
        result.put("warnings", warnings);
        return result;
    }

    private DataElementSubtypeConfig requireSubtypeConfig(String dataSubtype) {
        return requireSubtypeConfig(DataElementFieldDict.TYPE_BUSINESS, dataSubtype);
    }

    private DataElementSubtypeConfig requireSubtypeConfig(String dataType, String dataSubtype) {
        String normalizedDataType = normalizeDataType(dataType);
        String normalizedSubtype = dataSubtype != null ? dataSubtype.trim() : "";
        if (normalizedSubtype.isEmpty()) {
            throw new RuntimeException("数据子类不能为空");
        }
        DataElementSubtypeConfig config = subtypeConfigMapper.findByTypeAndSubtype(normalizedDataType, normalizedSubtype);
        if (config == null || Boolean.FALSE.equals(config.getVisible())) {
            throw new RuntimeException("数据子类不存在或已隐藏");
        }
        return config;
    }

    private String buildComplianceResultText(String message, List<String> issues, List<String> warnings) {
        StringBuilder sb = new StringBuilder(message);
        if (issues != null && !issues.isEmpty()) {
            sb.append("。问题：");
            for (int i = 0; i < issues.size(); i++) {
                if (i > 0) sb.append("；");
                sb.append(issues.get(i));
            }
        }
        if (warnings != null && !warnings.isEmpty()) {
            sb.append("。提示：");
            for (int i = 0; i < warnings.size(); i++) {
                if (i > 0) sb.append("；");
                sb.append(warnings.get(i));
            }
        }
        return sb.toString();
    }

    // ==================== 上传批次 ====================

    public DataElementUploadBatch findBatchById(Long id) {
        return uploadBatchMapper.findById(id);
    }

    public DataElementUploadBatch findBatchById(String dataType, Long id) {
        DataElementUploadBatch batch = uploadBatchMapper.findById(id);
        if (batch == null) return null;
        String normalizedDataType = normalizeDataType(dataType);
        String batchDataType = batch.getDataType() != null && !batch.getDataType().trim().isEmpty()
                ? batch.getDataType()
                : DataElementFieldDict.TYPE_BUSINESS;
        return normalizedDataType.equals(batchDataType) ? batch : null;
    }

    public List<DataElementUploadBatch> findRecentBatches(int limit) {
        return findRecentBatches(DataElementFieldDict.TYPE_BUSINESS, limit);
    }

    public List<DataElementUploadBatch> findRecentBatches(String dataType, int limit) {
        return uploadBatchMapper.findRecentByType(normalizeDataType(dataType), limit);
    }

    public Map<String, Object> findBatchesByPage(String keyword, String uploader,
                                                   String startTime, String endTime,
                                                   String subtype, Boolean isDuplicate,
                                                   int pageNum, int pageSize) {
        return findBatchesByPage(DataElementFieldDict.TYPE_BUSINESS, keyword, uploader, startTime, endTime,
                subtype, isDuplicate, pageNum, pageSize);
    }

    public Map<String, Object> findBatchesByPage(String dataType, String keyword, String uploader,
                                                   String startTime, String endTime,
                                                   String subtype, Boolean isDuplicate,
                                                   int pageNum, int pageSize) {
        String normalizedDataType = normalizeDataType(dataType);
        int offset = (pageNum - 1) * pageSize;
        List<DataElementUploadBatch> list = uploadBatchMapper.findByPageByType(
                normalizedDataType, keyword, uploader, startTime, endTime, subtype, isDuplicate, offset, pageSize);
        int total = uploadBatchMapper.countByPageByType(
                normalizedDataType, keyword, uploader, startTime, endTime, subtype, isDuplicate);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        int pages = (total + pageSize - 1) / pageSize;
        result.put("pages", pages);
        return result;
    }

    @Transactional
    public void deleteBatch(Long batchId) {
        deleteBatch(DataElementFieldDict.TYPE_BUSINESS, batchId);
    }

    @Transactional
    public void deleteBatch(String dataType, Long batchId) {
        DataElementUploadBatch batch = findBatchById(dataType, batchId);
        if (batch == null) {
            throw new RuntimeException("批次不存在或不属于当前数据一级类型");
        }
        // 删除该批次下的所有业务记录
        businessRecordMapper.deleteByBatchId(batchId);
        // 删除批次记录
        uploadBatchMapper.deleteById(batchId);
    }

    // ==================== 业务记录 ====================

    public DataElementBusinessRecord findRecordById(Long id) {
        return businessRecordMapper.findById(id);
    }

    public List<DataElementBusinessRecord> findRecordsByBatchId(Long batchId) {
        return findRecordsByBatchId(DataElementFieldDict.TYPE_BUSINESS, batchId);
    }

    public List<DataElementBusinessRecord> findRecordsByBatchId(String dataType, Long batchId) {
        if (findBatchById(dataType, batchId) == null) {
            return new ArrayList<DataElementBusinessRecord>();
        }
        return businessRecordMapper.findByBatchIdAndDataType(batchId, normalizeDataType(dataType));
    }

    /**
     * 分页查询某个子类的记录
     */
    public Map<String, Object> findRecordsBySubtype(String subtype, String keyword,
                                                     int pageNum, int pageSize) {
        return findRecordsBySubtype(DataElementFieldDict.TYPE_BUSINESS, subtype, keyword, pageNum, pageSize);
    }

    public Map<String, Object> findRecordsBySubtype(String dataType, String subtype, String keyword,
                                                     int pageNum, int pageSize) {
        String normalizedDataType = normalizeDataType(dataType);
        int offset = (pageNum - 1) * pageSize;
        List<DataElementBusinessRecord> list;
        int total;
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = businessRecordMapper.findBySubtypeAndKeywordAndDataType(normalizedDataType, subtype, keyword.trim(), offset, pageSize);
            total = businessRecordMapper.countBySubtypeAndKeywordAndDataType(normalizedDataType, subtype, keyword.trim());
        } else {
            list = businessRecordMapper.findBySubtypeAndDataType(normalizedDataType, subtype, offset, pageSize);
            total = businessRecordMapper.countBySubtypeAndDataType(normalizedDataType, subtype);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        int pages = (total + pageSize - 1) / pageSize;
        result.put("pages", pages);
        return result;
    }

    /**
     * 获取所有子类的汇总统计
     */
    public List<SubtypeSummary> getSubtypeSummaries() {
        return getSubtypeSummaries(DataElementFieldDict.TYPE_BUSINESS);
    }

    public List<SubtypeSummary> getSubtypeSummaries(String dataType) {
        return businessRecordMapper.getSubtypeSummariesByType(normalizeDataType(dataType));
    }

    // ==================== 删除 ====================

    @Transactional
    public void deleteRecord(Long id) {
        deleteRecord(DataElementFieldDict.TYPE_BUSINESS, id);
    }

    @Transactional
    public void deleteRecord(String dataType, Long id) {
        DataElementBusinessRecord record = businessRecordMapper.findById(id);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }
        String recordDataType = record.getDataType() != null && !record.getDataType().trim().isEmpty()
                ? record.getDataType()
                : DataElementFieldDict.TYPE_BUSINESS;
        if (!normalizeDataType(dataType).equals(recordDataType)) {
            throw new RuntimeException("记录不属于当前数据一级类型");
        }
        businessRecordMapper.deleteById(id);
    }

    // ==================== Excel 导入 ====================

    /**
     * Excel 导入的核心方法。
     * 1. 读取表头，识别字段
     * 2. 逐行解析，拆分为多个子类记录
     * 3. 保存到数据库
     */
    @Transactional
    public DataElementImportResult importExcel(org.springframework.web.multipart.MultipartFile file,
                                                String uploader,
                                                String duplicatePolicy) throws Exception {
        return importExcel(file, uploader, duplicatePolicy, DataElementFieldDict.TYPE_BUSINESS);
    }

    @Transactional
    public DataElementImportResult importExcel(org.springframework.web.multipart.MultipartFile file,
                                                String uploader,
                                                String duplicatePolicy,
                                                String dataType) throws Exception {
        String normalizedDataType = normalizeDataType(dataType);
        String defaultSubtype = DataElementFieldDict.getDefaultSubtype(normalizedDataType);
        DataElementImportResult importResult = new DataElementImportResult();
        LocalDateTime now = LocalDateTime.now();
        if (duplicatePolicy == null) duplicatePolicy = "skip";

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) originalFileName = "unknown.xlsx";

        // 先读取文件字节到内存
        byte[] fileBytes = file.getBytes();

        // 计算文件 SHA-256 Hash
        String fileHash = computeSha256(fileBytes);
        long fileSize = fileBytes.length;

        // 检测重复文件
        DataElementUploadBatch existingBatch = uploadBatchMapper.findByFileHashAndDataType(fileHash, normalizedDataType);
        if (existingBatch != null && "skip".equals(duplicatePolicy)) {
            throw new DuplicateFileException(
                "该文件内容已上传过。\n" +
                "原始文件名：" + existingBatch.getOriginalFileName() + "\n" +
                "首次上传时间：" + existingBatch.getUploadTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
                "上传批次：#" + existingBatch.getId() + "\n" +
                "系统已阻止重复导入，避免数据重复。\n" +
                "你可以在【上传记录】中查看历史导入数据。",
                existingBatch
            );
        }

        // 保存上传文件到 uploads 目录
        String storedPath = FileUploadUtil.saveFile(file, "data-elements");

        // 从字节数组解析 Excel
        Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(fileBytes));
        String sheetName = workbook.getSheetName(0);
        Sheet sheet = workbook.getSheetAt(0);

        if (sheet.getLastRowNum() < 1) {
            workbook.close();
            throw new RuntimeException("Excel文件为空，至少需要1行表头和1行数据");
        }

        // 读取表头行
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            workbook.close();
            throw new RuntimeException("Excel表头行为空");
        }
        int colCount = headerRow.getLastCellNum();
        if (colCount <= 0) {
            workbook.close();
            throw new RuntimeException("Excel表头列数为空");
        }

        // 解析每个列的表头 -> FieldEntry
        String[] headers = new String[colCount];
        DataElementFieldDict.FieldEntry[] fieldEntries = new DataElementFieldDict.FieldEntry[colCount];
        List<String> recognizedCols = new ArrayList<>();
        List<UnrecognizedColumnInfo> unrecognizedColInfos = new ArrayList<>();

        for (int c = 0; c < colCount; c++) {
            String headerText = getCellValueAsString(headerRow.getCell(c));
            headers[c] = headerText;

            // 标准化表头
            String normalized = DataElementFieldDict.normalizeHeader(headerText);

            // 如果是"仅扩展字段"（如备注），特殊处理：不标记为未识别
            if (DataElementFieldDict.isExtraOnlyField(normalized)) {
                // 标记为 null，但值会存入 extra_json
                fieldEntries[c] = null;
                recognizedCols.add(headerText + "(扩展字段)");
                continue;
            }

            DataElementFieldDict.FieldEntry fe = DataElementFieldDict.matchField(headerText, normalizedDataType);
            fieldEntries[c] = fe;
            if (fe != null) {
                recognizedCols.add(headerText);
            } else {
                // 收集未识别字段详情
                UnrecognizedColumnInfo uci = new UnrecognizedColumnInfo();
                uci.setSheetName(sheetName);
                uci.setColumnIndex(c);
                uci.setOriginalHeader(headerText);
                uci.setNormalizedHeader(normalized);
                uci.setSavedToExtraJson(true);
                uci.setSampleValues(new ArrayList<String>());
                unrecognizedColInfos.add(uci);
            }
        }

        // 收集涉及的子类
        Set<String> involvedSubtypes = new LinkedHashSet<>();
        for (int c = 0; c < colCount; c++) {
            if (fieldEntries[c] != null) {
                involvedSubtypes.add(fieldEntries[c].subtype);
            }
        }

        // 将未识别字段详情转为 JSON 字符串
        String unrecognizedColumnsJson = unrecognizedColInfosToJson(unrecognizedColInfos);

        // 创建上传批次记录
        DataElementUploadBatch batch = new DataElementUploadBatch();
        batch.setDataType(normalizedDataType);
        batch.setBatchName("导入_" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        batch.setOriginalFileName(originalFileName);
        batch.setStoredFilePath(storedPath);
        batch.setFileHash(fileHash);
        batch.setFileSize(fileSize);
        batch.setUploader(uploader);
        batch.setUploadTime(now);
        batch.setTotalRows(0);
        batch.setBusinessRecordCount(0);
        batch.setRecognizedColumns(String.join(", ", recognizedCols));
        batch.setUnrecognizedColumns(unrecognizedColInfos.isEmpty() ? "" : unrecognizedColumnsJson);
        batch.setInvolvedSubtypes(String.join(", ", involvedSubtypes));
        if (existingBatch != null) {
            batch.setDuplicateOfBatchId(existingBatch.getId());
        }
        batch.setImportPolicy(duplicatePolicy);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        uploadBatchMapper.insert(batch);
        Long batchId = batch.getId();

        // 逐行解析数据 - 收集样本值
        int maxSampleRow = Math.min(sheet.getLastRowNum(), 5); // 前5行作为样本
        for (int r = 1; r <= maxSampleRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int ci = 0; ci < unrecognizedColInfos.size(); ci++) {
                UnrecognizedColumnInfo uci = unrecognizedColInfos.get(ci);
                String val = getCellValueAsString(row.getCell(uci.getColumnIndex()));
                if (val != null && !val.trim().isEmpty()) {
                    List<String> samples = uci.getSampleValues();
                    if (!samples.contains(val) && samples.size() < 3) {
                        samples.add(val);
                    }
                }
            }
        }

        // 更新未识别字段 JSON（包含样本值）
        unrecognizedColumnsJson = unrecognizedColInfosToJson(unrecognizedColInfos);
        batch.setUnrecognizedColumns(unrecognizedColInfos.isEmpty() ? "" : unrecognizedColumnsJson);
        batch.setUpdatedAt(now);
        uploadBatchMapper.update(batch);

        // 逐行解析数据
        List<DataElementBusinessRecord> allRecords = new ArrayList<>();
        int dataRowCount = 0;

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // 判断是否为空行
            boolean emptyRow = true;
            for (int c = 0; c < colCount; c++) {
                String val = getCellValueAsString(row.getCell(c));
                if (val != null && !val.trim().isEmpty()) {
                    emptyRow = false;
                    break;
                }
            }
            if (emptyRow) continue;
            dataRowCount++;

            // 为每行生成一个 projectId
            String projectId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            // 按子类分组收集字段
            Map<String, Map<String, String>> subtypeRecords = new LinkedHashMap<>();
            Map<String, String> extraFields = new LinkedHashMap<>();

            for (int c = 0; c < colCount; c++) {
                String value = getCellValueAsString(row.getCell(c));
                if (value == null || value.trim().isEmpty()) continue;

                DataElementFieldDict.FieldEntry fe = fieldEntries[c];
                if (fe != null) {
                    // 已识别字段 -> 按子类分组
                    Map<String, String> fields = subtypeRecords.get(fe.subtype);
                    if (fields == null) {
                        fields = new LinkedHashMap<>();
                        subtypeRecords.put(fe.subtype, fields);
                    }
                    fields.put(fe.enName, value);
                } else {
                    // 未识别字段 -> extra（包括"备注"等extra-only字段）
                    extraFields.put(headers[c], value);
                }
            }

            // 判断是否包含五方主体字段，如果有则额外抽取
            boolean hasPartyData = false;
            Map<String, String> partyFields = new LinkedHashMap<>();
            if (DataElementFieldDict.TYPE_BUSINESS.equals(normalizedDataType)) {
                for (Map.Entry<String, String> entry : DataElementFieldDict.getPartyFieldMap().entrySet()) {
                    String fieldName = entry.getKey();
                    String partyType = entry.getValue();
                    Map<String, String> basicFields = subtypeRecords.get("工程基本信息");
                    if (basicFields != null) {
                        String val = basicFields.get(fieldName);
                        if (val != null && !val.trim().isEmpty()) {
                            partyFields.put(partyType, val);
                            hasPartyData = true;
                        }
                    }
                }
            }

            // 如果某行没有匹配到任何子类，但有不识别字段，归入扩展
            if (subtypeRecords.isEmpty() && !extraFields.isEmpty()) {
                Map<String, String> defaultMap = new LinkedHashMap<>();
                defaultMap.put("未分类字段", String.join("; ", extraFields.values()));
                subtypeRecords.put(defaultSubtype, defaultMap);
            }

            // 为每个子类创建业务记录
            for (Map.Entry<String, Map<String, String>> entry : subtypeRecords.entrySet()) {
                String subtype = entry.getKey();
                Map<String, String> fields = entry.getValue();

                String displayName = buildDisplayName(fields);
                String recordJson = mapToJson(fields);
                String extraJson = extraFields.isEmpty() ? null : mapToJson(extraFields);

                DataElementBusinessRecord record = new DataElementBusinessRecord();
                record.setBatchId(batchId);
                record.setDataType(normalizedDataType);
                record.setDataSubtype(subtype);
                record.setRowIndex(r);
                record.setProjectId(projectId);
                record.setDisplayName(displayName);
                record.setRecordJson(recordJson);
                record.setExtraJson(extraJson);
                record.setCreatedAt(now);
                record.setUpdatedAt(now);
                allRecords.add(record);
            }

            // 如果有五方主体数据，额外创建五方主体记录
            if (DataElementFieldDict.TYPE_BUSINESS.equals(normalizedDataType) && hasPartyData) {
                for (Map.Entry<String, String> pEntry : partyFields.entrySet()) {
                    Map<String, String> partyMap = new LinkedHashMap<>();
                    partyMap.put("party_type", pEntry.getKey());
                    partyMap.put("party_org_name", pEntry.getValue());
                    partyMap.put("project_id", projectId);

                    DataElementBusinessRecord partyRecord = new DataElementBusinessRecord();
                    partyRecord.setBatchId(batchId);
                    partyRecord.setDataType(normalizedDataType);
                    partyRecord.setDataSubtype("五方主体信息");
                    partyRecord.setRowIndex(r);
                    partyRecord.setProjectId(projectId);
                    partyRecord.setDisplayName(pEntry.getKey() + ": " + pEntry.getValue());
                    partyRecord.setRecordJson(mapToJson(partyMap));
                    partyRecord.setExtraJson(extraFields.isEmpty() ? null : mapToJson(extraFields));
                    partyRecord.setCreatedAt(now);
                    partyRecord.setUpdatedAt(now);
                    allRecords.add(partyRecord);
                }
            }
        }

        // 批量插入
        if (!allRecords.isEmpty()) {
            for (DataElementBusinessRecord record : allRecords) {
                if (record.getDataSubtype() != null && !record.getDataSubtype().trim().isEmpty()) {
                    involvedSubtypes.add(record.getDataSubtype().trim());
                }
            }
            resetSubtypeComplianceAfterImport(normalizedDataType, involvedSubtypes, now);

            int batchSize = 200;
            for (int i = 0; i < allRecords.size(); i += batchSize) {
                int end = Math.min(i + batchSize, allRecords.size());
                List<DataElementBusinessRecord> subList = allRecords.subList(i, end);
                businessRecordMapper.batchInsert(subList);
            }
        }

        workbook.close();

        // 设置导入结果
        importResult.setSuccess(true);
        importResult.setMessage("导入成功");
        importResult.setBatchId(batchId);
        importResult.setRecognizedColumnCount(recognizedCols.size());
        importResult.setUnrecognizedColumnCount(unrecognizedColInfos.size());
        importResult.setImportedRowCount(dataRowCount);
        importResult.setBusinessRecordCount(allRecords.size());
        importResult.setInvolvedSubtypes(new ArrayList<>(involvedSubtypes));
        importResult.setUnrecognizedColumns(unrecognizedColInfos);

        // 更新批次记录
        batch.setTotalRows(dataRowCount);
        batch.setBusinessRecordCount(allRecords.size());
        batch.setInvolvedSubtypes(String.join(", ", involvedSubtypes));
        batch.setUpdatedAt(now);
        uploadBatchMapper.update(batch);

        return importResult;
    }

    private void resetSubtypeComplianceAfterImport(Set<String> involvedSubtypes, LocalDateTime now) {
        resetSubtypeComplianceAfterImport(DataElementFieldDict.TYPE_BUSINESS, involvedSubtypes, now);
    }

    private void resetSubtypeComplianceAfterImport(String dataType, Set<String> involvedSubtypes, LocalDateTime now) {
        if (involvedSubtypes == null || involvedSubtypes.isEmpty()) {
            return;
        }
        List<String> subtypes = new ArrayList<>();
        for (String subtype : involvedSubtypes) {
            if (subtype != null && !subtype.trim().isEmpty() && !subtypes.contains(subtype.trim())) {
                subtypes.add(subtype.trim());
            }
        }
        if (subtypes.isEmpty()) {
            return;
        }
        subtypeConfigMapper.resetComplianceStatusForSubtypesByType(
                normalizeDataType(dataType),
                subtypes,
                "已上传新的 Excel 数据，请重新进行合规自检和温州数安港合规检查。",
                now
        );
    }

    /**
     * 将未识别字段详情列表转为 JSON 字符串
     */
    private String unrecognizedColInfosToJson(List<UnrecognizedColumnInfo> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            UnrecognizedColumnInfo uci = list.get(i);
            sb.append("{");
            sb.append("\"sheetName\":\"").append(escapeJson(uci.getSheetName())).append("\"");
            sb.append(",\"columnIndex\":").append(uci.getColumnIndex());
            sb.append(",\"columnLetter\":\"").append(escapeJson(uci.getColumnLetter())).append("\"");
            sb.append(",\"originalHeader\":\"").append(escapeJson(uci.getOriginalHeader())).append("\"");
            sb.append(",\"normalizedHeader\":\"").append(escapeJson(uci.getNormalizedHeader())).append("\"");
            sb.append(",\"sampleValues\":").append(listToJsonArray(uci.getSampleValues()));
            sb.append(",\"savedToExtraJson\":").append(uci.isSavedToExtraJson());
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String listToJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    // ==================== 导出 ====================

    public byte[] exportBusinessMetadataToExcel() throws IOException {
        List<DataElementSubtypeConfig> subtypeConfigs = findVisibleSubtypeConfigs(DataElementFieldDict.TYPE_BUSINESS);
        Map<String, List<String[]>> fieldMap = DataElementFieldDict.getSubtypeFieldDisplay(DataElementFieldDict.TYPE_BUSINESS);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("业务元数据字段清单");

        String[] headers = {"数据一级类型", "业务数据子类", "数据库表名", "字段序号", "表头字段", "数据库字段名", "元数据类型"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        int rowNum = 1;
        for (DataElementSubtypeConfig config : subtypeConfigs) {
            List<String[]> fields = fieldMap.get(config.getDataSubtype());
            if (fields == null || fields.isEmpty()) {
                continue;
            }
            for (int i = 0; i < fields.size(); i++) {
                String[] field = fields.get(i);
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(DataElementFieldDict.TYPE_BUSINESS);
                row.createCell(1).setCellValue(config.getDataSubtype());
                row.createCell(2).setCellValue(config.getTableName());
                row.createCell(3).setCellValue(i + 1);
                row.createCell(4).setCellValue(field[0]);
                row.createCell(5).setCellValue(field[1]);
                row.createCell(6).setCellValue("业务表头字段");
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    public byte[] exportToExcel() throws IOException {
        return exportToExcel(DataElementFieldDict.TYPE_BUSINESS);
    }

    public byte[] exportToExcel(String dataType) throws IOException {
        String normalizedDataType = normalizeDataType(dataType);
        List<DataElementBusinessRecord> allRecords = businessRecordMapper.findAllForExportByType(normalizedDataType);

        Workbook workbook = new XSSFWorkbook();

        // 按子类分组
        Map<String, List<DataElementBusinessRecord>> grouped = new LinkedHashMap<>();
        List<String> orderedSubtypes = new ArrayList<String>(Arrays.asList(DataElementFieldDict.getSubtypeList(normalizedDataType)));
        for (String st : orderedSubtypes) {
            grouped.put(st, new ArrayList<DataElementBusinessRecord>());
        }
        for (DataElementBusinessRecord record : allRecords) {
            List<DataElementBusinessRecord> list = grouped.get(record.getDataSubtype());
            if (list == null) {
                list = new ArrayList<>();
                grouped.put(record.getDataSubtype(), list);
                orderedSubtypes.add(record.getDataSubtype());
            }
            list.add(record);
        }

        for (String subtype : orderedSubtypes) {
            List<DataElementBusinessRecord> records = grouped.get(subtype);
            if (records == null) records = new ArrayList<>();

            Sheet sheet = workbook.createSheet(subtype);
            List<String[]> fieldDisplay = DataElementFieldDict.getFieldDisplayBySubtype(normalizedDataType, subtype);

            // 表头行
            Row headerRow = sheet.createRow(0);
            int colIdx = 0;
            for (String[] fd : fieldDisplay) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(fd[0]); // 中文名
            }
            // 最后一列：扩展字段
            headerRow.createCell(colIdx).setCellValue("扩展字段");

            // 数据行
            int rowNum = 1;
            for (DataElementBusinessRecord record : records) {
                Row row = sheet.createRow(rowNum++);
                Map<String, String> fieldMap = jsonToMap(record.getRecordJson());
                colIdx = 0;
                for (String[] fd : fieldDisplay) {
                    String val = fieldMap.get(fd[1]); // 用英文名取值
                    Cell cell = row.createCell(colIdx++);
                    if (val != null && !val.isEmpty()) {
                        cell.setCellValue(val);
                    } else {
                        cell.setCellValue("-");
                    }
                }
                // 扩展字段
                if (record.getExtraJson() != null && !record.getExtraJson().isEmpty()) {
                    row.createCell(colIdx).setCellValue(record.getExtraJson());
                } else {
                    row.createCell(colIdx).setCellValue("-");
                }
            }

            // 自动调整列宽
            for (int i = 0; i <= fieldDisplay.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    // ==================== 工具方法 ====================

    /**
     * 从字段中提取显示名称。
     * 优先级：工程名称 > 文件名称 > 案卷名称 > 合同名称 > 图纸名称 > 单位名称 > 标准地名 > 其他
     */
    private String buildDisplayName(Map<String, String> fields) {
        String[] priorityKeys = {"data_name", "service_name", "log_id", "metadata_id",
                "field_cn_name", "quality_rule_name", "service_identifier",
                "unit_name", "module_name", "event_content", "alert_message",
                "project_name", "file_title", "volume_title",
                "contract_title", "drawing_title", "party_org_name",
                "standard_place_name", "owner_name", "building_name",
                "acceptance_file_title"};
        for (String key : priorityKeys) {
            String val = fields.get(key);
            if (val != null && !val.trim().isEmpty()) {
                return val;
            }
        }
        // 取第一个非空字段值
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (e.getValue() != null && !e.getValue().trim().isEmpty()) {
                return e.getValue();
            }
        }
        return "未命名记录";
    }

    /**
     * Map 转 JSON 字符串
     */
    private String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
              .append(escapeJson(entry.getValue() != null ? entry.getValue() : "")).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * JSON 字符串转 Map
     */
    private Map<String, String> jsonToMap(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty() || "{}".equals(json.trim())) {
            return map;
        }
        try {
            String content = json.trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                content = content.substring(1, content.length() - 1);
            }
            // 简单解析 key:value 对
            int len = content.length();
            int i = 0;
            while (i < len) {
                // 跳过空白
                while (i < len && (content.charAt(i) == ' ' || content.charAt(i) == ',')) i++;
                if (i >= len) break;

                // 读取 key
                if (content.charAt(i) != '"') break;
                i++; // 跳过起始引号
                StringBuilder key = new StringBuilder();
                while (i < len && content.charAt(i) != '"') {
                    if (content.charAt(i) == '\\' && i + 1 < len) {
                        key.append(content.charAt(i + 1));
                        i += 2;
                    } else {
                        key.append(content.charAt(i));
                        i++;
                    }
                }
                if (i >= len) break;
                i++; // 跳过结束引号

                // 跳过 : 和空白
                while (i < len && (content.charAt(i) == ':' || content.charAt(i) == ' ')) i++;
                if (i >= len) break;

                // 读取 value
                if (content.charAt(i) != '"') break;
                i++; // 跳过起始引号
                StringBuilder value = new StringBuilder();
                while (i < len && content.charAt(i) != '"') {
                    if (content.charAt(i) == '\\' && i + 1 < len) {
                        value.append(content.charAt(i + 1));
                        i += 2;
                    } else {
                        value.append(content.charAt(i));
                        i++;
                    }
                }
                if (i >= len) break;
                i++; // 跳过结束引号

                map.put(key.toString(), value.toString());
            }
        } catch (Exception e) {
            // 解析失败返回空 map
        }
        return map;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 从 Excel Cell 读取字符串值
     * 优化：支持 STRING, NUMERIC(判断日期), FORMULA, BOOLEAN
     */
    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                String val = cell.getStringCellValue();
                return val != null ? val.trim() : "";
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else {
                    double d = cell.getNumericCellValue();
                    if (d == Math.floor(d) && !Double.isInfinite(d)) {
                        return String.valueOf((long) d);
                    } else {
                        return String.valueOf(d);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e2) {
                        return cell.getCellFormula();
                    }
                }
            default:
                return "";
        }
    }

    /**
     * 计算文件字节数组的 SHA-256 Hash（十六进制字符串）
     */
    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算文件哈希失败", e);
        }
    }

    /**
     * 重复文件异常，携带已存在批次的信息
     */
    public static class DuplicateFileException extends RuntimeException {
        private final DataElementUploadBatch existingBatch;

        public DuplicateFileException(String message, DataElementUploadBatch existingBatch) {
            super(message);
            this.existingBatch = existingBatch;
        }

        public DataElementUploadBatch getExistingBatch() {
            return existingBatch;
        }
    }
}
