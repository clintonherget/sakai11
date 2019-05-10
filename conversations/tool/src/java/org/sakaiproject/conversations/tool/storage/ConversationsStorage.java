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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.models.Topic;

@Slf4j
public class ConversationsStorage {

    public List<Topic> getAllTopics(final String siteId) {
        return DB.transaction
            ("Find all topics for site",
                new DBAction<List<Topic>>() {
                    @Override
                    public List<Topic> call(DBConnection db) throws SQLException {
                        List<Topic> topics = new ArrayList<>();
                        try (DBResults results = db.run("SELECT * FROM conversations_topic WHERE site_id = ?")
                            .param(siteId)
                            .executeQuery()) {
                            for (ResultSet result : results) {
                                topics.add(
                                    new Topic(
                                        result.getString("uuid"),
                                        result.getString("title"),
                                        result.getString("type")));
                            }

                            return topics;
                        }
                    }
                }
            );
    }

    public String createTopic(Topic topic, String siteId) {
        return DB.transaction("Create a topic for a site",
            new DBAction<String>() {
                @Override
                public String call(DBConnection db) throws SQLException {
                    String id = UUID.randomUUID().toString();

                    db.run("INSERT INTO conversations_topic (uuid, title, type, site_id) VALUES (?, ?, ?, ?)")
                        .param(id)
                        .param(topic.getTitle())
                        .param(topic.getType())
                        .param(siteId)
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
                                                             result.getString("type")));
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
                        try (DBResults results = db.run("SELECT conversations_post.*, sakai_user_id_map.eid FROM conversations_post" +
                                                        " INNER JOIN sakai_user_id_map ON sakai_user_id_map.user_id = conversations_post.posted_by" +
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
                                        result.getString("eid")));
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

    public String createPost(Post post, String topicUuid, String parentPostUuid) {
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

                    db.commit();

                    return id;
                }
            }
        );
    }

    public String setLastVisitedEvent(final String topicUuid, final String userId) {
        return DB.transaction("Create or update an event for visiting a topic",
                new DBAction<String>() {
                    @Override
                    public String call(DBConnection db) throws SQLException {
                        String id = UUID.randomUUID().toString();
                        Long timestamp = Calendar.getInstance().getTime().getTime();

                        db.run("DELETE FROM conversations_topic_event WHERE topic_uuid = ? AND user_id = ? AND event_name = ?")
                            .param(topicUuid)
                            .param(userId)
                            .param("TOPIC_LAST_VISITED")
                            .executeUpdate();

                        db.run("INSERT INTO conversations_topic_event (uuid, topic_uuid, user_id, event_name, event_time) VALUES (?, ?, ?, ?, ?)")
                            .param(id)
                            .param(topicUuid)
                            .param(userId)
                            .param("TOPIC_LAST_VISITED")
                            .param(timestamp)
                            .executeUpdate();

                        db.commit();

                        return id;
                    }
                }
        );
    }

    public Long getLastVisitedTopic(String topicUuid, String userId) {
        return DB.transaction
                ("Get time user last visited a topic",
                        new DBAction<Long>() {
                            @Override
                            public Long call(DBConnection db) throws SQLException {
                                try (DBResults results = db.run("SELECT * from conversations_topic_event WHERE topic_uuid = ? AND user_id = ? ORDER BY event_time DESC")
                                        .param(topicUuid)
                                        .param(userId)
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
