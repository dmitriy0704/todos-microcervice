package dev.folomkin.notifications.service;

import dev.folomkin.notifications.entity.Notification;
import dev.folomkin.notifications.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }


    public List<Notification> getAllTasks() {
        return repository.findAll();
    }

    public Optional<Notification> getTaskById(String id) {
        return repository.findById(id);
    }

    public Notification createTask(Notification task) {
        return repository.save(task);
    }
}
