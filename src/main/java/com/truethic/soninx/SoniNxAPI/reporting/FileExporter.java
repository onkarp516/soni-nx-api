package com.truethic.soninx.SoniNxAPI.reporting;

import com.truethic.soninx.SoniNxAPI.model.EmployeeExport;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.*;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FileExporter {

    //Excel Export
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    public void exportToExcel(List<EmployeeExport> listEmployee, HttpServletResponse response) throws IOException {
        workbook = new XSSFWorkbook();
        setResponseHeader(response, "application/octet-stream", ".xlsx", "Employees_");
        writeHeaderLine();
        writeDataLine(listEmployee);
        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    private void setResponseHeader(HttpServletResponse response, String contentType, String extension, String prefix) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String timeStamp = dateFormat.format(new Date());
        String filename = prefix + timeStamp + extension;

        response.setContentType(contentType);

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + filename;
        response.setHeader(headerKey, headerValue);

    }

    private void writeDataLine(List<EmployeeExport> listEmployee) {
        int rowIndex = 1;
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);
        cellStyle.setFont(font);
        for (EmployeeExport emp : listEmployee) {
            XSSFRow row = sheet.createRow(rowIndex++);
            int columnIndex = 0;
            createCell(row, columnIndex++, emp.getId(), cellStyle);
            createCell(row, columnIndex++, emp.getEmpName(), cellStyle);
            createCell(row, columnIndex++, emp.getDeptName(), cellStyle);

        }
    }

    private void writeHeaderLine() {
        sheet = workbook.createSheet("Employee");
        XSSFRow row = sheet.createRow(0);
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        cellStyle.setFont(font);

        createCell(row, 0, "ID", cellStyle);
        createCell(row, 1, "Name", cellStyle);
        createCell(row, 2, "Deparment Name", cellStyle);

    }

    private void createCell(XSSFRow row, int columnIndex, Object value, CellStyle style) {
        XSSFCell cell = row.createCell(columnIndex);
        sheet.autoSizeColumn(columnIndex);
        if (value instanceof Integer)
            cell.setCellValue((Integer) value);
        else if (value instanceof Boolean)
            cell.setCellValue((Boolean) value);
        else if (value instanceof Long)
            cell.setCellValue((Long) value);
        else
            cell.setCellValue((String) value);
        cell.setCellStyle(style);

    }


    public void exportToCSV(List<EmployeeExport> listEmployee, HttpServletResponse response) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String timestamp = dateFormat.format(new Date());
        String filename = "Employees_" + timestamp + ".csv";

        response.setContentType("text/csv");

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + filename;
        response.setHeader(headerKey, headerValue);

        ICsvBeanWriter csvBeanWriter = new CsvBeanWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);

        String[] csvHeader = {"Id", "Name", "Department Name"};
        String[] fieldMapping = {"id", "empName", "deptName"};

        csvBeanWriter.writeHeader(csvHeader);
        for (EmployeeExport emp : listEmployee) {
            csvBeanWriter.write(emp, fieldMapping);
        }
        csvBeanWriter.close();

    }

}
