package com.loyalty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
@SpringBootApplication @EnableMethodSecurity
public class LoyaltyApplication { public static void main(String[] args) { SpringApplication.run(LoyaltyApplication.class,args); } }
