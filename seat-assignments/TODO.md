
  * Start on the instructor UI

  * Backend JSON endpoints

    * Set a seat for a netid/meeting combo

        - student mode and instructor mode

        - student mode starts 5 minute edit window and does perm check

    * Clear a seat (instructor only)

        - student mode and instructor mode

        - student mode does perm check

    * Resplit section

        * when is this possible?  flag on section to indicate when
        it's allowed?

        * instructors only

    * Move student between groups

        * from group, to group.  Check that they're consistent

        * instructors only

    * Add a new empty group up to some (configurable) limit

    * Email a group (plaintext subject & body)

    * Add site (non-roster) member to group

        * TAs can be added more than once

    * Add ad-hoc person to group (?)

  * Syncing roster members

    * User added/dropped from roster

    * Roster attached/detached from site

    * Review groupsync job for other cases

    * Removed users cleared from seats/meetings/groups

    * Added users assigned to smallest group & meetings

        * What if incoming roster user netid matches a manual add?  Do
          we just skip over it?

    * email when users added to a group due to roster changes (post-split)
