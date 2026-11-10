package dev.folomkin.tasks.service;

import dev.folomkin.tasks.entity.Tasks;
import dev.folomkin.tasks.exception.ServiceUnavailableException;
import dev.folomkin.tasks.repository.TaskRepository;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskEventProducer producer;
    private final Counter tasksCreated;
    private final Counter tasksFailed;

    public TaskService(
            TaskRepository taskRepository,
            TaskEventProducer producer,
            MeterRegistry registry
    ) {
        this.taskRepository = taskRepository;
        this.producer = producer;
        this.tasksCreated = Counter.builder("tasks_created_count")
                .description("Total number of tasks successfully created")
                .tag("application", "tasks-service")
                .register(registry);
        this.tasksFailed = Counter.builder("tasks_failed_count")
                .description("Total number of task creation failures")
                .tag("application", "tasks-service")
                .register(registry);
    }

    public List<Tasks> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Tasks> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public void deleteTasks(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    //-> считает ошибки; если их больше 50% за последние 10 вызовов — размыкает цепь,
    // если цепь разомкнута или все попытки провалились — вызывается fallbackCreateTas.
//    @Retry(name = "postgres") - опасно для POST - возможны дубликаты
    //-> перехватывает вызов и повторяет его до 3 раз при ошибке
    //-> Реализация паттерна Автоматический выключатель
    // Fallback — вызывается, когда цепь разомкнута или все попытки исчерпаны
    @CircuitBreaker(name = "postgres", fallbackMethod = "fallbackCreateTask")
    //-> Bulkhead: ограничивает количество одновременных запросов
    @Bulkhead(name = "postgres")
    //-> убираем fallback, чтобы fail-increment не запускался дважды
    //-> @TimeLimiter(name = "mongoDb") //-> прерывает вызов, если он длится дольше 3 секунд. Для асинхронных методов
    //-> @RateLimiter(name = "mongoDb") - ограничение количества запросов, уточнить от кого
    public Tasks createTask(Tasks task) {
        log.info("ENTERED createTask()");
        log.info("SAVING TASK TO POSTGRES");
        Tasks savedTask = taskRepository.save(task);
        tasksCreated.increment();   // ← +1 при успехе
        log.info("RETURN TASK");
        return savedTask;
//        Tasks t = taskRepository.save(task);
//        producer.sendTaskCreatedEvent( //-> отправка сообщения в Kafka
//                new TaskEvent(
//                        task.getId(),
//                        task.getTitle(),
//                        "USER abc123",
//                        "CREATED")
//        );
//        return t;
    }

    /**
     * Единый fallback для всех трёх декораторов.
     * Различаем причину отказа через instanceof.
     */
    public Tasks fallbackCreateTask(Tasks task, Throwable t) throws ServiceUnavailableException {
        log.error("Fallback triggered. Exception: {}",
                t.getClass().getName());

        if (t instanceof BulkheadFullException) {
            log.warn("BULKHEAD FULL!");
        }

        if (t instanceof CallNotPermittedException) {
            log.warn("CIRCUIT BREAKER OPEN!");
        }

        if (t instanceof TimeoutException) {
            log.warn("TIMEOUT!");
        }
        tasksFailed.increment();
        throw new ServiceUnavailableException(
                "Не удалось создать задачу, попробуйте позже"
        );
    }
}
