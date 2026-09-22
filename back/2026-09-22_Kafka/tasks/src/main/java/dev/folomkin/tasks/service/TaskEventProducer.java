package dev.folomkin.tasks.service;

import dev.folomkin.tasks.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskEventProducer {

    // Указываем конкретные типы: Ключ — String, Значение — TaskEvent
    // Spring Boot автоматически настроит этот бин на основе application.properties
    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;


    public TaskEventProducer(KafkaTemplate<String, TaskEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Имя топика, куда отправляем сообщения
    private static final String TOPIC = "tasks-topic";


    public void sendTaskCreatedEvent(TaskEvent taskEvent) {
        kafkaTemplate.send(TOPIC, taskEvent.getTaskId(), taskEvent)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Сообщение успешно доставлено в топик {} partition {} offset {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("ОШИБКА отправки сообщения в Kafka", ex);
                    }
                });
    }
}
