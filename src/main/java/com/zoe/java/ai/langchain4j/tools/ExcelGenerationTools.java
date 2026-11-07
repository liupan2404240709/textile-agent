package com.zoe.java.ai.langchain4j.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Excel 文件生成工具
 * 用于根据数据生成 Excel 表格文件
 */
@Component
@Slf4j
public class ExcelGenerationTools {

    private static final String OUTPUT_DIR = "./generated_files";

    /**
     * 生成 Excel 文件
     *
     * @param title 表格标题
     * @param headers 表头列表
     * @param data 数据行，每行是一个字符串数组
     * @return 生成的文件路径
     */
    @Tool(name = "generate_excel", value = "根据提供的数据生成Excel文件，返回文件路径。适用于需要创建结构化表格数据的场景。")
    public String generateExcel(
            String title,
            List<String> headers,
            List<List<String>> data) {
        
        try {
            log.info("📊 开始生成 Excel 文件: title={}, headers={}, rows={}", 
                    title, headers.size(), data.size());

            // 确保输出目录存在
            Path outputDir = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // 生成文件名
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileName = "excel_" + timestamp + ".xlsx";
            String filePath = outputDir.resolve(fileName).toString();

            // 创建工作簿
            Workbook workbook = new XSSFWorkbook();
            
            // 创建工作表
            Sheet sheet = workbook.createSheet(title != null ? title : "数据表");

            // 创建标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 创建普通单元格样式
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                // 自动调整列宽
                sheet.setColumnWidth(i, 20 * 256);
            }

            // 填充数据
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<String> rowData = data.get(rowIndex);
                
                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    String value = rowData.get(colIndex);
                    
                    // 尝试判断是否为数字
                    if (value != null && value.matches("-?\\d+(\\.\\d+)?")) {
                        cell.setCellValue(Double.parseDouble(value));
                    } else {
                        cell.setCellValue(value != null ? value : "");
                    }
                    cell.setCellStyle(cellStyle);
                }
            }

            // 自动调整所有列的宽度
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                // 设置最大宽度限制（避免过宽）
                int width = sheet.getColumnWidth(i);
                if (width > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
            
            workbook.close();

            log.info("✅ Excel 文件生成成功: {}", filePath);
            return filePath;

        } catch (IOException e) {
            log.error("❌ Excel 文件生成失败: {}", e.getMessage(), e);
            return "生成失败: " + e.getMessage();
        }
    }

    /**
     * 从 JSON 格式数据生成 Excel（更灵活的方式）
     *
     * @param jsonData JSON 格式的数据，包含 headers 和 rows
     * @return 生成的文件路径
     */
    @Tool(name = "generate_excel_from_json", value = "从JSON数据生成Excel文件。JSON格式：{\"title\": \"标题\", \"headers\": [\"列1\", \"列2\"], \"rows\": [[\"值1\", \"值2\"], [\"值3\", \"值4\"]]}")
    public String generateExcelFromJson(String jsonData) {
        try {
            log.info("📊 从 JSON 数据生成 Excel");
            
            // 简单的 JSON 解析（实际项目中建议使用 Jackson 或 Gson）
            // 这里为了简化，假设大模型会提供结构化的参数
            
            // 由于 LangChain4j Tool 的限制，我们使用上面的 generateExcel 方法
            // 这个方法主要作为备用入口
            return "请使用 generate_excel 工具，直接提供 headers 和 data 参数";
            
        } catch (Exception e) {
            log.error("❌ 从 JSON 生成 Excel 失败: {}", e.getMessage(), e);
            return "生成失败: " + e.getMessage();
        }
    }
}
