package com.cinema.hall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@MapperScan({"com.cinema.auth.mapper", "com.cinema.hall.mapper"})
@ComponentScan(basePackages = {"com.cinema.auth", "com.cinema.hall"})
public class CinemaHallApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinemaHallApplication.class, args);
    }
}