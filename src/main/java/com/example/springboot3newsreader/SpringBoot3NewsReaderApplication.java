package com.example.springboot3newsreader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
// 启用定时任务（用于后台补图任务）
@EnableScheduling
public class SpringBoot3NewsReaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBoot3NewsReaderApplication.class, args);
	}

}
