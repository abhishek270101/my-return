package com.example.myreturn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MyreturnApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyreturnApplication.class, args);
	}

}
