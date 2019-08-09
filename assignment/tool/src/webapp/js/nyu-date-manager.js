Vue.component('date-manager', {
  template: `
<span>
  <a href="javascript:void(0);" @click="showManager()" class="button"><i class="fa fa-calendar"></i> Manage Assignment Dates <span class="badge">NEW</span></a>
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
      alert("DO ME");
    },
  },
  updated: function() {
  },
  mounted: function() {
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