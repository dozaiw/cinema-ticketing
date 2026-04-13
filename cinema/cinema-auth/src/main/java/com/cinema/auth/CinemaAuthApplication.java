package com.cinema.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.cinema.auth.mapper")
@Import(com.cinema.auth.config.SecurityConfig.class)
public class CinemaAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinemaAuthApplication.class, args);

    }
}