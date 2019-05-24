Vue.component('timeline', {
  template: `
<div class="conversations-timeline">
    <strong>Timeline</strong>
    <template v-if="initialPost">
        <div>{{formatEpochDate(minDate)}}</div>
        <ul>
            <li v-for="tick in ticks" @click="tick.count > 0 ? scrollToPost(tick.firstPostInTick) : null">
                <span class="tick-line" v-bind:style="{ width: '' + tick.size + 'px'}"></span>
                <span class="tick-label">{{tick.label}}</span>
            </li>
        </ul>
        <div>{{formatEpochDate(maxDate)}}</div>
    </template>
    <template v-else>
        Loading timeline...
    </template>
</div>
`,
  data: function () {
    return {
        maxTicks: 12,
        minTickLength: 24 * 60 * 60 * 1000,
    }
  },
  props: ['posts', 'initialPost'],
  computed: {
      minDate: function() {
          return this.initialPost.postedAt;
      },
      maxDate: function() {
          if (this.posts.length === 0) {
              return this.initialPost.postedAt;
          }

          return this.posts[this.posts.length - 1].postedAt;
      },
      ticks: function() {
          if (this.posts.length === 0) {
              return [
                  {
                      size: 100,
                      epoch: this.minDate,
                      label: this.formatEpochDate(this.minDate),
                  }
              ];
          }
          var range = this.maxDate - this.minDate;
          var tickLength = Math.max(range / this.maxTicks, this.minTickLength);

          var allPosts = [this.initialPost].concat(this.posts);

          var result = [];
          for (var i=this.minDate; i<this.maxDate;) {
              var tickEnd = i + tickLength;
              if (this.maxDate - tickEnd < tickLength) {
                  tickEnd = this.maxDate + 1;
              }
              var postsInTick = allPosts.filter((post) => {
                  return post.postedAt >= i && post.postedAt < tickEnd;
              });
              var tickEpoch = Math.min(tickEnd, this.maxDate);
              result.push({
                  size: Math.max(parseInt((postsInTick.length / allPosts.length) * 100), 5),
                  firstPostInTick: postsInTick[0],
                  label: this.formatEpochDate(tickEpoch),
                  count: postsInTick.length,
              });

              i = tickEnd;
          }
          return result;
      },
  },
  methods: {
      formatEpochDate: function(epoch) {
        return new Date(epoch).toLocaleDateString();
      },
      scrollToPost: function(post) {
          this.$parent.focusAndHighlightPost(post.uuid);
      }
  },
  mounted: function() {
      console.log(this.posts);
  }
});
