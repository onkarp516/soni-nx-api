package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.PushMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class PushMessageController {
    @Autowired
    private PushMessageService pushMessageService;

    @PostMapping(path = "/createPushMessage")
    public Object createPushMessage(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return pushMessageService.createPushMessage(jsonRequest, request).toString();
    }
    @PostMapping(path = "updatePushMessage")
    public Object updatePushMessage(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return pushMessageService.updatePushMessage(jsonRequest, request).toString();
    }
    @PostMapping(path = "findPushMessage")
    public Object findPushMessage(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return pushMessageService.findPushMessage(jsonRequest, request).toString();
    }
    @PostMapping(path = "/deletePushMessage")
    public Object deletePushMessage(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return pushMessageService.deletePushMessage(requestParam, request).toString();
    }
    @PostMapping(path = "/DTPushMessage")
    public Object DTPushMessage(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return pushMessageService.DTPushMessage(request, httpServletRequest);
    }
    @GetMapping(path = "/mobile/getPushMessageForMobileApp")
    public Object getPushMessageForMobileApp(HttpServletRequest request) {
        return pushMessageService.getPushMessageForMobileApp(request).toString();
    }
}
