package com.collaborative.sharing.controller;

import com.collaborative.sharing.entity.DataElementBusinessRecord;
import com.collaborative.sharing.entity.DataElementSubtypeConfig;
import com.collaborative.sharing.entity.DataElementUploadBatch;
import com.collaborative.sharing.entity.ServiceComponent;
import com.collaborative.sharing.mapper.SubtypeSummary;
import com.collaborative.sharing.service.DataElementImportResult;
import com.collaborative.sharing.service.DataElementService;
import com.collaborative.sharing.service.DataElementService.DuplicateFileException;
import com.collaborative.sharing.service.ServiceComponentService;
import com.collaborative.sharing.service.UnrecognizedColumnInfo;
import com.collaborative.sharing.util.AccessControlUtil;
import com.collaborative.sharing.util.DataElementFieldDict;
import com.collaborative.sharing.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/data-elements")
public class DataElementController {

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ServiceComponentService serviceComponentService;

    @GetMapping("")
    public String list() {
        return "redirect:/data-elements/business";
    }

    @GetMapping("/{typeKey}")
    public String listByType(@PathVariable String typeKey,
                             @RequestParam(value = "dataSubtype", required = false) String dataSubtype,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             Model model,
                             HttpSession session) {
        if (!DataElementFieldDict.isValidTypeKey(typeKey)) {
            return "redirect:/data-elements/business";
        }
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) return "redirect:/login";
        if (AccessControlUtil.isExternalDepartment(session)) return "redirect:/service-components";

        AccessControlUtil.fillModel(model, session);
        String dataType = DataElementFieldDict.getDataTypeByKey(typeKey);
        String basePath = DataElementFieldDict.getBasePath(typeKey);
        boolean businessMetadataPage = DataElementFieldDict.KEY_METADATA.equals(typeKey);

        List<SubtypeSummary> dbSummaries = dataElementService.getSubtypeSummaries(dataType);
        Map<String, SubtypeSummary> summaryMap = new LinkedHashMap<>();
        for (SubtypeSummary s : dbSummaries) {
            summaryMap.put(s.getDataSubtype(), s);
        }

        List<DataElementSubtypeConfig> subtypeConfigs = dataElementService.findVisibleSubtypeConfigs(
                businessMetadataPage ? DataElementFieldDict.TYPE_BUSINESS : dataType);
        List<Map<String, Object>> subtypeListData = new ArrayList<>();
        for (DataElementSubtypeConfig config : subtypeConfigs) {
            String st = config.getDataSubtype();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", config.getId());
            item.put("name", st);
            item.put("tableName", config.getTableName());
            item.put("fieldCount", config.getFieldCount() != null ? config.getFieldCount() : 0);
            item.put("builtin", config.getBuiltin());
            item.put("selfCheckStatus", config.getSelfCheckStatus());
            item.put("securityCheckStatus", config.getSecurityCheckStatus());
            item.put("complianceResult", config.getComplianceResult());

            SubtypeSummary summary = summaryMap.get(st);
            if (summary != null) {
                item.put("recordCount", summary.getRecordCount());
                item.put("lastUploadTime", summary.getLastUploadTime());
                item.put("lastUploader", summary.getLastUploader());
                item.put("lastFileName", summary.getLastFileName());
            } else {
                item.put("recordCount", 0);
                item.put("lastUploadTime", "-");
                item.put("lastUploader", "-");
                item.put("lastFileName", "-");
            }
            if (matchesListFilter(item, dataSubtype, keyword)) {
                subtypeListData.add(item);
            }
        }

        if (businessMetadataPage) {
            List<Map<String, Object>> metadataFieldRows = buildBusinessMetadataFieldRows(subtypeConfigs, dataSubtype, keyword);
            model.addAttribute("subtypeList", buildSubtypeOptions(subtypeConfigs));
            model.addAttribute("metadataFieldRows", metadataFieldRows);
            model.addAttribute("metadataFieldSummary", buildBusinessMetadataSummary(subtypeConfigs, metadataFieldRows));
            model.addAttribute("batches", new ArrayList<DataElementUploadBatch>());
        } else {
            model.addAttribute("subtypeList", subtypeListData);
            model.addAttribute("batches", dataElementService.findRecentBatches(dataType, 20));
        }
        model.addAttribute("dataSubtype", dataSubtype);
        model.addAttribute("keyword", keyword);
        model.addAttribute("keywordPlaceholder", keywordPlaceholder(dataType));
        model.addAttribute("dataType", dataType);
        model.addAttribute("typeKey", typeKey);
        model.addAttribute("businessMetadataPage", businessMetadataPage);
        model.addAttribute("pageTitle", DataElementFieldDict.getTypeTitle(dataType));
        model.addAttribute("recordLabel", DataElementFieldDict.getRecordLabel(dataType));
        model.addAttribute("basePath", basePath);
        model.addAttribute("activeNav", "data-elements");
        model.addAttribute("activeDataElementTypeKey", typeKey);
        if (DataElementFieldDict.KEY_SERVICE_INSTANCE.equals(typeKey)) {
            model.addAttribute("serviceInstanceOverview", buildServiceInstanceOverview());
        }
        return "data-elements";
    }

    // ==================== 数据子类主表行管理 ====================

    @PostMapping("/subtypes/create")
    @ResponseBody
    public Map<String, Object> createSubtypeRow(@RequestParam("dataSubtype") String dataSubtype,
                                                @RequestParam(value = "tableName", required = false) String tableName,
                                                @RequestParam(value = "fieldCount", required = false) Integer fieldCount,
                                                HttpSession session) {
        return createSubtypeRowByType(DataElementFieldDict.KEY_BUSINESS, dataSubtype, tableName, fieldCount, session);
    }

    @PostMapping("/{typeKey}/subtypes/create")
    @ResponseBody
    public Map<String, Object> createSubtypeRowByType(@PathVariable String typeKey,
                                                      @RequestParam("dataSubtype") String dataSubtype,
                                                      @RequestParam(value = "tableName", required = false) String tableName,
                                                      @RequestParam(value = "fieldCount", required = false) Integer fieldCount,
                                                      HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权管理数据要素")) return result;
            String dataType = DataElementFieldDict.getDataTypeByKey(typeKey);
            DataElementSubtypeConfig config = dataElementService.createSubtypeRow(dataType, dataSubtype, tableName, fieldCount);
            result.put("success", true);
            result.put("message", "数据子类行已保存");
            result.put("row", config);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/subtypes/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteSubtypeRow(@PathVariable Long id, HttpSession session) {
        return deleteSubtypeRowByType(DataElementFieldDict.KEY_BUSINESS, id, session);
    }

    @PostMapping("/{typeKey}/subtypes/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteSubtypeRowByType(@PathVariable String typeKey,
                                                      @PathVariable Long id,
                                                      HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权管理数据要素")) return result;
            dataElementService.deleteSubtypeRow(DataElementFieldDict.getDataTypeByKey(typeKey), id);
            result.put("success", true);
            result.put("message", "数据子类行已删除");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    // ==================== 数据要素合规检查 ====================

    @PostMapping("/compliance/self-check")
    @ResponseBody
    public Map<String, Object> selfCheck(@RequestParam("subtype") String subtype, HttpSession session) {
        return selfCheckByType(DataElementFieldDict.KEY_BUSINESS, subtype, session);
    }

    @PostMapping("/{typeKey}/compliance/self-check")
    @ResponseBody
    public Map<String, Object> selfCheckByType(@PathVariable String typeKey,
                                               @RequestParam("subtype") String subtype,
                                               HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权执行数据要素合规检查")) return result;
            Map<String, Object> checkResult = dataElementService.selfCheckSubtype(DataElementFieldDict.getDataTypeByKey(typeKey), subtype);
            result.put("success", true);
            result.putAll(checkResult);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "自检失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/compliance/security-check")
    @ResponseBody
    public Map<String, Object> securityCheck(@RequestParam("subtype") String subtype, HttpSession session) {
        return securityCheckByType(DataElementFieldDict.KEY_BUSINESS, subtype, session);
    }

    @PostMapping("/{typeKey}/compliance/security-check")
    @ResponseBody
    public Map<String, Object> securityCheckByType(@PathVariable String typeKey,
                                                   @RequestParam("subtype") String subtype,
                                                   HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权执行数据要素合规检查")) return result;
            Map<String, Object> checkResult = dataElementService.securityCheckSubtype(DataElementFieldDict.getDataTypeByKey(typeKey), subtype);
            result.put("success", true);
            result.putAll(checkResult);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查失败：" + e.getMessage());
        }
        return result;
    }

    // ==================== 预览数据 ====================

    @GetMapping("/preview")
    @ResponseBody
    public Map<String, Object> preview(@RequestParam("subtype") String subtype,
                                       @RequestParam(value = "keyword", required = false) String keyword,
                                       @RequestParam(value = "batchId", required = false) Long batchId,
                                       @RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "15") int pageSize,
                                       HttpSession session) {
        return previewByType(DataElementFieldDict.KEY_BUSINESS, subtype, keyword, batchId, pageNum, pageSize, session);
    }

    @GetMapping("/{typeKey}/preview")
    @ResponseBody
    public Map<String, Object> previewByType(@PathVariable String typeKey,
                                             @RequestParam("subtype") String subtype,
                                             @RequestParam(value = "keyword", required = false) String keyword,
                                             @RequestParam(value = "batchId", required = false) Long batchId,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "15") int pageSize,
                                             HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权预览数据要素")) return result;
            String dataType = DataElementFieldDict.getDataTypeByKey(typeKey);
            Map<String, Object> pageData = dataElementService.findRecordsBySubtype(dataType, subtype, keyword, pageNum, pageSize);
            result.put("success", true);
            result.put("data", pageData);

            List<String[]> fieldDisplay = DataElementFieldDict.getFieldDisplayBySubtype(dataType, subtype);
            List<Map<String, String>> fieldDefs = new ArrayList<>();
            for (String[] fd : fieldDisplay) {
                Map<String, String> f = new LinkedHashMap<>();
                f.put("cnName", fd[0]);
                f.put("enName", fd[1]);
                fieldDefs.add(f);
            }
            result.put("fields", fieldDefs);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 导入 ====================

    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "duplicatePolicy", defaultValue = "skip") String duplicatePolicy,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        return importExcelByType(DataElementFieldDict.KEY_BUSINESS, file, duplicatePolicy, session, redirectAttributes);
    }

    @PostMapping("/{typeKey}/import")
    public String importExcelByType(@PathVariable String typeKey,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "duplicatePolicy", defaultValue = "skip") String duplicatePolicy,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        String basePath = DataElementFieldDict.getBasePath(typeKey);
        try {
            if (!DataElementFieldDict.isValidTypeKey(typeKey)) {
                redirectAttributes.addFlashAttribute("error", "数据一级类型不存在");
                return "redirect:/data-elements/business";
            }
            String uploader = (String) session.getAttribute("currentUser");
            if (uploader == null) return "redirect:/login";
            if (AccessControlUtil.isExternalDepartment(session)) {
                redirectAttributes.addFlashAttribute("error", "外部部门账号无权导入数据要素");
                return "redirect:/service-components";
            }

            String dataType = DataElementFieldDict.getDataTypeByKey(typeKey);
            DataElementImportResult importResult = dataElementService.importExcel(file, uploader, duplicatePolicy, dataType);
            if (importResult.isSuccess()) {
                StringBuilder msg = new StringBuilder();
                msg.append("导入成功！")
                   .append("识别字段 ").append(importResult.getRecognizedColumnCount()).append(" 个，");

                if (importResult.getUnrecognizedColumnCount() > 0) {
                    msg.append("扩展字段 ").append(importResult.getUnrecognizedColumnCount()).append(" 个。\n");
                } else {
                    msg.append("扩展字段 0 个。\n");
                }

                msg.append("导入记录 ").append(importResult.getImportedRowCount()).append(" 行，")
                   .append("生成数据记录 ").append(importResult.getBusinessRecordCount()).append(" 条。\n")
                   .append("涉及数据子类：").append(importResult.getInvolvedSubtypesString())
                   .append("\n涉及数据子类的合规自检和温州数安港合规检查状态已重置，请重新检查。");

                List<UnrecognizedColumnInfo> unrecognizedCols = importResult.getUnrecognizedColumns();
                if (!unrecognizedCols.isEmpty()) {
                    msg.append("\n\n以下字段未匹配到当前类型的标准字段，已保存至扩展字段 extra_json，不会丢失：");
                    int showCount = Math.min(unrecognizedCols.size(), 10);
                    for (int i = 0; i < showCount; i++) {
                        if (i > 0) msg.append("、");
                        msg.append(unrecognizedCols.get(i).getOriginalHeader());
                    }
                    if (unrecognizedCols.size() > 10) {
                        msg.append(" …… 共").append(unrecognizedCols.size()).append("个");
                    }
                }

                redirectAttributes.addFlashAttribute("success", msg.toString());
                redirectAttributes.addFlashAttribute("importBatchId", importResult.getBatchId());
                redirectAttributes.addFlashAttribute("hasUnrecognized", importResult.getUnrecognizedColumnCount() > 0);
            }
        } catch (DuplicateFileException e) {
            DataElementUploadBatch existing = e.getExistingBatch();
            redirectAttributes.addFlashAttribute("error",
                "该文件内容已在当前数据一级类型下上传过。\n" +
                "原始文件名：" + existing.getOriginalFileName() + "\n" +
                "首次上传时间：" + existing.getUploadTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
                "上传批次：#" + existing.getId() + "\n" +
                "系统已阻止重复导入，避免数据重复。");
            redirectAttributes.addFlashAttribute("duplicateBatchId", existing.getId());
            redirectAttributes.addFlashAttribute("duplicateFileName", existing.getOriginalFileName());
            redirectAttributes.addFlashAttribute("duplicateFileHash", existing.getFileHash());
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("DuplicateFileException")) {
                msg = "该文件内容已上传过，系统已阻止重复导入。";
            }
            redirectAttributes.addFlashAttribute("error", "导入失败：" + (msg != null ? msg : "未知错误"));
        }
        return "redirect:" + basePath;
    }

    // ==================== 导出 ====================

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(HttpSession session) throws Exception {
        return exportByType(DataElementFieldDict.KEY_BUSINESS, session);
    }

    @GetMapping("/{typeKey}/export")
    public ResponseEntity<byte[]> exportByType(@PathVariable String typeKey, HttpSession session) throws Exception {
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) return ResponseEntity.badRequest().build();
        if (AccessControlUtil.isExternalDepartment(session)) return ResponseEntity.status(403).build();
        if (!DataElementFieldDict.isValidTypeKey(typeKey)) return ResponseEntity.notFound().build();

        String dataType = DataElementFieldDict.getDataTypeByKey(typeKey);
        byte[] excelData;
        String filename;
        if (DataElementFieldDict.KEY_METADATA.equals(typeKey)) {
            excelData = dataElementService.exportBusinessMetadataToExcel();
            filename = "业务元数据字段清单_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        } else {
            excelData = dataElementService.exportToExcel(dataType);
            filename = dataType + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", new String(filename.getBytes("UTF-8"), "ISO-8859-1"));
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }

    // ==================== 批次管理 ====================

    @GetMapping("/batches")
    @ResponseBody
    public Map<String, Object> listBatches(@RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "uploader", required = false) String uploader,
                                           @RequestParam(value = "startTime", required = false) String startTime,
                                           @RequestParam(value = "endTime", required = false) String endTime,
                                           @RequestParam(value = "subtype", required = false) String subtype,
                                           @RequestParam(value = "isDuplicate", required = false) Boolean isDuplicate,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "20") int pageSize,
                                           HttpSession session) {
        return listBatchesByType(DataElementFieldDict.KEY_BUSINESS, keyword, uploader, startTime, endTime,
                subtype, isDuplicate, pageNum, pageSize, session);
    }

    @GetMapping("/{typeKey}/batches")
    @ResponseBody
    public Map<String, Object> listBatchesByType(@PathVariable String typeKey,
                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "uploader", required = false) String uploader,
                                                 @RequestParam(value = "startTime", required = false) String startTime,
                                                 @RequestParam(value = "endTime", required = false) String endTime,
                                                 @RequestParam(value = "subtype", required = false) String subtype,
                                                 @RequestParam(value = "isDuplicate", required = false) Boolean isDuplicate,
                                                 @RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权查看数据导入批次")) return result;
            Map<String, Object> pageData = dataElementService.findBatchesByPage(
                    DataElementFieldDict.getDataTypeByKey(typeKey), keyword, uploader, startTime, endTime,
                    subtype, isDuplicate, pageNum, pageSize);
            result.put("success", true);
            result.put("data", pageData);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/batches/{id}")
    @ResponseBody
    public Map<String, Object> getBatch(@PathVariable Long id, HttpSession session) {
        return getBatchByType(DataElementFieldDict.KEY_BUSINESS, id, session);
    }

    @GetMapping("/{typeKey}/batches/{id}")
    @ResponseBody
    public Map<String, Object> getBatchByType(@PathVariable String typeKey, @PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权查看数据导入批次")) return result;
            DataElementUploadBatch batch = dataElementService.findBatchById(DataElementFieldDict.getDataTypeByKey(typeKey), id);
            if (batch == null) {
                result.put("success", false);
                result.put("message", "批次不存在");
                return result;
            }
            result.put("success", true);
            result.put("batch", batch);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/batches/{id}/records")
    @ResponseBody
    public Map<String, Object> getBatchRecords(@PathVariable Long id, HttpSession session) {
        return getBatchRecordsByType(DataElementFieldDict.KEY_BUSINESS, id, session);
    }

    @GetMapping("/{typeKey}/batches/{id}/records")
    @ResponseBody
    public Map<String, Object> getBatchRecordsByType(@PathVariable String typeKey,
                                                     @PathVariable Long id,
                                                     HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权查看数据要素记录")) return result;
            List<DataElementBusinessRecord> records = dataElementService.findRecordsByBatchId(DataElementFieldDict.getDataTypeByKey(typeKey), id);
            result.put("success", true);
            result.put("records", records);
            result.put("total", records.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/batches/{id}/unrecognized-columns")
    @ResponseBody
    public Map<String, Object> getBatchUnrecognizedColumns(@PathVariable Long id, HttpSession session) {
        return getBatchUnrecognizedColumnsByType(DataElementFieldDict.KEY_BUSINESS, id, session);
    }

    @GetMapping("/{typeKey}/batches/{id}/unrecognized-columns")
    @ResponseBody
    public Map<String, Object> getBatchUnrecognizedColumnsByType(@PathVariable String typeKey,
                                                                 @PathVariable Long id,
                                                                 HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权查看扩展字段")) return result;
            DataElementUploadBatch batch = dataElementService.findBatchById(DataElementFieldDict.getDataTypeByKey(typeKey), id);
            if (batch == null) {
                result.put("success", false);
                result.put("message", "批次不存在");
                return result;
            }
            result.put("success", true);
            result.put("unrecognizedColumns", batch.getUnrecognizedColumns());
            result.put("batchName", batch.getBatchName());
            result.put("originalFileName", batch.getOriginalFileName());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/batches/{id}/download")
    public ResponseEntity<byte[]> downloadBatchFile(@PathVariable Long id, HttpSession session) {
        return downloadBatchFileByType(DataElementFieldDict.KEY_BUSINESS, id, session);
    }

    @GetMapping("/{typeKey}/batches/{id}/download")
    public ResponseEntity<byte[]> downloadBatchFileByType(@PathVariable String typeKey,
                                                          @PathVariable Long id,
                                                          HttpSession session) {
        try {
            String currentUser = (String) session.getAttribute("currentUser");
            if (currentUser == null) return ResponseEntity.status(401).build();
            if (AccessControlUtil.isExternalDepartment(session)) return ResponseEntity.status(403).build();
            if (!DataElementFieldDict.isValidTypeKey(typeKey)) return ResponseEntity.notFound().build();

            DataElementUploadBatch batch = dataElementService.findBatchById(DataElementFieldDict.getDataTypeByKey(typeKey), id);
            if (batch == null || batch.getStoredFilePath() == null) {
                return ResponseEntity.notFound().build();
            }

            String absolutePath = FileUploadUtil.getAbsolutePath(batch.getStoredFilePath());
            java.io.File file = new java.io.File(absolutePath);
            if (!file.exists()) return ResponseEntity.notFound().build();

            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    new String(batch.getOriginalFileName().getBytes("UTF-8"), "ISO-8859-1"));

            return ResponseEntity.ok().headers(headers).body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/batches/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteBatch(@PathVariable Long id, HttpSession session) {
        return deleteBatchByType(DataElementFieldDict.KEY_BUSINESS, id, session);
    }

    @PostMapping("/{typeKey}/batches/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteBatchByType(@PathVariable String typeKey,
                                                 @PathVariable Long id,
                                                 HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权删除数据导入批次")) return result;
            dataElementService.deleteBatch(DataElementFieldDict.getDataTypeByKey(typeKey), id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    // ==================== 单条记录操作 ====================

    @PostMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteRecord(@PathVariable Long id, HttpSession session) {
        return deleteRecordByType(DataElementFieldDict.KEY_BUSINESS, id, session);
    }

    @PostMapping("/{typeKey}/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteRecordByType(@PathVariable String typeKey,
                                                  @PathVariable Long id,
                                                  HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权删除数据要素记录")) return result;
            dataElementService.deleteRecord(DataElementFieldDict.getDataTypeByKey(typeKey), id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/records/{id}/extra-json")
    @ResponseBody
    public Map<String, Object> getRecordExtraJson(@PathVariable Long id, HttpSession session) {
        return getRecordExtraJsonByType(DataElementFieldDict.KEY_BUSINESS, id, session);
    }

    @GetMapping("/{typeKey}/records/{id}/extra-json")
    @ResponseBody
    public Map<String, Object> getRecordExtraJsonByType(@PathVariable String typeKey,
                                                        @PathVariable Long id,
                                                        HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!validateTypeKey(typeKey, result)) return result;
        try {
            if (!ensureInternalUser(session, result, "外部部门账号无权查看数据要素扩展字段")) return result;
            DataElementBusinessRecord record = dataElementService.findRecordById(id);
            if (record == null || !matchesCurrentDataType(DataElementFieldDict.getDataTypeByKey(typeKey), record.getDataType())) {
                result.put("success", false);
                result.put("message", "记录不存在");
                return result;
            }
            result.put("success", true);
            result.put("extraJson", record.getExtraJson());
            result.put("displayName", record.getDisplayName());
            result.put("dataSubtype", record.getDataSubtype());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ========== 旧接口重定向 ==========

    @GetMapping("/detail/{id}")
    public String oldDetail(@PathVariable Long id, HttpSession session) {
        if (AccessControlUtil.isExternalDepartment(session)) return "redirect:/service-components";
        return "redirect:/data-elements/business";
    }

    @GetMapping("/edit/{id}")
    public String oldEdit(@PathVariable Long id, HttpSession session) {
        if (AccessControlUtil.isExternalDepartment(session)) return "redirect:/service-components";
        return "redirect:/data-elements/business";
    }

    private boolean validateTypeKey(String typeKey, Map<String, Object> result) {
        if (DataElementFieldDict.isValidTypeKey(typeKey)) return true;
        result.put("success", false);
        result.put("message", "数据一级类型不存在");
        return false;
    }

    private boolean ensureInternalUser(HttpSession session, Map<String, Object> result, String externalMessage) {
        String currentUser = (String) session.getAttribute("currentUser");
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return false;
        }
        if (AccessControlUtil.isExternalDepartment(session)) {
            result.put("success", false);
            result.put("message", externalMessage);
            return false;
        }
        return true;
    }

    private boolean matchesListFilter(Map<String, Object> item, String dataSubtype, String keyword) {
        if (dataSubtype != null && !dataSubtype.trim().isEmpty()) {
            Object name = item.get("name");
            if (name == null || !dataSubtype.trim().equals(name.toString())) {
                return false;
            }
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        String[] keys = {"name", "tableName", "lastUploader", "lastFileName"};
        for (String key : keys) {
            Object value = item.get(key);
            if (value != null && value.toString().toLowerCase().contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, Object>> buildSubtypeOptions(List<DataElementSubtypeConfig> subtypeConfigs) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (DataElementSubtypeConfig config : subtypeConfigs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", config.getId());
            item.put("name", config.getDataSubtype());
            item.put("tableName", config.getTableName());
            item.put("fieldCount", config.getFieldCount() != null ? config.getFieldCount() : 0);
            options.add(item);
        }
        return options;
    }

    private List<Map<String, Object>> buildBusinessMetadataFieldRows(List<DataElementSubtypeConfig> businessSubtypeConfigs,
                                                                     String dataSubtype,
                                                                     String keyword) {
        Map<String, List<String[]>> fieldMap = DataElementFieldDict.getSubtypeFieldDisplay(DataElementFieldDict.TYPE_BUSINESS);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DataElementSubtypeConfig config : businessSubtypeConfigs) {
            String subtype = config.getDataSubtype();
            List<String[]> fields = fieldMap.get(subtype);
            if (fields == null) continue;
            for (int i = 0; i < fields.size(); i++) {
                String[] field = fields.get(i);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("dataType", DataElementFieldDict.TYPE_BUSINESS);
                row.put("dataSubtype", subtype);
                row.put("tableName", config.getTableName());
                row.put("fieldNo", i + 1);
                row.put("cnName", field[0]);
                row.put("enName", field[1]);
                row.put("metadataType", "业务表头字段");
                if (matchesMetadataFieldFilter(row, dataSubtype, keyword)) {
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private Map<String, Object> buildBusinessMetadataSummary(List<DataElementSubtypeConfig> businessSubtypeConfigs,
                                                            List<Map<String, Object>> filteredRows) {
        Map<String, List<String[]>> fieldMap = DataElementFieldDict.getSubtypeFieldDisplay(DataElementFieldDict.TYPE_BUSINESS);
        int totalFieldCount = 0;
        for (DataElementSubtypeConfig config : businessSubtypeConfigs) {
            List<String[]> fields = fieldMap.get(config.getDataSubtype());
            if (fields != null) {
                totalFieldCount += fields.size();
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tableCount", businessSubtypeConfigs.size());
        summary.put("fieldCount", totalFieldCount);
        summary.put("visibleFieldCount", filteredRows != null ? filteredRows.size() : 0);
        return summary;
    }

    private boolean matchesMetadataFieldFilter(Map<String, Object> row, String dataSubtype, String keyword) {
        if (dataSubtype != null && !dataSubtype.trim().isEmpty()) {
            Object subtype = row.get("dataSubtype");
            if (subtype == null || !dataSubtype.trim().equals(subtype.toString())) {
                return false;
            }
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        String[] keys = {"dataSubtype", "tableName", "cnName", "enName", "metadataType"};
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null && value.toString().toLowerCase().contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String keywordPlaceholder(String dataType) {
        if (DataElementFieldDict.TYPE_METADATA.equals(dataType)) {
            return "数据子类、数据库表名、表头字段、数据库字段名";
        }
        if (DataElementFieldDict.TYPE_SERVICE_INSTANCE.equals(dataType)) {
            return "数据子类、数据库表名、服务名称、授权单位";
        }
        if (DataElementFieldDict.TYPE_LOG.equals(dataType)) {
            return "数据子类、数据库表名、日志ID、模块名称";
        }
        return "数据子类、数据库表名、最近上传文件";
    }

    private Map<String, Object> buildServiceInstanceOverview() {
        List<ServiceComponent> services = serviceComponentService.findAll();
        LinkedHashMap<String, Map<String, Object>> departmentRows = new LinkedHashMap<>();
        List<String> authorizedServices = new ArrayList<>();
        List<String> publishedServices = new ArrayList<>();
        int authorizedCount = 0;
        int publishedCount = 0;

        for (ServiceComponent service : services) {
            String ownerDepartment = normalizedDepartment(service.getOwnerDepartment());
            Map<String, Object> ownerRow = ensureDepartmentRow(departmentRows, ownerDepartment);
            increment(ownerRow, "applyCount");
            addServiceName(ownerRow, "submittedServiceList", service.getServiceName());

            String status = serviceStatus(service);
            if ("AUTHORIZED".equals(status)) {
                authorizedCount++;
                increment(ownerRow, "authorizedCount");
                addServiceName(ownerRow, "authorizedServiceList", service.getServiceName());
                addName(authorizedServices, service.getServiceName());
            } else if ("PUBLISHED".equals(status)) {
                publishedCount++;
                increment(ownerRow, "publishedCount");
                addServiceName(ownerRow, "publishedServiceList", service.getServiceName());
                addName(publishedServices, service.getServiceName());
            }

            List<String> units = splitUnits(service.getAuthorizedUnits());
            for (String unit : units) {
                ensureDepartmentRow(departmentRows, unit);
            }
        }

        for (Map.Entry<String, Map<String, Object>> entry : departmentRows.entrySet()) {
            String department = entry.getKey();
            Map<String, Object> row = entry.getValue();
            for (ServiceComponent service : services) {
                if (canDepartmentAccessService(department, service)) {
                    increment(row, "accessCount");
                    addServiceName(row, "accessibleServiceList", service.getServiceName());
                    if (service.getApiUrl() != null && !service.getApiUrl().trim().isEmpty()) {
                        increment(row, "apiCallCount");
                    }
                }
            }
            finalizeServiceNames(row);
        }

        int accessRelationCount = 0;
        int apiCallableCount = 0;
        for (Map<String, Object> row : departmentRows.values()) {
            accessRelationCount += intValue(row.get("accessCount"));
            apiCallableCount += intValue(row.get("apiCallCount"));
        }

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("departmentCount", departmentRows.size());
        overview.put("applicationCount", services.size());
        overview.put("authorizedCount", authorizedCount);
        overview.put("publishedCount", publishedCount);
        overview.put("accessRelationCount", accessRelationCount);
        overview.put("apiCallableCount", apiCallableCount);
        overview.put("authorizedServiceNames", joinNames(authorizedServices));
        overview.put("publishedServiceNames", joinNames(publishedServices));
        overview.put("departmentRows", new ArrayList<Map<String, Object>>(departmentRows.values()));
        return overview;
    }

    private Map<String, Object> ensureDepartmentRow(LinkedHashMap<String, Map<String, Object>> rows, String department) {
        Map<String, Object> row = rows.get(department);
        if (row != null) return row;
        row = new LinkedHashMap<>();
        row.put("department", department);
        row.put("applyCount", 0);
        row.put("authorizedCount", 0);
        row.put("publishedCount", 0);
        row.put("accessCount", 0);
        row.put("apiCallCount", 0);
        row.put("submittedServiceList", new ArrayList<String>());
        row.put("authorizedServiceList", new ArrayList<String>());
        row.put("publishedServiceList", new ArrayList<String>());
        row.put("accessibleServiceList", new ArrayList<String>());
        rows.put(department, row);
        return row;
    }

    private boolean canDepartmentAccessService(String department, ServiceComponent service) {
        if (department.equals(normalizedDepartment(service.getOwnerDepartment()))) {
            return true;
        }
        String status = serviceStatus(service);
        if ("PUBLISHED".equals(status)) {
            return true;
        }
        if (!"AUTHORIZED".equals(status)) {
            return false;
        }
        List<String> units = splitUnits(service.getAuthorizedUnits());
        return units.contains(department);
    }

    private List<String> splitUnits(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) return result;
        String normalized = value.replace("，", "、")
                .replace(",", "、")
                .replace("；", "、")
                .replace(";", "、")
                .replace("/", "、");
        String[] parts = normalized.split("、");
        for (String part : parts) {
            String unit = normalizedDepartment(part);
            if (!unit.isEmpty() && !result.contains(unit)) {
                result.add(unit);
            }
        }
        return result;
    }

    private void increment(Map<String, Object> row, String key) {
        row.put(key, intValue(row.get(key)) + 1);
    }

    @SuppressWarnings("unchecked")
    private void addServiceName(Map<String, Object> row, String key, String serviceName) {
        Object value = row.get(key);
        if (!(value instanceof List)) return;
        addName((List<String>) value, serviceName);
    }

    private void addName(List<String> list, String name) {
        if (name == null || name.trim().isEmpty()) return;
        String normalized = name.trim();
        if (!list.contains(normalized)) {
            list.add(normalized);
        }
    }

    @SuppressWarnings("unchecked")
    private void finalizeServiceNames(Map<String, Object> row) {
        row.put("submittedServiceNames", joinNames((List<String>) row.get("submittedServiceList")));
        row.put("authorizedServiceNames", joinNames((List<String>) row.get("authorizedServiceList")));
        row.put("publishedServiceNames", joinNames((List<String>) row.get("publishedServiceList")));
        row.put("accessibleServiceNames", joinNames((List<String>) row.get("accessibleServiceList")));
    }

    private String joinNames(List<String> list) {
        if (list == null || list.isEmpty()) return "-";
        return String.join("、", list);
    }

    private int intValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private String normalizedDepartment(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "未填写部门";
        }
        return value.trim();
    }

    private String serviceStatus(ServiceComponent service) {
        return service != null && service.getStatus() != null ? service.getStatus().name() : "";
    }

    private boolean matchesCurrentDataType(String expectedDataType, String actualDataType) {
        String actual = actualDataType != null && !actualDataType.trim().isEmpty()
                ? actualDataType
                : DataElementFieldDict.TYPE_BUSINESS;
        return expectedDataType.equals(actual);
    }
}
