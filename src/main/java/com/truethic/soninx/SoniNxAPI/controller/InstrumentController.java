package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.InstrumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class InstrumentController {
    @Autowired
    InstrumentService instrumentService;

    @PostMapping(path = "/create_instrument")
    public ResponseEntity<?> createInstrument(HttpServletRequest request) {
        return ResponseEntity.ok(instrumentService.createInstrument(request));
    }

    @PostMapping(path = "/DTInstrument")
    public Object DTInstrument(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return instrumentService.DTInstrument(request, httpServletRequest);
    }

    @PostMapping(path = "/findInstrument")
    public Object findInstrument(@RequestBody Map<String, String> request) {
        return instrumentService.findInstrument(request);
    }

    @PostMapping(path = "/updateInstrument")
    public Object updateInstrument(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return instrumentService.updateInstrument(requestParam, request);
    }

    @PostMapping(path = "/deleteInstrument")
    public Object deleteInstrument(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return instrumentService.deleteInstrument(requestParam, request);
    }

    @GetMapping(path = "/instrument-list")
    public Object getInstrument() {
        return instrumentService.getInstrument();
    }

}
