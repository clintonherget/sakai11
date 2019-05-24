/**********************************************************************************
 *
 * Copyright (c) 2019 The Sakai Foundation
 *
 * Original developers:
 *
 *   New York University
 *   Payten Giles
 *   Mark Triggs
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.conversations.tool.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.models.Poster;
import org.sakaiproject.conversations.tool.models.Topic;

@Slf4j
public class ConversationsStorage {

//    public List<Topic> getAllTopics(final String siteId) {
//        return DB.transaction
//            ("Find all topics for site",
//                new DBAction<List<Topic>>() {
//                    @Override
//                    public List<Topic> call(DBConnection db) throws SQLException {
//                        List<Topic> topics = new ArrayList<>();
//                        try (DBResults results = db.run("SELECT * FROM conversations_topic WHERE site_id = ?")
//                            .param(siteId)
//                            .executeQuery()) {
//                            for (ResultSet result : results) {
//                                topics.add(
//                                    new Topic(
//                                        result.getString("uuid"),
//                                        result.getString("title"),
//                                        result.getString("type")));
//                            }
//
//                            return topics;
//                        }
//                    }
//                }
//            );
//    }

    public List<Topic> getTopics(final String siteId, final Integer page, final Integer pageSize, final String orderBy, final String orderDirection) {
        // FIXME pagination
        return DB.transaction
            ("Get topics on page for site",
                new DBAction<List<Topic>>() {
                    @Override
                    public List<Topic> call(DBConnection db) throws SQLException {
                        List<Topic> topics = new ArrayList<>();

                        final int minRowNum = pageSize * page + 1;
                        final int maxRowNum = pageSize * page + pageSize;

                        // SELECT uuid from (SELECT uuid, rownum rnk FROM (SELECT uuid FROM conversations_topic WHERE site_id = 'ded0dc82-d306-43ce-bff2-d21aefba9fec' ORDER BY last_activity_at desc)) WHERE rnk between 1 and 5)
                        try (DBResults results = db.run(
                                "SELECT *" +
                                " FROM conversations_topic" +
                                " WHERE uuid in (" +
                                "   SELECT uuid FROM (" +
                                "     SELECT uuid, rownum rnk FROM (" +
                                "       SELECT uuid FROM conversations_topic" +
                                "       WHERE site_id = ?" +
                                "       ORDER BY " + orderBy + " " + orderDirection.toUpperCase() +
                                "     )" + 
                                "   ) WHERE rnk BETWEEN ? AND ?" +
                                " )" +
                                " ORDER BY " + orderBy + " " + orderDirection.toUpperCase())
                            .param(siteId)
                            .param(String.valueOf(minRowNum))
                            .param(String.valueOf(maxRowNum))
                            .executeQuery()) {
                            for (ResultSet result : results) {
                                topics.add(
                                    new Topic(
                                        result.getString("uuid"),
                                        result.getString("title"),
                                        result.getString("type"),
                                        result.getString("created_by"),
                                        result.getLong("created_at"),
                                        result.getLong("last_activity_at")));
                            }

                            return topics;
                        }
                    }
                }
            );
    }

    public Integer getTopicsCount(final String siteId) {
        return DB.transaction
                ("Count topics for site",
                        new DBAction<Integer>() {
                            @Override
                            public Integer call(DBConnection db) throws SQLException {
                                Integer count = 0;
                                try (DBResults results = db.run("SELECT count(*) as count FROM conversations_topic WHERE site_id = ?")
                                        .param(siteId)
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        count = result.getInt("count");
                                    }
                                }
                                return count;
                            }
                        }
                );
    }

    public void touchTopicLastActivityAt(final String topicUuid) {
        touchTopicLastActivityAt(topicUuid, Calendar.getInstance().getTime().getTime());
    }

    public void touchTopicLastActivityAt(final String topicUuid, final Long lastActivityAt) {
        DB.transaction("Create a post for a topic",
                new DBAction<Void>() {
                    @Override
                    public Void call(DBConnection db) throws SQLException {
                        db.run("UPDATE conversations_topic SET last_activity_at = ?" +
                               " WHERE uuid = ?")
                                .param(lastActivityAt)
                                .param(topicUuid)
                                .executeUpdate();

                        db.commit();

                        return null;
                    }
                }
        );
    }

    public Map<String, List<Poster>> getPostersForTopics(final List<String> topicUuids) {
        return DB.transaction
                ("Find all posters for topics",
                        new DBAction<Map<String, List<Poster>>>() {
                            @Override
                            public Map<String, List<Poster>> call(DBConnection db) throws SQLException {
                                Map<String, List<Poster>> postersByTopic = new HashMap();

                                String placeholders = topicUuids.stream().map(_p -> "?").collect(Collectors.joining(","));

                                try (PreparedStatement ps = db.prepareStatement(
                                     "SELECT poster.*, sakai_user_id_map.eid, nyu_t_users.fname, nyu_t_users.lname" +
                                     " FROM (SELECT posted_by, topic_uuid, MAX(posted_at) AS latest_posted_at" +
                                     "       FROM conversations_post" + 
                                     "       WHERE topic_uuid in (" + placeholders + ")" +
                                     "       GROUP BY posted_by, topic_uuid) poster" +
                                     " INNER JOIN sakai_user_id_map ON sakai_user_id_map.user_id = poster.posted_by" + 
                                     " LEFT JOIN nyu_t_users ON nyu_t_users.netid = sakai_user_id_map.eid" +
                                     " ORDER BY poster.latest_posted_at DESC")) {
                                    Iterator<String> it = topicUuids.iterator();
                                    for (int i = 0; it.hasNext(); i++) {
                                        ps.setString(i + 1, it.next());
                                    }

                                    try (ResultSet rs = ps.executeQuery()) {
                                        while (rs.next()) {
                                            String topicUuid = rs.getString("topic_uuid");
                                            if (!postersByTopic.containsKey(topicUuid)) {
                                                postersByTopic.put(topicUuid, new ArrayList<Poster>());
                                            }
                                            postersByTopic.get(topicUuid).add(new Poster(rs.getString("posted_by"),
                                                                                         rs.getString("eid"),
                                                                                         rs.getString("fname"),
                                                                                         rs.getString("lname"),
                                                                                         rs.getLong("latest_posted_at")));
                                        }
                                    }
                                }

                                return postersByTopic;
                            }
                        }
                );
    }

    public Map<String, Long> getPostCountsForTopics(final List<String> topicUuids) {
        // FIXME ORDER BY MOST RECENT CHANGES
        return DB.transaction
                ("Find post counts for topics",
                        new DBAction<Map<String, Long>>() {
                            @Override
                            public Map<String, Long> call(DBConnection db) throws SQLException {
                                Map<String, Long> postCountsByTopic = new HashMap();

                                String placeholders = topicUuids.stream().map(_p -> "?").collect(Collectors.joining(","));

                                try (PreparedStatement ps = db.prepareStatement("SELECT count(*) as count, topic_uuid FROM conversations_post WHERE topic_uuid in (" + placeholders + ") GROUP BY topic_uuid")) {
                                    Iterator<String> it = topicUuids.iterator();
                                    for (int i = 0; it.hasNext(); i++) {
                                        ps.setString(i + 1, it.next());
                                    }

                                    try (ResultSet rs = ps.executeQuery()) {
                                        while (rs.next()) {
                                            String topicUuid = rs.getString("topic_uuid");
                                            Long count = rs.getLong("count");
                                            postCountsByTopic.put(topicUuid, count);
                                        }
                                    }
                                }

                                return postCountsByTopic;
                            }
                        }
                );
    }

    public Map<String, Long> getLastActivityTimeForTopics(final List<String> topicUuids) {
        return DB.transaction
                ("Find last activity times for topics",
                        new DBAction<Map<String, Long>>() {
                            @Override
                            public Map<String, Long> call(DBConnection db) throws SQLException {
                                Map<String, Long> lastActivityByTopic = new HashMap();

                                String placeholders = topicUuids.stream().map(_p -> "?").collect(Collectors.joining(","));

                                try (PreparedStatement ps = db.prepareStatement("SELECT max(posted_at) as last_activity, topic_uuid FROM conversations_post WHERE topic_uuid in (" + placeholders + ") GROUP BY topic_uuid")) {
                                    Iterator<String> it = topicUuids.iterator();
                                    for (int i = 0; it.hasNext(); i++) {
                                        ps.setString(i + 1, it.next());
                                    }

                                    try (ResultSet rs = ps.executeQuery()) {
                                        while (rs.next()) {
                                            String topicUuid = rs.getString("topic_uuid");
                                            Long count = rs.getLong("last_activity");
                                            lastActivityByTopic.put(topicUuid, count);
                                        }
                                    }
                                }

                                return lastActivityByTopic;
                            }
                        }
                );
    }

    public String createTopic(Topic topic, String siteId, String userId) {
        return DB.transaction("Create a topic for a site",
            new DBAction<String>() {
                @Override
                public String call(DBConnection db) throws SQLException {
                    String id = UUID.randomUUID().toString();

                    Long createdAt = Calendar.getInstance().getTime().getTime();

                    db.run(
                        "INSERT INTO conversations_topic (uuid, title, type, site_id, created_by, created_at, last_activity_at)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?)")
                        .param(id)
                        .param(topic.getTitle())
                        .param(topic.getType())
                        .param(siteId)
                        .param(userId)
                        .param(createdAt)
                        .param(createdAt)
                        .executeUpdate();

                    db.commit();

                    return id;
                }
            }
        );
    }

    public Optional<Topic> getTopic(String uuid, final String siteId) {
        return DB.transaction
            ("Find a topic by uuid for a site",
                new DBAction<Optional<Topic>>() {
                    @Override
                    public Optional<Topic> call(DBConnection db) throws SQLException {
                        try (DBResults results = db.run("SELECT * from conversations_topic WHERE uuid = ? AND site_id = ?")
                            .param(uuid)
                            .param(siteId)
                            .executeQuery()) {
                            for (ResultSet result : results) {
                                return Optional.of(new Topic(result.getString("uuid"),
                                                             result.getString("title"),
                                                             result.getString("type"),
                                                             result.getString("created_by"),
                                                             result.getLong("created_at"),
                                                             result.getLong("last_activity_at")));
                            }

                            return Optional.empty();
                        }
                    }
                }
            );
    }

    public List<Post> getPosts(final String topicUuid) {
        return DB.transaction
            ("Find all posts for topic",
                new DBAction<List<Post>>() {
                    @Override
                    public List<Post> call(DBConnection db) throws SQLException {
                        List<Post> posts = new ArrayList<>();
                        try (DBResults results = db.run("SELECT conversations_post.*, sakai_user_id_map.eid, nyu_t_users.fname, nyu_t_users.lname FROM conversations_post" +
                                                        " INNER JOIN sakai_user_id_map ON sakai_user_id_map.user_id = conversations_post.posted_by" +
                                                        " LEFT JOIN nyu_t_users ON nyu_t_users.netid = sakai_user_id_map.eid" +
                                                        " WHERE conversations_post.topic_uuid = ?")
                            .param(topicUuid)
                            .executeQuery()) {
                            for (ResultSet result : results) {
                                posts.add(
                                    new Post(
                                        result.getString("uuid"),
                                        result.getString("content"),
                                        result.getString("posted_by"),
                                        result.getLong("posted_at"),
                                        result.getString("parent_post_uuid"),
                                        result.getString("eid"),
                                        result.getString("fname"),
                                        result.getString("lname")));
                            }

                            return posts;
                        }
                    }
                }
            );
    }

    public String createPost(Post post, String topicUuid) {
        return createPost(post, topicUuid, null);
    }

    public String createPost(final Post post, final String topicUuid, final String parentPostUuid) {
        return DB.transaction("Create a post for a topic",
            new DBAction<String>() {
                @Override
                public String call(DBConnection db) throws SQLException {
                    String id = UUID.randomUUID().toString();
                    Long postedAt = Calendar.getInstance().getTime().getTime();

                    db.run("INSERT INTO conversations_post (uuid, topic_uuid, parent_post_uuid, content, posted_by, posted_at) VALUES (?, ?, ?, ?, ?, ?)")
                        .param(id)
                        .param(topicUuid)
                        .param(parentPostUuid)
                        .param(post.getContent())
                        .param(post.getPostedBy())
                        .param(postedAt)
                        .executeUpdate();

                    touchTopicLastActivityAt(topicUuid, postedAt);

                    db.commit();

                    return id;
                }
            }
        );
    }

    public String setLastReadTopicEvent(final String topicUuid, final String userId) {
        return DB.transaction("Create or update an event for reading a topic",
                new DBAction<String>() {
                    @Override
                    public String call(DBConnection db) throws SQLException {
                        String id = UUID.randomUUID().toString();
                        Long timestamp = Calendar.getInstance().getTime().getTime();

                        db.run("DELETE FROM conversations_topic_event WHERE topic_uuid = ? AND user_id = ? AND event_name = ?")
                            .param(topicUuid)
                            .param(userId)
                            .param("TOPIC_LAST_READ")
                            .executeUpdate();

                        db.run("INSERT INTO conversations_topic_event (uuid, topic_uuid, user_id, event_name, event_time) VALUES (?, ?, ?, ?, ?)")
                            .param(id)
                            .param(topicUuid)
                            .param(userId)
                            .param("TOPIC_LAST_READ")
                            .param(timestamp)
                            .executeUpdate();

                        db.commit();

                        return id;
                    }
                }
        );
    }

    public Long getLastReadTopic(String topicUuid, String userId) {
        return DB.transaction
                ("Get time user last read a topic",
                        new DBAction<Long>() {
                            @Override
                            public Long call(DBConnection db) throws SQLException {
                                try (DBResults results = db.run("SELECT * from conversations_topic_event WHERE topic_uuid = ? AND user_id = ? AND event_name = ? ORDER BY event_time DESC")
                                        .param(topicUuid)
                                        .param(userId)
                                        .param("TOPIC_LAST_READ")
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        return result.getLong("event_time");
                                    }

                                    return 0L;
                                }
                            }
                        }
                );
    }
}