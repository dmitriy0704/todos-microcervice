package dev.folomkin.tasks.controller;


import dev.folomkin.tasks.entity.Tasks;
import dev.folomkin.tasks.entity.User;
import dev.folomkin.tasks.event.TaskEvent;
import dev.folomkin.tasks.service.TaskEventProducer;
import dev.folomkin.tasks.service.TaskService;
import dev.folomkin.tasks.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TasksController {

    private final TaskService taskService;
    private final UserService userService;
    private final TaskEventProducer producerService;

    public TasksController(
            TaskService taskService,
            UserService userService,
            TaskEventProducer producerService
    ) {
        this.taskService = taskService;
        this.userService = userService;
        this.producerService = producerService;
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

    @PostMapping("/create")
    public Tasks createTask(@RequestBody Tasks tasks) {
        return taskService.createTask(tasks);
    }


    //-> Получение объекта пользователя из сервиса users-service
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return new ResponseEntity<>(userService.getUserById(id), HttpStatus.OK);
    }
//
//    //-> Отправка сообщения в кафку
//    @PostMapping("/sendmessage")
//    public ResponseEntity<String> createOrder(@RequestBody TaskEvent taskEvent) {
//        // В качестве ключа Kafka-сообщения используем orderId (для сохранения порядка в партиции)
//
//        producerService.sendMessage(taskEvent);
//
//        return ResponseEntity.ok("Запрос на создание задачи принят и отправлен в Kafka!");
//    }

}
