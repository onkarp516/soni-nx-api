package com.truethic.soninx.SoniNxAPI.reporting;

import com.truethic.soninx.SoniNxAPI.repository.PayrollRepository;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DownloadPdfController {

    @Autowired
    private PayrollRepository payrollRepository;

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/downloadReceipt")
    public ResponseEntity<?> downloadReceipt(HttpServletResponse response) throws IOException {
        Map<String, Object> data = createTestData();
        ByteArrayInputStream exportedData = payrollRepository.exportReceiptPdf("receipt", data);
        /*response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=receipt.pdf");
        IOUtils.copy(exportedData, response.getOutputStream());

*/
        InputStreamResource file = new InputStreamResource(exportedData);

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt.pdf")
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(file);
    }

    private Map<String, Object> createTestData() {
        Map<String, Object> data = new HashMap<>();
        Employee employee = new Employee();
        employee.setFullName("Simple Solution");
        employee.setMobileNumber(Long.valueOf("78978787"));
        employee.setAddress("123, Simple Street");
        employee.setPoliceCaseDetails("contact@simplesolution.dev");
        employee.setDisabilityDetails("123 456 789");
        data.put("employee", employee);
/*
        List<ReceiptItem> receiptItems = new ArrayList<>();
        ReceiptItem receiptItem1 = new ReceiptItem();
        receiptItem1.setDescription("Test Item 1");
        receiptItem1.setQuantity(1);
        receiptItem1.setUnitPrice(100.0);
        receiptItem1.setTotal(100.0);
        receiptItems.add(receiptItem1);

        ReceiptItem receiptItem2 = new ReceiptItem();
        receiptItem2.setDescription("Test Item 2");
        receiptItem2.setQuantity(4);
        receiptItem2.setUnitPrice(500.0);
        receiptItem2.setTotal(2000.0);
        receiptItems.add(receiptItem2);

        ReceiptItem receiptItem3 = new ReceiptItem();
        receiptItem3.setDescription("Test Item 3");
        receiptItem3.setQuantity(2);
        receiptItem3.setUnitPrice(200.0);
        receiptItem3.setTotal(400.0);
        receiptItems.add(receiptItem3);

        data.put("receiptItems", receiptItems);*/
        return data;
    }
}
