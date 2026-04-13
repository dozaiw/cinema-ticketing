package com.cinema.movie;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableDiscoveryClient
// 同时扫描auth和movie的mapper包
@MapperScan({"com.cinema.auth.mapper", "com.cinema.movie.mapper"})
// 扫描所有模块的Bean（auth和movie）
@ComponentScan(basePackages = {"com.cinema.auth", "com.cinema.movie"})
@EnableScheduling
public class CinemaMovieApplication {
    public static void main(String[] args) {
       SpringApplication.run(CinemaMovieApplication.class, args);
    }
}