package org.sakaiproject.gradebookng.business;

public enum GbEvent {
    ADD_ASSIGNMENT("gradebook.newItem"),
    UPDATE_ASSIGNMENT("gradebook.updateAssignment"),
    DELETE_ASSIGNMENT("gradebook.deleteItem"),
    UPDATE_GRADE("gradebook.updateItemScore"),
    UPDATE_GRADES("gradebook.updateItemScores"),            // TODO
    UPDATE_UNGRADED("gradebook.updateUngradedScores"),
    UPDATE_COMMENT("gradebook.comment"),
    UPDATE_COURSE_GRADES("gradebook.updateCourseGrades"),   // TODO
    DOWNLOAD_COURSE_GRADE("gradebook.downloadCourseGrade"), // TODO
    DOWNLOAD_ROSTER("gradebook.downloadRoster"),            // TODO
    IMPORT_ENTIRE("gradebook.importEntire"),                // TODO
    IMPORT_ITEM("gradebook.importItem"),                    // TODO
    STUDENT_VIEW("gradebook.studentView"),
    EXPORT("gradebook.export"),
    IMPORT_BEGIN("gradebook.importBegin"),
    IMPORT_COMPLETED("gradebook.importCompleted"),
    OVERRIDE_COURSE_GRADE("gradebook.overrideCourseGrade"),
    UPDATE_SETTINGS("gradebook.updateSettings");

    private String event;

    GbEvent(final String event) {
        this.event = event;
    }

    public String getEvent() {
        return this.event;
    }
}
