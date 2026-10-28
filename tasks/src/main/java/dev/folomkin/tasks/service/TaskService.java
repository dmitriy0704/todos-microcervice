package dev.folomkin.tasks.service;

import dev.folomkin.tasks.entity.Tasks;
import dev.folomkin.tasks.event.TaskEvent;
import dev.folomkin.tasks.exception.ServiceUnavailableException;
import dev.folomkin.tasks.repository.TaskRepository;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskEventProducer producer;

    public TaskService(
            TaskRepository taskRepository,
            TaskEventProducer producer
    ) {
        this.taskRepository = taskRepository;
        this.producer = producer;
    }

    public List<Tasks> getAllTasks() {
        return taskRepository.findAll();
    }


    public Optional<Tasks> getTaskById(Long id) {
        return taskRepository.findById(id);
    }


    //-> считает ошибки; если их больше 50% за последние 10 вызовов — размыкает цепь,
    // если цепь разомкнута или все попытки провалились — вызывается fallbackCreateTas.
    @Retry(name = "postgres")
    //-> перехватывает вызов и повторяет его до 3 раз при ошибке
    //-> Реализация паттерна Автоматический выключатель
    // Fallback — вызывается, когда цепь разомкнута или все попытки исчерпаны
    @CircuitBreaker(name = "postgres", fallbackMethod = "fallbackCreateTask")
    //-> Bulkhead: ограничивает количество одновременных запросов
    @Bulkhead(name = "postgres", fallbackMethod = "fallbackCreateTask")
    //-> @TimeLimiter(name = "mongoDb") //-> прерывает вызов, если он длится дольше 3 секунд. Для асинхронных методов
    //-> @RateLimiter(name = "mongoDb") - ограничение количества запросов, уточнить от кого
    public Tasks createTask(Tasks task) {
//        return taskRepository.save(task);
        Tasks t = taskRepository.save(task);
        producer.sendTaskCreatedEvent(
                new TaskEvent(
                        task.getId(),
                        task.getTitle(),
                        "USER abc123",
                        "CREATED")
        );
        return t;
    }

    /**
     * Единый fallback для всех трёх декораторов.
     * Различаем причину отказа через instanceof.
     */
    public Tasks fallbackCreateTask(Tasks task, Throwable t) throws ServiceUnavailableException {
        // Уточнить, что это такое:
        // Вариант 1: вернуть «заглушку»
        // Вариант 2: сохранить в очередь для повторной обработки
        // Вариант 3: бросить кастомное исключение


        if (t instanceof BulkheadFullException) {
            log.warn("Bulkhead full: too many concurrent requests, task={}", task.getId());
            throw new ServiceUnavailableException(
                    "Сервис перегружен, попробуйте позже"
            );
        }

        // CallNotPermittedException - если не сработает, создать свой, но это не точно
        if (t instanceof CallNotPermittedException) {
            log.error("CircuitBreaker is OPEN: PostgreSQL unavailable, task={}", task.getId());
            throw new ServiceUnavailableException(
                    "База данных временно недоступна, попробуйте позже"
            );
        }

        log.error("PostgreSQL call failed after retries, task={}, error={}",
                task.getId(), t.getMessage(), t);
        throw new ServiceUnavailableException(
                "Не удалось создать задачу, попробуйте позже"
        );
    }
}
