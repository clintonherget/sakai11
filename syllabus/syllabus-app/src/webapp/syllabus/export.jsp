<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>
<%@ taglib uri="http://sakaiproject.org/jsf/sakai" prefix="sakai" %>
<% response.setContentType("text/html; charset=UTF-8"); %>
<f:view>
  <jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.tool.syllabus.bundle.Messages"/>
  </jsp:useBean>

  <sakai:view_container title="Export">
    <sakai:view_content>
      <f:verbatim>
        <h3>Export Settings</h3>
        <p><label><input type="checkbox" name="enableExport"/> Enable Export</label></p>
        <ul id="exportList">
        </f:verbatim>
        <t:dataList value="#{SyllabusTool.exportPdfs.keySet().toArray()}" var="entry">
          <f:verbatim><li></f:verbatim>
            <h:outputText value="#{entry.title}" />
            <f:verbatim><ul></f:verbatim>
            <t:dataList value="#{SyllabusTool.exportPdfs[entry]}" var="attachment">
              <f:verbatim><li></f:verbatim>
                <h:outputText value="#{attachment.name}" />
              <f:verbatim></li></f:verbatim>
            </t:dataList>
            <f:verbatim></ul></f:verbatim>
          <f:verbatim></li></f:verbatim>
        </t:dataList>
        <f:verbatim>
        </ul>
      </f:verbatim>
    </sakai:view_content>
  </sakai:view_container>
</f:view>
