Vue.component('topic-listing', {
  template: `
<div class="conversations-topics-listing">
  <table class="table table-hover table-condensed">
      <thead>
          <tr>
              <th :class="'col-sm-4' + sortClassesForColumn('title')" v-on:click="toggleSort('title')">Topic Title</th>
              <th :class="'col-sm-1' + sortClassesForColumn('type')" v-on:click="toggleSort('type')">Topic Type</th>
              <th class="col-sm-2">Posters</th>
              <th class="col-sm-1">Available To</th>
              <th class="col-sm-1">Posts</th>
              <th :class="'col-sm-2' + sortClassesForColumn('last_activity_at')" v-on:click="toggleSort('last_activity_at')">Last Activity</th>
              <th></th>
          </tr>
      </thead>
      <tbody>
          <tr v-for="topic in topics">
            <td>{{topic.title}}</td>
            <td>{{capitalize(topic.type)}}</td>
            <td>
              <!-- FIXME limit to first 6 and add plus number of others -->
              <div v-for="poster in postersToDisplay(topic.posters)" class="topic-poster-photo" :title="buildPosterTooltip(poster)">
                <img :src="buildPosterProfilePicSrc(poster)" :alt="buildPosterProfilePicAlt(poster)" />
              </div>
              <template v-if="topic.posters.length > maxPostersToDisplay">
                <div class="topic-poster-photo topic-poster-others" :title="'Plus ' + (topic.posters.length - maxPostersToDisplay) + ' other posters'">
                  +{{topic.posters.length - maxPostersToDisplay}}
                </div>
              </template>
            </td>
            <td>
              <!-- FIXME do permissions -->
              Entire Site
            </td>
            <td>
              {{count}}
            </td>
            <td>{{formatEpochTime(topic.lastActivityAt)}}</td>
            <td>
              <a :href="baseurl+'topic?uuid='+topic.uuid">View</a>
            </td>
          </tr>
      </tbody>
  </table>
</div>
`,
  data: function() {
    return {
      topics: [],
      page: this.initial_page,
      order_by: this.initial_order_by,
      order_direction: this.initial_order_direction,
      maxPostersToDisplay: 5,
    }
  },
  props: ['baseurl', 'initial_order_by', 'initial_order_direction', 'initial_page'],
  methods: {
    loadTopics: function() {
      $.ajax({
        url: this.baseurl+"feed/topics",
        type: 'get',
        data: { page: this.page, order_by: this.order_by, order_direction: this.order_direction},
        dataType: 'json',
        success: (json) => {
          this.count = json.count || 0;
          this.topics = json.topics || [];
        }
      });
    },
    capitalize: function(string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    },
    formatEpochTime: function(epoch) {
        return new Date(parseInt(epoch)).toLocaleString()
    },
    buildPosterTooltip: function(poster) {
        return poster.firstName + " " + poster.lastName + " last posted at " + this.formatEpochTime(poster.latestPostAt);
    },
    buildPosterProfilePicAlt(poster) {
        return "Profile picture for " + poster.netId;
    },
    buildPosterProfilePicSrc(poster) {
        return "/direct/profile/" + poster.userId + "/image";
    },
    toggleSort: function(column) {
      if (this.order_by == column) {
        if (this.order_direction === 'asc') {
          this.order_direction = 'desc';
        } else {
          this.order_direction = 'asc';
        }
      } else {
        this.order_by = column;
        this.order_direction = 'asc';
      }
      this.loadTopics();
    },
    sortClassesForColumn: function(column) {
      var classes = ['conversations-sortable'];
      if (column === this.order_by) {
        var sort_direction = this.order_direction.toLowerCase();
        classes.push('conversations-sortable-active');
        classes.push('conversations-sortable-active-'+sort_direction);
      }
      return ' ' + classes.join(' ');
    },
    postersToDisplay: function(posters) {
      if (posters.length < this.maxPostersToDisplay) {
        return posters;
      } else {
        return posters.slice(0, this.maxPostersToDisplay);
      }
    }
  },
  mounted: function() {
    this.loadTopics();
  }
});
