package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.ToolMgmtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class ToolManagementController {

    @Autowired
    ToolMgmtService toolMgmtService;

    @PostMapping(path = "/create_tool_mgmt")
    public ResponseEntity<?> createToolManagement(HttpServletRequest request) {
        return ResponseEntity.ok(toolMgmtService.createToolManagement(request));
    }

    @PostMapping(path = "/DTToolMgmt")
    public Object DTToolMgmt(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return toolMgmtService.DTToolMgmt(request, httpServletRequest);
    }

    @PostMapping(path = "/findToolMgmt")
    public Object findToolMgmt(@RequestBody Map<String, String> request) {
        return toolMgmtService.findToolMgmt(request);
    }

    @PostMapping(path = "/updateToolMgmt")
    public Object updateToolMgmt(HttpServletRequest request) {
        return toolMgmtService.updateToolMgmt(request);
    }

    @PostMapping(path = "/deleteToolMgmt")
    public Object deleteToolMgmt(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return toolMgmtService.deleteToolMgmt(requestParam, request);
    }

    @GetMapping(path = "/toolMgmt-list")
    public Object getToolMgmt() {
        return toolMgmtService.getToolMgmt();
    }

}
