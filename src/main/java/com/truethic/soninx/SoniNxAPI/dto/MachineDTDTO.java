package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class MachineDTDTO {
    private Long id;
    private String name;
    private String number;
    private String dateOfPurchase;
    private Integer cost;
    private String whatMachineMakes;
    private Boolean isMachineCount;
    private Long currentMachineCount;
    private Long defaultMachineCount;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
