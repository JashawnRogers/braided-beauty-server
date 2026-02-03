package com.braided_beauty.braided_beauty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BraidedBeautyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BraidedBeautyApplication.class, args);
	}
}
