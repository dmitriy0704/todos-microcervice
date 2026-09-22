package dev.folomkin.notifications.service;


import dev.folomkin.notifications.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskEventConsumer {

    // Обычное чтение объекта (Spring автоматически десериализует JSON в TaskEvent)

    @KafkaListener(
            topics = "tasks-topic",
            groupId = "notifications-group"
    )
    public void consumeTaskEvent(TaskEvent event,  Acknowledgment acknowledgment) {
        try {
            log.info("Обработка сообщения из Кафки: {}", event.getTaskId());
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

// ----------------------

// # Надежность отправки сообщений
//
//Да, этот код концептуально соответствует настройке ack-mode: manual_immediate,
// но в нем есть одна очень важная ловушка, из-за которой поведение системы может
// отличаться от ваших ожиданий (особенно в блоке catch).
//Давайте разберем подробно, как это работает на самом деле в Spring Boot, и где
// кроется подвох.
//
//## Что здесь работает правильно?
//В секции try всё отлично. При настройке manual_immediate, как только ваш код
// вызывает acknowledgment.acknowledge(), Spring Kafka моментально отправляет
// запрос на брокер для фиксации (коммита) этого смещения (offset). Указатель
// сдвигается, сообщение считается успешно обработанным.
//
//## В чем заключается ловушка (блок catch)?
//В вашем комментарии написано: «При следующем перезапуске под снова получит это
// сообщение...» — и вот здесь кроется критическая ошибка понимания работы
// Spring Kafka Container.
//Если вы просто поймали Exception, залогировали его и вышли из метода, для
// Spring Kafka это выглядит так: «Метод @KafkaListener завершился без ошибок».
//Даже если вы не вызвали acknowledgment.acknowledge(), цикл poll() консьюмера
// продолжит читать следующие сообщения из топика в рамках этой же сессии.
// Сообщение не будет переотправлено вам немедленно, и поток не заблокируется.
//
//Что произойдет на самом деле при вашем коде:
//
//   1. Вы упали в catch, залогировали ошибку и вышли из метода.
//   2. Консьюмер берет из буфера следующее сообщение и снова вызывает ваш
//   @KafkaListener.
//   3. Если следующее сообщение обработается успешно и вы вызовете
//   acknowledgment.acknowledge(), то Spring закоммитит новый, более дальний
//   offset.
//   4. Итог: Упавшее сообщение будет визуально «пропущено» брокером (хотя в
//   БД или логику оно не попало). Но если в этот момент перезапустить
//   приложение (до того, как обработалось следующее успешное сообщение),
//   то да — чтение начнется с упавшего сообщения.
//
//
//## Как сделать правильно?
//В Spring Kafka управление ошибками при ручном коммите должно быть явным. У
// вас есть три правильных пути в зависимости от бизнес-логики:
//
//## Вариант 1. Использовать @RetryableTopic (Рекомендуется)
//Не нужно писать try-catch внутри метода для логирования ошибок. Позвольте
// исключению вылететь наружу из метода @KafkaListener. Spring сам перехватит
// его, не закоммитит offset и отправит сообщение в Retry-топик, а после — в DLT.
//
//```java
//@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000))
//@KafkaListener(topics = "my-topic", groupId = "my-group")
// public void listen(MyEvent event, Acknowledgment acknowledgment) {
//    log.info("Обработка сообщения: {}", event.getTaskId());
//
//    // Ваша бизнес-логика (без try-catch)
//    orderService.process(event);
//
//    // Коммитим ТОЛЬКО при успехе
//    acknowledgment.acknowledge();
//}
//
//```
//
//## Вариант 2. Если нужно жестко заблокировать поток до исправления (Stop
// Container)
//Если данные критичны и их нельзя пропускать или нарушать порядок, при ошибке
// нужно принудительно останавливать чтение топика, выбрасывая специальное
// исключение Exception наружу (опять же, не глотать его в catch), чтобы
// сработал стандартный DefaultErrorHandler. Он будет пытаться вызвать метод
// снова или остановит контейнер.
//
//## Вариант 3. Ручной откат (nack) — если нужно повторить именно сейчас
//Если вы хотите при ошибке сразу же попробовать обработать это сообщение еще
// раз (или вернуть его в очередь внутри приложения), вместо пустого catch
// нужно вызвать nack(...):
//
//```java
//try {
//    log.info("Обработка сообщения: {}", event.getTaskId());
//    orderService.process(event);
//    acknowledgment.acknowledge(); // Успех
//} catch (Exception e) {
//    log.error("Ошибка! Просим Кафку вернуть сообщение на повтор", e);
//    // nack(sleepTime) — сбрасывает текущую пачку (poll)
//    // и заставляет консьюмер снова прочитать это упавшее сообщение через
//    1000 мс acknowledgment.nack(Duration.ofMillis(1000));
//}
//
//```
//
//
//## Резюме
//Ваш код в try идеален для manual_immediate. Но пустой catch без проброса
// Exception наружу или без вызова acknowledgment.nack(...) приведет к скрытой
// потере сообщений (они пропустятся при обработке последующих удачных записей).

