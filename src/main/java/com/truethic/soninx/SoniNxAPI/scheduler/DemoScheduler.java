package com.truethic.soninx.SoniNxAPI.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@Slf4j
public class DemoScheduler {

    //    @Scheduled(cron = "0 0/10 * * * *")
    public void reportCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        log.info("The time is now {}", dateFormat.format(new Date()));
    }
}
