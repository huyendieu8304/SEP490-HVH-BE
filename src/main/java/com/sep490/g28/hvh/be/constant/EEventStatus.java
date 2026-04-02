package com.sep490.g28.hvh.be.constant;

public enum EEventStatus {
    EDITING, //aka drafted
    SUBMITTED, //host submit event, waiting for approval from manager
    APPROVED_BY_MNG, //manager approve, waiting for approval from admin
    REJECTED_BY_MNG,
    REJECTED_BY_AD,
    RECRUITING,
    UPCOMING,
    ONGOING,
    ENDED,
    COMPLETED,
    CANCELLED;

    public static boolean canEventBeEdited(EEventStatus status) {
        return  (status.equals(EDITING) || status.equals(REJECTED_BY_MNG) || status.equals(REJECTED_BY_AD));
    }

    public static boolean canEventBeUpdated(EEventStatus status) {
        return  (status.equals(RECRUITING) );
    }

    public static boolean canEventBeCancelled(EEventStatus status) {
        return  (status.equals(RECRUITING) || status.equals(UPCOMING) || status.equals(ONGOING));
    }

    public static boolean canEventApplicationBeCancelledByVolunteer(EEventStatus status) {
        return  (status.equals(EDITING)
                || status.equals(SUBMITTED)
                || status.equals(APPROVED_BY_MNG)
                || status.equals(REJECTED_BY_MNG)
                || status.equals(REJECTED_BY_AD)
                || status.equals(RECRUITING)
                || status.equals(UPCOMING)
                || status.equals(ONGOING)
        );
    }

    public static boolean canEventApplicationBeProcessedByHost(EEventStatus status) {
        return  (status.equals(RECRUITING) || status.equals(UPCOMING));
    }

    public static boolean canEventBeAssignedHost(EEventStatus status) {
        return  (status.equals(RECRUITING) || status.equals(UPCOMING) || status.equals(ONGOING));
    }
}
