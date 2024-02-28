package com.example.producer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class Config {
    private final String topicName ="${topic.name}";
    private final KafkaProperties kafkaProperties;
    public Config(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder
                .name(topicName)
                .partitions(2)
                .replicas(1)
                .build();
    }
}
