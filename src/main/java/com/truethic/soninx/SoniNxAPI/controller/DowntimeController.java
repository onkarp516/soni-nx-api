package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.response.DowntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalTime;
import java.util.Map;

import static java.time.temporal.ChronoUnit.*;

@RestController
public class DowntimeController {
    @Autowired
    private DowntimeService downtimeService;

    @PostMapping(path = "/mobile/saveDowntime")
    public Object saveDowntime(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return downtimeService.saveDowntime(requestParam, request);
    }

    @GetMapping(path = "/getDiffFromTime")
    public void getDiffFromTime() {
        LocalTime l1 = LocalTime.parse("09:30:12");
        LocalTime l2 = LocalTime.parse("11:30:00");
        System.out.println(l1.until(l2, MILLIS));
        System.out.println(l1.until(l2, MINUTES));
        System.out.println("MINUTES " + MINUTES.between(l1, l2));
        System.out.println("SECONDS " + SECONDS.between(l1, l2));
        System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60.0));
    }

    @PostMapping(path = "/getDowntimes")
    public Object getDowntimes(@RequestBody Map<String, String> request) {
        return downtimeService.getDowntimes(request);
    }
}
