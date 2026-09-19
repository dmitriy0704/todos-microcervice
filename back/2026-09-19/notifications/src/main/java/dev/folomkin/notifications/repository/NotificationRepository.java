package dev.folomkin.notifications.repository;

import dev.folomkin.notifications.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {
}
