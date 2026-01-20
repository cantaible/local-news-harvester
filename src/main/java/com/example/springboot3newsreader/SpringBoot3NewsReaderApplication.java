package com.example.springboot3newsreader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringBoot3NewsReaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBoot3NewsReaderApplication.class, args);
	}

}
