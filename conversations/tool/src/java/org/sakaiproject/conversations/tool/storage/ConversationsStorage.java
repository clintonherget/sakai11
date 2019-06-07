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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.conversations.tool.models.Attachment;
import org.sakaiproject.conversations.tool.models.MissingUuidException;
import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.models.Poster;
import org.sakaiproject.conversations.tool.models.Topic;

import org.sakaiproject.conversations.tool.storage.migrations.BaseMigration;


@Slf4j
public class ConversationsStorage {

    public void runDBMigrations() {
        BaseMigration.runMigrations();
    }


    public void storeFileMetadata(final String key, final String mimeType, final String fileName, final String role) {
        DB.transaction
            ("Store file metadata",
             (DBConnection db) -> {
                String id = UUID.randomUUID().toString();

                Long createdAt = System.currentTimeMillis();

                db.run(
                       "INSERT INTO conversations_files (uuid, mime_type, filename, role)" +
                       " VALUES (?, ?, ?, ?)")
                    .param(key)
                    .param(mimeType)
                    .param(fileName)
                    .param(role)
                    .executeUpdate();

                db.commit();

                return null;
            });
    }

    public Optional<Attachment> readFileMetadata(final String key) {
        return DB.transaction
            ("Retrieve file metadata",
             (DBConnection db) -> {
                try (DBResults results = db.run("select * from conversations_files where uuid = ?")
                     .param(key)
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        return (Optional<Attachment>)Optional.of(new Attachment(key,
                                                          result.getString("mime_type"),
                                                          result.getString("filename"),
                                                          result.getString("role")));
                    }
                }

                Optional<Attachment> result = Optional.empty();
                return result;
            });
    }

    public List<Topic> getTopics(final String siteId, final Integer page, final Integer pageSize, final String orderBy, final String orderDirection) {
        // FIXME pagination
        return DB.transaction
            ("Get topics on page for site",
             (DBConnection db) -> {
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
            });
    }

    public Integer getTopicsCount(final String siteId) {
        return DB.transaction
            ("Count topics for site",
             (DBConnection db) -> {
                Integer count = 0;
                try (DBResults results = db.run("SELECT count(*) as count FROM conversations_topic WHERE site_id = ?")
                     .param(siteId)
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        count = result.getInt("count");
                    }
                }
                return count;
            });
    }

    public void touchTopicLastActivityAt(final String topicUuid) {
        touchTopicLastActivityAt(topicUuid, System.currentTimeMillis());
    }

    public void touchTopicLastActivityAt(final String topicUuid, final Long lastActivityAt) {
        DB.transaction
            ("Create a post for a topic",
             (DBConnection db) -> {
                db.run("UPDATE conversations_topic SET last_activity_at = ?" +
                       " WHERE uuid = ?")
                    .param(lastActivityAt)
                    .param(topicUuid)
                    .executeUpdate();

                db.commit();

                return null;
            });
    }

    public Map<String, List<Poster>> getPostersForTopics(final List<String> topicUuids) {
        return DB.transaction
            ("Find all posters for topics",
             (DBConnection db) -> {
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
            });
    }

    public Map<String, Long> getPostCountsForTopics(final List<String> topicUuids) {
        // FIXME ORDER BY MOST RECENT CHANGES
        return DB.transaction
            ("Find post counts for topics",
             (DBConnection db) -> {
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
            });
    }

    public Map<String, Long> getLastActivityTimeForTopics(final List<String> topicUuids) {
        return DB.transaction
            ("Find last activity times for topics",
             (DBConnection db) -> {
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
            });
    }

    public String createTopic(Topic topic, String siteId, String userId) {
        return DB.transaction
            ("Create a topic for a site",
             (DBConnection db) -> {
                String id = UUID.randomUUID().toString();

                Long createdAt = System.currentTimeMillis();

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
            });
    }

    public Optional<Topic> getTopic(String uuid, final String siteId) {
        return DB.transaction
            ("Find a topic by uuid for a site",
             (DBConnection db) -> {
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

                    Optional<Topic> result = Optional.empty();
                    return result;
                }
            });
    }

    private void loadAttachments(List<Post> posts) {
        if (posts.isEmpty()) {
            return;
        }

        DB.transaction
            ("Fetch attachments for posts",
             (DBConnection db) -> {
                Map<String, Post> postMap = new HashMap<>();
                for (Post p : posts) {
                    try {
                        postMap.put(p.getUuid(), p);
                    } catch (MissingUuidException e) {}
                }

                int pageSize = 200;
                List<String> workSet = new ArrayList<>(pageSize);
                for (int start = 0; start < posts.size();) {
                    int end = Math.min(start + pageSize, posts.size());
                    workSet.clear();

                    for (int i = start; i < end; i++) {
                        try {
                            workSet.add(posts.get(i).getUuid());
                        } catch (MissingUuidException e) {}
                    }

                    String placeholders = workSet.stream().map(_p -> "?").collect(Collectors.joining(","));

                    DBPreparedStatement ps = db.run("SELECT a.post_uuid, f.*" +
                                                    " FROM conversations_attachments a " +
                                                    " INNER JOIN conversations_files f on f.uuid = a.attachment_key" +
                                                    " WHERE a.post_uuid in (" + placeholders + ")");

                    for (String postUuid : workSet) {
                        ps.param(postUuid);
                    }

                    try (DBResults results = ps.executeQuery()) {
                        for (ResultSet result : results) {
                            Post p = postMap.get(result.getString("post_uuid"));

                            p.addAttachment(new Attachment(result.getString("uuid"),
                                                           result.getString("mime_type"),
                                                           result.getString("filename"),
                                                           result.getString("role")));
                        }
                    }

                    start = end;
                }

                return null;
            });
    }

    public List<Post> getPosts(final String topicUuid) {
        return DB.transaction
            ("Find all posts for topic",
             (DBConnection db) -> {
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
                                           result.getString("lname"),
                                           result.getLong("version")));
                    }

                    loadAttachments(posts);

                    return posts;
                }
            });
    }

    public Post getPost(final String postUuid) {
        return DB.transaction
            ("Find all posts for topic",
             (DBConnection db) -> {
                Post post = null;
                try (DBResults results = db.run("SELECT conversations_post.*, sakai_user_id_map.eid, nyu_t_users.fname, nyu_t_users.lname FROM conversations_post" +
                                                " INNER JOIN sakai_user_id_map ON sakai_user_id_map.user_id = conversations_post.posted_by" +
                                                " LEFT JOIN nyu_t_users ON nyu_t_users.netid = sakai_user_id_map.eid" +
                                                " WHERE conversations_post.uuid = ?")
                     .param(postUuid)
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        post = new Post(result.getString("uuid"),
                                        result.getString("content"),
                                        result.getString("posted_by"),
                                        result.getLong("posted_at"),
                                        result.getString("parent_post_uuid"),
                                        result.getString("eid"),
                                        result.getString("fname"),
                                        result.getString("lname"),
                                        result.getLong("version"));
                    }

                    loadAttachments(Arrays.asList(post));

                    return post;
                }
            });
    }

    public String createPost(Post post, String topicUuid) {
        return createPost(post, topicUuid, null, Collections.emptyList(), System.currentTimeMillis());
    }

    public String createPost(final Post post, final String topicUuid, final String parentPostUuid, final List<String> attachmentKeys, long postedAt) {
        return DB.transaction
            ("Create a post for a topic",
             (DBConnection db) -> {
                String id = UUID.randomUUID().toString();

                db.run("INSERT INTO conversations_post (uuid, topic_uuid, parent_post_uuid, content, posted_by, posted_at, updated_by, updated_at, version)" +
                       " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .param(id)
                    .param(topicUuid)
                    .param(parentPostUuid)
                    .param(post.getContent())
                    .param(post.getPostedBy())
                    .param(postedAt)
                    .param(post.getPostedBy())
                    .param(postedAt)
                    .param(1)
                    .executeUpdate();

                touchTopicLastActivityAt(topicUuid, postedAt);

                for (String attachmentKey : attachmentKeys) {
                    db.run("INSERT INTO conversations_attachments (uuid, post_uuid, attachment_key) VALUES (?, ?, ?)")
                        .param(UUID.randomUUID().toString())
                        .param(id)
                        .param(attachmentKey)
                        .executeUpdate();
                }

                db.commit();

                return id;
            });
    }

    public String updatePost(final Post post, final String topicUuid, final List<String> attachmentKeys, long postedAt) {
        return DB.transaction
            ("Create a post for a topic",
             (DBConnection db) -> {
                try {
                    db.run("UPDATE conversations_post SET content = ?, updated_by = ?, updated_at = ?, version = ? WHERE uuid = ? AND topic_uuid = ?")
                        .param(post.getContent())
                        .param(post.getUpdatedBy())
                        .param(postedAt)
                        .param(post.getVersion())
                        .param(post.getUuid())
                        .param(topicUuid)
                        .executeUpdate();

                    touchTopicLastActivityAt(topicUuid, postedAt);

                    for (String attachmentKey : attachmentKeys) {
                        db.run("DELETE FROM conversations_attachments WHERE post_uuid = ?")
                            .param(post.getUuid())
                            .executeUpdate();

                        db.run("INSERT INTO conversations_attachments (uuid, post_uuid, attachment_key) VALUES (?, ?, ?)")
                            .param(UUID.randomUUID().toString())
                            .param(post.getUuid())
                            .param(attachmentKey)
                            .executeUpdate();
                    }

                    db.commit();

                    return post.getUuid();
                } catch(MissingUuidException e) {
                    // FIXME
                    throw new RuntimeException("Unable to update post due to missing uuid");
                }
            });
    }

    public String setLastReadTopicEvent(final String topicUuid, final String userId) {
        return DB.transaction
            ("Create or update an event for reading a topic",
             (DBConnection db) -> {
                String id = UUID.randomUUID().toString();
                Long timestamp = System.currentTimeMillis();

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
            });
    }

    public Long getLastReadTopic(String topicUuid, String userId) {
        return DB.transaction
            ("Get time user last read a topic",
             (DBConnection db) -> {
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
            });
    }
}
