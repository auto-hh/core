package ru.hh.match;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HhMatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HhMatchServiceApplication.class, args);
    }
}
