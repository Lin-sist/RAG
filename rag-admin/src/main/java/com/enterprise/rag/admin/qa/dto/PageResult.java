package com.enterprise.rag.admin.qa.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分页结果
 */
@Data
@Builder
public class PageResult<T> {

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        int totalPages = (int) Math.ceil((double) total / size);
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .build();
    }
}
