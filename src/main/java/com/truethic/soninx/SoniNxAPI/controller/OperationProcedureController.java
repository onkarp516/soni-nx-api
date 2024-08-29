package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.OperationProcedureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class OperationProcedureController {
    @Autowired
    OperationProcedureService operationProcedureService;

    @PostMapping(path = "/createOperationProcedure")
    public ResponseEntity<?> createOperationProcedure(HttpServletRequest request) {
        return ResponseEntity.ok(operationProcedureService.createOperationProcedure(request));
    }

    @PostMapping(path = "/DTOperationProcedure")
    public Object DTOperationProcedure(HttpServletRequest request) {
        return operationProcedureService.DTOperationProcedure(Integer.parseInt(request.getParameter("start")),
                Integer.parseInt(request.getParameter("limit")));
    }

    @PostMapping(path = "/findOperationProcedure")
    public Object findOperationProcedure(@RequestBody Map<String, String> requestParam) {
        return operationProcedureService.findOperationProcedure(requestParam);
    }

    @PostMapping(path = "/updateOperationProcedure")
    public Object updateOperationProcedure(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return operationProcedureService.updateOperationProcedure(requestParam, request);
    }

    @PostMapping(path = "/deleteOperationProcedure")
    public Object deleteOperationProcedure(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return operationProcedureService.deleteOperationProcedure(requestParam, request);
    }
}
