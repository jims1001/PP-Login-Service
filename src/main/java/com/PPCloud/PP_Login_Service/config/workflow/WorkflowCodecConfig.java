package com.PPCloud.PP_Login_Service.config.workflow;

import com.PPCloud.PP_Login_Service.core.workflow.HmacStateTokenCodec;
import com.PPCloud.PP_Login_Service.core.workflow.JsonBase64StateCodec;
import com.PPCloud.PP_Login_Service.core.workflow.StateCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowCodecConfig {

    @Bean
    public StateCodec stateCodec(
            @Value("${pp.workflow.state-token.codec:hmac}") String type,
            JsonBase64StateCodec json,
            HmacStateTokenCodec hmac
    ) {
        return "json".equalsIgnoreCase(type) ? json : hmac;
    }
}