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

package org.sakaiproject.conversations.tool.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.models.Topic;
import org.sakaiproject.conversations.tool.storage.ConversationsStorage;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

public class CreateTopicHandler implements Handler {

    private String redirectTo = null;

    public CreateTopicHandler() {
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            String siteId = (String)context.get("siteId");

            String title = p.getString("title", null);
            String type = p.getString("type", null);
            String initialPost = p.getString("post", null);

            if (title == null || type == null) {
                // FIXME
                throw new RuntimeException("tile and type required");
            }

            Topic topic = new Topic(title, type);

            User currentUser = UserDirectoryService.getCurrentUser();

            String topicUuid = new ConversationsStorage().createTopic(topic, siteId, currentUser.getId());

            if (initialPost != null) {
                Post post = new Post(initialPost, currentUser.getId());

                String postUuid = new ConversationsStorage().createPost(post, topicUuid);
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("topicUuid", topicUuid);
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasRedirect() {
        return (redirectTo != null);
    }

    public String getRedirect() {
        return redirectTo;
    }

    public Errors getErrors() {
        return null;
    }

    public Map<String, List<String>> getFlashMessages() {
        return new HashMap<String, List<String>>();
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public boolean hasTemplate() {
        return false;
    }
}