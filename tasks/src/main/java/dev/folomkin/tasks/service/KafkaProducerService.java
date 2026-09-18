package dev.folomkin.tasks.service;

import dev.folomkin.tasks.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducerService {

    // Указываем конкретные типы: Ключ — String, Значение — TaskEvent
    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, TaskEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String TOPIC = "user-events";

    public void sendMessage(String topic, String key, TaskEvent taskEvent) {
        log.info("Отправка задачи {} в топик {}", taskEvent.getId(), topic);

        // Передаем объект taskEvent прямо в метод send()
        kafkaTemplate.send(topic, key, taskEvent)
                .thenAccept(result -> {
                    System.out.println("Сообщение успешно отправлено в смещение (offset): "
                            + result.getRecordMetadata().offset());
                })
                .exceptionally(ex -> {
                    System.err.println("Ошибка при отправке сообщения: " + ex.getMessage());
                    return null;
                });
    }
}
