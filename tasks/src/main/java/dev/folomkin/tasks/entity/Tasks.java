package dev.folomkin.tasks.entity;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "tasks")
public class Tasks {

    @Id
    private String id;
    private String title;
    private String description;
    private boolean completed;

    public Tasks(String title, String description, boolean completed) {
        this.title = title;
        this.description = description;
        this.completed = completed;
    }
}
