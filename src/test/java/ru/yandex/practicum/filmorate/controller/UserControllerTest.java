package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        userStorage = Mockito.mock(UserStorage.class);
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
        when(userStorage.create(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(userStorage.findAll()).thenReturn(List.of(user));

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
        when(userStorage.create(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userController.create(user);
        assertEquals(user.getLogin(), created.getName());
    }

    @Test
    void createUser_emptyName_usesLogin() {
        User user = createValidUser();
        user.setName("");
        when(userStorage.create(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userController.create(user);
        assertEquals(user.getLogin(), created.getName());
    }

    @Test
    void createUser_blankName_usesLogin() {
        User user = createValidUser();
        user.setName("   ");
        when(userStorage.create(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

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
        user.setId(1L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user));
        when(userStorage.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userStorage.findAll()).thenReturn(List.of(user));

        user.setEmail("updated@example.com");
        user.setName("Новое Имя");
        User updated = userController.update(user);

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
        when(userStorage.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userController.update(user));
    }

    @Test
    void findAll_returnsAllUsers() {
        User user1 = createValidUser();
        user1.setId(1L);
        User user2 = createValidUser();
        user2.setId(2L);
        user2.setEmail("user2@example.com");
        user2.setLogin("user2Login");
        when(userStorage.findAll()).thenReturn(List.of(user1, user2));

        Collection<User> all = userController.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void findById_valid_returnsUser() {
        User user = createValidUser();
        user.setId(1L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user));

        User found = userController.findById(1L);
        assertEquals(1L, found.getId());
        assertEquals("Иван Иванов", found.getName());
    }

    @Test
    void findById_unknownId_throwsNotFoundException() {
        when(userStorage.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userController.findById(999L));
    }

    @Test
    void addFriend_and_removeFriend_success() {
        User user1 = createValidUser();
        user1.setId(1L);
        User user2 = createValidUser();
        user2.setId(2L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user1));
        when(userStorage.findById(2L)).thenReturn(Optional.of(user2));
        when(userStorage.getFriends(1L)).thenReturn(List.of(user2)).thenReturn(List.of());
        when(userStorage.getFriends(2L)).thenReturn(List.of());

        userController.addFriend(user1.getId(), user2.getId());
        verify(userStorage).addFriend(1L, 2L);

        List<User> user1Friends = userController.getFriends(user1.getId());
        List<User> user2Friends = userController.getFriends(user2.getId());

        assertEquals(1, user1Friends.size());
        assertEquals(user2.getId(), user1Friends.get(0).getId());
        assertTrue(user2Friends.isEmpty());

        userController.removeFriend(user1.getId(), user2.getId());
        verify(userStorage).removeFriend(1L, 2L);
        assertTrue(userController.getFriends(user1.getId()).isEmpty());
    }

    @Test
    void addFriend_self_throwsValidationException() {
        assertThrows(ValidationException.class, () -> userController.addFriend(1L, 1L));
    }

    @Test
    void addFriend_unknownUser_throwsNotFoundException() {
        when(userStorage.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userController.addFriend(999L, 1L));
    }

    @Test
    void addFriend_unknownFriend_throwsNotFoundException() {
        User user = createValidUser();
        user.setId(1L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user));
        when(userStorage.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userController.addFriend(1L, 999L));
    }

    @Test
    void removeFriend_unknownUser_throwsNotFoundException() {
        when(userStorage.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userController.removeFriend(999L, 1L));
    }

    @Test
    void removeFriend_unknownFriend_throwsNotFoundException() {
        User user = createValidUser();
        user.setId(1L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user));
        when(userStorage.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userController.removeFriend(1L, 999L));
    }

    @Test
    void getCommonFriends_returnsCommonFriends() {
        User user1 = createValidUser();
        user1.setId(1L);
        User user2 = createValidUser();
        user2.setId(2L);
        User commonFriend = createValidUser();
        commonFriend.setId(3L);

        when(userStorage.findById(1L)).thenReturn(Optional.of(user1));
        when(userStorage.findById(2L)).thenReturn(Optional.of(user2));
        when(userStorage.getCommonFriends(1L, 2L)).thenReturn(List.of(commonFriend));

        List<User> common = userController.getCommonFriends(1L, 2L);
        assertEquals(1, common.size());
        assertEquals(3L, common.get(0).getId());
    }
}
