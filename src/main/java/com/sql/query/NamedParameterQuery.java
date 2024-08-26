package com.sql.query;

import com.sql.entity.JsqlParserResult;
import com.sql.entity.Param;
import com.sql.parser.JsqlParser;
import com.sql.parser.MyExpressionDeParser;
import com.sql.parser.SqlCondition;
import com.sql.utils.ParamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Myron
 * @since 2022/7/7 11:34
 */
@Slf4j
public class NamedParameterQuery {

    /**
     * 解析SQL，移除SQL中不存在值的参数（使用${}或#{}标识），移除条件为：SQL中的参数据在params中不存在键
     *
     * @param namedParameterJdbcTemplate
     * @param sql
     * @param params                     sql中参数名称以及对应的值
     * @return
     * @throws Exception
     */
    public static List<Map<String, Object>> queryForList(NamedParameterJdbcTemplate namedParameterJdbcTemplate, String sql, Map<String, Object> params) throws Exception {
        if (namedParameterJdbcTemplate == null) {
            throw new IllegalArgumentException("参数namedParameterJdbcTemplate为空");
        }
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("参数sql为空");
        }

        //解析传入SQL
        JsqlParserResult jsqlParserResult = JsqlParser.parserSql(sql, params);
        if (jsqlParserResult != null) {
            //解析后的SQL
            String newSql = jsqlParserResult.getNewSql();
            MyExpressionDeParser expressionDeParser = (MyExpressionDeParser) jsqlParserResult.getExpressionDeParser();
            //解析出SQL中的条件及对应的表达式
            Map<String, SqlCondition> paramExpression = expressionDeParser.paramExpression;
            //NamedParameterJdbcTemplate 查询的SQL   参数用 :参数名  标识
            String namedParameterSql = newSql;
            log.info("\nSQL\n{}\n解析后为\n{}", sql, newSql);
            //解析  解析后SQL仍存在的参数参数名称
            List<Param> paramList = ParamUtil.parseAllParam(newSql);
            Map<String, Object> namedParameterMap = new LinkedHashMap<>();
            if (!CollectionUtils.isEmpty(paramList)) {
                for (int i = paramList.size() - 1; i >= 0; i--) {
                    Param param = paramList.get(i);
                    String paramName = param.getName();
                    Object value = params.get(paramName);
                    //处理like查询的 %
                    if (paramExpression.containsKey(paramName)
                            && paramExpression.get(paramName).isLikeCondition()) {
                        String expression = param.getExpression();
                        //查询like表达式%开头，但值不是%开头，则值的左边加上%
                        if (expression.startsWith("%") && !value.toString().startsWith("%")) {
                            value = "%" + value;
                        }
                        //查询like表达式%结尾，但值不是%结尾，则值的右边加上%
                        if (expression.endsWith("%") && !value.toString().endsWith("%")) {
                            value = value + "%";
                        }
                        log.info("处理{}参数like查询，原表达式为：{}，处理后值为：{}", paramName, expression, value);
                    }
                    namedParameterMap.put(paramName, value);
                    namedParameterSql = namedParameterSql.substring(0, param.getBeginIndex()) + ":" + paramName + namedParameterSql.substring(param.getEndIndex());
                }
            }
            log.info("\nSQL\n{}\n替换为NamedParameterJdbcTemplate查询语句为\n{}", newSql, namedParameterSql);
            //执行查询
            List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(namedParameterSql, namedParameterMap);
            return list;
        }
        return null;
    }
}
