package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage,
                       UserStorage userStorage,
                       GenreStorage genreStorage,
                       MpaStorage mpaStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public Film create(Film film) {
        validateMpaAndGenres(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id фильма должен быть указан");
        }
        Film existing = findById(film.getId());
        film.setLikes(existing.getLikes());
        validateMpaAndGenres(film);
        return filmStorage.update(film);
    }

    public void delete(Long id) {
        filmStorage.delete(id);
    }

    public void addLike(Long filmId, Long userId) {
        findById(filmId);
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        findById(filmId);
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopular(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть больше нуля");
        }
        return filmStorage.getPopular(count);
    }

    private void validateMpaAndGenres(Film film) {
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            Mpa mpa = mpaStorage.findById(film.getMpa().getId())
                    .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id = " + film.getMpa().getId() + " не найден"));
            film.setMpa(mpa);
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Long> requestedGenreIds = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());

            List<Genre> foundGenres = genreStorage.findAllByIds(requestedGenreIds);

            if (foundGenres.size() != requestedGenreIds.size()) {
                Set<Long> foundGenreIds = foundGenres.stream()
                        .map(Genre::getId)
                        .collect(Collectors.toSet());
                for (Long requestedId : requestedGenreIds) {
                    if (!foundGenreIds.contains(requestedId)) {
                        log.warn("Жанр с id = {} не найден", requestedId);
                        throw new NotFoundException("Жанр с id = " + requestedId + " не найден");
                    }
                }
            }
            film.setGenres(new LinkedHashSet<>(foundGenres));
        }
    }
}
