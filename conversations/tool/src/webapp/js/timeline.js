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
  data: function() {
    return {
      maxTicks: 12,
      minTickLength: 24 * 60 * 60 * 1000,
    };
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
          },
        ];
      }

      const range = this.maxDate - this.minDate;
      const tickLength = Math.max(Math.ceil(range / this.maxTicks), this.minTickLength);
      const allPosts = [this.initialPost].concat(this.posts);

      const bucketedPosts = {};
      let lastBucket = 0;

      for (const post of allPosts) {
        const bucket = Math.floor((post.postedAt - this.minDate) / tickLength);

        bucketedPosts[bucket] = bucketedPosts[bucket] || [];
        bucketedPosts[bucket].push(post);

        if (bucket > lastBucket) {
          lastBucket = bucket;
        }
      }

      const result = [];
      for (let i = 0; i <= lastBucket; i++) {
        const bucketPosts = bucketedPosts[i] || [];
        const bucketEnd = this.minDate + ((i + 1) * tickLength);

        result.push({
          size: Math.max(Math.floor((bucketPosts.length / allPosts.length) * 100), 5),
          firstPostInTick: bucketPosts[0],
          label: this.formatEpochDate(bucketEnd),
          count: bucketPosts.length,
        });
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
    },
  },
  mounted: function() {
    console.log(this.posts);
  },
});
