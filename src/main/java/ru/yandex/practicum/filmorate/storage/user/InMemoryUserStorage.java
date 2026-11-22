package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component("inMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 0;

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User create(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User newUser) {
        if (newUser.getId() == null) {
            throw new ValidationException("Id пользователя должен быть указан");
        }
        if (!users.containsKey(newUser.getId())) {
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }
        if (newUser.getName() == null || newUser.getName().isBlank()) {
            newUser.setName(newUser.getLogin());
        }
        users.put(newUser.getId(), newUser);
        return newUser;
    }

    @Override
    public void delete(Long id) {
        users.remove(id);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        findById(userId).ifPresent(user -> user.getFriends().add(friendId));
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        findById(userId).ifPresent(user -> user.getFriends().remove(friendId));
    }

    @Override
    public List<User> getFriends(Long userId) {
        return findById(userId)
                .map(user -> user.getFriends().stream()
                        .map(this::findById)
                        .flatMap(Optional::stream)
                        .toList())
                .orElse(Collections.emptyList());
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        Optional<User> userOpt = findById(userId);
        Optional<User> otherOpt = findById(otherId);
        if (userOpt.isEmpty() || otherOpt.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userFriends = userOpt.get().getFriends();
        Set<Long> otherFriends = otherOpt.get().getFriends();
        return userFriends.stream()
                .filter(otherFriends::contains)
                .map(this::findById)
                .flatMap(Optional::stream)
                .toList();
    }

    private long getNextId() {
        return ++currentId;
    }
}
