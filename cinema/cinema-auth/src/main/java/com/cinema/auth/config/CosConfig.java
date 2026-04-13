package com.cinema.auth.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos")
@Data
public class CosConfig {

    private String secretId;
    private String secretKey;
    private String region;
    private String bucketName;

    private String domain = "https://cinema-bucket-1360069327.cos.accelerate.myqcloud.com";

    private String defaultAvatarUrl = "https://cinema-bucket-1360069327.cos.ap-guangzhou.myqcloud.com/user_avatar/v2-6afa72220d29f045c15217aa6b275808_hd.png";

    private Long maxPosterSize;
    private Long maxTrailerSize;

    @Bean
    public COSClient cosClient() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region regionObj = new Region(region);
        ClientConfig clientConfig = new ClientConfig(regionObj);
        // 开启 CDN 加速（如果需要）
        // clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(cred, clientConfig);
    }
}