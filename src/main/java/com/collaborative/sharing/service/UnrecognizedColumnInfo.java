package com.collaborative.sharing.service;

import java.util.List;

/**
 * 未识别字段的详细信息。
 * Java 8 兼容。
 */
public class UnrecognizedColumnInfo {
    private String sheetName;
    private int columnIndex;
    private String originalHeader;
    private String normalizedHeader;
    private List<String> sampleValues;
    private boolean savedToExtraJson;

    public UnrecognizedColumnInfo() {}

    public UnrecognizedColumnInfo(String sheetName, int columnIndex, String originalHeader,
                                  String normalizedHeader, List<String> sampleValues, boolean savedToExtraJson) {
        this.sheetName = sheetName;
        this.columnIndex = columnIndex;
        this.originalHeader = originalHeader;
        this.normalizedHeader = normalizedHeader;
        this.sampleValues = sampleValues;
        this.savedToExtraJson = savedToExtraJson;
    }

    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }

    public int getColumnIndex() { return columnIndex; }
    public void setColumnIndex(int columnIndex) { this.columnIndex = columnIndex; }

    public String getOriginalHeader() { return originalHeader; }
    public void setOriginalHeader(String originalHeader) { this.originalHeader = originalHeader; }

    public String getNormalizedHeader() { return normalizedHeader; }
    public void setNormalizedHeader(String normalizedHeader) { this.normalizedHeader = normalizedHeader; }

    public List<String> getSampleValues() { return sampleValues; }
    public void setSampleValues(List<String> sampleValues) { this.sampleValues = sampleValues; }

    public boolean isSavedToExtraJson() { return savedToExtraJson; }
    public void setSavedToExtraJson(boolean savedToExtraJson) { this.savedToExtraJson = savedToExtraJson; }

    /** 获取 Excel 列字母标识，例如第3列 -> C */
    public String getColumnLetter() {
        return columnIndexToLetter(columnIndex);
    }

    private static String columnIndexToLetter(int col) {
        StringBuilder sb = new StringBuilder();
        int c = col;
        while (c >= 0) {
            sb.insert(0, (char) ('A' + (c % 26)));
            c = c / 26 - 1;
        }
        return sb.toString();
    }
}
