package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class SiteController {
    @Autowired
    private SiteService siteService;

    @PostMapping(path = "createSite")
    public Object createSite(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return siteService.createSite(jsonRequest, request);
    }

    @PostMapping(path = "DTSite")
    public Object DTSite(@RequestBody Map<String, String> jsonRequest, HttpServletRequest httpServletRequest) {
        return siteService.DTSite(jsonRequest, httpServletRequest);
    }

    @PostMapping(path = "/findSite")
    public Object findSite(@RequestBody Map<String, String> requestParam) {
        return siteService.findSite(requestParam);
    }

    @PostMapping(path = "/updateSite")
    public Object updateSite(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return siteService.updateSite(requestParam, request);
    }

    @PostMapping(path = "/changeSiteStatus")
    public Object changeSiteStatus(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return siteService.changeSiteStatus(requestParam, request);
    }

    @GetMapping(path = "/listOfSites")
    public Object listOfSites(HttpServletRequest request) {
        JsonObject object = siteService.listOfSites(request);
        return object.toString();
    }
}
