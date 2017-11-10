/**********************************************************************************
 *
 * Copyright (c) 2015 The Sakai Foundation
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

package org.sakaiproject.drive.tool.handlers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentEntity;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.util.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FolderTreeHandler implements Handler {

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            String siteId = (String) context.get("siteId");
            String baseCollection = "/group/" + siteId + "/";

            ContentHostingService chs = (ContentHostingService) ComponentManager.get("org.sakaiproject.content.api.ContentHostingService");

            // FIXME create new API for just returning collections sorted by IN_COLLECTION or the tree itself?
            ContentCollection root = chs.getCollection(baseCollection);
            List<ContentEntity> entities = chs.getAllEntities(baseCollection);
            entities = entities.stream().filter(entity -> entity.isCollection()).collect(Collectors.toList()); ;
            Collections.sort(entities, new Comparator<ContentEntity>() {
                public int compare(ContentEntity a, ContentEntity b) {
                    return a.getId().compareTo(b.getId());
                }
            });

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), constructTree(root, entities));
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

    public boolean hasTemplate() {
        return false;
    }

    private Folder constructTree(ContentCollection root, List<ContentEntity> entities) {
        Folder rootFolder = new Folder(root.getId(), getLabel(root));

        Deque<Folder> stack = new ArrayDeque<>();
        for (ContentEntity entity: entities) {
            Folder folder = new Folder(entity.getId(), getLabel(entity));

            if (stack.isEmpty()) {
                // must be the root!
                stack.push(folder);
            } else {
                ContentCollection parent = entity.getContainingCollection();
                while (!stack.isEmpty()) {
                    if (parent.getId().equals(stack.peek().getId())) {
                        stack.peek().addChild(folder);
                        stack.push(folder);
                        break;
                    } else {
                        stack.pop();
                    }
                }
            }
        }

        return stack.getLast();
    }

    private String getLabel(ContentEntity other) {
        String label = (String)other.getProperties().get(ResourceProperties.PROP_DISPLAY_NAME);
        // FIXME label shouldn't be null, but sometimes it is??
        if (label == null) {
            return "Error: no label";
        } else {
            return label;
        }
    }

    private class Folder implements Comparable<Folder> {
        private String id;
        private String name;
        private List<Folder> children;

        public Folder(String id, String name) {
            this.id = id;
            this.name = name;
            this.children = new ArrayList<Folder>();
        }

        public void addChild(Folder folder) {
            children.add(folder);
            Collections.sort(children);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<Folder> getChildren() {
            return children;
        }

        public boolean hashChildren() {
            return !children.isEmpty();
        }

        @Override
        public int compareTo(Folder other) {
            return this.getName().compareTo(other.getName());
        }
    }
}
