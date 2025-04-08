package com.machinechecklist.dto;

import lombok.Data;

import lombok.Data;

@Data
public class ChecklistItemDTO {
    private String questionDetail;
    private String answerChoice;

    public ChecklistItemDTO() {}

    public ChecklistItemDTO(String questionDetail, String answerChoice) {
        this.questionDetail = questionDetail;
        this.answerChoice = answerChoice;
    }
}