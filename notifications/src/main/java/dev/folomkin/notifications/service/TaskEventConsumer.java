package dev.folomkin.notifications.service;


import dev.folomkin.notifications.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskEventConsumer {


    // Обычное чтение объекта (Spring автоматически десериализует JSON в TaskEvent)

    @KafkaListener(
            topics = "tasks-topic",
            groupId = "notifications-group"
    )
    public void consumeTaskEvent(TaskEvent event) {
        log.info("Получена новая задача: ID={}, Заголовок={}", event.getTaskId(), event.getTitle());
        System.out.println("НОВАЯ ЗАДАЧА: " + event.getTaskId() + " " + event.getTitle());
    }


//    // Продвинутый вариант: если вам нужны метаданные (ключ, заголовки, партиция)
//    @KafkaListener(topics = "important-messages-topic")
//    public void listenWithMetadata(
//            @Payload String message,
//            @Header(KafkaHeaders.RECEIVED_KEY) String key,
//            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
//            @Header(KafkaHeaders.OFFSET) long offset) {
//        log.info("Получено сообщение: '{}' с ключом '{}' из партиции {} (смещение {})",
//                message, key, partition, offset);
//    }
}
