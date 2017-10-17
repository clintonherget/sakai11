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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;


/**
 * Return a PDF preview URL
 */
public class PreviewHandler implements Handler {

    private String redirectTo = null;

    private static Set<String> LIBREOFFICE_SUPPORTED_TYPES = new HashSet<>();

    static {
        LIBREOFFICE_SUPPORTED_TYPES.add("application/msword");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-excel");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-excel.addin.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-excel.sheet.binary.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-excel.sheet.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-excel.template.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-powerpoint");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-powerpoint.addin.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-powerpoint.presentation.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-powerpoint.slideshow.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-powerpoint.template.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-word.document.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.ms-word.template.macroEnabled.12");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.presentationml.template");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        LIBREOFFICE_SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        LIBREOFFICE_SUPPORTED_TYPES.add("image/jpeg");
        LIBREOFFICE_SUPPORTED_TYPES.add("image/jpg");
        LIBREOFFICE_SUPPORTED_TYPES.add("image/png");
        LIBREOFFICE_SUPPORTED_TYPES.add("image/gif");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            String path = (String)request.getParameter("path");

            ContentHostingService chs = (ContentHostingService) ComponentManager.get("org.sakaiproject.content.api.ContentHostingService");

            // FIXME: all sorts of sanity checking should happen here...
            ContentResource resource = chs.getResource(path);
            ConvertedDoc doc = toPDF(resource);

            if (doc != null) {
                try {
                    serve(response, doc.getInputStream(), "application/pdf");
                } finally {
                    doc.close();
                }
            } else {
                throw new RuntimeException("Unrecognized file type");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private interface ConvertedDoc {
        public InputStream getInputStream() throws Exception;
        public void close();
    }

    private ConvertedDoc toPDF(ContentResource resource) {
        if (resource.getContentType().endsWith("/pdf")) {
            return new ConvertedDoc () {
                public InputStream getInputStream() throws Exception {
                    return resource.streamContent();
                }

                public void close() {};
            };
        }

        try {
            // FIXME: security holes wide enough to drive a truck through... want to sandbox this somehow.
            if (LIBREOFFICE_SUPPORTED_TYPES.contains(resource.getContentType())) {
                final Path tempDir = Files.createTempDirectory("PreviewHandler");
                final Path inputFile = Files.createTempFile("PreviewHandlerInput", "");

                Files.copy(resource.streamContent(), inputFile, StandardCopyOption.REPLACE_EXISTING);

                ProcessBuilder pb = new ProcessBuilder("libreoffice",
                        "--headless",
                        "--invisible",
                        "--nologo",
                        "--nolockcheck",
                        "--convert-to", "pdf",
                        inputFile.toString(),
                        "--outdir", tempDir.toString());

                pb.redirectErrorStream(true);

                Process p = pb.start();
                p.getOutputStream().close();

                InputStream processOutput = p.getInputStream();
                byte[] outputBuffer = new byte[8192];

                int len;
                while ((len = processOutput.read(outputBuffer)) >= 0) {
                    System.err.write(outputBuffer, 0, len);
                }

                int status = p.waitFor();

                if (status != 0) {
                    throw new RuntimeException("Conversion failed (exit status: " + status + ")");
                }

                for (File f : tempDir.toFile().listFiles()) {
                    return new ConvertedDoc () {
                        public InputStream getInputStream() throws Exception {
                            return new FileInputStream(f);
                        }

                        public void close() {
                            for (File outFile : tempDir.toFile().listFiles()) {
                                outFile.delete();
                            }

                            inputFile.toFile().delete();
                            tempDir.toFile().delete();
                        }
                    };
                }
            }
        } catch (Exception e) {
            // FIXME: log
            System.err.println("EXCEPTION: " + e);
        }

        throw new RuntimeException("Couldn't preview this file");
    }


    private void serve(HttpServletResponse response, InputStream content, String mimeType) throws IOException {
        response.setContentType(mimeType);
        byte[] buffer = new byte[8192];
        int len;

        OutputStream out = response.getOutputStream();

        while ((len = content.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
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

}
