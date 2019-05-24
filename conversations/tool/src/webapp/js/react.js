Vue.component('react-post', {
  template: `
<div :class="postCssClasses(post)" :data-post-uuid="post.uuid">
  <div class="conversations-postedby-photo">
    <img :src="'/direct/profile/'+post.postedBy + '/image'"/>
  </div>
  <span v-if="post.unread" class="badge badge-primary">NEW</span>
  <small class="text-muted"><strong>{{post.postedByDisplayName}}</strong>&nbsp;&nbsp;&nbsp;{{formatEpochTime(post.postedAt)}}</small>
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
      <template v-if="post.comments.length > 0">
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
</div>
`,
  data: function () {
    return {
      showCommentForm: false,
      commentContent: '',
    }
  },
  props: ['post', 'baseurl', 'topic_uuid'],
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
    postCssClasses: function(post) {
      return this.$parent.postCssClasses(post);
    },
    toggleCommentForm: function() {
        if (this.showCommentForm) {
            this.showCommentForm = false;
            this.commentContent = "";
        } else {
            this.showCommentForm = true;
        }
    },
  },
  mounted: function() {
  }
});


Vue.component('react-topic', {
  template: `
  <div class="conversations-topic react">
    <div class="conversations-topic-main">
        <template v-if="initialPost">
          <div class="conversations-post conversations-initial-post" :data-post-uuid="initialPost.uuid">
            <span v-if="initialPost.unread" class="badge badge-primary">NEW</span>
            <div class="conversations-post-content">
              <h2>{{topic_title}}</h2>
              <p>
                <small class="text-muted">Created by {{initialPost.postedByDisplayName}} on {{formatEpochTime(initialPost.postedAt)}}</small>
              </p>
              <span v-html="initialPost.content"></span>
            </div>
          </div>
        </template>
        <div class="conversations-post-form">
          <div class="conversations-postedby-photo">
            <img :src="'/direct/profile/'+ current_user_id + '/image'"/>
          </div>
          <div class="post-to-topic-textarea form-control">
            <div class="stretchy-editor" v-bind:class='{ "full-editor-height": editorFocused }'>
              <div class="topic-ckeditor"></div>
            </div>
            <div>
              <hr>
              <button v-on:click="newAttachment()"
                      class="conversations-minimal-btn">
                <i class="fa fa-paperclip"></i>&nbsp;Add attachment
              </button>
              <ul class="conversations-attachment-list">
                <li v-for="a in attachments">
                  <i class="fa" v-bind:class='a.icon'></i>&nbsp;<a :href='a.url'>{{a.name}}</a>
                </li>
              </ul>
            </div>
          </div>
          <template v-if="postAllowed">
            <button class="button" v-on:click="post()">Post</button>
          </template>
          <template v-else>
            <button class="button" disabled>Uploading...</button>
          </template>
          <button class="button" v-on:click="markTopicRead(true)">Mark all as read</button>
        </div>
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
      postAllowed: true,
      initialPost: null,
      firstUnreadPost: null,
      editor: null,
      postToFocusAndHighlight: null,
      editorFocused: false,
      attachments: [],
      mimeToIcon: {
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
      },
    }
  },
  props: ['baseurl', 'topic_uuid', 'topic_title', 'current_user_id'],
  methods: {
    post: function() {
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

      $.ajax({
        url: this.baseurl+"create-post",
        type: 'post',
        data: {
          topicUuid: this.topic_uuid,
          content: content,
          attachmentKeys: this.attachments.map((attachment) => { return attachment.key }),
        },
        dataType: 'json',
        success: (json) => {
          this.clearEditor();
          this.postToFocusAndHighlight = json.uuid;
          this.refreshPosts();
        }
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

        // Disable the post button while we upload.
        self.postAllowed = false;

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
            self.postAllowed = true;
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
    postCssClasses: function(post) {
        var classes = ['conversations-post'];
        if (post.unread) {
            classes.push('unread');
        }
        return classes.join(' ');
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
    initRichTextareas: function() {
      $(this.$el).find('.topic-ckeditor').each((idx, elt) => {
        RichText.initialize({
          baseurl: this.baseurl,
          elt: elt,
          placeholder: 'Post to topic...',
          onCreate: (newEditor) => { this.editor = newEditor; },
          onFocus: (event, name, isFocused) => {
            if (isFocused) {
              this.editorFocused = isFocused;
            } else {
              if (this.editor.getData() === '') {
                console.log(this.editor.getData());
                this.editorFocused = false;
              }
            }
          }
        });
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
      }
    },
  },
  mounted: function() {
    this.refreshPosts();
    setInterval(() => {
      this.refreshPosts();
    }, 5*1000);
    this.resetMarkTopicReadEvents();
    this.initRichTextareas();
  },
  updated: function() {
    // If we added a new rich text area, enrich it!
    this.$nextTick(function () {
      if (this.postToFocusAndHighlight) {
        this.focusAndHighlightPost(this.postToFocusAndHighlight);
        this.postToFocusAndHighlight = null;
      }
    });
  }
});
