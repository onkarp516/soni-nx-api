package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class DocumentController {
    @Autowired
    DocumentService documentService;

    @PostMapping(path = "/createDocument")
    public Object createDocument(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return documentService.createDocument(requestParam, request);
    }

    @GetMapping(path = "/listOfDocument")
    public Object listOfDocument(HttpServletRequest httpServletRequest) {
        JsonObject res = documentService.listOfDocument(httpServletRequest);
        return res.toString();
    }

    @PostMapping(path = "/DTDocuments")
    public Object DTDocuments(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return documentService.DTDocuments(request, httpServletRequest);
    }

    @PostMapping(path = "/findDocument")
    public Object findDocument(@RequestBody Map<String, String> requestParam) {
        return documentService.findDocument(requestParam);
    }

    @PostMapping(path = "/updateDocument")
    public Object updateDocument(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return documentService.updateDocument(requestParam, request);
    }

    @PostMapping(path = "/deleteDocument")
    public Object deleteDocument(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return documentService.deleteDocument(requestParam, request);
    }
}
