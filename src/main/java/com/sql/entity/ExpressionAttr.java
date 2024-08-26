package com.sql.entity;

import lombok.*;
import net.sf.jsqlparser.expression.Expression;

/**
 * @author Myron
 * @since 2022/6/24 16:50
 * 表达式节点属性
 */
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExpressionAttr {

    private Expression expression;

    /**
     * 是否为根节点
     */
    private boolean root;

    /**
     * 是否为左节点
     */
    private boolean left;

    /**
     * 是否为叶子节点
     */
    private boolean rigth;


}
