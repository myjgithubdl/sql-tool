package com.sql.parser;

import com.sql.entity.JsqlParserResult;
import com.sql.utils.ParamUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

import java.util.Map;

/**
 * @author Myron
 * @since 2022/6/10 9:15
 */
public class JsqlParser {

    public static JsqlParserResult parserSql(String sql, Map<String, Object> params) throws JSQLParserException {

        if (sql == null) {
            sql = "select null as nn , ${col_a} as ca, ${col_b} , concat(${col_c},${col_d},'a') as ca from emps e where a=${a}" +
//                    " and 1=1 " +
                    "  and id = '${id}'  " +
                    " and age>= ${ageGe}" +
                    " and age>= 10" +
                    " and age<= ${ageLe}' " +
                    " and age<= 20 " +
                    " and name='${name}' " +
                    " and city in (select city from area where 1=1 and province='${province}') " +
//                    " and name  in ('${nameIn}') " +
                    " and account like '%#{account}%' " +
                    " and age between 10 and 20 " +
                    " and age2 between '${betweenStart}' and '${betweenEnd}' " +
//                    " or addr='${addr}' " +
                    //                   " or addr like concat('%' , '${addr}' , '%') " +
                   " and exists (select 1 from dept d where e.dept_id=d.id and status=1 and is_del='${isDel}')" +
                    "";
        }
        MyExpressionDeParser expressionDeParser = new MyExpressionDeParser(params);
        JsqlParserResult jsqlParserResult = JsqlParserResult.builder().originalSql(sql).expressionDeParser(expressionDeParser).build();
        System.out.println("原始SQL:" + sql);
        sql = ParamUtil.ensureParamSingleQuote(sql);

        if (1 == 2) {
            return null;
        }

        Statement statement = CCJSqlParserUtil.parse(sql);
        String respSql = null;
        String expressionSql = null;

        System.out.println("单引号处理后SQL:" + statement.toString());

        if (statement instanceof Select) {
            Select select = (Select) statement;


            SelectBody selectBody = select.getSelectBody();

            expressionDeParser.selectBody=selectBody;

            expressionDeParser.dealSelect(select);

            respSql = PlaceholderExpression.removePlaceCondition(statement.toString());
        }
        System.out.println("sql      ：" + sql);
        System.out.println("statement：" + statement);
        System.out.println("respSql：" + respSql);
        jsqlParserResult.setNewSql(respSql.toString());
        return jsqlParserResult;
    }


}
