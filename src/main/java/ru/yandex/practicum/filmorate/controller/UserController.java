package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 0;

    @GetMapping
    public Collection<User> findAll() {
        log.info("Получен запрос на получение списка всех пользователей. Текущее количество: {}", users.size());
        return users.values();
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user);
        if (user.getName() == null || user.getName().isBlank()) {
            log.info("Имя пользователя не указано, будет использован логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Пользователь успешно создан: {}", user);
        return user;
    }

    @PutMapping
    public User update(@Valid @RequestBody User newUser) {
        log.info("Получен запрос на обновление пользователя: {}", newUser);
        if (newUser.getId() == null) {
            log.warn("Ошибка валидации: Id пользователя не указан");
            throw new ValidationException("Id пользователя должен быть указан");
        }
        if (!users.containsKey(newUser.getId())) {
            log.warn("Пользователь с id = {} не найден для обновления", newUser.getId());
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }
        if (newUser.getName() == null || newUser.getName().isBlank()) {
            log.info("Имя пользователя не указано, будет использован логин: {}", newUser.getLogin());
            newUser.setName(newUser.getLogin());
        }
        users.put(newUser.getId(), newUser);
        log.info("Пользователь успешно обновлен: {}", newUser);
        return newUser;
    }

    private long getNextId() {
        return ++currentId;
    }
}
