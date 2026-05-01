package com.godswill.matrimony;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GodswillApplication {

	public static void main(String[] args) {
		SpringApplication.run(GodswillApplication.class, args);
	}
}	