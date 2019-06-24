Vue.component('timeline', {
  template: `
<div class="conversations-timeline-wrapper">
    <div class="conversations-timeline" ref="timeline">
        <strong>Timeline</strong>
        <template v-if="initialPost">
            <div>{{formatEpochDate(minDate)}}</div>
            <div class="timeline-slider-rail" ref="slider_rail">
              <div class="timeline-slider" ref="slider"></div>
            </div>
            <div>{{formatEpochDate(maxDate)}}</div>
        </template>
        <template v-else>
            <div>Loading timeline...</div>
        </template>
    </div>
</div>
`,
  data: function() {
    return {
      maxTicks: 12,
      minTickLength: 24 * 60 * 60 * 1000,
      dragEnabled: false,
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
    resize: function() {
        var height = $(window).height() - 240;
      $(this.$refs.timeline).height(height);

      var sliderRailHeight = $(this.$refs.slider_rail).height();
      var sliderHeight = parseInt((window.innerHeight/document.body.offsetHeight) * sliderRailHeight);
      $(this.$refs.slider).height(Math.min(sliderRailHeight, sliderHeight));
    },
    resyncSlider: function() {
        $(this.$refs.slider).css('top', 0); // FIXME sync with body scrollbar
    },
    onScroll: function() {
        // position at top
        if (window.scrollY > 150) {
            $(this.$el).addClass('fixed');
        } else {
            $(this.$el).removeClass('fixed');
        }

        // sync!
        // FIXME
    },
  },
  updated: function() {
    if (!this.dragEnabled) {
      $(this.$refs.slider).draggable({ axis: "y", containment: "parent"});
      this.dragEnabled = true;
      this.resize();
    }
  },
  mounted: function() {
      $(window).resize(() => {
          this.resize();
          this.resyncSlider();
      });
      this.$nextTick(() => {
          this.resize();
      });

      $(window).on('scroll', () => {
          this.onScroll();
      });
  },
});
