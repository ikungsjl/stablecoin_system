package com.stablecoin.collateral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CollateralServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollateralServiceApplication.class, args);
    }
}
