package com.cinema.ticketing;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
// 同时扫描auth和movie的mapper包
@MapperScan({"com.cinema.auth.mapper", "com.cinema.ticketing.mapper"})
// 扫描所有模块的Bean（auth和movie）
@ComponentScan(basePackages = {"com.cinema.auth", "com.cinema.ticketing"})
public class CinemaTicketingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinemaTicketingApplication.class, args);
    }
}
