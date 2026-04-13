// com.cinema.ai.CinemaAiApplication.java
package com.cinema.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.cinema.ai.client")
// 同时扫描 auth 和 movie 的 mapper 包
@MapperScan({"com.cinema.auth.mapper", "com.cinema.ai.mapper"})
// 扫描所有模块的 Bean（auth 和 movie）
@ComponentScan(basePackages = {"com.cinema.auth", "com.cinema.ai"})
public class CinemaAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinemaAiApplication.class, args);
    }
}