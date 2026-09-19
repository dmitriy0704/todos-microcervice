package dev.folomkin.tasks.repository;

import dev.folomkin.tasks.entity.Tasks;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskRepository extends MongoRepository<Tasks, String> {
}
