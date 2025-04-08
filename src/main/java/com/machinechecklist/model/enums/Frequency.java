package com.machinechecklist.model.enums;

public enum Frequency {
    DAILY("ทุกวัน"),
    WEEKLY("1 ครั้ง/สัปดาห์");

    private final String description;

    Frequency(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
