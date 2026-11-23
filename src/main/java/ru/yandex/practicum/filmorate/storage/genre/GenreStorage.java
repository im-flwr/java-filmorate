package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GenreStorage {
    Collection<Genre> findAll();

    Optional<Genre> findById(Long id);

    List<Genre> findAllByIds(Collection<Long> ids);

    Map<Long, List<Genre>> getGenresForFilms(List<Long> filmIds);
}
