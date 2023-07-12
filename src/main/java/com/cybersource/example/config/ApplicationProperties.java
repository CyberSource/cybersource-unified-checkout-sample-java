package com.cybersource.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Data
@Component
@ConfigurationProperties("app")
// Spring automatically looks for an application.properties in src/main/resources and binds to corresponding fields.
public class ApplicationProperties {
    String merchantID;
    String requestHost;
    String merchantKeyId;
    String merchantSecretKey;
    String userAgent;
    String runEnvironment;
    String authenticationType;

    public Properties getAsJavaProperties() {
        Properties props = new Properties();
        props.setProperty("merchantID", getMerchantID());
        props.setProperty("merchantKeyId", getMerchantKeyId());
        props.setProperty("merchantsecretKey", getMerchantSecretKey());
        props.setProperty("userAgent", getUserAgent());
        props.setProperty("requestHost", requestHost);
        props.setProperty("runEnvironment", getRunEnvironment());
        props.setProperty("authenticationType", getAuthenticationType());

        return props;
    }
}
