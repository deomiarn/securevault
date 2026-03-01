package com.securevault.vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VaultServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaultServiceApplication.class, args);
	}

}
