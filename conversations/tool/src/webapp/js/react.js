Vue.component('react-post', {
  template: `
<div class="conversations-post">
  <span v-if="post.unread" class="badge badge-primary">NEW</span>
  <small class="text-muted">{{post.postedByEid}} - {{formatEpochTime(post.postedAt)}}</small>
  <div class="conversations-post-content">
    {{post.content}}
    <div class="conversations-post-comments">
      <template v-if="showCommentForm">
        <div class="conversations-react-comment-form">
          <textarea class="form-control" placeholder="Comment on post..." v-model="commentContent"></textarea>
          <button class="button" v-on:click="addComment()">Post Comment</button>
        </div>
      </template>
      <template v-else>
          <button class="button" v-on:click="showCommentForm = true">Comment</button>
      </template>
      <template v-if="post.comments.length > 0">
        <div v-for="comment in post.comments" class="conversations-post-comment">
          <span v-if="comment.unread" class="badge badge-primary">NEW</span>
          <small class="text-muted">{{comment.postedByEid}} - {{formatEpochTime(comment.postedAt)}}</small>
          <br>
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
        <small class="text-muted">{{initialPost.postedByEid}} - {{formatEpochTime(initialPost.postedAt)}}</small>
        <div class="conversations-post-content">
          {{initialPost.content}}
        </div>
      </div>
    </template>
    <div class="conversations-post-form">
      <textarea class="form-control" placeholder="Post to topic..." v-model="newPostContent"></textarea>
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
      newPostContent: '',
      initialPost: null,
      firstUnreadPost: null,
    }
  },
  props: ['baseurl', 'topic_uuid'],
  methods: {
    post: function() {
      if (this.newPostContent.trim() == "") {
          this.newPostContent = "";
        return;
      }

      $.ajax({
        url: this.baseurl+"create-post",
        type: 'post',
        data: { topicUuid: this.topic_uuid, content: this.newPostContent },
        dataType: 'json',
        success: (json) => {
          this.newPostContent = "";
          this.refreshPosts();
        }
      });
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
  },
  mounted: function() {
    this.refreshPosts();
    this.resetMarkTopicReadEvents();
  }
});