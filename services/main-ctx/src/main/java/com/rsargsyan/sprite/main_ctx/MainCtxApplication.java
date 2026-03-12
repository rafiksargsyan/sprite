package com.rsargsyan.sprite.main_ctx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MainCtxApplication {
	public static void main(String[] args) {
		SpringApplication.run(MainCtxApplication.class, args);
	}
}
