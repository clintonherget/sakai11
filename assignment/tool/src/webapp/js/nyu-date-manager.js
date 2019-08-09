Vue.component('date-manager', {
  template: `
<span>
  <a ref="button" href="javascript:void(0);" @click="showManager()" class="button"><i class="fa fa-calendar"></i> Manage Assignment Dates <span class="badge">NEW</span></a>
  <page-modal ref="modal" v-on:hide="onModelClose" v-on:show="onModalShow">
    <date-manager-form></date-manager-form>
  </page-modal>
</span>
`,
  data: function() {
    return {
    };
  },
  props: [],
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
      this.$refs.modal.show();
    },
    onModelClose: function() {
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
 
  <template v-if="loaded">
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
        <tr v-for="assignment in assignments">
          <td>{{assignment.name}}</td>
          <td><input type="text"/></td>
          <td><input type="text"/></td>
          <td><input type="text"/></td>
          <td><input type="text"/></td>
        </tr>
      </tbody>
    </table>
  </template>
  <template v-else>
    <p>Loading assignment data...</p>
  </template>
</div>
`,
  data: function() {
    return {
      loaded: false,
      assignments: [],
    };
  },
  props: [],
  computed: {
  },
  methods: {
    loadAssignments: function() {
      this.assignments = [{
        'name': "TEST",
      }];
      this.loaded = true;
    },
  },
  updated: function() {
  },
  mounted: function() {
    setTimeout(() => {
      this.loadAssignments();
    }, 2000);
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
      $(this.$el).on('resize', (event) => {
        this.resize();
      });
      this.resize();
      $(this.$refs.content).focus()
    },
    resize: function() {
      $(this.$el).height($(window).height() - $(this.$el).offset().top + 'px');
    },
    hide: function() {
      $(this.$el).off('resize');
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


function NYUDateManager() {
  this.insertButton();
  this.initVue();
};

NYUDateManager.prototype.insertButton = function() {
  var $container = $('<div>').addClass('vue-enabled').addClass('pull-right');
  var $component = $('<date-manager>');
  $container.append($component);
  $('#content .page-header').before($container);
};

NYUDateManager.prototype.initVue = function() {
  new Vue({ el: '.vue-enabled' });
};