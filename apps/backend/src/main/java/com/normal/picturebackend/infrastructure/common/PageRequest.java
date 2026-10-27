package com.normal.picturebackend.infrastructure.common;

import lombok.Data;

/**
 * 分页请求基本参数
 *
 * @author normal
 * @date 2025年04月15日 下午12:44
 */
@Data
public class PageRequest {

    /**
     * 当前页码（默认第一页）
     */
    private int currentNum = 1;

    /**
     * 每页数量大小（默认 10 条）
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式 默认 降序排序(desc)
     *
     */
    private String sortOrder = "desc";
}
