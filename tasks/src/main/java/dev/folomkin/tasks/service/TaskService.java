package dev.folomkin.tasks.service;

import dev.folomkin.tasks.entity.Tasks;
import dev.folomkin.tasks.event.TaskEvent;
import dev.folomkin.tasks.repository.TaskRepository;
import dev.folomkin.tasks.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    public Optional<Tasks> getTaskById(String id) {
        return taskRepository.findById(id);
    }


    // Fallback — вызывается, когда цепь разомкнута или все попытки исчерпаны

    @CircuitBreaker(name = "mongoDb", fallbackMethod = "fallbackCreateTask")
    //-> считает ошибки; если их больше 50% за последние 10 вызовов — размыкает цепь,
    // если цепь разомкнута или все попытки провалились — вызывается fallbackCreateTas.
    @Retry(name = "mongoDb") //-> перехватывает вызов и повторяет его до 3 раз при ошибке
//    @TimeLimiter(name = "mongoDb") //-> прерывает вызов, если он длится дольше 3 секунд. Для асинхронных методов
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
    public Tasks fallbackCreateTask(Tasks task, Throwable t) throws ServiceUnavailableException {
        // Вариант 1: вернуть «заглушку»
        // Вариант 2: сохранить в очередь для повторной обработки
        // Вариант 3: бросить кастомное исключение
        System.out.println("=== FALLBACK CALLED ===");   // ← если этого нет в логах — fallback не вызывается
        log.error("MongoDB недоступна, fallback triggered", t);
        throw new ServiceUnavailableException("Сервис временно недоступен, попробуйте позже");
    }
}
