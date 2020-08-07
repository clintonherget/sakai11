package edu.nyu.classes.seats.models;

import java.util.Locale;

public class Member {
    public enum Modality {
        ONLINE,
        IN_PERSON,
        UNSURE,
    }

    public String netid;
    public Modality modality;
    public boolean official;
    public Role role;

    public enum Role {
        INSTRUCTOR,
        COURSE_SITE_ADMIN,
        STUDENT,
        TEACHING_ASSISTANT;

        public static Role forCMRole(String cmRole) {
            cmRole = cmRole.toUpperCase(Locale.ROOT);

            if ("I".equals(cmRole)) {
                return INSTRUCTOR;
            } else if ("S".equals(cmRole)) {
                return STUDENT;
            } else if ("A".equals(cmRole)) {
                return COURSE_SITE_ADMIN;
            } else {
                // Shouldn't happen, but student is a safe default.
                return STUDENT;
            }
        }
    }

    public Member(String netid, boolean official, Role role) {
        this.netid = netid;
        this.modality = Modality.values()[(int)Math.floor(Math.random() * 3)];
        this.official = official;
        this.role = role;
    }

    public int hashCode() {
        return netid.hashCode();
    }

    public boolean equals(Object other) {
        if (!(other instanceof Member)) {
            return false;
        }

        return ((Member) other).netid.equals(netid);
    }

    public boolean isInstructor() {
        return Role.INSTRUCTOR.equals(role) || Role.COURSE_SITE_ADMIN.equals(role);
    }
}
