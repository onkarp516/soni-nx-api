package com.truethic.soninx.SoniNxAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericData<T> {
    private List<T> data;
    private Long total;//total rows
    private int page; // page number
    private int per_page; // page size/ limit
    private int total_pages; // total pages
}