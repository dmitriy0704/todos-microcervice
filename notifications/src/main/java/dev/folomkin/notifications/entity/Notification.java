package dev.folomkin.notifications.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;
    private String message;
    private String recipient;

    public Notification(String message, String recipient) {
        this.message = message;
        this.recipient = recipient;
    }
}
