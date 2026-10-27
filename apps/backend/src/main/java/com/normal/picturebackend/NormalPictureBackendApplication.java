package com.normal.picturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 程序运行入口
 *
 * @author normal
 */
@SpringBootApplication
@MapperScan("com.normal.picturebackend.infrastructure.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling //
public class NormalPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NormalPictureBackendApplication.class, args);
    }

}
