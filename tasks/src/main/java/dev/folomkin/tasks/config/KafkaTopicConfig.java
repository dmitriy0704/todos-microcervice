package dev.folomkin.tasks.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic tasksTopic() {
        return TopicBuilder.name("tasks-topic")
                .partitions(3) // Делим на 3 части для будущего масштабирования
                .replicas(1)   // В Minikube у нас одна нода, поэтому реплика 1
                .build();
    }
}