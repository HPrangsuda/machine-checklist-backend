package com.machinechecklist.dto;

import lombok.Data;

@Data
public class ChecklistItemDTO {
    private Long id;
    private String questionDetail;
    private String answerChoice;
    private Boolean checkStatus;

    public ChecklistItemDTO() {}

    public ChecklistItemDTO(Long id, String questionDetail, String answerChoice, Boolean checkStatus) {
        this.id = id;
        this.questionDetail = questionDetail;
        this.answerChoice = answerChoice;
        this.checkStatus = checkStatus;
    }
}