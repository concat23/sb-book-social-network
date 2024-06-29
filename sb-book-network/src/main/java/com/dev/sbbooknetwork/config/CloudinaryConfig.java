package com.dev.sbbooknetwork.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Component
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryConfig {

    @Value("${cloudinary.cloudName}")
    private String cloudName;
    @Value("${cloudinary.apiKey}")
    private String apiKey;
    @Value("${cloudinary.apiSecret}")
    private String apiSecret;

    public Map<String, String> toMap() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("cloud_name", this.cloudName);
        configMap.put("api_key", this.apiKey);
        configMap.put("api_secret", this.apiSecret);
        return configMap;
    }

}

