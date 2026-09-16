package dev.folomkin.tasks.service;

import dev.folomkin.tasks.entity.Tasks;
import dev.folomkin.tasks.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Tasks> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Tasks> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    public Tasks createTask(Tasks task) {
        return taskRepository.save(task);
    }

}
