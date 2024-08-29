package com.truethic.soninx.SoniNxAPI;

import com.truethic.soninx.SoniNxAPI.reporting.FileExporter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SoniNxApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SoniNxApplication.class);
    }

    public static void main(String[] args) {

        SpringApplication.run(SoniNxApplication.class, args);
        System.out.println("Successfully Executed.....!");
    }

    @Bean
    public FileExporter fileExporter() {
        return new FileExporter();
    }
}
