package com.gn.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@EnableEurekaClient
@EnableFeignClients(basePackages = {"com.gn.feign"})
@SpringBootApplication
@MapperScan({"com.gn.repository.mapper"})
@ComponentScan(basePackages ={"com.gn.api","com.gn"})
public class GnApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GnApiApplication.class, args);
    }

}
