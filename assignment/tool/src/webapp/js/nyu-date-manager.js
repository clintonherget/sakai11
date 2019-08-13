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
    <smart-date-updater-modal ref="smartDateUpdaterModal"></smart-date-updater-modal>
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
            <th>Published Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(assignment, idx) in assignments">
            <td style="width: 35%" :id="'cell_assignment_' + idx">
                {{assignment.title}}
                <div><small class="errors"></small></div>
            </td>
            <td style="width: 20%" :id="'cell_open_date_' + idx">
                <input type="hidden" :data-idx="idx" data-field="open_date" v-model="assignment.open_date"/>
                <input class="form-control datepicker" type="text"/>
                <small class="errors"></small>
            </td>
            <td style="width: 20%" :id="'cell_due_date_' + idx">
                <input type="hidden" :data-idx="idx" data-field="due_date" v-model="assignment.due_date"/>
                <input class="form-control datepicker" type="text"/>
                <small class="errors"></small>
            </td>
            <td style="width: 20%" :id="'cell_accept_until_' + idx">
                <input type="hidden" :data-idx="idx" data-field="accept_until" v-model="assignment.accept_until"/>
                <input class="form-control datepicker" type="text"/>
                <small class="errors"></small>
            </td>
            <td style="width: 5%">
                <input class="form-control" type="checkbox" v-model="assignment.published" :disabled="assignment.publishedOnServer ? 'disabled' : null"/>
            </td>
          </tr>
        </tbody>
      </table>
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
    loadAssignments: function() {
      var self = this;
      $.getJSON(this.toolurl + "/date-manager/assignments", (json) => {
        this.assignments = json.map(function (elt) {
          elt.publishedOnServer = elt.published;
          return elt;
        });

        this.loaded = true;

        self.$nextTick(function () {
          $('.datepicker').each(function (idx, elt) {
            $(elt).css("width", "calc(100% - 50px)").css("display", "inline-block");
            var hidden = $(elt).closest('td').find('input:hidden');

            // Vue doesn't track changes to input type=hidden, so we handle our own binding.
            $(hidden).on('change', function () {
              var idx = $(this).data('idx');
              var field = $(this).data('field');

              self.assignments[idx][field] = $(this).val();
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
  <div style="background: #eeffff; padding: 10px; border-bottom: #aadddd solid 1px;} solid 1px;">A public service announcement</div>
  <div ref="content" class="page-modal" style="padding: 20px; background: #FFF" tabindex="0">
    <a href="javascript:void(0)" class="pull-right" @click="hide">X</a>
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
      $(this.$el).attr('aria-hidden', 'false').show();
      $(window).on('resize', (event) => {
        this.resize();
      });
      this.resize();
      $(this.$refs.content).focus()
    },
    resize: function() {
      $(this.$el).height($(window).height() - $(this.$el).offset().top + 'px');
    },
    hide: function() {
      $(this.$el).off('resize', (event) => {
        this.resize();
      });
      $(this.$el).attr('aria-hidden', 'true').hide();
      this.$emit('hide');
    },
    captureBlur: function() {
      $(this.$el).off('blur').on('blur', ':focusable', (event) => {
        if (event.relatedTarget === null) {
          $(this.$refs.content).focus();
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
    }
  },
  updated: function() {
  },
  mounted: function() {
    this.captureBlur();
  },
});

Vue.component('smart-date-updater', {
  template: `
<div>
  <center>
    <p>Note: Weekends and holidays are not automatically accounted for. You can add substitutions for days of the week below.
  </center>
  <div>
    <p>DO THINGS HERE</p>
  </div>
</div>
`,
  data: function() {
    return {};
  },
});

Vue.component('smart-date-updater-modal', {
  template: `
<div v-if="visible">
  <center>
    <h4 class="modal-title">Smart Date Updater</h4>
  </center>

  <smart-date-updater></smart-date-updater>

  <center>
    <a href="javascript:void(0);" class="button_color">Apply Date Updates</a>
    <a href="javascript:void(0);" class="button" @click="hide()">Cancel</a>
  </center>
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
      this.visible = true;
      this.$nextTick(() => {
        $(document.body).append(this.$el);
        $.featherlight("<div>", {
          afterOpen: () => {
            setTimeout(function() {
              $.featherlight.current().$content.closest('.featherlight-content').attr('tabindex', '0').focus();
            });

            // FIXME BLOCK TAB TO BELOW...
          },
          afterClose: () => {
            this.$parent.$refs.smartDateUpdaterButton.focus();
          }
        });
        $.featherlight.current().$content.append(this.$el);
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
