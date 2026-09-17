package dev.folomkin.tasks.controller;


import dev.folomkin.tasks.entity.Tasks;
import dev.folomkin.tasks.entity.User;
import dev.folomkin.tasks.service.TaskService;
import dev.folomkin.tasks.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TasksController {

    private final TaskService taskService;
    private final UserService userService;

    public TasksController(
            TaskService taskService,
            UserService userService
    ) {
        this.taskService = taskService;
        this.userService = userService;
    }

    @GetMapping
    public List<Tasks> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tasks> getTaskById(@PathVariable String id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Tasks createTask(@RequestBody Tasks tasks) {
        return taskService.createTask(tasks);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return new ResponseEntity<>(userService.getUserById(id), HttpStatus.OK);
    }
}
