Vue.component('react-post', {
  template: `
<div class="conversations-react-post well">
  <span v-if="post.unread" class="badge badge-primary">NEW</span>
  <small class="text-muted">{{post.postedByEid}} - {{formatEpochTime(post.postedAt)}}</small>
  <br>
  <br>
  {{post.content}}
  <br>
  <br>
  <template v-if="post.comments.length > 0">
    <div v-for="comment in post.comments">
      <span v-if="comment.unread" class="badge badge-primary">NEW</span>
      <small class="text-muted">{{comment.postedByEid}} - {{formatEpochTime(comment.postedAt)}}</small>
      <br>
      {{comment.content}}
    </div>
  </template>
  <template v-if="showCommentForm">
    <div class="conversations-react-comment-form">
      <textarea class="form-control" placeholder="Comment on post..." v-model="commentContent"></textarea>
      <button class="button" v-on:click="addComment()">Post Comment</button>
    </div>
  </template>
  <template v-else>
      <button class="button" v-on:click="showCommentForm = true">Comment</button>
  </template>
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
      <div class="well">
        <span v-if="initialPost.unread" class="badge badge-primary">NEW</span>
        {{initialPost.content}}
        <br>
        <br>
        <small class="text-muted">{{initialPost.postedByEid}} - {{formatEpochTime(initialPost.postedAt)}}</small>
      </div>
    </template>
    <div class="conversations-post-form">
      <textarea class="form-control" placeholder="Post to topic..." v-model="newPostContent"></textarea>
      <button class="button" v-on:click="post()">Post</button>
    </div>
    <div class="conversations-posts">
      <div v-for="(post, index) in posts" class="conversations-post">
        <react-post :topic_uuid="topic_uuid" :post="post" :baseurl="baseurl"></react-post>
      </div>
    </div>
  </div>
`,
  data: function () {
    return {
      posts: [],
      newPostContent: '',
      initialPost: null,
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
      $.ajax({
        url: this.baseurl+"posts",
        type: 'get',
        data: { topicUuid: this.topic_uuid },
        dataType: 'json',
        success: (json) => {
          if (json.length > 0) {
            this.initialPost = json.shift();
            this.posts = json;
          } else {
            this.initialPost = null;
            this.posts = [];
          }
        }
      });
    },
    formatEpochTime: function(epoch) {
      return new Date(epoch).toLocaleString();
    }
  },
  mounted: function() {
    this.refreshPosts();
  }
});