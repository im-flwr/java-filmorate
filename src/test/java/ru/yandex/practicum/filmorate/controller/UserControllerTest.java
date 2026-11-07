package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserControllerTest {
    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    private User createValidUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("validLogin");
        user.setName("Иван Иванов");
        user.setBirthday(LocalDate.of(1990, 5, 15));
        return user;
    }

    @Test
    void createUser_valid_success() {
        User user = createValidUser();
        User created = userController.create(user);

        assertNotNull(created.getId());
        assertEquals(1L, created.getId());
        assertEquals("user@example.com", created.getEmail());
        assertEquals(1, userController.findAll().size());
    }

    @Test
    void createUser_nullUser_throwsValidationException() {
        assertThrows(ValidationException.class, () -> userController.create(null));
    }

    @Test
    void createUser_nullEmail_throwsValidationException() {
        User user = createValidUser();
        user.setEmail(null);

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_emptyEmail_throwsValidationException() {
        User user = createValidUser();
        user.setEmail("");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_blankEmail_throwsValidationException() {
        User user = createValidUser();
        user.setEmail("   ");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_emailWithoutAt_throwsValidationException() {
        User user = createValidUser();
        user.setEmail("invalid-email.com");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_nullLogin_throwsValidationException() {
        User user = createValidUser();
        user.setLogin(null);

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_emptyLogin_throwsValidationException() {
        User user = createValidUser();
        user.setLogin("");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_blankLogin_throwsValidationException() {
        User user = createValidUser();
        user.setLogin("   ");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_loginWithSpaces_throwsValidationException() {
        User user = createValidUser();
        user.setLogin("login with spaces");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_nullName_usesLogin() {
        User user = createValidUser();
        user.setName(null);

        User created = userController.create(user);
        assertEquals(user.getLogin(), created.getName());
    }

    @Test
    void createUser_emptyName_usesLogin() {
        User user = createValidUser();
        user.setName("");

        User created = userController.create(user);
        assertEquals(user.getLogin(), created.getName());
    }

    @Test
    void createUser_blankName_usesLogin() {
        User user = createValidUser();
        user.setName("   ");

        User created = userController.create(user);
        assertEquals(user.getLogin(), created.getName());
    }

    @Test
    void createUser_birthdayToday_success() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now());

        User created = userController.create(user);
        assertEquals(LocalDate.now(), created.getBirthday());
    }

    @Test
    void createUser_birthdayInFuture_throwsValidationException() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void updateUser_valid_success() {
        User user = createValidUser();
        User created = userController.create(user);

        created.setEmail("updated@example.com");
        created.setName("Новое Имя");
        User updated = userController.update(created);

        assertEquals("updated@example.com", updated.getEmail());
        assertEquals("Новое Имя", updated.getName());
        assertEquals(1, userController.findAll().size());
    }

    @Test
    void updateUser_nullUser_throwsValidationException() {
        assertThrows(ValidationException.class, () -> userController.update(null));
    }

    @Test
    void updateUser_nullId_throwsValidationException() {
        User user = createValidUser();
        assertThrows(ValidationException.class, () -> userController.update(user));
    }

    @Test
    void updateUser_unknownId_throwsNotFoundException() {
        User user = createValidUser();
        user.setId(999L);

        assertThrows(NotFoundException.class, () -> userController.update(user));
    }

    @Test
    void findAll_returnsAllUsers() {
        User user1 = createValidUser();
        User user2 = createValidUser();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2Login");

        userController.create(user1);
        userController.create(user2);

        Collection<User> all = userController.findAll();
        assertEquals(2, all.size());
    }
}
