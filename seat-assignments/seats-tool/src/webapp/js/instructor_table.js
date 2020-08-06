Vue.component('modal', {
  template: `
<div class="modal" tabindex="-1" role="dialog" ref="modal">
  <div class="modal-dialog" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <div class="modal-title" style="float: left">
          <slot name="header"></slot>
        </div>
        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
      <div class="modal-body">
        <slot name="body"></slot>
      </div>
      <div class="modal-footer">
        <slot name="footer"></slot>
      </div>
    </div>
  </div>
</div>
  `,
  props: [],
  methods: {
    open: function() {
      $(this.$refs.modal).modal('show');
    },
    close: function() {
      $(this.$refs.modal).modal('hide');
    }
  },
  mounted: function() {
    var self = this;

    $(document).ready(function() {
      $(self.$refs.modal).modal({
        show: false,
      });
    });
  }
});

Vue.component('seat-assignment-widget', {
  template: `
    <span>
      <template v-if="editing || seatValue">
        <label :for="inputId" class="sr-only">
          Seat assignment for {{assignment.netid}}
        </label>
        <input
          :id="inputId"
          type="text" 
          v-model="inputValue"
          ref="input"
          :class="'form-control ' + inputCSSClasses"
          :readonly="!editing"
          v-on:keydown.enter="editOrSave()"
          v-on:keydown.esc="cancel()"
        />
        <template v-if="editing">
          <button class="btn-primary" @click="save()">
            <i class="glyphicon glyphicon-ok" aria-hidden="true"></i> Save
          </button>
          <button @click="cancel()">
            <i class="glyphicon glyphicon-ban-circle" aria-hidden="true"></i> Cancel
          </button>
        </template>
        <template v-else>
          <button @click="edit()">
            <i class="glyphicon glyphicon-pencil" aria-hidden="true"></i> Edit
          </button>
          <button v-show="seatValue !== null" @click="clear()">
            <i class="glyphicon glyphicon-remove" aria-hidden="true"></i> Clear
          </button>
        </template>
      </template>
      <template v-else>
        <button @click="edit()">
          <i class="glyphicon glyphicon-plus" aria-hidden="true"></i> Seat Seat Assignment
        </button>
      </template>
    </span>
  `,
  data: function() {
    return {
      hasError: false,
      seatValue: null,
      editing: false,
      inputValue: '',
    };
  },
  props: ['assignment', 'meeting', 'group', 'section'],
  computed: {
      inputId: function() {
          return [this.meeting.id, this.assignment.netid].join('__');
      },
      inputCSSClasses: function() {
        if (this.hasError) {
          return "has-error";
        } else {
          return ""
        }
      },
      baseurl: function() {
          return this.$parent.baseurl;
      },
      cleanSeatValue: function() {
        if (this.assignment.seat == null) {
              return '';
          } else {
              return '' + this.assignment.seat;
          }
      }
  },
  watch: {
    assignment: function(a, b) {
      if (!this.editing) {
        this.resetSeatValue();
        if (a.seat !== b.seat) {
          // FIXME notify user of change
        }
      }
    }
  },
  methods: {
    resetSeatValue: function() {
      this.seatValue = this.assignment.seat;
      this.inputValue = this.cleanSeatValue;
    },
    cancel: function() {
      this.hasError = false;
      this.editing = false;
      this.resetSeatValue();
      this.focusInput();
    },
    editOrSave: function() {
      if (this.editing) {
        this.save();
      } else {
        this.edit()
      }
    },
    clear: function() {
      this.inputValue = '';
      this.save();
    },
    edit: function() {
      this.editing = true;
      this.selectInput();
    },
    focusInput: function() {
      var self = this;
      self.$nextTick(function() {
        self.$refs.input.focus();
      });
    },
    selectInput: function() {
      var self = this;
      self.$nextTick(function() {
        self.$refs.input.select();
      });
    },
    markHasError: function() {
      this.hasError = true;
    },
    clearHasError: function() {
      this.hasError = false;
    },
    save: function() {
      var self = this;
      self.clearHasError();

      if (self.seatValue === (self.inputValue === '' ? null : self.inputValue)) {
        // noop
        self.editing = false;
        return;
      }

      $.ajax({
        url: this.baseurl + "/seat-assignment",
        type: 'post',
        dataType: 'json',
        data: {
          sectionId: self.section.id,
          groupId: self.group.id,
          meetingId: self.meeting.id,
          netid: self.assignment.netid,
          seat: self.inputValue,
          currentSeat: self.seatValue,
        },
        success: function(json) {
          if (json.error) {
            self.markHasError();
            self.selectInput();
            // FIXME notify user of error
          } else {
            self.editing = false;
            self.seatValue = self.inputValue === '' ? null : self.inputValue;
            self.focusInput();
            // FIXME notify user of save success
          }
        }
      })
    }
  },
  mounted: function() {
    this.seatValue = this.assignment.seat;
    this.inputValue = this.cleanSeatValue;
  },
});

Vue.component('split-action', {
  template: `
<div>
  <button @click="openModal()">Split</button>
  <modal ref="splitModal">
    <template v-slot:header>Split Section into Groups</template>
    <template v-slot:body>
      <div>
        <label for="numberofgroups">Number of Groups</label>
        <select id="numberofgroups" v-model="numberOfGroups">
          <option>1</option>
          <option>2</option>
          <option>3</option>
          <option>4</option>
        </select>
      </div>
      <div>
        <label for="selectionType">Selection Type</label>
        <select id="selectionType" v-model="selectionType">
          <option>RANDOM</option>
          <option>WEIGHTED</option>
        </select>
      </div>
    </template>
    <template v-slot:footer>
      <button @click="performSplit()" class="pull-left primary">Perform Split</button>
      <button @click="closeModal()">Cancel</button>
    </template>
  </modal>
</div>
`,
  props: ['section'],
  data: function() {
    return {
      numberOfGroups: 1,
      selectionType: 'RANDOM',
    }
  },
  computed: {
      baseurl: function() {
          return this.$parent.baseurl;
      },
  },
  methods: {
    openModal: function() {
      this.$refs.splitModal.open();
    },
    closeModal: function() {
      this.$refs.splitModal.close();
    },
    performSplit: function() {
      var self = this;

      $.ajax({
        url: this.baseurl + "/split-section",
        type: 'post',
        data: {
          sectionId: this.section.id,
          numberOfGroups: this.numberOfGroups,
          selectionType: this.selectionType,
        },
        success: function() {
          self.$emit('splat');
          self.closeModal();
        }
      })
    }
  }
});

Vue.component('group-meeting', {
  template: `
<div>
  <h3>{{meeting.name}} ({{meeting.id}})</h3>
  <table class="seat-assignment-listing">
    <thead>
      <tr>
        <th>Picture</th>
        <th>Name</th>
        <th>Seat Assignment</th>
        <th>Modality</th>
        <th>Section Subgroup</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="assignment in sortedSeatAssignments">
        <td>
          <div class="profile-pic">
            <img :src="'/direct/profile/' + assignment.netid + '/image'"/>
          </div>
        </td>
        <td>TODO_NAME ({{assignment.netid}})</td>
        <td>
          <seat-assignment-widget :assignment="assignment" :meeting="meeting" :group="group" :section="section">
          </seat-assignment-widget>
        </td>
        <td>
          TODO_MODALITY 
          <template v-if="!assignment.official">(Unofficial)</template>
        </td>
        <td>
          TODO
        </td>
      </tr>
    </tbody>
  </table>
</div>
`,
  props: ['section', 'group', 'meeting'],
  computed: {
    baseurl: function() {
        return this.$parent.baseurl;
    },
    sortedSeatAssignments: function() {
      return this.meeting.seatAssignments.sort(function(a, b) {
        return (a.netid < b.netid) ? -1 : 1;
      })
    },
  },
});

Vue.component('section-group', {
  template: `
<div>
  <h2>{{group.name}} ({{group.id}})</h2>
  <template v-for="meeting in group.meetings">
    <group-meeting :group="group" :section="section" :meeting="meeting"></group-meeting>
  </template>
</div>`,
  props: ['section', 'group'],
  computed: {
    baseurl: function() {
        return this.$parent.baseurl;
    },
  },
});

Vue.component('section-table', {
  template: `
    <div>
      <template v-if="section">
          <h1>{{section.id}}</h1>
          <split-action v-show="!section.split" :section="section" v-on:splat="resetPolling()">
          </split-action>
          <template v-for="group in sortedGroups">
            <section-group :group="group" :section="section"></section-group>
          </template>
      </template>
    </div>
  `,

  data: function() {
    return {
        section: null,
        pollInterval: null,
    };
  },
  props: ['sectionId'],
  watch: {
    sectionId: function(a, b) {
      if (a != b) {
        this.resetPolling();
      }
    },
  },
  beforeDestroy: function() {
    this.cancelPolling();
  },
  computed: {
      baseurl: function() {
          return this.$parent.baseurl;
      },
      sortedGroups: function() {
        if (this.section == null) {
          return [];
        } else {
          return this.section.groups.sort(function(a, b) {
            return (a.name < b.name) ? -1 : 1;
          })
        }
      },
  },
  methods: {
      fetchData: function() {
          var self = this;

          $.ajax({
              url: self.baseurl + 'section',
              data: {
                  sectionId: self.sectionId,
              },
              type: 'get',
              dataType: 'json',
              success: function (json) {
                  self.section = json;
              }
          });
      },
      cancelPolling: function() {
        var self = this;
        if (self.pollInterval) {
          clearInterval(self.pollInterval);
        }
        
      },
      resetPolling: function() {
        var self = this;

        self.cancelPolling();

        self.fetchData();

        self.pollInterval = setInterval(function() {
            self.fetchData();
        }, 5000);
      }
  },
  mounted: function() {
    this.resetPolling();
  }
});


Vue.component('section-selector', {
  template: `
<div>
  <select v-model="selectedSectionId">
    <option value="">Select a section...</option>
    <option v-for="section in sections" :value="section.id">
      {{section.id}}
    </option>
  </select>
</div>
`,
  props: ['sections'],
  data: function() {
    return {
      selectedSectionId: '',
    };
  },
  watch: {
    selectedSectionId: function() {
      this.$emit('selectSection', this.selectedSectionId == '' ? null : this.selectedSectionId);
    },
  },
  methods: {
  },
});


Vue.component('instructor-table', {
  template: `
      <div>
          <template v-if="sections.length > 0">
              <section-selector
                  ref="sectionSelector"
                  :sections="sections"
                  v-on:selectSection="handleSectionSelect">
              </section-selector>
              <hr />
              <template v-if="selectedSectionId">
                  <section-table :sectionId="selectedSectionId"></section-table>
              </template>
              <template v-else>
                <p>No section selected.</p>
              </template>
          </template>
          <template v-else-if="fetched">
            <p>This tool is yet to be provisioned. Please try again in a moment.</p>
          </template>
      </div>

  `,
  data: function() {
    return {
        sections: [],
        selectedSectionId: null,
        fetched: false,
    };
  },
  props: ['baseurl'],
  methods: {
      fetchData: function() {
          var self = this;

          $.ajax({
              url: self.baseurl + 'sections',
              type: 'get',
              dataType: 'json',
              success: function (json) {
                  self.fetched = true;
                  self.sections = json;
              }
          });
      },
      handleSectionSelect: function(sectionId) {
        this.selectedSectionId = sectionId;
      },
  },
  mounted: function() {
      this.fetchData();
  },
});


Vue.component('react-topic', {
  template: `
  <div class="conversations-topic react">
    <div class="conversations-topic-main" ref="main">
        <template v-if="initialPost">
          <react-post :post="initialPost" initial_post="true"></react-post>
        </template>
        <post-editor ref="postEditor" :baseurl="baseurl">
          <template v-slot:author>
            <div class="conversations-postedby-photo">
              <img :src="'/direct/profile/'+ current_user_id + '/image'"/>
            </div>
          </template>
          <template v-slot:actions>
            <button class="button" v-on:click="markTopicRead(true)">
              Mark all as read
            </button>
          </template>
        </post-editor>
        <div class="conversations-posts">
          <template v-for="post in posts">
            <template v-if="post.isFirstUnreadPost">
              <div class="conversations-posts-unread-line">
                <span class="badge badge-primary">NEW</span>
              </div>
            </template>
            <react-post :topic_uuid="topic_uuid" :post="post"
                :baseurl="baseurl">
            </react-post>
          </template>
        </div>
    </div>
    <div class="conversations-topic-right">
        <template v-if="popupTimeline">
            <div :class="this.popupTimelinePopped ? 'conversations-timeline-toggle expanded' : 'conversations-timeline-toggle collapsed'" ref="timelineToggle">
              <a href="#" @click="togglePopupTimeline()">
                {{timelineDisplayString()}}
              </a>
              <div class="conversations-timeline-toggle-container">
                  <timeline :initialPost="initialPost" :posts="posts" ref="timeline"></timeline>
              </div>
            </div>
        </template>
        <template v-else>
            <timeline :initialPost="initialPost" :posts="posts"></timeline>
        </template>
        <topic-sidebar :current_user_role="current_user_role" :topic="topic" :posts="posts" :initial_post="initialPost"></topic_sidebar>
    </div>
  </div>
`,
  data: function() {
    return {
      posts: [],
      activeUploads: 0,
      initialPost: null,
      firstUnreadPost: null,
      postToFocusAndHighlight: null,
      topic: JSON.parse(this.topic_json),
      popupTimeline: false,
      popupTimelinePopped: false,
    };
  },
  props: [
    'baseurl',
    'topic_uuid',
    'topic_json',
    'settings_json',
    'current_user_id',
    'current_user_role'],
  methods: {
    refreshPosts: function(opts) {
      if (!opts) {
        opts = {};
      }

      this.firstUnreadPost = null;

      $.ajax({
        url: this.baseurl+'feed/posts',
        type: 'get',
        data: {topicUuid: this.topic_uuid},
        dataType: 'json',
        success: (json) => {
          if (json.length > 0) {
            this.initialPost = json.shift();
            this.posts = opts.fullRefresh ?
                json : this.mergePosts(json, this.posts);

            // FIXME IE support?
            const firstUnreadPost = this.posts.find(function(post) {
              return post.unread;
            });
            if (firstUnreadPost) {
              firstUnreadPost.isFirstUnreadPost = true;
            }
          } else {
            this.initialPost = null;
            this.posts = [];
          }
        },
      });
    },
    mergePosts: function(newPosts, origPosts) {
      // We want to preserve the unread statuses that were displayed at the
      // point the page loaded.
      const unreadStatuses = {};
      for (const post of origPosts) {
        unreadStatuses[post.uuid] = post.unread;
      }

      for (const post of newPosts) {
        if (unreadStatuses[post.uuid]) {
          post.unread = true;
        }
      }

      return newPosts;
    },
    formatEpochTime: function(epoch) {
      return new Date(epoch).toLocaleString();
    },
    markTopicRead: function(reloadPosts) {
      $.ajax({
        url: this.baseurl+'mark-topic-read',
        type: 'post',
        data: {topicUuid: this.topic_uuid},
        dataType: 'json',
        success: (json) => {
          if (reloadPosts) {
            this.refreshPosts({fullRefresh: true});
          }
        },
      });
    },
    resetMarkTopicReadEvents: function() {
      const markAsRead = () => {
        this.markTopicRead(false);
      };

      // If we're visible right now, mark as read immediately
      if (!document.hidden) {
        setTimeout(markAsRead, 0);
      }

      // Mark as read when the page unloads
      $(window).off('unload').on('unload', markAsRead);

      // Or when the tab becomes visible
      $(document).on('visibilitychange', () => {
        if (!document.hidden) {
          markAsRead();
        }
      });
    },
    focusAndHighlightPost: function(postUuid) {
      const $post = $(this.$el).find('[data-post-uuid='+postUuid+']');
      if ($post.length > 0) {
        $post[0].scrollIntoView({
          behavior: 'smooth',
          block: 'center',
        });
        $post.addClass('conversations-post-highlight');
        setTimeout(() => {
          $post.removeClass('conversations-post-highlight');
        }, 1000);
        return true;
      } else {
        return false;
      }
    },
    iconForMimeType: function(mimeType) {
      return this.$refs.postEditor.iconForMimeType(mimeType);
    },
    urlForAttachmentKey: function(key) {
      return this.$refs.postEditor.urlForAttachmentKey(key);
    },
    savePost: function(content, attachments) {
      $.ajax({
        url: this.baseurl+'create-post',
        type: 'post',
        data: {
          topicUuid: this.topic_uuid,
          content: content,
          attachmentKeys: attachments.map((attachment) => {
            return attachment.key;
          }),
        },
        dataType: 'json',
        success: (json) => {
          this.$refs.postEditor.clearEditor();
          this.postToFocusAndHighlight = json.uuid;
          this.refreshPosts();
        },
      });
    },
    handleResize: function() {
      if ($(window).width() < 1280) {
        if (this.popupTimeline === false) {
          this.popupTimelinePopped = false;
        }
        this.popupTimeline = true;
        this.$nextTick(() => {
          var collapsedHeight = $(this.$refs.timelineToggle).find('> a').outerHeight();
          $(this.$refs.timelineToggle).height(collapsedHeight);
        });
      } else {
        if (this.popupTimeline === true) {
          this.popupTimelinePopped = false;
        }
        this.popupTimeline = false;
      }
    },
    timelineDisplayString: function() {
      if (this.$refs.timeline) {
        return  this.$refs.timeline.popupDisplayString || '...' ;
      } else {
        return '...';
      }
    },
    togglePopupTimeline: function() {
      this.popupTimelinePopped = !this.popupTimelinePopped;
      if (this.popupTimelinePopped) {
        var expandedHeight = $(this.$refs.timelineToggle).find('.conversations-timeline').height() + $(this.$refs.timelineToggle).find('> a').outerHeight();
        $(this.$refs.timelineToggle).height(expandedHeight);
      } else {
        var collapsedHeight = $(this.$refs.timelineToggle).find('> a').outerHeight();
        $(this.$refs.timelineToggle).height(collapsedHeight);
      }
    },
  },
  computed: {
    settings: function() {
      return this.topic.settings;
    },
    topic_title: function() {
      return this.topic.title;
    },
    allowComments: function() {
        return this.settings.allow_comments;
    },
    allowLikes: function() {
        return this.settings.allow_like;
    },
  },
  mounted: function() {
    this.refreshPosts();
    setInterval(() => {
      this.refreshPosts();
    }, 5*1000);
    this.resetMarkTopicReadEvents();
    $(window).on('resize', () => {
      this.handleResize();
    });
    this.handleResize();
  },
  updated: function() {
    // If we added a new rich text area, enrich it!
    this.$nextTick(() => {
      if (this.postToFocusAndHighlight) {
        if (this.focusAndHighlightPost(this.postToFocusAndHighlight)) {
          this.postToFocusAndHighlight = null;
        }
      }
    });
  },
});


Vue.component('post-editor', {
  template: `
<div class="conversations-post-form">
  <slot name="author"></slot>
  <div class="post-to-topic-textarea form-control">
    <div class="stretchy-editor"
        v-bind:class='{ "full-editor-height": editorFocused }'>
      <div class="topic-ckeditor"><slot name="content"></slot></div>
    </div>
    <div>
      <hr>
      <button v-on:click="newAttachment()" class="conversations-minimal-btn">
        <i class="fa fa-paperclip"></i>&nbsp;Add attachment
      </button>
      <ul class="conversations-attachment-list">
        <li v-for="a in attachments">
          <i class="fa" v-bind:class='a.icon'></i>
          &nbsp;
          <a :href='a.url'>{{a.name}}</a>
        </li>
      </ul>
    </div>
  </div>
  <template v-if="activeUploads === 0">
    <button class="button" v-on:click="savePost()">Post</button>
  </template>
  <template v-else>
    <button class="button" disabled>Uploading...</button>
  </template>
  <slot name="actions"></slot>
</div>
`,
  data: function() {
    const mimeToIconMap = {
      'application/pdf': 'fa-file-pdf-o',
      'text/pdf': 'fa-file-pdf-o',

      'application/msword': 'fa-file-word-o',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'fa-file-word-o',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.template': 'fa-file-word-o',

      'application/vnd.ms-powerpoint': 'fa-file-powerpoint-o',
      'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'fa-file-powerpoint-o',
      'application/vnd.openxmlformats-officedocument.presentationml.template': 'fa-file-powerpoint-o',
      'application/vnd.openxmlformats-officedocument.presentationml.slideshow': 'fa-file-powerpoint-o',

      'application/vnd.ms-excel': 'fa-file-excel-o',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'fa-file-excel-o',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.template': 'fa-file-excel-o',

      'image/jpeg': 'fa-file-image-o',
      'image/png': 'fa-file-image-o',
      'image/gif': 'fa-file-image-o',
      'image/tiff': 'fa-file-image-o',
      'image/bmp': 'fa-file-image-o',

      'application/zip': 'fa-file-archive-o',
      'application/x-rar-compressed': 'fa-file-archive-o',

      'text/plain': 'fa-file-text-o',

      'video/mp4': 'fa-file-video-o',
      'video/x-flv': 'fa-file-video-o',
      'video/quicktime': 'fa-file-video-o',
      'video/mpeg': 'fa-file-video-o',
      'video/ogg': 'fa-file-video-o',

      'audio/mpeg': 'fa-file-audio-o',
      'audio/ogg': 'fa-file-audio-o',
      'audio/midi': 'fa-file-audio-o',
      'audio/flac': 'fa-file-audio-o',
      'audio/aac': 'fa-file-audio-o',
    };

    const existingAttachments = [];

    if (this.existing_attachments) {
      this.existing_attachments.forEach((attachment) => {
        existingAttachments.push({
          name: attachment.fileName,
          icon: mimeToIconMap[attachment.mimeType] || 'fa-file',
          key: attachment.key,
          url: this.urlForAttachmentKey(attachment.key),
        });
      });
    }

    return {
      editorFocused: false,
      attachments: existingAttachments,
      activeUploads: 0,
      editor: null,
      mimeToIcon: mimeToIconMap,
    };
  },
  computed: {
    topic_uuid: function() {
      return this.$parent.topic_uuid;
    },
  },
  props: ['existing_attachments', 'baseurl'],
  methods: {
    initRichTextareas: function() {
      $(this.$el).find('.topic-ckeditor').each((idx, elt) => {
        RichText.initialize({
          baseurl: this.baseurl,
          elt: elt,
          placeholder: 'React to the post...',
          onCreate: (newEditor) => {
            this.editor = newEditor;
          },
          onUploadEvent: (status) => {
            if (status === 'started') {
              this.activeUploads += 1;
            } else {
              this.activeUploads -= 1;
            }
          },
          onFocus: (event, name, isFocused) => {
            if (isFocused) {
              this.editorFocused = isFocused;
            } else {
              if (this.editor.getData() === '') {
                this.editorFocused = false;
              }
            }
          },
        });
      });
    },
    iconForMimeType: function(mimeType) {
      return this.mimeToIcon[mimeType] || 'fa-file';
    },
    urlForAttachmentKey: function(key) {
      return this.baseurl + 'file-view?mode=view&key=' + key;
    },
    newAttachment: function() {
      const self = this;
      const fileInput = $('<input type="file" style="display: none;"></input>');

      $(this.$el).append(fileInput);

      fileInput.click();

      fileInput.on('change', function() {
        const file = fileInput[0].files[0];
        const formData = new FormData();
        formData.append('file', file);
        formData.append('mode', 'attachment');

        self.activeUploads += 1;

        $.ajax({
          url: self.baseurl + 'file-upload',
          type: 'POST',
          contentType: false,
          cache: false,
          processData: false,
          data: formData,
          dataType: 'json',
          success: function(response) {
            self.attachments.push({
              name: file.name,
              icon: self.iconForMimeType(file.type),
              key: response.key,
              url: self.urlForAttachmentKey(response.key),
            });
          },
          error: function(xhr, statusText) {},
          complete: function() {
            self.activeUploads -= 1;
          },
        });
      });
    },
    clearEditor: function() {
      if (this.editor) {
        this.attachments = [];
        this.editor.setData('');
        this.editorFocused = false;
      }
    },
    newPostContent: function() {
      if (this.editor) {
        return this.editor.getData();
      } else {
        return '';
      }
    },
    savePost: function() {
      let content = this.newPostContent().trim();

      if (content === '') {
        if (this.attachments.length === 0) {
          this.clearEditor();
          return;
        } else {
          // Blank content is OK if we have attachments.  Store a placeholder.
          content = '&nbsp;';
        }
      }

      this.$parent.savePost(content, this.attachments);
    },
  },
  mounted: function() {
    this.initRichTextareas();
  },
  updated: function() {},
});
