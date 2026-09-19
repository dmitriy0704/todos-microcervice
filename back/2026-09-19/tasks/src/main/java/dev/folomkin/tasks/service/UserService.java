package dev.folomkin.tasks.service;

import dev.folomkin.tasks.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {

    @Autowired
    public RestTemplate restTemplate;

    public User getUserById(String id) {
        String url = "http://users-service/users/" + id;
        return restTemplate.getForObject(url, User.class);
    }
}
