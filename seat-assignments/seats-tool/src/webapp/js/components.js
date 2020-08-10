Vue.component('modal', {
  template: `
<div class="modal" tabindex="-1" role="dialog" ref="modal">
  <div class="modal-dialog" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <div class="modal-title" style="float: left">
          <slot name="header"></slot>
        </div>
        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
      <div class="modal-body">
        <slot name="body"></slot>
      </div>
      <div class="modal-footer">
        <slot name="footer"></slot>
      </div>
    </div>
  </div>
</div>
  `,
  props: [],
  methods: {
    open: function(callback) {
      var self = this;

      if (callback) {
        $(this.$refs.modal).on('shown.bs.modal', function () {
          callback(self.$refs.modal);
        })
      }

      $(this.$refs.modal).modal('show');
    },
    close: function() {
      $(this.$refs.modal).modal('hide');
    }
  },
  mounted: function() {
    var self = this;

    $(document).ready(function() {
      $(self.$refs.modal).modal({
        show: false,
      });
    });
  }
});

Vue.component('seat-assignment-widget', {
  template: `
    <span class="seat-assignment-widget">
      <div v-if="isStudent && isEditable && !!editableUntil" class="alert alert-warning">
        Note: You have {{timeLeftToEdit}} to make further edits to your seat assignment.
      </div>
      <template v-if="editing || seatValue">
        <label :for="inputId" class="sr-only">
          Seat assignment for {{netid}}
        </label>
        <input
          :id="inputId"
          type="text" 
          v-model="inputValue"
          ref="input"
          :class="'form-control ' + inputCSSClasses"
          :readonly="!editing"
          v-on:keydown.enter="editOrSave()"
          v-on:keydown.esc="cancel()"
          :required="isStudent"
        />
        <template v-if="isEditable && editing">
          <button class="btn-primary" @click="save()">
            <i class="glyphicon glyphicon-ok" aria-hidden="true"></i> Save
          </button>
          <button @click="cancel()">
            <i class="glyphicon glyphicon-ban-circle" aria-hidden="true"></i> Cancel
          </button>
        </template>
        <template v-else-if="isEditable">
          <button @click="edit()">
            <i class="glyphicon glyphicon-pencil" aria-hidden="true"></i> Edit
          </button>
          <button v-show="!isStudent && seatValue !== null" @click="clear()">
            <i class="glyphicon glyphicon-remove" aria-hidden="true"></i> Clear
          </button>
        </template>
      </template>
      <template v-else-if="isEditable">
        <button ref="enterSeatButton" @click="edit()">
          <i class="glyphicon glyphicon-plus" aria-hidden="true"></i> Enter Seat Assignment
        </button>
      </template>
    </span>
  `,
  data: function() {
    return {
      hasError: false,
      seatValue: null,
      editing: false,
      inputValue: '',
      currentTime: 0,
      currentTimePoll: null,
    };
  },
  props: ['seat', 'netid', 'meetingId', 'groupId', 'sectionId', 'isStudent', 'editableUntil'],
  computed: {
      inputId: function() {
          return [this.meetingId, this.netid].join('__');
      },
      inputCSSClasses: function() {
        if (this.hasError) {
          return "has-error";
        } else {
          return ""
        }
      },
      baseurl: function() {
          return this.$parent.baseurl;
      },
      cleanSeatValue: function() {
        if (this.seat == null) {
              return '';
          } else {
              return '' + this.seat;
          }
      },
      isEditable: function() {
          if (!this.isStudent) {
              return true;
          }

          if (this.editableUntil === null) {
            // no seat value set so editable
            return true;
          }

          if (this.editableUntil === 0) {
            // seat was set by instructor
            return false;
          }

          // check the edit window
          return this.currentTime < this.editableUntil;
      },
      timeLeftToEdit: function() {
        var ms = this.editableUntil - this.currentTime;
        if (ms <= 0) {
          return 'no time';
        }

        var sec = parseInt(ms / 1000);
        if (sec <= 60) {
          return sec + " seconds"
        }

        var min = parseInt(sec / 60);
        return min + " minutes " + (sec % 60) + " seconds";
      },
  },
  watch: {
    seat: function(a, b) {
      if (!this.editing) {
        this.resetSeatValue();
        if (a !== b) {
          // FIXME notify user of change
        }
      }
    }
  },
  methods: {
    resetSeatValue: function() {
      this.seatValue = this.seat;
      this.inputValue = this.cleanSeatValue;
    },
    cancel: function() {
      this.hasError = false;
      this.editing = false;
      this.resetSeatValue();
      this.focusInput();
    },
    editOrSave: function() {
      if (!this.isEditable) {
          return;
      }

      if (this.editing) {
        this.save();
      } else {
        this.edit()
      }
    },
    clear: function() {
      if (!this.isEditable) {
          return;
      }
      this.inputValue = '';
      this.save();
    },
    edit: function() {
      if (!this.isEditable) {
          return;
      }

      this.editing = true;
      this.selectInput();
    },
    focusInput: function() {
      var self = this;
      self.$nextTick(function() {
        if (self.seatValue) {
          self.$refs.input.focus();
        } else {
          self.$refs.enterSeatButton.focus();
        }
      });
    },
    selectInput: function() {
      var self = this;
      self.$nextTick(function() {
        self.$refs.input.select();
      });
    },
    markHasError: function() {
      this.hasError = true;
    },
    clearHasError: function() {
      this.hasError = false;
    },
    save: function() {
      if (!this.isEditable) {
          return;
      }

      var self = this;
      self.clearHasError();

      if (self.seatValue === (self.inputValue === '' ? null : self.inputValue)) {
        // noop
        self.editing = false;
        return;
      }

      $.ajax({
        url: this.baseurl + "/seat-assignment",
        type: 'post',
        dataType: 'json',
        data: {
          sectionId: self.sectionId,
          groupId: self.groupId,
          meetingId: self.meetingId,
          netid: self.netid,
          seat: self.inputValue,
          currentSeat: self.seatValue,
        },
        success: function(json) {
          if (json.error) {
            self.markHasError();
            self.selectInput();
            // FIXME notify user of error
          } else {
            self.editing = false;
            // assume save succeeded and set what we expect the seat to be
            // the next poll-update will re-sync if required
            self.seat = self.inputValue === '' ? null : self.inputValue
            self.seatValue = self.seat;

            self.focusInput();
            // FIXME notify user of save success
          }
        }
      });
    }
  },
  beforeDestroy: function() {
    if (this.currentTimePoll) {
      clearInterval(currentTimePoll);
    }
  },
  mounted: function() {
    var self = this;

    self.seatValue = self.seat;
    self.inputValue = self.cleanSeatValue;

    if (self.isStudent) {
      self.currentTime = new Date().getTime();
      self.currentTimePoll = setInterval(function() {
        self.currentTime = new Date().getTime();
      }, 1000);
    }
  },
});

Vue.component('split-action', {
  template: `
<div>
  <button @click="openModal()">Create Section Cohorts</button>
  <modal ref="splitModal">
    <template v-slot:header>Create Section Cohorts</template>
    <template v-slot:body>
      <div>
        <p>Break down your section into cohorts, which can be used to create multiple seating charts for the same room
           (e.g., if different cohorts will alternate in-class attendance by week).</p>
        <p>Section membership will be randomly distributed among the cohorts according to the rules defined below.</p>
        <label for="numberofgroups">Number of cohorts to create:</label>
        <select id="numberofgroups" v-model="numberOfGroups" class="form-control">
          <option>1</option>
          <option>2</option>
          <option>3</option>
          <option>4</option>
        </select>
      </div>
      <div>
        <label for="selectionType">Remote student random cohort assignment:</label>
        <div class="well">
          <label>
            <input type="radio" v-model="selectionType" value="RANDOM"/> 
            Split remote students evenly among the created cohorts
          </label>
          <label>
            <input type="radio" v-model="selectionType" value="WEIGHTED"/> 
            Group remote students together in one cohort (if possible)
          </label>
          <p>Note: In-Person section members will be distributed randomly across the cohorts regardless of the options selected above.</p>
        </div>
      </div>
    </template>
    <template v-slot:footer>
      <button @click="performSplit()" class="pull-left btn-primary">Save</button>
      <button @click="closeModal()">Cancel</button>
    </template>
  </modal>
</div>
`,
  props: ['section'],
  data: function() {
    return {
      numberOfGroups: 1,
      selectionType: 'RANDOM',
    }
  },
  computed: {
      baseurl: function() {
          return this.$parent.baseurl;
      },
  },
  methods: {
    openModal: function() {
      this.$refs.splitModal.open();
    },
    closeModal: function() {
      this.$refs.splitModal.close();
    },
    performSplit: function() {
      var self = this;

      $.ajax({
        url: this.baseurl + "/split-section",
        type: 'post',
        data: {
          sectionId: this.section.id,
          numberOfGroups: this.numberOfGroups,
          selectionType: this.selectionType,
        },
        success: function() {
          self.$emit('splat');
          self.closeModal();
        }
      })
    }
  }
});

Vue.component('group-meeting-summary', {
  template: `
<div>
  <small v-show="meeting.seatAssignments.length > 0">
    <span>{{meeting.seatAssignments.length}} member<template v-if="meeting.seatAssignments.length > 0">s</template></span>
    <span v-bind:style="{ marginLeft: '10px' }">{{locationSummary}}</span>
  </small>
</div>`,
  props: ['meeting'],
  computed: {
    locationSummary: function() {
      return '(TODO summary)';
    }
  }
});

Vue.component('group-meeting', {
  template: `
<div>
  <group-meeting-summary :meeting="meeting"></group-meeting-summary>
  <table class="seat-table seat-assignment-listing">
    <thead>
      <tr>
        <th>Picture</th>
        <th>Name</th>
        <th>Seat Assignment</th>
        <th>Student Location</th>
        <th>
          <template v-if="$parent.isNotOnlyGroup">
            Section Cohort
          </template>
          <template v-else>
            Section
          </template>
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="assignment in sortedSeatAssignments">
        <td>
          <div class="profile-pic">
            <img :src="'/direct/profile/' + assignment.netid + '/image/official'"/>
          </div>
        </td>
        <td>{{assignment.displayName}} ({{assignment.netid}})</td>
        <td>
          <seat-assignment-widget
            :seat="assignment.seat"
            :netid="assignment.netid"
            :meetingId="meeting.id"
            :groupId="group.id"
            :sectionId="section.id"
            :isStudent="false">
          </seat-assignment-widget>
        </td>
        <td>
          TODO 
          <template v-if="!assignment.official">(Unofficial)</template>
        </td>
        <td>
          <template v-if="$parent.isNotOnlyGroup">
            {{group.name}}
          </template>
          <template v-else>
            {{section.shortName}}
          </template>
        </td>
      </tr>
    </tbody>
  </table>
</div>
`,
  props: ['section', 'group', 'meeting'],
  computed: {
    baseurl: function() {
        return this.$parent.baseurl;
    },
    sortedSeatAssignments: function() {
      return this.meeting.seatAssignments.sort(function(a, b) {
        return (a.netid < b.netid) ? -1 : 1;
      })
    },
  },
});

Vue.component('section-group', {
  template: `
<div>
  <template v-if="isNotOnlyGroup">
    <h2>{{group.name}} {{groupLabel}}</h2>
    <p>{{section.name}}</p>
  </template>
  <template v-if="group.description">
    <p>{{group.description}} <a href="javascript:void(0)" @click="showDescriptionModal()">Edit</a></p>
  </template>
  <template v-else>
    <button @click="showDescriptionModal()">Add Description (optional)</button>
  </template>
  <modal ref="descriptionModal">
    <template v-slot:header>Add Cohort Description {{group.name}}</template>
    <template v-slot:body>
      <div>
        <p>Add a short description for your cohort, which will display to students.</p>

        <div class="form-group">
          <label :for="_uid + '_description'">Description text:</label>
          <input :id="_uid + '_description'" ref="descriptionInput" type="text" class="form-control">
        </div>
     </div>
    </template>
    <template v-slot:footer>
      <button @click="saveDescription()" class="pull-left btn-primary">Save</button>
      <button @click="closeDescriptionModal()">Cancel</button>
    </template>
  </modal>
  <template v-if="group.isGroupEmpty">
    <button @click="deleteGroup">Delete Group</button>
  </template>
  <template v-for="meeting in group.meetings">
    <group-meeting :group="group" :section="section" :meeting="meeting"></group-meeting>
  </template>
  <button @click="addAdhocMembers()">Add Non-Official Site Member(s)</button>
  <modal ref="membersModal">
    <template v-slot:header>Add Non-Official Site Member(s) {{group.name}}</template>
    <template v-slot:body>
      <div>
        <p>Select manually-added, non-official site members in this site to ad to this cohort. For more information on manually adding members to your course <a href="">click here</a>.</p>

        <table class="seat-table">
          <thead>
            <tr>
              <th></th>
              <th>Participant</th>
              <th>Role</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="membersForAdd === null">
              <tr><td colspan="3">Loading...</td></tr>
            </template>
            <template v-else>
              <tr v-for="user in membersForAdd">
                <td><input type="checkbox" v-model="selectedMembers" :value="user.netid" /></td>
                <td>{{user.displayName}} ({{user.netid}})</td>
                <td>{{user.role}}</td>
              </tr>
            </template>
          </tbody>
        </table>
     </div>

    </template>
    <template v-slot:footer>
      <button @click="saveUsers()" class="pull-left primary">Add Member(s)</button>
      <button @click="closeModal()">Cancel</button>
    </template>
  </modal>
</div>`,
  props: ['section', 'group'],
  data: function () {
    return {
      selectedMembers: [],
      membersForAdd: [],
    }
  },
  methods: {
    deleteGroup: function() {
      var self = this;
      $.ajax({
        url: self.baseurl + "delete-group",
        type: 'post',
        data: {
          sectionId: self.section.id,
          groupId: self.group.id,
        },
        success: function() {
          self.$emit('splat');
        },
      });
    },
    showDescriptionModal: function() {
      this.$refs.descriptionModal.open();
      this.$refs.descriptionInput.value = this.group.description || '';
    },
    saveDescription: function() {
      var self = this;

      $.ajax({
        url: self.baseurl + "/save-group-description",
        type: 'post',
        data: {
          groupId: self.group.id,
          description: self.$refs.descriptionInput.value,
        },
        dataType: 'json',
        success: function(json) {
          self.$refs.descriptionModal.close();
          self.$emit('splat');
        }
      });
    },
    closeDescriptionModal: function() {
      this.$refs.descriptionModal.close();
    },
    addAdhocMembers: function() {
      var self = this;

      self.selectedMembers = [];
      self.membersForAdd = null;

      this.$refs.membersModal.open(function (modalElt) {
        $.ajax({
          url: self.baseurl + "/available-site-members",
          type: 'get',
          data: {
            sectionId: self.section.id,
            groupId: self.group.id,
          },
          dataType: 'json',
          success: function(json) {
            self.membersForAdd = json.sort(function (a, b) { return a.netid.localeCompare(b.netid) });
          }
        })
      });
    },
    saveUsers: function() {
      var self = this;

      $.ajax({
        url: self.baseurl + "/add-group-users",
        type: 'post',
        data: {
          sectionId: self.section.id,
          groupId: self.group.id,
          selectedMembers: self.selectedMembers,
        },
        success: function() {
          self.$emit('splat');
          self.closeModal();
        }
      });
    },
    closeModal: function() {
      this.$refs.membersModal.close();
    },
  },
  computed: {
    baseurl: function() {
        return this.$parent.baseurl;
    },
    isNotOnlyGroup: function() {
      return this.section.groups.length > 1;
    },
    groupLabel: function() {
      return "(" + (this.section.groups.indexOf(this.group) + 1) + " of " + this.section.groups.length +  ")";
    }
  },
});

Vue.component('section-table', {
  template: `
    <div>
      <template v-if="section">
          <h2 v-show="section.groups.length === 1">{{section.name}}</h2>
          <split-action
            v-show="!section.split"
            :section="section"
            v-on:splat="resetPolling()">
          </split-action>
          <template v-for="group in sortedGroups">
            <section-group :group="group" :section="section" v-on:splat="resetPolling()"></section-group>
          </template>
          <template v-if="section.groups.length < section.maxGroups">
            <hr/>
            <button @click="addGroup()">Add Another Section Cohort</button>
          </template>
      </template>
    </div>
  `,

  data: function() {
    return {
        section: null,
        pollInterval: null,
    };
  },
  props: ['sectionId'],
  watch: {
    sectionId: function(a, b) {
      if (a != b) {
        this.resetPolling();
      }
    },
  },
  beforeDestroy: function() {
    this.cancelPolling();
  },
  computed: {
      baseurl: function() {
          return this.$parent.baseurl;
      },
      sortedGroups: function() {
        if (this.section == null) {
          return [];
        } else {
          return this.section.groups.sort(function(a, b) {
            return (a.name < b.name) ? -1 : 1;
          })
        }
      },
  },
  methods: {
      addGroup: function() {
        var self = this;
        $.ajax({
          url: self.baseurl + "add-group",
          type: 'post',
          data: {
            sectionId: self.section.id,
          },
          success: function() {
            self.resetPolling();
          },
        });
      },
      fetchData: function() {
          var self = this;

          $.ajax({
              url: self.baseurl + 'section',
              data: {
                  sectionId: self.sectionId,
              },
              type: 'get',
              dataType: 'json',
              success: function (json) {
                  self.section = json;
              }
          });
      },
      cancelPolling: function() {
        var self = this;
        if (self.pollInterval) {
          clearInterval(self.pollInterval);
        }
        
      },
      resetPolling: function() {
        var self = this;

        self.cancelPolling();

        self.fetchData();

        self.pollInterval = setInterval(function() {
            self.fetchData();
        }, 5000);
      }
  },
  mounted: function() {
    this.resetPolling();
  }
});


Vue.component('section-selector', {
  template: `
<div>
  <label for="sectionSelector">
    Viewing seat assignments for:
  </label>
  <select v-model="selectedSectionId" id="sectionSelector">
    <option value="">Select section / cohorts</option>
    <option v-for="section in sections" :value="section.id">
      {{section.name}}
      <template v-if="section.groupCount > 1">
        ({{section.groupCount}} cohorts)
      </template>
    </option>
  </select>
</div>
`,
  props: ['sections'],
  data: function() {
    return {
      selectedSectionId: '',
    };
  },
  watch: {
    selectedSectionId: function() {
      this.$emit('selectSection', this.selectedSectionId == '' ? null : this.selectedSectionId);
    },
  },
  methods: {
  },
});


Vue.component('instructor-table', {
  template: `
      <div>
          <template v-if="sections.length > 0">
              <section-selector
                  ref="sectionSelector"
                  :sections="sections"
                  v-on:selectSection="handleSectionSelect">
              </section-selector>
              <hr />
              <template v-if="selectedSectionId">
                  <section-table :sectionId="selectedSectionId"></section-table>
              </template>
              <template v-else>
                <center>
                  <img src="/seats-tool/images/splash.png" alt="Select a section instructional art"/>
                  <p>Select a section / cohort from the dropdown menu above.</p>
                </center>
              </template>
          </template>
          <template v-else-if="fetched">
            <p>This tool is yet to be provisioned. Please try again in a moment.</p>
          </template>
      </div>

  `,
  data: function() {
    return {
        sections: [],
        selectedSectionId: null,
        fetched: false,
    };
  },
  props: ['baseurl'],
  methods: {
      fetchData: function() {
          var self = this;

          $.ajax({
              url: self.baseurl + 'sections',
              type: 'get',
              dataType: 'json',
              success: function (json) {
                  self.fetched = true;
                  self.sections = json;
              }
          });
      },
      handleSectionSelect: function(sectionId) {
        this.selectedSectionId = sectionId;
      },
  },
  mounted: function() {
      this.fetchData();
  },
});


Vue.component('student-home', {
  template: `
    <div>
        <template v-if="fetched">
            <div v-for="meeting in meetings">
                <h2>{{meeting.groupName}}</h2>
                <p>{{meeting.sectionName}}<p>
                <p>{{meeting.groupDescription}}</p>
                <p>{{meeting.studentName}}, enter your seat number here:</p>
                <seat-assignment-widget
                  :seat="meeting.seat"
                  :netid="meeting.netid"
                  :meetingId="meeting.meetingId"
                  :groupId="meeting.groupId"
                  :sectionId="meeting.sectionId"
                  :isStudent="true"
                  :editableUntil="meeting.editableUntil">
                </seat-assignment-widget>
                <p>Note: Only enter your seat assignment once your are in class and have chosen your seat</p>
            </div>
        </template>
    </div>
  `,
  data: function() {
    return {
        meetings: [],
        fetched: false,
        pollInterval: null,
    };
  },
  props: ['baseurl'],
  methods: {
      fetchData: function() {
          var self = this;

          $.ajax({
              url: self.baseurl + 'student-meetings',
              type: 'get',
              dataType: 'json',
              success: function (json) {
                  self.fetched = true;
                  self.meetings = json;
              }
          });
      },
      handleSectionSelect: function(sectionId) {
        this.selectedSectionId = sectionId;
      },
  },
  beforeDestroy: function() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  },
  mounted: function() {
      var self = this;

      self.fetchData();
      self.pollInterval = setInterval(function() {
          self.fetchData();
      }, 5000);
  },
});