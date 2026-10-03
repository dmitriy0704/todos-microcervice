package dev.folomkin.tasks.config;

import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.stereotype.Component;

@Component
public class MongoDebug {

    public MongoDebug(
            MongoProperties properties) {
        System.out.println("========== MONGO DEBUG ==========");
        System.out.println("uri      = " + properties.getUri());
        System.out.println("host     = " + properties.getHost());
        System.out.println("port     = " + properties.getPort());
        System.out.println("database = " + properties.getDatabase());
        System.out.println("=================================");
    }
}