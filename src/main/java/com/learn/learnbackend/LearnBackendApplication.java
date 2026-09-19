package com.learn.learnbackend;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.learn.learnbackend", annotationClass = Mapper.class)
public class LearnBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnBackendApplication.class, args);
    }

}
