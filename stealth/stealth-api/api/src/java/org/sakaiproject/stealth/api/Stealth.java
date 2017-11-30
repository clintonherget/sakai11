/**********************************************************************************
 *
 * Original developers:
 *
 *   New York University
 *   Bhavesh Vasandani
 *   Eric Lin
 *   Steven Adam
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.stealth.api;

/**
 * The interface for the Stealth System service.
 */
public interface Stealth {

    public void init();

    public void destroy();

    /**
     * Return a list of active user based on query
     */
    public UserService getUsers();

    /**
     * Return a list of active site ids with query
     */
    public SiteService getSites();

    /**
     * Return a list of active tool rules
     */
    public ToolService getRules();

    /**
     * Return an I18N translator for a given file and locale.
     */
    public I18n getI18n(ClassLoader loader, String resourceBase);

}
