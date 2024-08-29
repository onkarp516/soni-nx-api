package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.ActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class ActionController {
    @Autowired
    ActionService actionService;

    @PostMapping(path = "/create_action")
    public ResponseEntity<?> createAction(HttpServletRequest request) {
        return ResponseEntity.ok(actionService.createAction(request));
    }

    @PostMapping(path = "/DTAction")
    public Object DTAction(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return actionService.DTAction(request, httpServletRequest);
    }

    @PostMapping(path = "/findAction")
    public Object findAction(@RequestBody Map<String, String> request) {
        return actionService.findAction(request);
    }

    @PostMapping(path = "/updateAction")
    public Object updateAction(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return actionService.updateAction(requestParam, request);
    }

    @PostMapping(path = "/deleteAction")
    public Object deleteAction(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return actionService.deleteAction(requestParam, request);
    }

    @GetMapping(path = "/action-list")
    public Object getAction() {
        return actionService.getAction();
    }

}
