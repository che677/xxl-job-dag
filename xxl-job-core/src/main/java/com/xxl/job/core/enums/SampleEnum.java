package com.xxl.job.core.enums;

public enum SampleEnum {

    EVENT_HANDLER("eventHandler");

    private String title;
    private SampleEnum (String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }

}
