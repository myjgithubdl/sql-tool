package com.sql.entity;

import lombok.*;

/**
 * @author Myron
 * @since 2022/6/21 17:17
 */
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Param {

    /**
     * 查询条件列名称
     */
    private String name;

    /**
     * 表达式参数的起始字符位置
     */
    private int beginIndex = -1;

    /**
     * 表达式参数的结束字符位置
     */
    private int endIndex = -1;

    /**
     * 参数表达式(前后存在单引号的情况 不带单引号)
     */
    private String expression;

    /**
     * 参数表达式(前后存在单引号的情况 带单引号)
     */
    private String expression2;

    /**
     * 参数值
     */
    private Object value;

}
