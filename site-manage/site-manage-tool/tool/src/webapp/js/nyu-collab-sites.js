(function(exports) {

  function NYUCollabSiteRosterForm($form, siteId) {
    this.$form = $form;
    this.siteId = siteId;
    this.setupSessionDropdown();
  }

  NYUCollabSiteRosterForm.prototype.setupSessionDropdown = function() {
      var self = this;
      self.$sections = $('<div>').attr('id', 'sections');
      self.$form.prepend(self.$sections);
      $.getJSON('/sakai-site-manage-tool/nyu-collab-service/?action=list-sessions', function(json) {
          
          self.$select = $('<select>').attr('id', 'sessions');
          self.$select.append('<option>');
          json.forEach(function(session) {
              self.$select.append($('<option>').val(session.eid).text(session.title));
          });
          self.$form.prepend(self.$select);
          self.$form.prepend('<label for="sessions">Academic Session</label><br>');
          self.$select.on('change', function() {
              self.renderSectionsForCurrentTerm();
          });
      });
  };

  NYUCollabSiteRosterForm.prototype.renderSectionsForCurrentTerm = function() {
    var self = this;
    if (self.$select.val() == '') {
        self.$sections.empty();
    } else {
        self.$sections.html('Loading...');
        $.getJSON('/sakai-site-manage-tool/nyu-collab-service/',
                  {
                    'action': 'sections-for-session',
                    'sessionEid': self.$select.val(),
                    'siteId': self.siteId,
                  },
                  function(json) {
                    self.$sections.empty();
                    json.forEach(function(section) {
                      var $div = $('<div>');
                      var $checkbox = $("<input type='checkbox' name='section_eid[]'>").val(section.sectionEid).attr('id', section.sectionEid);
                      var $label = $('<label>').attr('for', section.sectionEid);
                      $label.text(section.sectionTitle);
                      $label.prepend($checkbox);
                      $div.append($label);
                      self.$sections.append($div);
                    });
                  });
    }
  }

  exports.NYUCollabSiteRosterForm = NYUCollabSiteRosterForm;

})(window); 