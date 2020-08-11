SeatToolEventBus = new Vue();

StudentLocationLabels = {
  'IN_PERSON': 'In-Person',
  'UNSURE': 'Unsure',
  'REMOTE': 'Remote',
};

StudentLocationSortOrder = {
  'IN_PERSON': 0,
  'UNSURE': 1,
  'REMOTE': 2,
};

Alerts = {
  messages: {
    "EDIT_CLOSED": "The edit period has closed for this seat. Please contact your instructor.",
    "SEAT_TAKEN": "Someone is already in the seat you have selected. Please check your seat number and contact your instructor if necessary.",
    "CONCURRENT_UPDATE": "This seat has been updated since you started editing. Please check the new value and retry if needed.",
    "SAVE_SUCCESS": "Seat successfully updated",
  }
}

Alerts.success = function (code) {
  $.growl.notice({
    title: "Success!",
    message: (Alerts.messages[code] || ""),
    size: "large",
  });
};

Alerts.error_for_code = function (error_code) {
  $.growl.error({
    message: (Alerts.messages[error_code] || "Unexpected error!  Please retry."),
    size: "large",
  });
};


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
        $(this.$refs.modal).on('shown.bs.modal', function() {
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
      <template v-if="editing || seat">
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
          v-bind:style="{width: inputWidth}"
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
          <button v-show="!isStudent && seat !== null" @click="clear()">
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
      seatValueUponEditing: null,
      editing: false,
      inputValue: '',
      currentTime: 0,
      currentTimePoll: null,
    };
  },
  props: ['seat', 'netid', 'meetingId', 'groupId', 'sectionId', 'isStudent', 'editableUntil'],
  computed: {
      inputWidth: function() {
        if (this.isStudent) {
          return this.inputValue.length + 4 + 'vw';
        }

        return 'inherit';
      },
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
      if (this.isStudent) {
        this.resetSeatValue();
        this.editing = false;
        return;
      }

      if (!this.editing) {
        this.resetSeatValue();
        if (a !== b) {
          this.highlightInput();
        }
      }
    },
  },
  methods: {
    resetSeatValue: function() {
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

      SeatToolEventBus.$emit("editing", this._uid);

      this.editing = true;
      this.seatValueUponEditing = this.seat;
      this.selectInput();
    },
    focusInput: function() {
      var self = this;
      self.$nextTick(function() {
        if (self.seat) {
          self.$refs.input.focus();
        } else {
          self.$refs.enterSeatButton.focus();
        }
      });
    },
    highlightInput: function() {
      var self = this;
      self.$nextTick(function() {
        if (self.$refs.input) {
          var elt = $(self.$refs.input);
          elt.animate({
            backgroundColor: "#90EE90",
          }, 400, function () {
            elt.animate({
              backgroundColor: "rgba(0, 0, 0, 0)",
            }, 1000)
          });
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
          currentSeat: self.seatValueUponEditing,
        },
        success: function(json) {
          if (json.error) {
            self.markHasError();
            self.selectInput();

            Alerts.error_for_code(json.error_code);
          } else {
            self.editing = false;
            self.seatValueUponEditing = null;
            self.focusInput();
            self.$emit("splat");
          }
        }
      });
    }
  },
  beforeDestroy: function() {
    if (this.currentTimePoll) {
      clearInterval(this.currentTimePoll);
    }
  },
  mounted: function() {
    var self = this;

    self.inputValue = self.cleanSeatValue;

    if (self.isStudent) {
      self.currentTime = new Date().getTime();
      self.currentTimePoll = setInterval(function() {
        self.currentTime = new Date().getTime();
      }, 1000);
    }

    SeatToolEventBus.$on('editing', function(componentUid) {
      if (self._uid != componentUid && self.editing) {
        self.cancel();
      }
    });
  },
});

Vue.component('split-action', {
  template: `
<div>
  <button @click="openModal()">Create Section Cohorts</button>
  <modal ref="splitModal">
    <template v-slot:header>Create Section Cohorts</template>
    <template v-slot:body>
      <div v-if="hasSeatAssignments" class="alert alert-danger">
        <b>WARNING:</b> One or more members of this section has a seat assignment.  Any existing seat assignments will be cleared.
      </div>
      <div>
        <p>Break down your section into cohorts, which can be used to create multiple seating charts for the same room
           (e.g., if different cohorts will alternate in-class attendance by week).</p>
        <p>Section membership will be randomly distributed among the cohorts according to the rules defined below.</p>
        <label for="numberofgroups">Number of cohorts to create:</label>
        <select id="numberofgroups" v-model="numberOfGroups" class="form-control">
          <option v-for="i in section.maxGroups" :value="i">{{i}}</option>
        </select>
      </div>
      <div v-if="instructionModeSupportsWeighted">
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
      instructionModeSupportsWeighted: function() {
        return this.section.instructionMode === 'OB';
      },
      hasSeatAssignments: function() {
          for (var g = 0; g < this.section.groups.length; g++) {
              var group = this.section.groups[g];
              for (var m = 0; m < group.meetings.length; m++) {
                  var meeting = group.meetings[m];
                  for (var a = 0; a < meeting.seatAssignments.length; a++) {
                      var seatAssignment = meeting.seatAssignments[a];

                      if (seatAssignment.seat) {
                          return true;
                      }
                  }
              }
          }

          return false;
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
<div class="pull-left">
  <div v-show="meeting.seatAssignments.length > 0">
    <span>{{meeting.seatAssignments.length}} member<template v-if="meeting.seatAssignments.length > 0">s</template></span>
    <span v-bind:style="{ marginLeft: '10px' }">{{locationSummary}}</span>
  </div>
</div>`,
  props: ['meeting'],
  computed: {
    locationSummary: function() {
      var counts = {};
      for (var i=0; i<this.meeting.seatAssignments.length; i++) {
        var studentLocation = this.meeting.seatAssignments[i].studentLocation;
        if (!counts[studentLocation]) {
          counts[studentLocation] = 0;
        }
        counts[studentLocation]++;
      }

      var result = [];

      $.each(counts, function(location, count) {
        result.push(count + ' ' + StudentLocationLabels[location]);
      });
      return '(' + result.join(', ') + ')';
    }
  }
});

Vue.component('group-meeting', {
  template: `
<div>
  <div v-bind:style="{clear: 'both'}">
    <group-meeting-summary :meeting="meeting"></group-meeting-summary>
    <label class="pull-right">Sort by: 
      <select v-model="sortBy">
        <option value="STUDENT_LOCATION">Student Location</option>
        <option value="NETID">NetID</option>
      </select>
    </label>
  </div>
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
      <tr v-for="assignment in sortedSeatAssignments" :key="assignment.netid + group.id">
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
            :isStudent="false"
            v-on:splat="$emit('splat')">
          </seat-assignment-widget>
        </td>
        <td>
          {{labelForStudentLocation(assignment.studentLocation)}} 
          <template v-if="!assignment.official">(Unofficial)</template>
        </td>
        <td>
          <template v-if="$parent.isNotOnlyGroup">
            {{group.name}}
            <a href="javascript:void(0)" @click="openMoveModal(assignment)">Move</a>
          </template>
          <template v-else>
            {{section.shortName}}
          </template>
        </td>
      </tr>
    </tbody>
  </table>
  <modal v-if="assignmentToBeMoved" ref="moveModal">
    <template v-slot:header>Move Member ({{assignmentToBeMoved.displayName}})</template>
    <template v-slot:body>
      <div>

        <div v-if="assignmentToBeMoved.seat" class="alert alert-danger">
            <p>This member has an existing seat assignment. Moving to another cohort will clear their existing seat assignment.</p>
        </div>

        <p>Select the cohort to which you would like to move this section member.</p>

        <div class="form-group">
          <label :for="_uid + '_group'">Move to:</label>
          <select :id="_uid + '_group'" ref="moveToGroupSelect" type="text" class="form-control">
            <option v-for="otherGroup in otherGroups" :value="otherGroup.id">{{otherGroup.name}}</option>
          </select>
        </div>
     </div>
    </template>
    <template v-slot:footer>
      <button @click="move()" class="pull-left btn-primary">Move</button>
      <button @click="closeMoveModal()">Cancel</button>
    </template>
  </modal>
</div>
`,
  data: function() {
    return {
      assignmentToBeMoved: null,
      sortBy: 'STUDENT_LOCATION',
    };
  },
  props: ['section', 'group', 'meeting'],
  computed: {
    baseurl: function() {
        return this.$parent.baseurl;
    },
    sortedSeatAssignments: function() {
      var self = this;

      return self.meeting.seatAssignments.sort(function(a, b) {
        if (self.sortBy === 'NETID') {
          return (a.netid < b.netid) ? -1 : 1;

        } else if (self.sortBy === 'SEAT') {
          if (a.seat === null) {
            return 1;
          } else if (b.seat === null) {
            return -1;
          } else {
            return (a.seat < b.seat) ? -1 : 1;
          }

        } else {
          if (a.studentLocation === b.studentLocation) {
            return (a.netid < b.netid) ? -1 : 1;
          } else {
            return (StudentLocationSortOrder[a.studentLocation] < StudentLocationSortOrder[b.studentLocation]) ? -1 : 1;
          }
        }
      })
    },
    otherGroups: function() {
      var otherGroups = [];

      for (var i=0; i<this.section.groups.length; i++) {
        var otherGroup = this.section.groups[i];
        if (this.group.id !== otherGroup.id) {
          otherGroups.push(otherGroup);
        }
      }

      return otherGroups.sort(function(a, b) {
        return a.name < b.name ? -1 : 1;
      });
    },
  },
  methods: {
    labelForStudentLocation: function(studentLocation) {
      return StudentLocationLabels[studentLocation];
    },
    openMoveModal: function(assignment) {
      var self = this;

      self.assignmentToBeMoved = assignment;
      self.$nextTick(function() {
        self.$refs.moveModal.open();
      });
    },
    move: function() {
      var self = this;

      $.ajax({
        url: this.baseurl + "transfer-groups",
        type: 'post',
        data: {
          sectionId: self.section.id,
          fromGroupId: self.group.id,
          toGroupId: self.$refs.moveToGroupSelect.value,
          netid: self.assignmentToBeMoved.netid,
        },
        success: function() {
          self.closeMoveModal();
          self.$emit('splat');
        },
      });
    },
    closeMoveModal: function() {
      if (this.$refs.moveModal) {
        this.$refs.moveModal.close();
      }
    },
  }
});

Vue.component('input-with-char-count', {
  template: `
    <div style="text-align: right">
      <input :id="id" ref="textInput" type="text" class="form-control" v-model="value" v-on:keyup="adjustCount()" :maxlength="chars" />
      <small><span>{{remaining}}</span> characters remaining</small>
    </div>
  `,
  props: ['id', 'chars'],
  data: function() {
    return {
      value: '',
      remaining: this.chars,
    }
  },
  methods: {
    setValue: function (s) {
      this.value = s;
      this.$nextTick(function() {
        this.adjustCount();
      });
    },
    getValue: function() {
      return this.value;
    },
    adjustCount: function() {
      this.remaining = Math.max(this.chars - this.$refs.textInput.value.length, 0);
    }
  }
});

Vue.component('email-cohort', {
    template: `
        <div>
            <modal ref="emailModal">
                <template v-slot:header>Email cohort: {{group.name}}</template>
                <template v-slot:body>
                    <div class="form-group">
                        <label :for="_uid + '_subject'">Subject:</label>
                        <input type="text" ref="emailSubject" class="form-control" :id="_uid + '_subject'" name="subject" />

                        <label :for="_uid + '_body'">Message body:</label>
                        <textarea :id="_uid + '_body'" ref="emailBody" name="body" class="form-control seats-email-body"></textarea>
                    </div>
                </template>
                <template v-slot:footer>
                    <button @click="sendEmail()" class="pull-left btn-primary"><i class="fa fa-envelope"></i> Send</button>
                    <button @click="closeEmailModal()">Cancel</button>
                </template>
            </modal>

            <button @click="showModal()" :class="htmlClass"><i class="fa fa-envelope"></i> Send Message to Cohort</button>
        </div>`,
    props: ['htmlClass', 'section', 'group'],
    methods: {
        showModal: function() {
            this.$refs.emailSubject.value = '';
            this.$refs.emailBody.value = '';

            this.$refs.emailModal.open();
        },
        sendEmail: function() {
            var self = this;

            $.ajax({
                url: self.baseurl + "email-group",
                type: 'post',
                data: {
                    sectionId: self.section.id,
                    groupId: self.group.id,
                    subject: self.$refs.emailSubject.value,
                    body: self.$refs.emailBody.value,
                },
                success: function() {
                    self.closeEmailModal();
                },
            });

        },
        closeEmailModal: function() {
            this.$refs.emailModal.close();
        },
    },
    computed: {
        baseurl: function() {
            return this.$parent.baseurl;
        },
    }
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
  <email-cohort v-if="!group.isGroupEmpty" htmlClass="pull-right" :group="group" :section="section" />
  <modal ref="descriptionModal">
    <template v-slot:header>Add Cohort Description {{group.name}}</template>
    <template v-slot:body>
      <div>
        <p>Add a short description for your cohort, which will display to students.</p>

        <div class="form-group">
          <label :for="_uid + '_description'">Description text:</label>
          <input-with-char-count chars=200 :id="_uid + '_description'" ref="descriptionInput" />
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
    <group-meeting
      :group="group"
      :section="section"
      :meeting="meeting"
      v-on:splat="$emit('splat')"
    >
    </group-meeting>
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
  data: function() {
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
      this.$refs.descriptionInput.setValue(this.group.description || '');
    },
    saveDescription: function() {
      var self = this;

      $.ajax({
        url: self.baseurl + "/save-group-description",
        type: 'post',
        data: {
          groupId: self.group.id,
          description: self.$refs.descriptionInput.getValue(),
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
      },
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
                  v-if="sections.length > 1"
                  ref="sectionSelector"
                  :sections="sections"
                  v-on:selectSection="handleSectionSelect">
              </section-selector>
              <hr v-if="sections.length > 1" />
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
                  if (self.sections.length === 1) {
                    self.selectedSectionId = self.sections[0].id;
                  }
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
            <div v-for="meeting in meetings" :key="meeting.netid">
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
                  :editableUntil="meeting.editableUntil"
                  v-on:splat="resetPolling()">
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
      resetPolling: function() {
        var self = this;

        if (self.pollInterval) {
          clearInterval(self.pollInterval);
        }

        self.fetchData();

        self.pollInterval = setInterval(function() {
            self.fetchData();
        }, 5000);
      }
  },
  beforeDestroy: function() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  },
  mounted: function() {
      var self = this;

      self.resetPolling();
  },
});