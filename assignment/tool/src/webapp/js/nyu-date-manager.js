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
                <input class="form-control" type="text" v-model="assignment.open_date"/>
                <small class="errors"></small>
            </td>
            <td style="width: 20%" :id="'cell_due_date_' + idx">
                <input class="form-control" type="text" v-model="assignment.due_date"/>
                <small class="errors"></small>
            </td>
            <td style="width: 20%" :id="'cell_accept_until_' + idx">
                <input class="form-control" type="text" v-model="assignment.accept_until"/>
                <small class="errors"></small>
            </td>
            <td style="width: 5%">
                <input class="form-control" type="checkbox" v-model="assignment.published" :disabled="assignment.published ? 'disabled' : null"/>
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
      $.getJSON(this.toolurl + "/date-manager/assignments", (json) => {
        this.assignments = json;
        this.loaded = true;
      });
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
  },
  updated: function() {
  },
  watch: {
    errors: function (val) {
      $('.errors').empty();
      $('.danger').removeClass('danger');

      $(val).each(function (idx, elt) {
        console.log(elt);
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
