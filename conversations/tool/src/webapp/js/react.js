Vue.component('react-topic', {
  template: `
  <div class="conversations-topic react">
    <template v-if="intialPost">
      <div class="well">
        {{intialPost.content}}
        <br>
        <br>
        <small class="text-muted">{{intialPost.postedByEid}} - {{formatEpochTime(intialPost.postedAt)}}</small>
      </div>
    </template>
    <div class="conversations-post-form">
      <textarea class="form-control" placeholder="Post to topic..." v-model="newPostContent"></textarea>
      <button class="button" v-on:click="post()">Post</button>
    </div>
    <div class="conversations-posts">
      <div v-for="(post, index) in posts" class="conversations-post">
        <template v-if="post.uuid != intialPost.uuid">
          <div class="well">
            {{post.content}}
            <br>
            <br>
            <small class="text-muted">{{post.postedByEid}} - {{formatEpochTime(post.postedAt)}}</small>
          </div>
        </template>
      </div>
    </div>
  </div>
`,
  data: function () {
    return {
      posts: [],
      newPostContent: '',
      intialPost: null,
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
            this.intialPost = json.pop();
            this.posts = json;
          } else {
            this.intialPost = null;
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
})