package com.sql.constant;


import com.sql.utils.ParamUtil;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Column;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Myron
 */
public class Constant {

    /**
     * SQL 剔除条件占位
     */
    public static final String SQL_ELI_CONDI_PLACEHOLDER = "-0000";

    public static final Column SQL_PLACEHOLDER_COLUMN = new Column(SQL_ELI_CONDI_PLACEHOLDER + "");

    public static final LongValue SQL_PLACEHOLDER_VALUE = new LongValue(SQL_ELI_CONDI_PLACEHOLDER);

    /**
     * 匹配 形如 ${}  #{}  '${}'  '#{}' '%${}%'  '%#{}%'  的参数
     */
    public static final String[] REG = {"'?%?\\$\\{(.*?)\\}%?'?", "'?%?\\#\\{(.*?)\\}%?'?"};

    //public static final Pattern pattern = Pattern.compile(PARAM_REG, Pattern.DOTALL);
    /**
     * 匹配 形如 ${}  #{}  '${}'  '#{}' 的参数 的正则表达式
     */
    public static final List<Pattern> PATTERN_LIST = Collections.synchronizedList(new ArrayList<>(REG.length));

    static {
        synchronized (ParamUtil.class) {
            for (String reg : REG) {
                PATTERN_LIST.add(Pattern.compile(reg, Pattern.DOTALL));
            }
        }
    }
}
