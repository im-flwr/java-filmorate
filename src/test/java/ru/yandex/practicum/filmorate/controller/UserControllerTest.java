package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest {
    private static Validator validator;
    private UserStorage userStorage;
    private UserService userService;
    private UserController userController;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        userController = new UserController(userService);
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
    void validateUser_valid_noViolations() {
        User user = createValidUser();
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validateUser_nullEmail_hasViolations() {
        User user = createValidUser();
        user.setEmail(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateUser_emptyEmail_hasViolations() {
        User user = createValidUser();
        user.setEmail("");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateUser_blankEmail_hasViolations() {
        User user = createValidUser();
        user.setEmail("   ");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateUser_emailWithoutAt_hasViolations() {
        User user = createValidUser();
        user.setEmail("invalid-email.com");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateUser_nullLogin_hasViolations() {
        User user = createValidUser();
        user.setLogin(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateUser_emptyLogin_hasViolations() {
        User user = createValidUser();
        user.setLogin("");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateUser_blankLogin_hasViolations() {
        User user = createValidUser();
        user.setLogin("   ");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateUser_loginWithSpaces_hasViolations() {
        User user = createValidUser();
        user.setLogin("login with spaces");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
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
    void validateUser_birthdayToday_noViolations() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now());

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validateUser_birthdayInFuture_hasViolations() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
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

    @Test
    void findById_valid_returnsUser() {
        User user = createValidUser();
        User created = userController.create(user);

        User found = userController.findById(created.getId());
        assertEquals(created.getId(), found.getId());
        assertEquals("Иван Иванов", found.getName());
    }

    @Test
    void findById_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> userController.findById(999L));
    }

    @Test
    void addFriend_and_removeFriend_success() {
        User user1 = userController.create(createValidUser());
        User user2 = createValidUser();
        user2.setEmail("friend@example.com");
        user2.setLogin("friendLogin");
        user2 = userController.create(user2);

        userController.addFriend(user1.getId(), user2.getId());
        List<User> user1Friends = userController.getFriends(user1.getId());
        List<User> user2Friends = userController.getFriends(user2.getId());

        assertEquals(1, user1Friends.size());
        assertEquals(user2.getId(), user1Friends.get(0).getId());
        assertTrue(user2Friends.isEmpty());

        userController.removeFriend(user1.getId(), user2.getId());
        assertTrue(userController.getFriends(user1.getId()).isEmpty());
    }

    @Test
    void addFriend_self_throwsValidationException() {
        User user = userController.create(createValidUser());
        assertThrows(ValidationException.class, () -> userController.addFriend(user.getId(), user.getId()));
    }

    @Test
    void addFriend_unknownUser_throwsNotFoundException() {
        User user = userController.create(createValidUser());
        assertThrows(NotFoundException.class, () -> userController.addFriend(999L, user.getId()));
    }

    @Test
    void addFriend_unknownFriend_throwsNotFoundException() {
        User user = userController.create(createValidUser());
        assertThrows(NotFoundException.class, () -> userController.addFriend(user.getId(), 999L));
    }

    @Test
    void removeFriend_unknownUser_throwsNotFoundException() {
        User user = userController.create(createValidUser());
        assertThrows(NotFoundException.class, () -> userController.removeFriend(999L, user.getId()));
    }

    @Test
    void removeFriend_unknownFriend_throwsNotFoundException() {
        User user = userController.create(createValidUser());
        assertThrows(NotFoundException.class, () -> userController.removeFriend(user.getId(), 999L));
    }

    @Test
    void getCommonFriends_returnsCommonFriends() {
        User user1 = userController.create(createValidUser());

        User user2 = createValidUser();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2Login");
        user2 = userController.create(user2);

        User commonFriend = createValidUser();
        commonFriend.setEmail("common@example.com");
        commonFriend.setLogin("commonLogin");
        commonFriend = userController.create(commonFriend);

        userController.addFriend(user1.getId(), commonFriend.getId());
        userController.addFriend(user2.getId(), commonFriend.getId());

        List<User> common = userController.getCommonFriends(user1.getId(), user2.getId());
        assertEquals(1, common.size());
        assertEquals(commonFriend.getId(), common.get(0).getId());
    }
}
