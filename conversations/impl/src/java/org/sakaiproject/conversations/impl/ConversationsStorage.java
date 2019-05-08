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

package org.sakaiproject.conversations.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.conversations.api.Conversations;
import org.sakaiproject.conversations.api.Conversation;

@Slf4j
public class ConversationsStorage implements Conversations {

    @Override
    public List<Conversation> getAll(String siteId) {
        return DB.transaction
            ("Find all conversations",
                new DBAction<List<Conversation>>() {
                    @Override
                    public List<Conversation> call(DBConnection db) throws SQLException {
                        List<Conversation> conversations = new ArrayList<>();
                        try (DBResults results = db.run("SELECT * FROM conversations").executeQuery()) {
                            for (ResultSet result : results) {
                                conversations.add(
                                    new Conversation(
                                        result.getString("uuid"),
                                        result.getString("type"),
                                        result.getString("title")));
                            }

                            return conversations;
                        }
                    }
                }
            );
    }

}
