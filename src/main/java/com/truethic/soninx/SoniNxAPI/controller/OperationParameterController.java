package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.OperationParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class OperationParameterController {
    @Autowired
    OperationParameterService operationParameterService;

    @PostMapping(path = "/createOperationParameter")
    public ResponseEntity<?> createOperationParameter(HttpServletRequest request) {
        return ResponseEntity.ok(operationParameterService.createOperationParameter(request));
    }

    @PostMapping(path = "/DTOperationParameter")
    public Object DTOperationParameter(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return operationParameterService.DTOperationParameter(request, httpServletRequest);
    }

    @PostMapping(path = "/findOperationParameter")
    public Object findOperationParameter(@RequestBody Map<String, String> request) {
        return operationParameterService.findOperationParameter(request);
    }

    @PostMapping(path = "/updateOperationParameter")
    public Object updateOperationParameter(HttpServletRequest request) {
        return operationParameterService.updateOperationParameter(request);
    }

    @PostMapping(path = "/deleteOperationParameter")
    public Object deleteOperationParameter(HttpServletRequest request) {
        return operationParameterService.deleteOperationParameter(request);
    }

    @PostMapping(path = "/mobile/getDrawingSizes")
    public Object getDrawingSizes(@RequestBody Map<String, String> request) {
        return operationParameterService.getDrawingSizes(request).toString();
    }
}
