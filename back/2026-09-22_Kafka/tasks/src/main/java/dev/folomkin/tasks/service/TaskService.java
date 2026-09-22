package dev.folomkin.tasks.service;

import dev.folomkin.tasks.entity.Tasks;
import dev.folomkin.tasks.event.TaskEvent;
import dev.folomkin.tasks.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

}
