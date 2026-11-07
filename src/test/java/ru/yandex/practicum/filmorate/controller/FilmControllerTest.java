package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilmControllerTest {
    private FilmController filmController;

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
    void createFilm_nullFilm_throwsValidationException() {
        assertThrows(ValidationException.class, () -> filmController.create(null));
    }

    @Test
    void createFilm_nullName_throwsValidationException() {
        Film film = createValidFilm();
        film.setName(null);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_emptyName_throwsValidationException() {
        Film film = createValidFilm();
        film.setName("");

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_blankName_throwsValidationException() {
        Film film = createValidFilm();
        film.setName("   ");

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_descriptionExact200Chars_success() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(200));

        Film created = filmController.create(film);
        assertEquals(200, created.getDescription().length());
    }

    @Test
    void createFilm_description201Chars_throwsValidationException() {
        Film film = createValidFilm();
        film.setDescription("a".repeat(201));

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_releaseDateBoundary18951228_success() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        Film created = filmController.create(film);
        assertEquals(LocalDate.of(1895, 12, 28), created.getReleaseDate());
    }

    @Test
    void createFilm_releaseDateBefore18951228_throwsValidationException() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_nullReleaseDate_throwsValidationException() {
        Film film = createValidFilm();
        film.setReleaseDate(null);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_zeroDuration_throwsValidationException() {
        Film film = createValidFilm();
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_negativeDuration_throwsValidationException() {
        Film film = createValidFilm();
        film.setDuration(-10);

        assertThrows(ValidationException.class, () -> filmController.create(film));
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
    void updateFilm_nullFilm_throwsValidationException() {
        assertThrows(ValidationException.class, () -> filmController.update(null));
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
