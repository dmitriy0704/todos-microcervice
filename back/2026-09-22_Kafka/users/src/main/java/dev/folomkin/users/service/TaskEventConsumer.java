package dev.folomkin.users.service;


import dev.folomkin.users.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskEventConsumer {

    @KafkaListener(
            topics = "tasks-topic",
            groupId = "users-group"
    )
    public void consumeTaskEvent(TaskEvent event,  Acknowledgment acknowledgment) {
        try {
            log.info("🚀 Обработка сообщения из Кафки: {}", event.getTaskId());

            // Тут ваша бизнес-логика (запись в БД, логика и т.д.)

            // СТРОГО В САМОМ КОНЦЕ: Если код дошел сюда без ошибок,
            // мы вежливо говорим Кафке: "Я всё успешно сделал, сдвигай указатель!"
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения. Указатель НЕ сдвигается, попробуем позже: ", e);
            // Мы НЕ вызываем acknowledgment.acknowledge().
            // При следующем перезапуске под снова получит это сообщение и попытается обработать его еще раз.
        }
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
