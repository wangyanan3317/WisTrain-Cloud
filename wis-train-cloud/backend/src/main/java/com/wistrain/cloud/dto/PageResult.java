package com.wistrain.cloud.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页响应（7.1 接口约定）：data = { total, items }
 */
@Data
public class PageResult<T> {
    private long total;
    private List<T> items;

    public static <T> PageResult<T> of(long total, List<T> items) {
        PageResult<T> p = new PageResult<>();
        p.total = total;
        p.items = items;
        return p;
    }
}
