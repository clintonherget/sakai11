Vue.component('react-post', {
  template: `
<div :class="css_classes" :data-post-uuid="post.uuid">
  <span v-if="post.unread" class="badge badge-primary">NEW</span>
  <template v-if="post.editable && !editing">
    <a href="javascript:void(0)" class="edit pull-right" title="Edit Post" @click="edit()">
      <i class="fa fa-pencil" aria-hidden="true"></i>
    </a>
  </template>
  <template v-if="editing">
    <div class="conversations-post-content">
      <post-editor ref="postEditor" :existing_attachments="post.attachments" :baseurl="baseurl">
        <template v-slot:content><div v-html="post.content"></div></template>
        <template v-slot:actions>
          <a class="button" @click="cancelEdit()">Cancel</a>
        </template>
      </post-editor>
    </div>
  </template>
  <template v-else>
    <template v-if="initialPost">
        <div class="conversations-post-content">
          <h2>{{topic_title}}</h2>
          <p>
            <small class="text-muted">
              Created by {{post.postedByDisplayName}} on {{formatEpochTime(post.postedAt)}}
              <template v-if="post.version > 1"> <em>Edited</em></template>
            </small>
          </p>
          <span v-html="post.content"></span>
          <ul class="conversations-attachment-list">
            <li v-for="a in post.attachments">
              <i class="fa" v-bind:class='$parent.iconForMimeType(a.mimeType)'></i>&nbsp;<a :href='$parent.urlForAttachmentKey(a.key)'>{{a.fileName}}</a>
            </li>
          </ul>
        </div>
    </template>
    <template v-else>
      <small class="text-muted">
        <strong>{{post.postedByDisplayName}}</strong>&nbsp;&nbsp;&nbsp;{{formatEpochTime(post.postedAt)}}
        <template v-if="post.version > 1"> <em>Edited</em></template>
      </small>
      <div class="conversations-postedby-photo">
        <img :src="'/direct/profile/'+post.postedBy + '/image'"/>
      </div>
      <div class="conversations-post-content">
        <span v-html="post.content"></span>
        <div class="conversations-post-comments">
          <ul class="conversations-attachment-list">
            <li v-for="a in post.attachments">
              <i class="fa" v-bind:class='$parent.iconForMimeType(a.mimeType)'></i>&nbsp;<a :href='$parent.urlForAttachmentKey(a.key)'>{{a.fileName}}</a>
            </li>
          </ul>
          <template v-if="showCommentForm">
            <div class="conversations-comment-form">
              <textarea class="form-control" placeholder="Comment on post..." v-model="commentContent"></textarea>
              <button class="button" v-on:click="addComment()">Post Comment</button>
              <button class="button" v-on:click="toggleCommentForm()">Cancel</button>
            </div>
          </template>
          <template v-else>
              <button class="button" v-on:click="toggleCommentForm()">Comment</button>
          </template>
          <template v-if="post.comments && post.comments.length > 0">
            <div v-for="comment in post.comments" class="conversations-post-comment" :data-post-uuid="comment.uuid">
              <div class="conversations-postedby-photo">
                <img :src="'/direct/profile/'+comment.postedBy + '/image'"/>
              </div>
              <div>
                <span v-if="comment.unread" class="badge badge-primary">NEW</span>
                <small class="text-muted"><strong>{{comment.postedByDisplayName}}</strong>&nbsp;&nbsp;&nbsp;{{formatEpochTime(comment.postedAt)}}</small>
              </div>
              <div class="conversations-comment-content">
                {{comment.content}}
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>
  </template>
</div>
`,
  data: function () {
    return {
      showCommentForm: false,
      commentContent: '',
      initialPost: this.initial_post == 'true',
      editing: false,
    }
  },
  props: ['post', 'initial_post'],
  computed: {
    baseurl: function() {
      return this.$parent.baseurl;
    },
    topic_uuid: function() {
      return this.$parent.topic_uuid;
    },
    topic_title: function() {
      return this.$parent.topic_title;
    },
    css_classes: function() {
      var classes = ['conversations-post'];
      if (this.initialPost) {
        classes.push('conversations-initial-post');
      }
      if (this.post.unread) {
          classes.push('unread');
      }
      return classes.join(' ');
    },
  },
  methods: {
    addComment: function() {
      if (this.commentContent.trim() == "") {
          this.commentContent = "";
        return;
      }

      $.ajax({
        url: this.baseurl+"create-post",
        type: 'post',
        data: { topicUuid: this.topic_uuid, content: this.commentContent, post_uuid: this.post.uuid },
        dataType: 'json',
        success: (json) => {
          this.commentContent = "";
          this.showCommentForm = false;
          this.$parent.postToFocusAndHighlight = json.uuid;
          this.$parent.refreshPosts();
        }
      });
    },
    formatEpochTime: function(epoch) {
      return new Date(epoch).toLocaleString();
    },
    toggleCommentForm: function() {
        if (this.showCommentForm) {
            this.showCommentForm = false;
            this.commentContent = "";
        } else {
            this.showCommentForm = true;
        }
    },
    savePost: function(content, attachments) {
      $.ajax({
        url: this.baseurl+"update-post",
        type: 'post',
        data: {
          topic_uuid: this.topic_uuid,
          post_uuid: this.post.uuid,
          content: content,
          attachmentKeys: attachments.map((attachment) => { return attachment.key }),
          version: this.post.version,
        },
        dataType: 'json',
        success: (json) => {
          this.$refs.postEditor.clearEditor();
          this.$parent.postToFocusAndHighlight = json.uuid;
          this.$parent.refreshPosts();
          this.cancelEdit();
        }
      });
    },
    edit: function() {
      this.editing = true;
    },
    cancelEdit: function() {
      this.editing = false;
    }
  },
  mounted: function() {
  }
});


Vue.component('react-topic', {
  template: `
  <div class="conversations-topic react">
    <div class="conversations-topic-main">
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
            <button class="button" v-on:click="markTopicRead(true)">Mark all as read</button>
          </template>
        </post-editor>
        <div class="conversations-posts">
          <template v-for="post in posts">
            <template v-if="post.isFirstUnreadPost">
              <div class="conversations-posts-unread-line">
                <span class="badge badge-primary">NEW</span>
              </div>
            </template>
            <react-post :topic_uuid="topic_uuid" :post="post" :baseurl="baseurl"></react-post>
          </template>
        </div>
    </div>
    <div class="conversations-topic-sidebar">
        <timeline :initialPost="initialPost" :posts="posts"></timeline>
    </div>
  </div>
`,
  data: function () {
    return {
      posts: [],
      activeUploads: 0,
      initialPost: null,
      firstUnreadPost: null,
      postToFocusAndHighlight: null,
    }
  },
  props: ['baseurl',
          'topic_uuid',
          'topic_title',
          'current_user_id',
          'current_user_role'],
  methods: {
    refreshPosts: function(opts) {
      if (!opts) { opts = {}; }
      this.firstUnreadPost = null;

      $.ajax({
        url: this.baseurl+"feed/posts",
        type: 'get',
        data: { topicUuid: this.topic_uuid },
        dataType: 'json',
        success: (json) => {
          if (json.length > 0) {
            this.initialPost = json.shift();
            this.posts = opts.fullRefresh ? json : this.mergePosts(json, this.posts);

            // FIXME IE support?
            var firstUnreadPost = this.posts.find(function(post) {
              return post.unread;
            });
            if (firstUnreadPost) {
                firstUnreadPost.isFirstUnreadPost = true;
            }

          } else {
            this.initialPost = null;
            this.posts = [];
          }
        }
      });
    },
    mergePosts: function(newPosts, origPosts) {
      // We want to preserve the unread statuses that were displayed at the point the page loaded.
      var unreadStatuses = {};
      for (const post of origPosts) {
        unreadStatuses[post.uuid] = post.unread
      }

      for (var post of newPosts) {
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
        url: this.baseurl+"mark-topic-read",
        type: 'post',
        data: { topicUuid: this.topic_uuid },
        dataType: 'json',
        success: (json) => {
          if (reloadPosts) {
            this.refreshPosts({fullRefresh: true});
          }
        }
      });
    },
    resetMarkTopicReadEvents: function() {
      var autoMarkedAsRead = false;
      var markAsRead = () => {
        autoMarkedAsRead = true;
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
      var $post = $(this.$el).find('[data-post-uuid='+postUuid+']');
      if ($post.length > 0) {
        $post[0].scrollIntoView({
          behavior: 'smooth',
          block: 'center'
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
    iconForMimeType: function (mimeType) {
      return this.$refs.postEditor.iconForMimeType(mimeType);
    },
    urlForAttachmentKey: function (key) {
      return this.$refs.postEditor.urlForAttachmentKey(key);
    },
    savePost: function(content, attachments) {
      $.ajax({
        url: this.baseurl+"create-post",
        type: 'post',
        data: {
          topicUuid: this.topic_uuid,
          content: content,
          attachmentKeys: attachments.map((attachment) => { return attachment.key }),
        },
        dataType: 'json',
        success: (json) => {
          this.$refs.postEditor.clearEditor();
          this.postToFocusAndHighlight = json.uuid;
          this.refreshPosts();
        }
      });
    }
  },
  mounted: function() {
    this.refreshPosts();
    setInterval(() => {
      this.refreshPosts();
    }, 5*1000);
    this.resetMarkTopicReadEvents();
  },
  updated: function() {
    // If we added a new rich text area, enrich it!
    this.$nextTick(function () {
      if (this.postToFocusAndHighlight) {
        if (this.focusAndHighlightPost(this.postToFocusAndHighlight)) {
          this.postToFocusAndHighlight = null;
        }
      }
    });
  }
});


Vue.component('post-editor', {
  template: `
<div class="conversations-post-form">
  <slot name="author"></slot>
  <div class="post-to-topic-textarea form-control">
    <div class="stretchy-editor" v-bind:class='{ "full-editor-height": editorFocused }'>
      <div class="topic-ckeditor"><slot name="content"></slot></div>
    </div>
    <div>
      <hr>
      <button v-on:click="newAttachment()" class="conversations-minimal-btn">
        <i class="fa fa-paperclip"></i>&nbsp;Add attachment
      </button>
      <ul class="conversations-attachment-list">
        <li v-for="a in attachments">
          <i class="fa" v-bind:class='a.icon'></i>&nbsp;<a :href='a.url'>{{a.name}}</a>
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
  data: function () {
    var mimeToIconMap = {
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

    var existingAttachments = [];

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
    }
  },
  computed: {
    topic_uuid: function() {
      return this.$parent.topic_uuid;
    }
  },
  props: ['existing_attachments', 'baseurl'],
  methods: {
    newAttachment: function() {},
    initRichTextareas: function() {
      $(this.$el).find('.topic-ckeditor').each((idx, elt) => {
        RichText.initialize({
          baseurl: this.baseurl,
          elt: elt,
          placeholder: 'React to the post...',
          onCreate: (newEditor) => { this.editor = newEditor; },
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
          }
        });
      });
    },
    iconForMimeType: function (mimeType) {
      return this.mimeToIcon[mimeType] || 'fa-file';
    },
    urlForAttachmentKey: function (key) {
      return this.baseurl + "file-view?mode=view&key=" + key;
    },
    newAttachment: function () {
      var self = this;
      var fileInput = $('<input type="file" style="display: none;"></input>');

      $(this.$el).append(fileInput);

      fileInput.click();

      fileInput.on('change', function () {
        var file = fileInput[0].files[0];
        var formData = new FormData();
        formData.append('file', file);
        formData.append('mode', 'attachment');

        self.activeUploads += 1;

        $.ajax({
          url: self.baseurl + "file-upload",
          type: "POST",
          contentType: false,
          cache: false,
          processData: false,
          data: formData,
          dataType: 'json',
          success: function (response) {
            self.attachments.push({
              name: file.name,
              icon: self.iconForMimeType(file.type),
              key: response.key,
              url: self.urlForAttachmentKey(response.key),
            });
          },
          error: function (xhr, statusText) {},
          complete: function () {
            self.activeUploads -= 1;
          }
        });

      });
    },
    clearEditor: function () {
      if (this.editor) {
        this.attachments = []
        this.editor.setData("");
        this.editorFocused = false;
      }
    },
    newPostContent: function () {
      if (this.editor) {
        return this.editor.getData();
      } else {
        return "";
      }
    },
    savePost: function() {
      var content = this.newPostContent().trim();

      if (content === "") {
        if (this.attachments.length === 0) {
          this.clearEditor();
          return;
        } else {
          // Blank content is OK if we have attachments.  Store a placeholder.
          content = "&nbsp;";
        }
      }

      this.$parent.savePost(content, this.attachments);
    },
  },
  mounted: function() {
    this.initRichTextareas();
  },
  updated: function() {}
});
