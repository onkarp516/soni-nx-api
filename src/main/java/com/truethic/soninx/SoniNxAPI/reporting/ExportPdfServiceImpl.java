package com.truethic.soninx.SoniNxAPI.reporting;

import com.lowagie.text.DocumentException;
import com.truethic.soninx.SoniNxAPI.repository.PayrollRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class ExportPdfServiceImpl implements PayrollRepository {
    private final Logger logger = LoggerFactory.getLogger(ExportPdfServiceImpl.class);

    @Autowired
    private TemplateEngine templateEngine;

    @Override
    public ByteArrayInputStream exportReceiptPdf(String templateName, Map<String, Object> data) {
        Context context = new Context();
        context.setVariables(data);
        String htmlContent = templateEngine.process(templateName, context);

        ByteArrayInputStream byteArrayInputStream = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(byteArrayOutputStream, false);
            renderer.finishPDF();
            byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (DocumentException e) {
            logger.error(e.getMessage(), e);
        }

        return byteArrayInputStream;
    }
}
