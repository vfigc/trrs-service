package com.visionfund.trrs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * AWS SDK clients use the default credentials provider chain - on an EC2 instance that resolves
 * to this service's own instance role via IMDS. No static keys.
 */
@Configuration
public class AwsClientConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}
