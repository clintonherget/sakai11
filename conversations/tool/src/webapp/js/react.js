Vue.component('react-post', {
  template: `
<div class="conversations-post">
  <span v-if="post.unread" class="badge badge-primary">NEW</span>
  <small class="text-muted"><strong>{{post.postedByEid}}</strong>&nbsp;&nbsp;&nbsp;{{formatEpochTime(post.postedAt)}}</small>
  <div class="conversations-post-content">
    <span v-html="post.content"></span>
    <div class="conversations-post-comments">
      <template v-if="showCommentForm">
        <div class="conversations-comment-form">
          <textarea class="form-control" placeholder="Comment on post..." v-model="commentContent"></textarea>
          <button class="button" v-on:click="addComment()">Post Comment</button>
        </div>
      </template>
      <template v-else>
          <button class="button" v-on:click="showCommentForm = true">Comment</button>
      </template>
      <template v-if="post.comments.length > 0">
        <div v-for="comment in post.comments" class="conversations-post-comment">
          <div>
            <span v-if="comment.unread" class="badge badge-primary">NEW</span>
            <small class="text-muted"><strong>{{comment.postedByEid}}</strong>&nbsp;&nbsp;&nbsp;{{formatEpochTime(comment.postedAt)}}</small>
          </div>
          {{comment.content}}
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
          this.$parent.refreshPosts();
        }
      });
    },
    formatEpochTime: function(epoch) {
      return new Date(epoch).toLocaleString();
    }
  },
  mounted: function() {
  }
});


Vue.component('react-topic', {
  template: `
  <div class="conversations-topic react">
    <template v-if="initialPost">
      <div class="conversations-post conversations-initial-post">
        <span v-if="initialPost.unread" class="badge badge-primary">NEW</span>
        <div class="conversations-post-content">
          <h2>{{topic_title}}</h2>
          <p>
            <small class="text-muted">Created by {{initialPost.postedByEid}} on {{formatEpochTime(initialPost.postedAt)}}</small>
          </p>
          {{initialPost.content}}
        </div>
      </div>
    </template>
    <div class="conversations-post-form">
      <div class="post-to-topic-textarea form-control" placeholder="Post to topic..."></div>
      <button class="button" v-on:click="post()">Post</button>
      <button class="button" v-on:click="markTopicRead(true)">Mark all as read</button>
    </div>
    <div class="conversations-posts">
      <template v-for="post in posts">
        <template v-if="isFirstUnreadPost(post)">
          <div class="conversations-posts-unread-line">
            <span class="badge badge-primary">NEW</span>
          </div>
        </template>
        <div  :class="postCssClasses(post)">
          <react-post :topic_uuid="topic_uuid" :post="post" :baseurl="baseurl"></react-post>
        </div>
      </template>
    </div>
  </div>
`,
  data: function () {
    return {
      posts: [],
      initialPost: null,
      firstUnreadPost: null,
      editor: null,
    }
  },
  props: ['baseurl', 'topic_uuid', 'topic_title'],
  methods: {
    post: function() {
      if (this.newPostContent().trim() == "") {
        return;
      }

      $.ajax({
        url: this.baseurl+"create-post",
        type: 'post',
        data: { topicUuid: this.topic_uuid, content: this.newPostContent() },
        dataType: 'json',
        success: (json) => {
          this.clearEditor();
          this.refreshPosts();
        }
      });
    },
    clearEditor: function () {
      if (this.editor) {
        this.editor.setData('');
      }
    },
    newPostContent: function () {
      if (this.editor) {
        return this.editor.getData();
      } else {
        return "";
      }
    },
    refreshPosts: function() {
      this.firstUnreadPost = null;

      $.ajax({
        url: this.baseurl+"posts",
        type: 'get',
        data: { topicUuid: this.topic_uuid },
        dataType: 'json',
        success: (json) => {
          if (json.length > 0) {
            this.initialPost = json.shift();
            this.posts = json;

            // FIXME IE support?
            this.firstUnreadPost = this.posts.find(function(post) {
              return post.unread;
            });

          } else {
            this.initialPost = null;
            this.posts = [];
          }
        }
      });
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
            this.refreshPosts();
          }
        }
      });
    },
    isFirstUnreadPost: function(post) {
      if (post == this.firstUnreadPost) {
        return true;
      }

      return false;
    },
    postCssClasses: function(post) {
        var classes = ['conversations-post'];
        if (post.unread) {
            classes.push('unread');
        }
        return classes.join(' ');
    },
    resetMarkTopicReadEvents: function() {
      // FIXME do something smRTr to determine when a topic has been read
      $( window ).off('unload').on('unload', () => {
        console.log('TESTING');
        this.markTopicRead(false);
      });
    },
    initRichTextareas: function() {
      var self = this;

      $(this.$el).find('.post-to-topic-textarea').not('.rich-text-initialized').each(function () {
        InlineEditor
          .create(this)
          .then(newEditor => {
            self.editor = newEditor;
          })
          .catch(function (error) {
            console.error(error);
          });

        $(this).addClass('rich-text-initialized');
      });
    },
  },
  mounted: function() {
    this.refreshPosts();
    this.resetMarkTopicReadEvents();
  },
  updated: function() {
    // If we added a new rich text area, enrich it!
    this.$nextTick(function () {
      this.initRichTextareas();
    });
  }
});
