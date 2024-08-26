package com.sql.entity;

import lombok.*;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * @author Myron
 * @since 2022/6/22 16:36
 */
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JsqlParserResult {

    /**
     * 原传入SQL
     */
    private String originalSql;

    /**
     * 解析出的SQL
     */
    private String newSql;

    /**
     * 使用的解析类，里面可以获取到查询条件表达式
     */
    private ExpressionDeParser expressionDeParser;

}
