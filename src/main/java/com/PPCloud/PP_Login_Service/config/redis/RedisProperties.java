package com.PPCloud.PP_Login_Service.config.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "redis")
@Getter
@Setter
@Validated
public class RedisProperties {
    private String address;
    private String password;
}
