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
 * A data object representing Tool permissions.
 */
public class StealthRules implements Comparable<StealthRules> {
    private final String netid;
    private final String siteid;
    private final String term;
    private final String toolid;

    public StealthRules(String netid, String siteid,String term, String toolid) {
        this.netid  = netid;
        this.siteid = siteid    ;
        this.term   = term;
        this.toolid = toolid;
    }

    public String getNetId() {
        return this.netid;
    }

    public String getSiteId() {
        return this.siteid;
    }

    public String getTerm() {
        return this.term;
    }

    public String getToolId() {
        return this.toolid;
    }

    @Override
    public int compareTo(StealthRules other) {
        return this.toolid.compareTo(other.toolid);
    }
}