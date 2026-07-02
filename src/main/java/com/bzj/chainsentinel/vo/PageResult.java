package com.bzj.chainsentinel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    private List<T> records;

    private Long total;

    private Long page;

    private Long size;
}