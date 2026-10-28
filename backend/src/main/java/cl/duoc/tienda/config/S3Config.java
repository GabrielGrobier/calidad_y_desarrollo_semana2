package cl.duoc.tienda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
    @Bean
    public S3Client s3Client(
            @Value("${app.s3.region}") String region,
            @Value("${app.aws.access-key}") String accessKey,
            @Value("${app.aws.secret-key}") String secretKey,
            @Value("${app.aws.session-token}") String sessionToken) {
        if (accessKey.isBlank() || secretKey.isBlank() || sessionToken.isBlank()) {
            throw new IllegalStateException("Faltan credenciales temporales AWS Labs (Access Key, Secret Key o Session Token)");
        }
        AwsSessionCredentials credentials = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
