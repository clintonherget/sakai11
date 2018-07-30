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
      <script>includeLatestJQuery('export.jsp');</script>
      <h:form id="exportForm">
        <f:verbatim>
          <h3>Export Settings</h3></f:verbatim>
          <h:outputText value="#{SyllabusTool.alertMessage}" styleClass="alertMessage" rendered="#{SyllabusTool.alertMessage != null}" />
          <h:outputText value="#{SyllabusTool.successMessage}" styleClass="messageSuccess" rendered="#{SyllabusTool.successMessage != null}" /><f:verbatim>
          <p>
            <label></f:verbatim>
            <h:selectBooleanCheckbox value="#{SyllabusTool.exportEnabled}" id="exportEnabled" /><f:verbatim> 
            Enable Export</label>
          </p>
          <ul id="exportList">
          </f:verbatim>
          <t:dataList value="#{SyllabusTool.exportPdfs.keySet().toArray()}" var="entry">
            <f:verbatim><li></f:verbatim>
              <h:outputText value="#{entry.title}" />
              <f:verbatim><ul></f:verbatim>
              <t:dataList value="#{SyllabusTool.exportPdfs[entry]}" var="attachment">
                <f:verbatim><li>
                  <label>
                    <input type="radio" name="selectedExportPdf" value="</f:verbatim><h:outputText value="#{attachment.syllabusAttachId}" /><f:verbatim>"> </f:verbatim>
                    <h:outputText value="#{attachment.name}" /><f:verbatim>
                  </label>
                </li></f:verbatim>
              </t:dataList>
              <f:verbatim></ul></f:verbatim>
            <f:verbatim></li></f:verbatim>
          </t:dataList>
          <f:verbatim>
          </ul>
        </f:verbatim>
        <h:inputHidden value="#{SyllabusTool.selectedExportAttachmentId}" id="selectedExportAttachmentId"/>
        <sakai:button_bar>
          <h:commandButton
            action="#{SyllabusTool.processSaveExportSettings}"
            styleClass="active"
            value="Save Changes" />
          <h:commandButton
            action="#{SyllabusTool.cancelSaveExportSettings}"
            value="Cancel" />
        </sakai:button_bar>
      </h:form>

      <script>
        function ExportForm() {
          this.$form = $('#exportForm');
          this.setupEnableCheckbox();
          this.setupAttachmentRadios();
        };

        ExportForm.prototype.setupEnableCheckbox = function() {
          var self = this;

          function enableDisableRadios() {
            var $checkbox = self.$form.find(':checkbox[id="exportForm:exportEnabled"]');
            if ($checkbox.is(':checked')) {
              // enable radios
              $('#exportList :radio').prop('disabled', false);
              $('#exportList label.disabled').removeClass('disabled');
            } else {
              // disable radios
              $('#exportList :radio').prop('disabled', true);
              $('#exportList label').addClass('disabled');
            }
          }

          var $checkbox = self.$form.find(':checkbox[id="exportForm:exportEnabled"]');
          $checkbox.on('click', function() {
            enableDisableRadios();
          });
          enableDisableRadios();
        };

        ExportForm.prototype.setupAttachmentRadios = function() {
          var $hidden = $(':hidden[id="exportForm:selectedExportAttachmentId"]');
          $('#exportList :radio').on('click', function() {
            var $radio = $(this);
            $hidden.val($radio.val()).trigger('changed');
          });

          if ($hidden.val() != "") {
            $('#exportList :radio[value="'+$hidden.val()+'"]').prop('checked', true);
          }
        };

        new ExportForm();
      </script>
    </sakai:view_content>
  </sakai:view_container>
</f:view>
