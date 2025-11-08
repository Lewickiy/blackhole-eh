package ru.levitsky.blackholeeh.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.levitsky.blackholeeh.service.BlockClient;

@Configuration
public class ClientConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public BlockClient blockClient(RestTemplate restTemplate) {
        return new BlockClient(restTemplate);
    }
}
