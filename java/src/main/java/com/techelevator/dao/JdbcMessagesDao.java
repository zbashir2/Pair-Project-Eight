package com.techelevator.dao;

import com.techelevator.model.Messages;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Component

public class JdbcMessagesDao implements MessagesDao{
    private JdbcTemplate jdbcTemplate;
    public  JdbcMessagesDao(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    @Override
    public Messages addMessages(Messages messages, int userId) {
       Integer messagesId = null;
       String getMessagesSql = "INSERT INTO messages (forum_id, user_id, message_content) " +
               "VALUES (?, ?, ?) RETURNING message_id;";
        messagesId  = jdbcTemplate.queryForObject(getMessagesSql, Integer.class, messages.getForumId(), userId, messages.getMessageContent());
        messages.setMessageId(messagesId);
        messages.setUserId(userId);
        return messages;
    }

    @Override
    public List<Messages> getMessages() {
        List<Messages> messages = new ArrayList<>();
        String sql = "SELECT  forum_id, user_id, message_content " +
                "FROM messages";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
        while ( results.next()) {
            Messages message = mapRowToMessages(results);
            messages.add(message);

        }
        return messages;
    }

    private Messages mapRowToMessages(SqlRowSet results) {
        Messages messages = new Messages();
        messages.setUserId(results.getInt("user_id"));
        messages.setForumId(results.getInt("forum_id"));
        messages.setMessageId(results.getInt("message_id"));
        messages.setMessageContent(results.getString("message_content"));
        return messages;

    }
}
