Vue.component('create-topic-workflow', {
  template: `
  <div class="conversations-create-topic-workflow">
    <template v-if="step == 'SELECT_TYPE'">
      <div class="row">
        <div class="col-sm-4">
          <div class="well">
            <strong>React</strong>
            <p>Stuff goes here.</p>
            <button class="button" v-on:click="selectTopicType('react')">Select Topic Type</button>
          </div>
        </div>
        <div class="col-sm-4">
          <div class="well">
            <strong>Brainstorm</strong>
            <p>Stuff goes here.</p>
          </div>
        </div>
        <div class="col-sm-4">
          <div class="well">
            <strong>Discuss</strong>
            <p>Stuff goes here.</p>
          </div>
        </div>
      </div>
    </template> 
    <template v-else-if="step == 'SET_TITLE'">
      <input class="form-control" ref="topicTitleInput" placeholder="Topic title">
      <button class="button" v-on:click="selectTopicTitle()">Next</button>
    </template>
    <template v-else-if="step == 'CREATE_FIRST_POST'">
      <textarea class="form-control" ref="topicFirstPostElement" placeholder="Type the initial post..."></textarea>
      <button class="button" v-on:click="createTopic()">Create Post</button>
    </template>
  </div>
`,
  data: function () {
    return {
      step: 'SELECT_TYPE',
      topicType: null,
      topicTitle: null,
      topicFirstPost: null,
    };
  },
  props: ['baseurl'],
  methods: {
    selectTopicType: function(type) {
      this.topicType = type;
      this.step = 'SET_TITLE';
    },
    selectTopicTitle: function() {
      this.topicTitle = this.$refs.topicTitleInput.value;
      if (this.topicTitle != "") {
        this.step = 'CREATE_FIRST_POST';
      }
    },
    createTopic: function() {
      this.topicFirstPost = this.$refs.topicFirstPostElement.value;
      if (this.topicFirstPost != "") {
        $.ajax({
          url: this.baseurl + 'create-topic',
          method: 'post',
          data: {
            title: this.topicTitle,
            type: this.topicType,
            post: this.topicFirstPost,
          },
          success: function() {
            location.reload();
          }
        });
      }
    }
  },
  mounted: function() {
  }
});

Vue.component('create-topic-modal', {
  template: `
  <div class="conversations-create-topic-modal">
    <div class="modal" ref="dialog" role="dialog">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <span class="modal-title">Create Topic</span>
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <create-topic-workflow :baseurl="baseurl"></create-topic-workflow>
          </div>
        </div>
      </div>
    </div>
  </div>
`,
  data: function () {
    return {};
  },
  props: ['baseurl'],
  methods: {
    show: function() {
      var $dialog = $(this.$refs.dialog);
      $(this.$refs.dialog).modal();
      this.resize();
    },
    resize: function() {
      var $dialog = $(this.$refs.dialog);
      if ($dialog.find('.modal-dialog').is(':visible')) {
        $dialog.find('.modal-dialog').width('95%');
        $dialog.find('.modal-content').height($(window).height() - 70);
      }
    },
  },
  mounted: function() {
    $(window).resize(() => {
      this.resize();
    });

    var $dialog = $(this.$refs.dialog);
    $dialog.on('shown.bs.modal', function() {
      $(document.body).css('overflow', 'hidden');
    }).on('hidden.bs.modal', function() {
      $(document.body).css('overflow', '');
    });
  }
});

Vue.component('create-topic-wrapper', {
  template: `
  <div class="conversations-create-topic-wrapper">
    <button class="button" v-on:click="showModal()">Create Topic</button>
    <create-topic-modal ref="createTopicModal" :baseurl="baseurl"></create-topic-modal>
  </div>
`,
  data: function () {
    return {};
  },
  props: ['baseurl'],
  methods: {
    showModal: function() {
      this.$refs.createTopicModal.show();
    },
  },
  mounted: function() {
  }
});