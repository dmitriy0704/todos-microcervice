package dev.folomkin.notifications.controller;


import dev.folomkin.notifications.entity.Notification;
import dev.folomkin.notifications.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notification> getAllTasks() {
        return service.getAllTasks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getTaskById(@PathVariable String id) {
        return service.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Notification createTask(@RequestBody Notification tasks) {
        return service.createTask(tasks);
    }
}
