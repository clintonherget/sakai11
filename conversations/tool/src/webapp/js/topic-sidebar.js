Vue.component('topic-sidebar', {
  template: `
<div :class="sidebarCss">
    <ul>
        <li>
            <a href="javascript:void(0);" @click="toggleInfo()" :class="linkCSS('info')">
                <i class="fa fa-info-circle"></i>
                <span class="sr-only">Show Topic Info</span>
            </a>
        </li>
        <li v-if="isInstructor">
            <a href="javascript:void(0);" @click="toggleGrading()" :class="linkCSS('grading')">
                <i class="fa fa-star" style="color: green"></i>
                <span class="sr-only">Show Grading Info</span>
            </a>
        </li>
        <li v-if="isInstructor">
            <a href="javascript:void(0);" @click="toggleModeration()" :class="linkCSS('moderation')">
                <i class="fa fa-user" style="color: orange"></i>
                <span class="sr-only">Show Moderation Info</span>
            </a>
        </li>
    </ul>
    <div class="conversations-topic-sidebar-panel">
        <a href="javascript:void(0)" @click="toggle()" class="pull-right"><i class="fa fa-times"></i></a>
        FIXME Show {{this.panel}}
    </div>
</div>
`,
  data: function() {
    return {
        expanded: false,
        panel: undefined,
    };
  },
  props: ['current_user_role', 'topic_uuid'],
  computed: {
      sidebarCss: function() {
          if (this.expanded) {
              return 'conversations-topic-sidebar expanded';
          } else {
              return 'conversations-topic-sidebar collapsed';
          }
      },
      isInstructor: function() {
          console.log(this.current_user_role);
          return this.current_user_role === 'instructor';
      }
  },
  methods: {
      toggle: function() {
          this.expanded = !this.expanded;
          if (!this.expanded) {
              this.panel = undefined;
          }
      },
      toggleInfo: function() {
          if (this.panel === undefined) {
            this.toggle();
            this.panel = 'info';
          } else if (this.panel === 'info') {
            this.toggle();
          } else {
              this.panel = 'info';
          }
      },
      toggleGrading: function() {
          if (this.panel === undefined) {
            this.toggle();
            this.panel = 'grading';
          } else if (this.panel === 'grading') {
            this.toggle();
          } else {
              this.panel = 'grading';
          }
      },
      toggleModeration: function() {
          if (this.panel === undefined) {
            this.toggle();
            this.panel = 'moderation';
          } else if (this.panel === 'moderation') {
            this.toggle();
          } else {
              this.panel = 'moderation';
          }
      },
      linkCSS: function(linkPanel) {
          if (this.expanded) {
              if (this.panel === linkPanel) {
                  return 'active';
              }
          }

          return '';
      }
  },
  mounted: function() {
  },
});
