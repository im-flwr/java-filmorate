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

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmControllerTest {
    private static Validator validator;
    private FilmController filmController;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Интерстеллар");
        film.setDescription("Научно-фантастический фильм Кристофера Нолана");
        film.setReleaseDate(LocalDate.of(2014, 11, 7));
        film.setDuration(169);
        return film;
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
}
