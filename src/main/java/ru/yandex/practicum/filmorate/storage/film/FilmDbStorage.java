package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final GenreStorage genreStorage;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate,
                         GenreStorage genreStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.genreStorage = genreStorage;
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name AS mpa_name " +
                     "FROM films f " +
                     "LEFT JOIN mpa_rating m ON f.mpa_id = m.id " +
                     "ORDER BY f.id";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        loadGenresAndLikes(films);
        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name AS mpa_name " +
                     "FROM films f " +
                     "LEFT JOIN mpa_rating m ON f.mpa_id = m.id " +
                     "WHERE f.id = ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) {
            return Optional.empty();
        }
        Film film = films.get(0);
        loadGenresAndLikes(List.of(film));
        return Optional.of(film);
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            if (film.getMpa() != null && film.getMpa().getId() != null) {
                ps.setLong(5, film.getMpa().getId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        Long filmId = keyHolder.getKeyAs(Long.class);
        film.setId(filmId);

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String insertGenreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> batch = new ArrayList<>();
            for (Genre genre : film.getGenres()) {
                batch.add(new Object[]{filmId, genre.getId()});
            }
            jdbcTemplate.batchUpdate(insertGenreSql, batch);
        }
        return film;
    }

    @Override
    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id фильма должен быть указан");
        }
        String checkSql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, film.getId());
        if (count == null || count == 0) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        String updateSql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
        Long mpaId = (film.getMpa() != null) ? film.getMpa().getId() : null;
        jdbcTemplate.update(updateSql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                mpaId,
                film.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String insertGenreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> batch = new ArrayList<>();
            for (Genre genre : film.getGenres()) {
                batch.add(new Object[]{film.getId(), genre.getId()});
            }
            jdbcTemplate.batchUpdate(insertGenreSql, batch);
        }
        return film;
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name AS mpa_name, " +
                     "COUNT(fl.user_id) AS likes_count " +
                     "FROM films f " +
                     "LEFT JOIN mpa_rating m ON f.mpa_id = m.id " +
                     "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                     "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name " +
                     "ORDER BY likes_count DESC, f.id ASC " +
                     "LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);
        loadGenresAndLikes(films);
        return films;
    }

    private void loadGenresAndLikes(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        List<Long> filmIds = films.stream().map(Film::getId).toList();
        Map<Long, List<Genre>> genresMap = genreStorage.getGenresForFilms(filmIds);

        String inSql = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String likesSql = "SELECT film_id, user_id FROM film_likes WHERE film_id IN (" + inSql + ")";
        Map<Long, Set<Long>> likesMap = new HashMap<>();
        jdbcTemplate.query(likesSql, rs -> {
            Long filmId = rs.getLong("film_id");
            Long userId = rs.getLong("user_id");
            likesMap.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        }, filmIds.toArray());

        for (Film film : films) {
            List<Genre> genres = genresMap.get(film.getId());
            if (genres != null) {
                film.setGenres(new LinkedHashSet<>(genres));
            } else {
                film.setGenres(new LinkedHashSet<>());
            }
            Set<Long> likes = likesMap.get(film.getId());
            if (likes != null) {
                film.setLikes(likes);
            } else {
                film.setLikes(new HashSet<>());
            }
        }
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        Date releaseDate = rs.getDate("release_date");
        if (releaseDate != null) {
            film.setReleaseDate(releaseDate.toLocalDate());
        }
        film.setDuration(rs.getInt("duration"));
        long mpaId = rs.getLong("mpa_id");
        if (!rs.wasNull()) {
            film.setMpa(new Mpa(mpaId, rs.getString("mpa_name")));
        }
        return film;
    }
}
