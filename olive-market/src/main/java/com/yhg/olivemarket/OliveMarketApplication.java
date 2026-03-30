package com.yhg.olivemarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class OliveMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(OliveMarketApplication.class, args);
    }
}
