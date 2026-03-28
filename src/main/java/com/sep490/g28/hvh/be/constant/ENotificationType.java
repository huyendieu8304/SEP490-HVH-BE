package com.sep490.g28.hvh.be.constant;

public enum ENotificationType {
    //EVENT
    MNG_EVENT_CREATED, //sent to mng, event created by host

    HOST_EVENT_APPROVED_BY_MNG, //send to host and admin
    ADM_EVENT_APPROVED_BY_MNG, //send to admin

    HOST_EVENT_REJECTED_BY_MNG, //send to host only

    HOST_EVENT_APPROVED_BY_AD, //send to host
    MNG_EVENT_APPROVED_BY_AD, //send to mng

    HOST_EVENT_REJECTED_BY_AD, //send to host and mng
    MNG_EVENT_REJECTED_BY_AD, //send to host and mng


    VOL_APPLICATION_APPROVED, //send to vol
    VOL_APPLICATION_REJECTED, //send to vol
    VOL_APPLICATION_CANCELLED, //send to vol

    VOL_EVENT_ANNOUNCEMENT,
    VOL_EVENT_CANCELLED_BY_HOST,
    VOL_EVENT_CANCELLED_BY_AD,
}
