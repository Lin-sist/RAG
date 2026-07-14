package com.enterprise.rag.auth.bootstrap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth.bootstrap")
public class AuthBootstrapProperties {

    private boolean enabled;
    private String username;
    private String password;
    private String email;
}
