Vue.component('date-manager', {
  template: `
<span>
  <a ref="button" href="javascript:void(0);" @click="showManager()" class="button"><i class="fa fa-calendar"></i> Manage Assignment Dates <span class="badge">NEW</span></a>
  <page-modal ref="modal" v-on:hide="onModelClose" v-on:show="onModalShow">
    <template v-if="visible">
      <date-manager-form :toolurl="toolurl"></date-manager-form>
    </template>
  </page-modal>
</span>
`,
  data: function() {
    return {
      visible: false,
    };
  },
  props: ['toolurl'],
  computed: {
  },
  methods: {
    showManager: function() {
      this.hideToolContent();
      this.showPopup();
    },
    hideToolContent: function() {
      $('#pageBody').hide();
    },
    showPopup: function() {
      this.visible = true;
      this.$refs.modal.show();
    },
    onModelClose: function() {
      this.visible = false;
      $('#pageBody').show();
      $(this.$refs.button).focus();
      
    },
    onModalShow: function() {
    },
  },
  updated: function() {
  },
  mounted: function() {
    $('#pageBody').before(this.$refs.modal.$el);
  },
});


Vue.component('date-manager-form', {
  template: `
<div>
  <center>
    <h2>Manage Assignment Dates</h2>
    <p>Manage the dates and published status of your assignments, all from one place.</p>
  </center>

  <div>
    <a href="javascript:void(0);" ref="smartDateUpdaterButton" @click="showSmartDateUpdater()" class="button"><i aria-hidden="true" class="fa fa-magic"></i> Smart Date Updater</a>
    <a href="javascript:void(0);" @click="publishAll()" class="pull-right">Publish All</a>
    <smart-date-updater-modal ref="smartDateUpdaterModal" :assignments="assignments">
      <center>
        <h4 class="modal-title">Smart Date Updater</h4>
      </center>
      <smart-date-updater ref="updater" :toolurl="toolurl" :assignments="assignments"></smart-date-updater>
      <center>
        <a href="javascript:void(0);" class="button_color" @click="doUpdates()">Apply Date Updates</a>
        <a href="javascript:void(0);" class="button" @click="hideUpdater()">Cancel</a>
      </center>
    </smart-date-updater-modal>
  </div>

  <div aria-atomic="true" v-bind:aria-busy="!loaded">
    <template v-if="loaded">
      <div class="alert alert-danger" v-if="errors.length > 0">
          <p>Your update could not be saved due to the following errors:</p>
          <ul>
              <li v-for="error in errors">{{assignments[error.idx].title}} - {{error.msg}}</li>
          </ul>
      </div>

      <table class="table table-condensed table-striped table-bordered">
        <thead>
          <tr>
            <th>Assignment</th>
            <th>Open Date</th>
            <th>Due Date</th>
            <th>Accept Until</th>
            <th>Published</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(assignment, idx) in assignments" :id="assignment.id">
            <td style="width: 35%" :id="'cell_assignment_' + idx">
                {{assignment.title}}
                <div><small class="errors"></small></div>
            </td>
            <td style="width: 20%" :id="'cell_open_date_' + idx">
                <input type="hidden" :data-idx="idx" data-field="open_date" v-model="assignment.open_date"/>
                <input class="form-control datepicker field_type_open_date" type="text"/>
                <div><small class="text-muted">{{assignment.open_date_day_of_week}}</small></div>
                <div><small class="errors"></small></div>
            </td>
            <td style="width: 20%" :id="'cell_due_date_' + idx">
                <input type="hidden" :data-idx="idx" data-field="due_date" v-model="assignment.due_date"/>
                <input class="form-control datepicker field_type_due_date" type="text"/>
                <div><small class="text-muted">{{assignment.due_date_day_of_week}}</small></div>
                <div><small class="errors"></small></div>
            </td>
            <td style="width: 20%" :id="'cell_accept_until_' + idx">
                <input type="hidden" :data-idx="idx" data-field="accept_until" v-model="assignment.accept_until"/>
                <input class="form-control datepicker field_type_accept_until" type="text"/>
                <div><small class="text-muted">{{assignment.accept_until_day_of_week}}</small></div>
                <div><small class="errors"></small></div>
            </td>
            <td style="width: 5%">
                <input class="form-control" type="checkbox" v-model="assignment.published" :disabled="assignment.publishedOnServer ? 'disabled' : null"/>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="timezone" style="text-align: right">All times shown are in timezone {{timezone.replace('_', ' ')}}</div>
      <center>
        <p><a href="javascript:void(0);" class="button_color" @click="submitChanges">Save Changes</a> <a href="javascript:void(0);"  class="button" @click="cancelChanges">Cancel</a><p>
      </center>
    </template>
    <template v-else>
      <p>Loading assignment data...</p>
    </template>
  </div>
</div>
`,
  data: function() {
    return {
      errors: [],
      loaded: false,
      assignments: [],
    };
  },
  props: ['toolurl'],
  computed: {
  },
  methods: {
    publishAll: function() {
      this.assignments.forEach(function(assignment) {
        assignment.published = true;
      });
    },
    dayOfWeek: function(dateString) {
      if (dateString === '') {
        return ''
      }

      return moment(dateString).format('dddd');
    },
    doUpdates: function() {
      var self = this;
      self.$refs.updater.applyChanges();

      setTimeout(function() {
        self.hideUpdater();
      });
    },
    hideUpdater: function() {
      this.$refs.smartDateUpdaterModal.hide();
    },
    loadAssignments: function() {
      var self = this;
      $.getJSON(self.toolurl + "/date-manager/assignments", function(json) {
        self.timezone = json.timezone;

        var sortedAssignments = json.assignments.sort(function(a, b) {
          if (a.open_date > b.open_date) {
            return 1;
          } else if (a.open_date < b.open_date) {
            return -1;
          }
          return 0;
        });

        sortedAssignments.forEach(function (elt) {
          elt.publishedOnServer = elt.published;
          elt.open_date_day_of_week = undefined;
          elt.due_date_day_of_week = undefined;
          elt.accept_until_day_of_week = undefined;
          self.assignments.push(elt);
        });

        self.loaded = true;

        self.$nextTick(function () {
          $('.datepicker').each(function (idx, elt) {
            $(elt).css("width", "calc(100% - 50px)").css("display", "inline-block");
            var hidden = $(elt).closest('td').find('input:hidden');

            // Vue doesn't track changes to input type=hidden, so we handle our own binding.
            $(hidden).on('change', function () {
              var idx = $(this).data('idx');
              var field = $(this).data('field');

              // Take the value but drop the timezone.  The date picker blindly
              // assumes that the user's browser zone is what's in use, but we
              // know better.
              self.assignments[idx][field] = $(this).val().split('+')[0];
              self.assignments[idx][field + '_label'] = $(this).siblings('input.datepicker').val();
              self.assignments[idx][field + '_day_of_week'] = self.dayOfWeek(self.assignments[idx][field]);
            });

            hidden.attr('id', 'hidden_datepicker_' + idx);
            localDatePicker({
              input: elt,
              useTime: 1,
              parseFormat: 'YYYY-MM-DD HH:mm',
              val: hidden.val(),
              ashidden: {
                iso8601: 'hidden_datepicker_' + idx,
              }
            });
          });
        });
      })
    },
    submitChanges: function() {
      var self = this;

      this.errors = [];
      $.post(this.toolurl + "/date-manager/update",
             {
               json: JSON.stringify(this.assignments),
             }).done(
               function (response) {
                 if (response.status === 'ERROR') {
                   self.errors = response.errors;
                 } else {
                   window.location.reload();
                 }
                 console.log(response);
               }).fail(function () {
                 console.error("POST failed");
               });
    },
    cancelChanges: function() {
      this.$parent.hide();
    },
    showSmartDateUpdater: function() {
      this.$refs.smartDateUpdaterModal.show();
    },
  },
  updated: function() {
  },
  watch: {
    errors: function (val) {
      $('.errors').empty();
      $('.danger').removeClass('danger');

      $(val).each(function (idx, elt) {
        var id = ['cell', elt.field, elt.idx].join('_');
        var cell = $(document.getElementById(id));
        cell.addClass('danger');
        $(document.getElementById(id)).find('.errors').text(elt.msg);
      });

    }
  },
  mounted: function() {
    this.loadAssignments();
  },
});


Vue.component('page-modal', {
  template: `
<div style="display:none; overflow: auto; " aria-hidden="true" id="pageModal">
  <div aria-hidden="true" style="display: none; background: #eeffff; padding: 10px; border-bottom: #aadddd solid 1px;} solid 1px;">A public service announcement</div>
  <div ref="content" class="page-modal" style="padding: 20px; background: #FFF" tabindex="0">
    <a href="javascript:void(0)" class="pull-right" @click="hide" aria-label="Hide Popup"><i aria-hidden="true" class="fa fa-times fa-2x"></i></a>
    <slot></slot>
  </div>
</div>
`,
  data: function() {
    return {
    };
  },
  props: [],
  computed: {
  },
  methods: {
    show: function() {
      var self = this;
      $(self.$el).attr('aria-hidden', 'false').show();
      $(window).on('resize', function(event) {
        self.resize();
      });
      self.resize();
      $(self.$refs.content).focus()
    },
    resize: function() {
      $(this.$el).height($(window).height() - $(this.$el).offset().top + 'px');
    },
    hide: function() {
      var self = this;
      $(self.$el).off('resize', function(event) {
        self.resize();
      });
      $(self.$el).attr('aria-hidden', 'true').hide();
      self.$emit('hide');
    },
    captureUserInput: function() {
      var self = this;

      $(self.$el).off('blur').on('blur', ':focusable', function(event) {
        if (event.relatedTarget === null) {
          $(self.$refs.content).focus();
          return true;
        } else if ($(event.relatedTarget).is('.featherlight-content')) {
          // this is cool.
          return false;
        } else if ($(event.relatedTarget).closest('#pageModal').length == 0) {
          // can only focus above the modal, so loop back to bottom of modal
          $('#pageModal :focusable:last').focus();
          return true;
        }
      });
      $(self.$el).off('keydown').on('keydown', function(event) {
        if (event.key == 'Escape') {
          self.hide();
          return false;
        }
        return true;
      });
    }
  },
  updated: function() {
  },
  mounted: function() {
    this.captureUserInput();
  },
});

Vue.component('smart-date-updater', {
  template: `
<div>
  <center>
    <p>Note: Weekends and holidays are not automatically accounted for. You can add substitutions for days of the week below.
  </center>
  <div>
    <div class="row">
      <div class="col-sm-6 text-right">
        <label for="magicopendate" style="padding: 12px 0;">Set earliest Open Date:</label>
      </div>
      <div class="col-sm-6">
        <input type="text" id="magicopendate" class="form-control datepicker" style="width: calc(100% - 100px);display: inline-block;"/>
        <div><small class="text-muted">Currently: {{earliestOpenDate.open_date_label}}</small></div>
      </div>
    </div>
  </div>
  <div v-for="(substitution, idx) in substitutions" class="row">
    <div class="col-sm-12">
      <label :for="'substitution_from_' + idx">If dates occur on </label>
      <select id="'substitution_from_' + idx" v-model="substitution.from">
        <option v-for="day in availableDaysForSubstitution(substitution.from)">{{day}}</option>
      </select>
      <label :for="'substitution_to_' + idx"> change to </label>
      <select id="'substitution_to_' + idx" v-model="substitution.to">
        <option v-for="day in allDays">{{day}}</option>
      </select>
      <a href="javascript:void(0)" @click.prevent.stop="removeSubstitution(substitution)" aria-label="Remove substitution" class="button"><i aria-hidden="true" class="fa fa-trash"></i></a>
    </div>
  </div>
  <div class="row" v-if="daysWithSubstitutions.length < 7">
    <div class="col-sm-12">
      <a href="javascript:void(0);" @click.prevent.stop="addSubstitution()" class="button">Add Day of Week Substitution</a>
    </div>
  </div>
</div>
`,
  data: function() {
    return {
      magicopendate: '',
      substitutions: [],
      allDays: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
    };
  },
  props: ['assignments', 'toolurl'],
  computed: {
    earliestOpenDate: function() {
      return this.assignments.reduce(function (min, elt) {
        return (min.open_date < elt.open_date) ? min : elt;
      })
    },
    daysWithSubstitutions: function() {
      var days = [];
      this.substitutions.forEach(function(substitution) {
        days.push(substitution.from);
      });
      return days;
    },
  },
  methods: {
    availableDaysForSubstitution: function(current) {
      var self = this;
      var days = [];

      self.allDays.forEach(function(day) {
        if (current === day || self.daysWithSubstitutions.indexOf(day) < 0) {
          days.push(day);
        }
      });

      return days;
    },
    applyDiffToAssignmentDateString: function(isoDateTimeString, diffInDays) {
      var newDate = moment(isoDateTimeString).add(diffInDays, 'days');

      return newDate.format();
    },
    applyChanges: function() {
      var self = this;

      $.post(self.toolurl + "/date-manager/smart-updater-calculate",
             {
               browser_zone: jstz.determine().name(),
               old_earliest: self.earliestOpenDate.open_date,
               new_earliest: $('#hidden_datepicker_magicopendate').val().split('+')[0],
               substitutions: JSON.stringify(self.substitutions),
               assignments: JSON.stringify(self.assignments),
             }).done(
               function (response) {
                 // We're going to get back a new `assignments` response that we need to merge
                 // On the server side: calculate days difference with this new date, parse each json date and increment it
                 // Remember to parse relative to the user's profile timezone

                 adjustments = {}
                 $(response).each(function (_idx, assignment) {
                   adjustments[assignment.id] = {};

                   $(['open_date', 'due_date', 'accept_until']).each(function (_idx, property) {
                     if (assignment[property + '_adjusted']) {
                       $('#' + assignment.id).find('.datepicker.field_type_' + property).datetimepicker('setDate', new Date(assignment[property + '_adjusted']));
                       $('#' + assignment.id).find('.datepicker.field_type_' + property).trigger('select');
                       $('#' + assignment.id).find('.datepicker.field_type_' + property).datepicker('hide');
                     }
                   })
                 });
               });
    },
    addSubstitution: function() {
      this.substitutions.push({
        from: this.availableDaysForSubstitution(undefined)[0],
        to: this.allDays[0],
      });
    },
    removeSubstitution: function(substitutionToRemove) {
      var substitutionsToKeep = [];
      this.substitutions.forEach(function(substitution) {
        if (substitution.from != substitutionToRemove.from) {
          substitutionsToKeep.push(substitution)
        }
      });
      this.substitutions = substitutionsToKeep;
    }
  },
  mounted: function () {
    localDatePicker({
      input: $('#magicopendate'),
      useTime: 0,
      parseFormat: 'YYYY-MM-DD',
      val: this.earliestOpenDate.open_date.split('T')[0],
      ashidden: {
        iso8601: 'hidden_datepicker_magicopendate',
      }
    });
  },
});

Vue.component('smart-date-updater-modal', {
  template: `
<div v-if="visible">
  <slot></slot>
</div>
`,
  data: function() {
    return {
      'visible': false,
    }
  },
  methods: {
    hide: function() {
      $.featherlight.current().close();
    },
    show: function() {
      var self = this;

      self.visible = true;
      self.$nextTick(function() {
        $(document.body).append(self.$el);
        $.featherlight("<div>", {
          afterOpen: function() {
            setTimeout(function() {
              $.featherlight.current().$content.closest('.featherlight-content').attr('tabindex', '0').focus();
            });

            $.featherlight.current().$instance.off('keydown').on('keydown', ':focusable', function(event) {
              if (event.keyCode === 9) {
                var $first = $.featherlight.current().$instance.find(':focusable:first')[0];
                var $last = $.featherlight.current().$instance.find(':focusable:last')[0];
                if (event.shiftKey && $first == document.activeElement) {
                  event.preventDefault();
                  $last.focus();
                  return false;
                } else if (!event.shiftKey && $last == document.activeElement) {
                  event.preventDefault();
                  $first.focus();
                  return false;
                } else {
                  return true;
                }
              } else {
                return true;
              }
            });
          },
          afterClose: function() {
            self.$parent.$refs.smartDateUpdaterButton.focus();
          }
        });
        $.featherlight.current().$content.append(self.$el);
      });
    },
  },
});


function NYUDateManager(toolURL) {
  this.toolURL = toolURL.replace(/\?.*/, '');
  this.insertButton();
  this.initVue();
};

NYUDateManager.prototype.insertButton = function() {
  var $container = $('<div>').addClass('vue-enabled').addClass('pull-right');
  var $component = $('<date-manager>').attr('toolurl', this.toolURL);
  $container.append($component);
  $('#content .page-header').before($container);
};

NYUDateManager.prototype.initVue = function() {
  new Vue({ el: '.vue-enabled' });
};
