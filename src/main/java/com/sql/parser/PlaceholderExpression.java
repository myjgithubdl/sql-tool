package com.sql.parser;

import com.sql.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;

import java.util.*;

/**
 * @author Myron
 * @since 2022/6/20 16:54
 */
@Slf4j
public class PlaceholderExpression {


    private static final Set<String> EXPRESSION_CONDITION = Collections.synchronizedSet(new LinkedHashSet<>());

    private static final Column placeholderColumn = Constant.SQL_PLACEHOLDER_COLUMN;
    private static final LongValue placeholderValue = Constant.SQL_PLACEHOLDER_VALUE;
    private static final AndExpression AND_EXPRESSION = new AndExpression();
    private static final OrExpression OR_EXPRESSION = new OrExpression();

    /**
     * 获取所有的 占位符固定条件表达式
     *
     * @return
     */
    public static Set<String> getAllExpressCondition() {
        if (EXPRESSION_CONDITION.size() > 0) {
            return EXPRESSION_CONDITION;
        }

        synchronized (EXPRESSION_CONDITION) {
            if (EXPRESSION_CONDITION.size() > 0) {
                return EXPRESSION_CONDITION;
            }

            //处理相等比较谓词条件 ==  > >= < <= !=  <>
            EqualsTo equalsTo = new EqualsTo();
            GreaterThan greaterThan = new GreaterThan();
            GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
            MinorThan minorThan = new MinorThan();
            MinorThanEquals minorThanEquals = new MinorThanEquals();
            GeometryDistance geometryDistance = new GeometryDistance();
            NotEqualsTo notEqualsTo = new NotEqualsTo();
            binaryExpressionLeftRightPlaceAndOr(equalsTo);
            binaryExpressionLeftRightPlaceAndOr(greaterThan);
            binaryExpressionLeftRightPlaceAndOr(greaterThanEquals);
            binaryExpressionLeftRightPlaceAndOr(minorThan);
            binaryExpressionLeftRightPlaceAndOr(minorThanEquals);
            binaryExpressionLeftRightPlaceAndOr(geometryDistance);
            binaryExpressionLeftRightPlaceAndOr(notEqualsTo);

            // between 条件
            Between between = new Between();
            between.setLeftExpression(placeholderColumn);
            between.setBetweenExpressionStart(placeholderValue);
            between.setBetweenExpressionEnd(placeholderValue);
            addPlaceAndOr(between);

            between.setNot(true);//处理 NOT BETWEEN
            addPlaceAndOr(between);


            // like 、 ilike 条件 以及 not like 、not ilike
            LikeExpression likeExpression = new LikeExpression();
            binaryExpressionLeftRightPlaceAndOr(likeExpression);

            likeExpression.setNot(true);
            binaryExpressionLeftRightPlaceAndOr(likeExpression);

            likeExpression.setNot(false);
            likeExpression.setCaseInsensitive(true);
            binaryExpressionLeftRightPlaceAndOr(likeExpression);

            likeExpression.setNot(true);
            binaryExpressionLeftRightPlaceAndOr(likeExpression);

            //处理In表达式
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(placeholderColumn);
            ExpressionList expressionList = new ExpressionList();
            List<Expression> expressions = new ArrayList<>();
            expressions.add(placeholderValue);
            expressionList.setExpressions(expressions);
            inExpression.setRightItemsList(expressionList);
            addPlaceAndOr(inExpression);
            inExpression.setNot(true);
            addPlaceAndOr(inExpression);

        }
        return EXPRESSION_CONDITION;
    }

    /**
     * 二分表达式设置左右为占位  并添加 and 和 or 条件
     *
     * @param binaryExpression
     */
    private static void binaryExpressionLeftRightPlaceAndOr(BinaryExpression binaryExpression) {
        if (binaryExpression != null) {
            binaryExpression.setLeftExpression(placeholderColumn);
            binaryExpression.setRightExpression(placeholderValue);
            addPlaceAndOr(binaryExpression);
        }
    }

    /**
     * 添加 and 和 or 条件
     *
     * @param expression
     */
    private static void addPlaceAndOr(Expression expression) {
        if (expression != null) {
            String andBinaryExpression = AND_EXPRESSION.getStringExpression() + " " + expression;
            String orBinaryExpression = OR_EXPRESSION.getStringExpression() + " " + expression;
            log.info("条件：{}", andBinaryExpression);
            log.info("条件：{}", orBinaryExpression);
            EXPRESSION_CONDITION.add(andBinaryExpression);
            EXPRESSION_CONDITION.add(orBinaryExpression);
        }

    }


    /**
     * sql中的占位符固定条件表达式替换为 空字符
     *
     * @param sql
     * @return
     */
    public static String removePlaceCondition(String sql) {
        if (sql == null) {
            return null;
        }

        Set<String> allExpressCondition = getAllExpressCondition();
        for (String expression : allExpressCondition) {
            while (sql.contains(expression)) {// 循环 单个替换，如果用replaceAll会再存在()的情况下可能替换后不正确
//                System.out.println("sql.contains("+expression+"):"+sql.contains(expression));
//                System.out.println("替换前："+sql);
                sql = sql.replace(expression + " ", "");//防止替换后出现连续两个或多个空格
                sql = sql.replace(expression, "");
//                System.out.println("替换后："+sql);
            }
        }
        return sql;
    }


}
