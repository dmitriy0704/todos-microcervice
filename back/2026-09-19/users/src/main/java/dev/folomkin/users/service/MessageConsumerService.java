package dev.folomkin.users.service;


import dev.folomkin.users.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageConsumerService {


    // Обычное чтение объекта (Spring автоматически десериализует JSON в OrderEvent)
    @KafkaListener(topics = "tasks-topic", groupId = "my-cool-group")
    public void listenOrderEvents(TaskEvent event) {
        log.info("Получена новая задача: ID={}, Заголовок={}", event.getId(), event.getTitle());
        // Ваша бизнес-логика здесь
    }

//
//    // Продвинутый вариант: если вам нужны метаданные (ключ, заголовки, партиция)
//    @KafkaListener(topics = "important-messages-topic")
//    public void listenWithMetadata(
//            @Payload String message,
//            @Header(KafkaHeaders.RECEIVED_KEY) String key,
//            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
//            @Header(KafkaHeaders.OFFSET) long offset) {
//
//        log.info("Получено сообщение: '{}' с ключом '{}' из партиции {} (смещение {})",
//                message, key, partition, offset);
//    }
}
