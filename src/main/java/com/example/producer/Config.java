package com.example.producer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class Config {
    private final KafkaProperties kafkaProperties;


    public Config(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

//    @Bean
//    public ProducerFactory<String,String> producerFactory(){
//            Map<String,Object> properties = kafkaProperties.buildProducerProperties();
//        return new DefaultKafkaProducerFactory<>(properties);
//    }

//    @Bean
//    public KafkaTemplate<String, String> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder
                .name("t.food.order")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
