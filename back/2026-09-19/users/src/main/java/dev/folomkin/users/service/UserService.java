package dev.folomkin.users.service;

import dev.folomkin.users.entity.User;
import dev.folomkin.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<User> getAllTasks() {
        return repository.findAll();
    }

    public Optional<User> getTaskById(String id) {
        return repository.findById(id);
    }

    public User createTask(User task) {
        return repository.save(task);
    }
}
