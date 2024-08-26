package com.sql.parser;

import com.sql.entity.Param;
import lombok.*;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;

/**
 * @author Myron
 * @since 2022/6/21 16:22
 */
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SqlCondition {

    /**
     * 查询条件列名称
     */
    private String name;

    /**
     * where查询条件表达式
     */
    private Expression expression;

    /**
     * sql中的参数信息
     */
    private Param param;

    /**
     * 是否是like查询条件
     *
     * @return
     */
    public boolean isLikeCondition() {
        return expression == null ? false : expression instanceof LikeExpression;
    }

    public boolean isInCondition() {
        return expression == null ? false : expression instanceof InExpression;
    }


}
