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

public class Topic implements Comparable<Topic> {
    private final String uuid;
    @Getter
    private final String type;
    @Getter
    private final String title;

    public Topic(String uuid, String title, String type) {
        this.uuid = uuid;
        this.title = title;
        this.type = type;
    }

    public Topic(String title, String type) {
        this.uuid = null;
        this.title = title;
        this.type = type;
    }

    @Override
    public int compareTo(Topic other) {
        return getTitle().compareTo(other.getTitle());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Topic)) {
            return false;
        }

        try {
            return uuid.equals(((Topic)obj).getUuid());
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
