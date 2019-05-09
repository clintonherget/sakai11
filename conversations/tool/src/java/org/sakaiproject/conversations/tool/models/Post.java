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

package org.sakaiproject.conversations.tool.models;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import lombok.Getter;

public class Post implements Comparable<Post> {
    private final String uuid;
    @Getter
    private final String content;
    @Getter
    private final String postedBy;
    @Getter
    private final String postedByEid;
    @Getter
    private final Long postedAt;

    public Post(String uuid, String content, String postedBy, Long postedAt, String postedByEid) {
        this.uuid = uuid;
        this.content = content;
        this.postedBy = postedBy;
        this.postedAt = postedAt;
        this.postedByEid = postedByEid;
    }

    public Post(String content, String postedBy) {
        this.uuid = null;
        this.content = content;
        this.postedBy = postedBy;
        this.postedByEid = null;
        this.postedAt = null;
    }

    @Override
    public int compareTo(Post other) {
        // Sort newest first!
        return other.getPostedAt().compareTo(getPostedAt());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Post)) {
            return false;
        }

        try {
            return uuid.equals(((Post)obj).getUuid());
        } catch (MissingUuidException e) {
            return false;
        }
    }

    public int hashCode() {
        return uuid.hashCode();
    }

    public String getUuid() throws MissingUuidException {
        if (this.uuid == null) {
            throw new MissingUuidException("No UUID has been set for this topic");
        }

        return this.uuid;
    }

    public Errors validate() {
        Errors errors = new Errors();

        return errors;
    }
}
