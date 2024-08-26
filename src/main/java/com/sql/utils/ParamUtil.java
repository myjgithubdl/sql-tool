package com.sql.utils;

import com.sql.constant.Constant;
import com.sql.entity.Param;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数处理工具类
 *
 * @author bdf
 */
public class ParamUtil {




    /**
     * 解析字符串中所有的参数
     * 如str为:${a}b${c}则返回a、c
     *
     * @param str
     * @return
     */
    public static List<Param> parseAllParam(String str) {
        if (str != null) {
            List<Param> respList = new LinkedList<>();
            for (Pattern pattern : Constant.PATTERN_LIST) {
                Matcher matcher = pattern.matcher(str);
                while (matcher.find()) {
                    String name = matcher.group(1);
                    int start = matcher.start();
                    int end = matcher.end();
                    String paramExp2 = str.substring(matcher.start(), matcher.end());
                    String paramExp = paramExp2;

                    if (paramExp.startsWith("'")) {
                        paramExp = paramExp.substring(1);
                    }
                    if (paramExp.endsWith("'")) {
                        paramExp = paramExp.substring(0, paramExp.length() - 1);
                    }
                    Param param = Param.builder().name(name).beginIndex(start).endIndex(end).expression(paramExp).expression2(paramExp2).build();
                    respList.add(param);
                }
            }
            return respList.size() == 0 ? null : respList;
        }
        return null;
    }

    /**
     * 确保参数前后是单引号，没有单引号的加上单引号
     *
     * @param str
     * @return
     */
    public static String ensureParamSingleQuote(String str) {
        if (str != null) {
            List<Param> paramList = ParamUtil.parseAllParam(str);
            if (paramList != null) {
                int size = paramList.size();
                for (int index = size - 1; index >= 0; index--) {
                    Param param = paramList.get(index);
                    int beginIndex = param.getBeginIndex();
                    int endIndex = param.getEndIndex();
                    String expression2 = param.getExpression2();
                    if(!expression2.endsWith("'")){
                        str=str.substring(0,endIndex ) +"'"  +str.substring( endIndex )  ;
                    }
                    if(!expression2.startsWith("'")){
                        str=str.substring(0,beginIndex ) +"'"  +str.substring( beginIndex )  ;
                    }
                }
            }
        }
        return str;
    }


}
