(function(exports) {

  function NYUCollabSiteRosterForm($form, siteId) {
    this.$form = $form;
    this.siteId = siteId;
    this.setupSessionDropdown();
  }

  NYUCollabSiteRosterForm.prototype.setupSessionDropdown = function() {
      var self = this;
      self.$sections = $('<div style="padding: 20px;">').attr('id', 'sections');
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

                    if (json.length == 0) {
                      self.$sections.append($('<p>').text("No matching rosters found"));
                      return;
                    }


                    json.forEach(function(section) {
                      var $div = $('<div>');
                      var $checkbox = $("<input type='checkbox' name='section_eid[]'>").val(section.sectionEid).attr('id', section.sectionEid);
                      if (section.added) {
                        $checkbox.prop('disabled', true);
                        $checkbox.prop('checked', true);
                      }
                      var $label = $('<label>').attr('for', section.sectionEid);
                      $label.text(' ' + section.sectionTitle);
                      $label.prepend($checkbox);
                      $div.append($label);
                      $label.append($('<div>').html($('<small class="text-muted" style="padding-left: 2em; font-weight: normal;">').text(section.sectionEid)));
                      if (section.crosslistedNonSponsors.length > 0) {
                        var $crosslisted = $('<div style="padding-left: 2em">');
                        $crosslisted.append('<div><small>Crosslisted with:</small></div>');
                        section.crosslistedNonSponsors.forEach(function(nonsponsor) {
                          var $nonsponsor = $('<div class="text-muted">');
                          $nonsponsor.append($('<input type="hidden" name="section_eid[]" disabled>').val(nonsponsor.sectionEid));
                          $nonsponsor.append($('<small>').text(nonsponsor.sectionTitle));
                          $nonsponsor.append(' ');
                          $nonsponsor.append($('<small>').text('(' + nonsponsor.sectionEid + ')'));
                          $crosslisted.append($nonsponsor);
                        });
                        $div.append($crosslisted);

                        $checkbox.on('change', function() {
                          $crosslisted.find(':hidden').prop('disabled', !$(this).is(':checked'));
                        });
                      }
                      self.$sections.append($div);
                    });
                  });
    }
  }

  exports.NYUCollabSiteRosterForm = NYUCollabSiteRosterForm;

})(window);
