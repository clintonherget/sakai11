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

import org.sakaiproject.component.cover.ServerConfigurationService;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileStore {

    public class Handle {
        public InputStream inputStream;
        public String mimeType;
    }

    public String storeInline(byte[] content, String filename, String mimeType) throws Exception {
        String basedir = ServerConfigurationService.getString("conversation-tool.storage", "/tmp/conversation");

        String key = UUID.randomUUID().toString();

        // FIXME: Need to think more about this.  Should store metadata in the
        // DB for starters.
        String subdir = key.split("-")[0];

        Paths.get(basedir, subdir).toFile().mkdirs();

        File output = Paths.get(basedir, subdir, key).toFile();

        try (FileOutputStream out = new FileOutputStream(output)) {
            out.write(content);
        }

        File metadata = Paths.get(basedir, subdir, key + ".metadata").toFile();

        try (FileOutputStream out = new FileOutputStream(metadata)) {
            out.write((mimeType + "\n").getBytes("UTF-8"));
            out.write((filename + "\n").getBytes("UTF-8"));
        }

        return key;
    }

    public Handle read(String key) throws Exception {
        String basedir = ServerConfigurationService.getString("conversation-tool.storage", "/tmp/conversation");
        String subdir = key.split("-")[0];

        File dataFile = Paths.get(basedir, subdir, key).toFile();
        File metadataFile = Paths.get(basedir, subdir, key + ".metadata").toFile();

        Handle result = new Handle();

        try (BufferedReader metadata = new BufferedReader(new FileReader(metadataFile))) {
            result.mimeType = metadata.readLine().trim();
        }

        result.inputStream = new FileInputStream(dataFile);

        return result;
    }
}
