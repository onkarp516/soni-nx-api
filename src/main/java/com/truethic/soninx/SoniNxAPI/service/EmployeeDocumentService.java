package com.truethic.soninx.SoniNxAPI.service;

import com.truethic.soninx.SoniNxAPI.repository.DocumentRepository;
import com.truethic.soninx.SoniNxAPI.repository.EmployeeDocumentRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.model.Document;
import com.truethic.soninx.SoniNxAPI.model.EmployeeDocument;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class EmployeeDocumentService {

    @Autowired
    EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    public Object createEmployeeDocument(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        EmployeeDocument employeeDocument = new EmployeeDocument();
        Document document = documentRepository.findByIdAndStatus(Long.parseLong(request.getParameter("documentId")), true);
        employeeDocument.setDocument(document);
        employeeDocument.setImagePath(request.getParameter("imagePath"));
        employeeDocument.setImageKey(request.getParameter("imagekey"));
        employeeDocument.setStatus(true);
        if (request.getHeader("Authorization") != null) {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            employeeDocument.setCreatedBy(user.getId());
            employeeDocument.setInstitute(user.getInstitute());
        }
        try {
            employeeDocumentRepository.save(employeeDocument);
            responseObject.setMessage("Employee Document added successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {

            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }
}
