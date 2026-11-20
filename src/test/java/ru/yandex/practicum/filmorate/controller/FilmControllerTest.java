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
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
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

class FilmControllerTest {
    private static Validator validator;
    private FilmStorage filmStorage;
    private UserStorage userStorage;
    private FilmService filmService;
    private FilmController filmController;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
        userStorage = new InMemoryUserStorage();
        filmService = new FilmService(filmStorage, userStorage);
        filmController = new FilmController(filmService);
    }

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Интерстеллар");
        film.setDescription("Научно-фантастический фильм Кристофера Нолана");
        film.setReleaseDate(LocalDate.of(2014, 11, 7));
        film.setDuration(169);
        return film;
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
    void createFilm_valid_success() {
        Film film = createValidFilm();
        Film created = filmController.create(film);

        assertNotNull(created.getId());
        assertEquals(1L, created.getId());
        assertEquals("Интерстеллар", created.getName());
        assertEquals(1, filmController.findAll().size());
    }

    @Test
    void validateFilm_valid_noViolations() {
        Film film = createValidFilm();
        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validateFilm_nullName_hasViolations() {
        Film film = createValidFilm();
        film.setName(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateFilm_emptyName_hasViolations() {
        Film film = createValidFilm();
        film.setName("");

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateFilm_blankName_hasViolations() {
        Film film = createValidFilm();
        film.setName("   ");

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateFilm_descriptionExact200Chars_noViolations() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(200));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validateFilm_description201Chars_hasViolations() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(201));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateFilm_releaseDateBoundary18951228_noViolations() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validateFilm_releaseDateBefore18951228_hasViolations() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateFilm_nullReleaseDate_hasViolations() {
        Film film = createValidFilm();
        film.setReleaseDate(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateFilm_zeroDuration_hasViolations() {
        Film film = createValidFilm();
        film.setDuration(0);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validateFilm_negativeDuration_hasViolations() {
        Film film = createValidFilm();
        film.setDuration(-10);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty());
    }

    @Test
    void updateFilm_valid_success() {
        Film film = createValidFilm();
        Film created = filmController.create(film);

        created.setName("Начало");
        created.setDuration(148);
        Film updated = filmController.update(created);

        assertEquals("Начало", updated.getName());
        assertEquals(148, updated.getDuration());
        assertEquals(1, filmController.findAll().size());
    }

    @Test
    void updateFilm_nullId_throwsValidationException() {
        Film film = createValidFilm();
        assertThrows(ValidationException.class, () -> filmController.update(film));
    }

    @Test
    void updateFilm_unknownId_throwsNotFoundException() {
        Film film = createValidFilm();
        film.setId(999L);

        assertThrows(NotFoundException.class, () -> filmController.update(film));
    }

    @Test
    void findAll_returnsAllFilms() {
        Film film1 = createValidFilm();
        Film film2 = createValidFilm();
        film2.setName("Матрица");

        filmController.create(film1);
        filmController.create(film2);

        Collection<Film> all = filmController.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void findById_valid_returnsFilm() {
        Film film = createValidFilm();
        Film created = filmController.create(film);

        Film found = filmController.findById(created.getId());
        assertEquals(created.getId(), found.getId());
        assertEquals("Интерстеллар", found.getName());
    }

    @Test
    void findById_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> filmController.findById(999L));
    }

    @Test
    void addLike_and_removeLike_success() {
        Film film = filmController.create(createValidFilm());
        User user = userStorage.create(createValidUser());

        filmController.addLike(film.getId(), user.getId());
        assertTrue(filmController.findById(film.getId()).getLikes().contains(user.getId()));

        filmController.removeLike(film.getId(), user.getId());
        assertFalse(filmController.findById(film.getId()).getLikes().contains(user.getId()));
    }

    @Test
    void addLike_unknownFilm_throwsNotFoundException() {
        User user = userStorage.create(createValidUser());
        assertThrows(NotFoundException.class, () -> filmController.addLike(999L, user.getId()));
    }

    @Test
    void addLike_unknownUser_throwsNotFoundException() {
        Film film = filmController.create(createValidFilm());
        assertThrows(NotFoundException.class, () -> filmController.addLike(film.getId(), 999L));
    }

    @Test
    void removeLike_unknownFilm_throwsNotFoundException() {
        User user = userStorage.create(createValidUser());
        assertThrows(NotFoundException.class, () -> filmController.removeLike(999L, user.getId()));
    }

    @Test
    void removeLike_unknownUser_throwsNotFoundException() {
        Film film = filmController.create(createValidFilm());
        assertThrows(NotFoundException.class, () -> filmController.removeLike(film.getId(), 999L));
    }

    @Test
    void getPopular_returnsSortedByLikes() {
        Film film1 = filmController.create(createValidFilm());
        Film film2 = createValidFilm();
        film2.setName("Матрица");
        film2 = filmController.create(film2);

        User user1 = userStorage.create(createValidUser());
        User user2 = createValidUser();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2 = userStorage.create(user2);

        filmController.addLike(film2.getId(), user1.getId());
        filmController.addLike(film2.getId(), user2.getId());
        filmController.addLike(film1.getId(), user1.getId());

        List<Film> popular = filmController.getPopular(10);
        assertEquals(2, popular.size());
        assertEquals(film2.getId(), popular.get(0).getId());
        assertEquals(film1.getId(), popular.get(1).getId());
    }

    @Test
    void getPopular_negativeCount_throwsValidationException() {
        assertThrows(ValidationException.class, () -> filmController.getPopular(-1));
    }
}
