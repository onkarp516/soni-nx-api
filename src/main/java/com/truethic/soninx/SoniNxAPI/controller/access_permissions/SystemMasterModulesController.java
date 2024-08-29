package com.truethic.soninx.SoniNxAPI.controller.access_permissions;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.access_permissions.SystemMasterModulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class SystemMasterModulesController {
    @Autowired
    private SystemMasterModulesService service;
    /* Create System Modules */
    @PostMapping(path = "/create_system_modules")
    public Object createSystemModules(HttpServletRequest request) {
        JsonObject object = service.createSystemModules(request);
        return object.toString();

    }

    @GetMapping(path = "/get_system_modules")
    public Object getSystemModules() {
        JsonObject res = service.getSystemModules();
        return res.toString();
    }

    @GetMapping(path = "/get_parents_modules")
    public Object getParents() {
        JsonObject res = service.getParents();
        return res.toString();
    }

}
