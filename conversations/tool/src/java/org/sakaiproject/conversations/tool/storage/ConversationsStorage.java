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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.conversations.tool.models.Topic;

@Slf4j
public class ConversationsStorage {

    public List<Topic> getAllTopics(String siteId) {
        return DB.transaction
            ("Find all topics for site",
                new DBAction<List<Topic>>() {
                    @Override
                    public List<Topic> call(DBConnection db) throws SQLException {
                        List<Topic> topics = new ArrayList<>();
                        try (DBResults results = db.run("SELECT * FROM conversations_topic").executeQuery()) {
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
}
