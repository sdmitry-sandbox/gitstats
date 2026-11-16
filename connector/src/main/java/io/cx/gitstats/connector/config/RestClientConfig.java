package io.cx.gitstats.connector.config;

import io.cx.gitstats.connector.gitlab.client.DefaultApi;
import io.cx.gitstats.connector.gitlab.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${gitlab.base-url:https://gitlab.cldx.io}")
    private String baseUrl;

    @Value("${gitlab.private-token:}")
    private String privateToken;

    @Bean
    public DefaultApi gitlabApi() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl);

        if (privateToken != null && !privateToken.isBlank()) {
            builder.defaultHeader("PRIVATE-TOKEN", privateToken);
        }

        ApiClient apiClient = new ApiClient(builder.build());
        apiClient.setBasePath(baseUrl);
        return new DefaultApi(apiClient);
    }
}
