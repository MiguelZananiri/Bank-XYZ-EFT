package com.duoc.demo.Config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.duoc.demo.Kafka.TopicNames;

@Configuration
public class KafkaTopicsConfiguration {

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean NewTopic transferenciaRealizada() {
        return topic(TopicNames.TRANSFERENCIA_REALIZADA);
    }

    @Bean NewTopic retiroRealidado() {
        return topic(TopicNames.RETIRO_REALIZADO);
    }

    @Bean NewTopic depositoRealizado() {
        return topic(TopicNames.DEPOSITO_REALIZADO);
    }

    

    
}
