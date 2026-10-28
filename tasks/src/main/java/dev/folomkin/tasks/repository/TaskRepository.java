package dev.folomkin.tasks.repository;

import dev.folomkin.tasks.entity.Tasks;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Tasks, Long> {
}
