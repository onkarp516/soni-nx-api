package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class SalarySlipDTO {
    private double ctcAmount;
    private double basicPer;
    private double basic;
    private double pf1Per;
    private double pf1;
    private double total1;
    private double esi1Per;
    private double esi1;
    private double total2;
    private double perDay;
    private double perHour;
    private double pf2Per;
    private double pf2;
    private double esi2Per;
    private double esi2;
    private double pfTax;
    private double netSalary;
    private double point;
}
