package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.RoleMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class RoleMasterController {
    @Autowired
    private RoleMasterService roleMasterService;
    @PostMapping(path = "/register_role")
    public Object createRole(HttpServletRequest request) {
        JsonObject res = roleMasterService.addRole(request);
        return res.toString();
    }
    @PostMapping(path = "/get_role_by_id")
    public Object getRoleById(HttpServletRequest requestParam) {
        JsonObject response = roleMasterService.getRolesById(requestParam.getParameter("role_id"));
            return response.toString();
    }

    @PostMapping(path = "/get_role_by_id_for_edit")
    public Object getRoleByIdForEdit(HttpServletRequest requestParam) {
        JsonObject response = roleMasterService.getRoleByIdForEdit(requestParam);
        return response.toString();
    }

    @PostMapping(path = "/update_role")
    public ResponseEntity<?> updateRole(HttpServletRequest request) {
        return ResponseEntity.ok(roleMasterService.updateRole(request));
    }

    @PostMapping(path="/remove_role")
    public Object removeRole(HttpServletRequest request)
    {
        JsonObject result=roleMasterService.removerRole(request);
        return result.toString();
    }

    @PostMapping(path = "/get_role_permissions")
    public Object getRolePermissions(HttpServletRequest request) {
        JsonObject res = roleMasterService.getRolePermissions(request);
        return res.toString();
    }

    @GetMapping(path = "/get_all_roles")
    public Object getAllRoles(HttpServletRequest request) {
        JsonObject res = roleMasterService.getAllRoles(request);
        return res.toString();
    }

    @PostMapping(path = "/DTRole")
    public Object DTRole(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return roleMasterService.DTRole(request, httpServletRequest);
    }
}
