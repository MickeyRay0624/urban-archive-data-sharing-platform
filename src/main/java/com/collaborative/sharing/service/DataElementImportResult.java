package com.collaborative.sharing.service;

import java.util.List;

/**
 * Excel 导入结果，包含所有统计信息和未识别字段详情。
 * Java 8 兼容。
 */
public class DataElementImportResult {
    private boolean success;
    private String message;
    private Long batchId;
    private int recognizedColumnCount;
    private int unrecognizedColumnCount;
    private int importedRowCount;
    private int businessRecordCount;
    private List<String> involvedSubtypes;
    private List<UnrecognizedColumnInfo> unrecognizedColumns;

    public DataElementImportResult() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public int getRecognizedColumnCount() { return recognizedColumnCount; }
    public void setRecognizedColumnCount(int recognizedColumnCount) { this.recognizedColumnCount = recognizedColumnCount; }

    public int getUnrecognizedColumnCount() { return unrecognizedColumnCount; }
    public void setUnrecognizedColumnCount(int unrecognizedColumnCount) { this.unrecognizedColumnCount = unrecognizedColumnCount; }

    public int getImportedRowCount() { return importedRowCount; }
    public void setImportedRowCount(int importedRowCount) { this.importedRowCount = importedRowCount; }

    public int getBusinessRecordCount() { return businessRecordCount; }
    public void setBusinessRecordCount(int businessRecordCount) { this.businessRecordCount = businessRecordCount; }

    public List<String> getInvolvedSubtypes() { return involvedSubtypes; }
    public void setInvolvedSubtypes(List<String> involvedSubtypes) { this.involvedSubtypes = involvedSubtypes; }

    public List<UnrecognizedColumnInfo> getUnrecognizedColumns() { return unrecognizedColumns; }
    public void setUnrecognizedColumns(List<UnrecognizedColumnInfo> unrecognizedColumns) { this.unrecognizedColumns = unrecognizedColumns; }

    public String getInvolvedSubtypesString() {
        if (involvedSubtypes == null || involvedSubtypes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < involvedSubtypes.size(); i++) {
            if (i > 0) sb.append("、");
            sb.append(involvedSubtypes.get(i));
        }
        return sb.toString();
    }
}
