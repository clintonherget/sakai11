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

import java.util.List;

public interface ToolService {

    public List<StealthRules> searchByNetId(String netId);
    public List<StealthRules> searchBySiteId(String siteId);
    public void addPermissionBySite(String siteId, String toolId);
    public void removePermissionBySite(String siteId);
	public void addPermissionByUser(String netId, String term, String toolId);
    public void removePermissionByUser(String netId, int term);
    public List<StealthTool> getAllStealthTools();
    public void removeToolsFromPilotTable(String toolId);
}