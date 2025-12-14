package com.PPCloud.PP_Login_Service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.flow")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.core.workflow")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.registry")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.port")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.core.store")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.config.utils")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.api.controllers")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.config")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.security")
@ComponentScan(basePackages = "com.PPCloud.PP_Login_Service.config.workflow")
public class PpLoginServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PpLoginServiceApplication.class, args);
	}

}
