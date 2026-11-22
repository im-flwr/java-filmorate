package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class, FilmDbStorage.class, GenreDbStorage.class, MpaDbStorage.class})
class FilmorateApplicationTests {
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM friendship");
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
    }

    private User createSampleUser(String email, String login, String name) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    private Film createSampleFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание фильма " + name);
        film.setReleaseDate(LocalDate.of(2010, 5, 20));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));
        return film;
    }

    @Test
    public void testFindUserById() {
        User user = userStorage.create(createSampleUser("test@mail.ru", "testLogin", "Test User"));
        Optional<User> userOptional = userStorage.findUserById(user.getId());

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(u ->
                        assertThat(u).hasFieldOrPropertyWithValue("id", user.getId())
                );
    }

    @Test
    public void testCreateAndFindUser() {
        User user = createSampleUser("ivan@mail.ru", "ivan", "Иван");
        User created = userStorage.create(user);

        assertThat(created.getId()).isNotNull();
        Optional<User> found = userStorage.findById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Иван");
        assertThat(found.get().getEmail()).isEqualTo("ivan@mail.ru");
    }

    @Test
    public void testUpdateUser() {
        User user = userStorage.create(createSampleUser("update@mail.ru", "updateLogin", "Имя"));
        user.setName("Новое имя");
        userStorage.update(user);

        Optional<User> found = userStorage.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Новое имя");
    }

    @Test
    public void testFindAllUsers() {
        userStorage.create(createSampleUser("u1@mail.ru", "user1", "User 1"));
        userStorage.create(createSampleUser("u2@mail.ru", "user2", "User 2"));

        Collection<User> users = userStorage.findAll();
        assertThat(users).hasSize(2);
    }

    @Test
    public void testAddAndGetFriends() {
        User user1 = userStorage.create(createSampleUser("u1@mail.ru", "user1", "User 1"));
        User user2 = userStorage.create(createSampleUser("u2@mail.ru", "user2", "User 2"));

        userStorage.addFriend(user1.getId(), user2.getId());

        List<User> user1Friends = userStorage.getFriends(user1.getId());
        List<User> user2Friends = userStorage.getFriends(user2.getId());

        assertThat(user1Friends).hasSize(1);
        assertThat(user1Friends.get(0).getId()).isEqualTo(user2.getId());
        assertThat(user2Friends).isEmpty();
    }

    @Test
    public void testRemoveFriend() {
        User user1 = userStorage.create(createSampleUser("u1@mail.ru", "user1", "User 1"));
        User user2 = userStorage.create(createSampleUser("u2@mail.ru", "user2", "User 2"));

        userStorage.addFriend(user1.getId(), user2.getId());
        userStorage.removeFriend(user1.getId(), user2.getId());

        List<User> user1Friends = userStorage.getFriends(user1.getId());
        assertThat(user1Friends).isEmpty();
    }

    @Test
    public void testCommonFriends() {
        User user1 = userStorage.create(createSampleUser("u1@mail.ru", "user1", "User 1"));
        User user2 = userStorage.create(createSampleUser("u2@mail.ru", "user2", "User 2"));
        User friend = userStorage.create(createSampleUser("friend@mail.ru", "friend", "Friend"));

        userStorage.addFriend(user1.getId(), friend.getId());
        userStorage.addFriend(user2.getId(), friend.getId());

        List<User> common = userStorage.getCommonFriends(user1.getId(), user2.getId());
        assertThat(common).hasSize(1);
        assertThat(common.get(0).getId()).isEqualTo(friend.getId());
    }

    @Test
    public void testCreateAndFindFilm() {
        Film film = createSampleFilm("Фильм 1");
        Film created = filmStorage.create(film);

        assertThat(created.getId()).isNotNull();
        Optional<Film> found = filmStorage.findById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Фильм 1");
        assertThat(found.get().getMpa()).isNotNull();
        assertThat(found.get().getMpa().getId()).isEqualTo(1);
    }

    @Test
    public void testUpdateFilm() {
        Film film = filmStorage.create(createSampleFilm("Фильм Старый"));
        film.setName("Фильм Новый");
        film.setDuration(150);
        filmStorage.update(film);

        Optional<Film> found = filmStorage.findById(film.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Фильм Новый");
        assertThat(found.get().getDuration()).isEqualTo(150);
    }

    @Test
    public void testFindAllFilms() {
        filmStorage.create(createSampleFilm("Фильм 1"));
        filmStorage.create(createSampleFilm("Фильм 2"));

        Collection<Film> films = filmStorage.findAll();
        assertThat(films).hasSize(2);
    }

    @Test
    public void testAddAndRemoveLikeAndPopular() {
        Film film1 = filmStorage.create(createSampleFilm("Фильм 1"));
        Film film2 = filmStorage.create(createSampleFilm("Фильм 2"));
        User user = userStorage.create(createSampleUser("user@mail.ru", "user", "User"));

        filmStorage.addLike(film2.getId(), user.getId());

        List<Film> popular = filmStorage.getPopular(10);
        assertThat(popular).hasSize(2);
        assertThat(popular.get(0).getId()).isEqualTo(film2.getId());

        filmStorage.removeLike(film2.getId(), user.getId());
        Optional<Film> found = filmStorage.findById(film2.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLikes()).doesNotContain(user.getId());
    }

    @Test
    public void testFilmWithGenres() {
        Film film = createSampleFilm("Фильм с жанрами");
        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        genres.add(new Genre(1, "Комедия"));
        genres.add(new Genre(2, "Драма"));
        film.setGenres(genres);

        Film created = filmStorage.create(film);
        Optional<Film> found = filmStorage.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getGenres()).hasSize(2);
    }

    @Test
    public void testFindMpaByIdAndFindAll() {
        Optional<Mpa> mpa1 = mpaStorage.findById(1);
        assertThat(mpa1).isPresent();
        assertThat(mpa1.get().getName()).isEqualTo("G");

        Collection<Mpa> allMpa = mpaStorage.findAll();
        assertThat(allMpa).hasSize(5);
    }

    @Test
    public void testFindGenreByIdAndFindAll() {
        Optional<Genre> genre1 = genreStorage.findById(1);
        assertThat(genre1).isPresent();
        assertThat(genre1.get().getName()).isEqualTo("Комедия");

        Collection<Genre> allGenres = genreStorage.findAll();
        assertThat(allGenres).hasSize(6);
    }
}
