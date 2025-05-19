package com.machinechecklist.model;

import com.machinechecklist.dto.ChecklistItemDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class ChecklistRequestDTO {
    private String machineCode;
    private String machineName;
    private String machineStatus;
    private List<ChecklistItemDTO> checklistItems;
    private String note;
    private String machineImage;
    private String userId;
    private String userName;
    private String supervisor;
    private String manager;
    private String reasonNotChecked;
}
