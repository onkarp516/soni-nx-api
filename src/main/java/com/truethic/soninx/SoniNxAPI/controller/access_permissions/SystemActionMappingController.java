package com.truethic.soninx.SoniNxAPI.controller.access_permissions;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.access_permissions.SystemActionMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class SystemActionMappingController {
    @Autowired
    private SystemActionMappingService service;

    /* Create System Modules */
    @PostMapping(path = "/create_access_actions")
    public Object createAccessActions(HttpServletRequest request) {
        JsonObject object = service.createAccessActions(request);

        return object.toString();
    }
    @PostMapping(path = "/create_actions_mappings")
    public Object createActionsMappings(HttpServletRequest request) {
        JsonObject object = service.createActionsMappings(request);
        return object.toString();
    }

    /* Get all Actions Mapping like create, edit, delete, list */
    @GetMapping(path = "/get_master_actions")
    public Object getMasterActions() {
        JsonObject res = service.getMasterActions();
        return res.toString();
    }

  /*  @GetMapping(path = "/get_systems_all_permissions")
    public Object getActionsMappingsPermissions() {
        JsonObject res = service.getActionsMappingsPermissions();
        return res.toString();
    }*/
    @GetMapping(path = "/get_systems_all_permissions")
    public Object getMappings() {
        JsonObject res = service.getMappings();
        return res.toString();
    }

    /* Get all ACtions Mappings  for list of mappings */
    @GetMapping(path = "/get_actions_mappings")
    public Object getActionsMappings() {
        JsonObject res = service.getActionsMappings();
        return res.toString();
    }
}
