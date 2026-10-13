package bot.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ReminderRepository {

    private final String databaseUrl;

    public ReminderRepository(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public void initialize() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     CREATE TABLE IF NOT EXISTS reminders (
                         id BIGSERIAL PRIMARY KEY,
                         channel_id BIGINT NOT NULL,
                         user_id BIGINT NOT NULL,
                         message TEXT NOT NULL,
                         remind_at TIMESTAMPTZ NOT NULL,
                         status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         delivered_at TIMESTAMPTZ
                     )
                     """)) {
            statement.execute();
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE reminders SET status = 'PENDING' WHERE status = 'PROCESSING'"
             )) {
            statement.executeUpdate();
        }
    }

    public long save(long channelId, long userId, String message, OffsetDateTime remindAt)
            throws SQLException {
        String sql = """
                INSERT INTO reminders (channel_id, user_id, message, remind_at)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, channelId);
            statement.setLong(2, userId);
            statement.setString(3, message);
            statement.setObject(4, remindAt);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    public List<Reminder> claimDueReminders() throws SQLException {
        String sql = """
                WITH due_reminders AS (
                    SELECT id
                    FROM reminders
                    WHERE status = 'PENDING' AND remind_at <= CURRENT_TIMESTAMP
                    ORDER BY remind_at
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE reminders
                SET status = 'PROCESSING'
                WHERE id IN (SELECT id FROM due_reminders)
                RETURNING id, channel_id, user_id, message, remind_at
                """;

        List<Reminder> reminders = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                reminders.add(new Reminder(
                        resultSet.getLong("id"),
                        resultSet.getLong("channel_id"),
                        resultSet.getLong("user_id"),
                        resultSet.getString("message"),
                        resultSet.getObject("remind_at", OffsetDateTime.class)
                ));
            }
        }
        return reminders;
    }

    public void markDelivered(long reminderId) throws SQLException {
        updateStatus(reminderId, "DELIVERED", true);
    }

    public void markPending(long reminderId) throws SQLException {
        updateStatus(reminderId, "PENDING", false);
    }

    private Connection openConnection() throws SQLException {
        if (databaseUrl.startsWith("jdbc:")) {
            return DriverManager.getConnection(databaseUrl);
        }

        URI databaseUri = URI.create(databaseUrl);
        String host = databaseUri.getHost();
        if (host == null) {
            throw new SQLException("DATABASE_URL must include a database host.");
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(host);
        if (databaseUri.getPort() != -1) {
            jdbcUrl.append(':').append(databaseUri.getPort());
        }
        jdbcUrl.append(databaseUri.getRawPath());
        if (databaseUri.getRawQuery() != null) {
            jdbcUrl.append('?').append(databaseUri.getRawQuery());
        }

        Properties properties = new Properties();
        String userInfo = databaseUri.getUserInfo();
        if (userInfo != null) {
            String[] credentials = userInfo.split(":", 2);
            properties.setProperty("user", credentials[0]);
            if (credentials.length == 2) {
                properties.setProperty("password", credentials[1]);
            }
        }

        return DriverManager.getConnection(jdbcUrl.toString(), properties);
    }

    private void updateStatus(long reminderId, String status, boolean delivered) throws SQLException {
        String sql = delivered
                ? "UPDATE reminders SET status = ?, delivered_at = CURRENT_TIMESTAMP WHERE id = ?"
                : "UPDATE reminders SET status = ?, delivered_at = NULL WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, reminderId);
            statement.executeUpdate();
        }
    }
}
