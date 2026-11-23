package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<User> findAll() {
        String sql = "SELECT id, email, login, name, birthday FROM users ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, email, login, name, birthday FROM users WHERE id = ?";
        return jdbcTemplate.query(sql, this::mapRowToUser, id).stream().findFirst();
    }

    public Optional<User> findUserById(long id) {
        return findById(id);
    }

    @Override
    public User create(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null);
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKeyAs(Long.class);
        user.setId(id);
        return user;
    }

    @Override
    public User update(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id пользователя должен быть указан");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        int rowsUpdated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null,
                user.getId());

        if (rowsUpdated == 0) {
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
        return user;
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sql = "MERGE INTO friendship (user_id, friend_id) KEY (user_id, friend_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.id, u.email, u.login, u.name, u.birthday " +
                     "FROM users u " +
                     "JOIN friendship f ON u.id = f.friend_id " +
                     "WHERE f.user_id = ? " +
                     "ORDER BY u.id";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        String sql = "SELECT u.id, u.email, u.login, u.name, u.birthday " +
                     "FROM users u " +
                     "JOIN friendship f1 ON u.id = f1.friend_id " +
                     "JOIN friendship f2 ON u.id = f2.friend_id " +
                     "WHERE f1.user_id = ? AND f2.user_id = ? " +
                     "ORDER BY u.id";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId, otherId);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        Date birthday = rs.getDate("birthday");
        if (birthday != null) {
            user.setBirthday(birthday.toLocalDate());
        }
        return user;
    }
}
