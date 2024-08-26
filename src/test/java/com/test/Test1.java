package com.test;


import com.alibaba.druid.pool.DruidDataSource;
import com.sql.entity.JsqlParserResult;
import com.sql.parser.JsqlParser;
import com.sql.query.NamedParameterQuery;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test1 {


    public static void main(String[] args) throws JSQLParserException {
        //test2();

    }

    @Test
    public void test1() throws JSQLParserException {
        Map<String, Object> params = new HashMap<>();
        //  params.put("name","Myron");
        params.put("account", "account");
        //   params.put("province","province");
        params.put("ageLe", "ageLe");
        params.put("isDel", "isDel");
        params.clear();
        String sql = null;
        //sql = "select * from dim_area_cn where 1=1 and  province=1 and a=${a}  and city in (2)    ";
        //sql = "select * from dim_area_cn where 1=1   and a=${a}      ";
        //sql = "select * from fact_air_cn where c=${c} or name in (2) and a=${a} and b=${b} and dt between '2014-01-01' and '2014-01-15' and area in ('${city}')  ";
        sql = "SELECT NULL AS prodname, class, lastday, today, lastmonth, thismonth, thisyear, history, dt FROM bi.rpt_crm_report_fund_d WHERE grp IN (substr('${prodname}', 1, 2), 'disp') AND substr(class, 1, 2) IN ('00', '01', '02', '03', '04', '05', '06') ORDER BY class, grp ";
        sql = "select t1.pack_id , t1.title , t1.tel_count,t2.send_type,t2.is_send,t2.send_time , t2.sms_count ,t2.sms_success_count,\n" +
                "t2.sms_fail_count ,\n" +
                "concat(round(sms_success_count /(sms_success_count+sms_fail_count)*100 ,2), '%') success_per ,\n" +
                "concat(round(sms_fail_count /(sms_success_count+sms_fail_count)*100,2) , '%') fail_per\n" +
                "from (\n" +
                "\tselect tu.id , concat('A',id) pack_id , tu.title , tu.telCount tel_count , tu.uploadTelTime  upload_date, tu.queryFlag query_flag\n" +
                "\tfrom tel_upload tu\n" +
                "\tUNION ALL \n" +
                "\tSELECT lqs.id, concat('B',id) pack_id ,query_name title , lqs.sms_count tel_count, lqs.create_time upload_date, lqs.query_flag  query_flag \n" +
                "\tFROM label_query_his lqs  \n" +
                "\tWHERE lqs.app_state='审批通过' \n" +
                ") t1 , (\n" +
                "\tSELECT case when s.label_query_id >0 then concat('B',s.label_query_id) ELSE concat('A',s.tel_upload_id) END pack_id ,\n" +
                "\tsend_type , is_send ,  substr(replace(s.send_time,'-',''),1,8) send_time ,\n" +
                "\tsum(s.sms_count) sms_count , sum(s.sms_success_count) sms_success_count , sum(s.sms_fail_count ) sms_fail_count\n" +
                "\tFROM sms s left join sms_content sc on s.sms_content_id=sc.id\n" +
                "\twhere 1=1 \n" +
                "\n" +
                "  \n" +
                "\tand sc.send_type='${send_type}'\n" +
                "\tand (s.label_query_id='${pack_id}'  or s.tel_upload_id='${pack_id}' )\n" +
                "\tGROUP BY s.label_query_id , s.tel_upload_id , sc.send_type , s.is_send , substr(replace(s.send_time,'-',''),1,8)\n" +
                ") t2\n" +
                "where t1.pack_id=t2.pack_id\n" +
                "and  send_time >='${startTime}'\n" +
                "and send_time <='${endTime}'   \n" +
                "\n" +
                "order by send_time desc , send_type desc";
        sql = "select * FROM sms s where 1=1 and send_type='${send_type}' and (label_query_id='${pack_id}'  or tel_upload_id='${pack_id}' or upload_id=222)";
        sql = "select * FROM sms s where send_type='${send_type' and name='123'  and (label_query_id='{pack_id}'  or tel_upload_id='${pack_id}' or upload_id=3) ";
        sql = "SELECT * FROM (SELECT send_mounth, send_type, sum(sms_count) sms_count FROM (SELECT sms.sms_count, sc.send_type, SUBSTR(sms.send_time, 1, 7) send_mounth FROM tel_upload tu, sms, sms_content sc WHERE sms.tel_upload_id = tu.id AND sms.sms_content_id = sc.id AND sms.send_time >= '${begin_send_time}' AND sms.send_time <= '${end_send_time}' AND sc.send_type = '${send_type}' AND tu.organization_id = '${organizationId}' UNION ALL SELECT sms.sms_count, sc.send_type, SUBSTR(sms.send_time, 1, 7) send_mounth FROM label_query_his lq, sms, sms_content sc WHERE sms.label_query_id = lq.id AND sms.sms_content_id = sc.id AND sms.send_time >= '${begin_send_time}' AND sms.send_time <= '${end_send_time}' AND sc.send_type = '${send_type}' AND lq.organization_id = '${organizationId}') tt GROUP BY send_mounth, send_type) ttt ORDER BY send_mounth DESC ";
        sql = "WITH tb_sms_id AS (SELECT sms_id, count(1) resp_count FROM sms_respond WHERE apply_dt = '${apply_dt}' AND send_dt = '${send_dt}' GROUP BY sms_id) SELECT * FROM (SELECT 'A' || tu.id AS PACK_ID, sms_id, tu.title, t2.resp_count FROM tel_upload tu, tb_sms_id t2, sms s WHERE tu.id = s.tel_upload_id AND s.id = t2.sms_id UNION ALL SELECT 'B' || lq.id AS PACK_ID, sms_id, lq.query_name, t2.resp_count FROM label_query_his lq, tb_sms_id t2, sms s WHERE lq.id = s.label_query_id AND s.id = t2.sms_id) ORDER BY resp_count DESC ";
        sql = "SELECT apply_dt, '实际值' data_type, apply_num data_value FROM bi.rpt_loan_apply_rollup WHERE apply_dt >= '${startTime}' AND apply_dt <= '${endTime}' AND business_type_name = '${business_type_name}' AND loan_term = '${loan_term}' AND channel_belong = '${channel_belong}' AND is_new = '${is_new}' UNION ALL SELECT apply_dt, '预测值' data_type, CAST(pred_num AS int) data_value FROM bi.rpt_loan_apply_monitor WHERE apply_dt >= '${startTime}' AND apply_dt <= '${endTime}' AND business_type_name = '${business_type_name}' AND loan_term = '${loan_term}' AND channel_belong = '${channel_belong}' AND is_new = '${is_new}' ORDER BY apply_dt\n";
        sql = "WITH cte1 AS (SELECT to_date('${startTime}', 'yyyymmdd') - 1 + level AS dt, to_char((to_date('${startTime}', 'yyyymmdd') - 1 + level), 'yyyymm') mt, to_char((to_date('${startTime}', 'yyyymmdd') - 1 + level), 'yyyy') yt FROM dual CONNECT BY level < to_date('${endTime}', 'yyyymmdd') + 2 - to_date('${startTime}', 'yyyymmdd')), cte2 AS (SELECT 'wx' channel_source, '微信' channel_source_name, '01' channel_desc FROM dual UNION ALL SELECT 'sms' channel_source, '短信' channel_source_name, '02' channel_desc FROM dual UNION ALL SELECT 'app' channel_source, 'APP' channel_source_name, '03' channel_desc FROM dual UNION ALL SELECT 'ts' channel_source, '推送' channel_source_name, '04' channel_desc FROM dual UNION ALL SELECT 'tc' channel_source, '弹窗' channel_source_name, '05' channel_desc FROM dual UNION ALL SELECT 'others' channel_source, '其它' channel_source_name, '06' channel_desc FROM dual UNION ALL SELECT 'all' channel_source, '全部' channel_source_name, '07' channel_desc FROM dual), cte3 AS (SELECT dt, mt, yt, channel_source, channel_source_name, channel_desc FROM CTE1 CROSS JOIN CTE2) SELECT to_char(T1.dt, 'yyyymmdd') dt, T1.channel_source_name, coalesce(t2.guide_cnt, 0) guide_cnt, coalesce(t2.login_cnt, 0) login_cnt, coalesce(t2.yh1_cnt, 0) yh1_cnt, coalesce(t2.yh2_cnt, 0) yh2_cnt, coalesce(t2.yh3_cnt, 0) yh3_cnt, coalesce(t2.yh4_cnt, 0) yh4_cnt, coalesce(t2.yh5_cnt, 0) yh5_cnt, coalesce(t2.yh6_cnt, 0) yh6_cnt, coalesce(t2.info1_cnt, 0) info1_cnt, coalesce(t2.info2_cnt, 0) info2_cnt, coalesce(t2.info3_cnt, 0) info3_cnt, coalesce(t2.info4_cnt, 0) info4_cnt, coalesce(t2.info5_cnt, 0) info5_cnt, coalesce(t2.quota_cnt, 0) quota_cnt, coalesce(t2.record_cnt, 0) record_cnt, coalesce(t2.detail_cnt, 0) detail_cnt FROM cte3 t1 LEFT JOIN rpt_click_label_rxh_new t2 ON t1.dt = TO_DATE(t2.dt, 'YYYYMMDD') AND t1.channel_source = t2.data_from AND t2.dt != 'NULL' ORDER BY dt\n";
        sql = "WITH cte1 AS (SELECT NULL AS dt, NULL mt, NULL yt FROM dual CONNECT BY level < to_date('${endTime}', 'yyyymmdd') + 2 - to_date('${startTime}', 'yyyymmdd')) select * from cte1\n";
        // sql="SELECT * FROM (SELECT if(tu_i.pack_num IS NOT NULL, tu_i.pack_num, lh_i.pack_num) pack_num, if(tu_i.pack_name IS NOT NULL, tu_i.pack_name, lh_i.pack_name) pack_name, if(tu_i.dept_name IS NOT NULL, tu_i.dept_name, lh_i.dept_name) dept_name, if(tu_i.pack_number_count IS NOT NULL, tu_i.pack_number_count, lh_i.pack_number_count) pack_number_count, s.tel_upload_id, s.label_query_id, s.is_send, if(s.is_send = 'Y', '是', '否') send_status, s.send_time, substr(s.send_time, 1, 10) send_date, s.unpack_index, s.send_batch_symbol, s.sms_count, s.sms_success_count + s.sms_fail_count actual_send_num, s.sms_success_count, s.sms_fail_count, sc.send_type, CASE WHEN sc.channel = 'lakala' THEN concat('【拉卡拉】', sc.content) WHEN sc.channel = 'yfq' THEN concat('【易分期】', sc.content) WHEN sc.channel = 'yfqdk' THEN concat('【易分期贷款】', sc.content) WHEN sc.channel = 'syd' THEN concat('【生意贷】', sc.content) WHEN sc.channel = 'huarun' THEN concat('【华润信托】', sc.content) ELSE sc.content END content FROM sms s LEFT JOIN sms_content sc ON s.sms_content_id = sc.id LEFT JOIN (SELECT tu.id tel_upload_id, concat('A', tu.id) pack_num, tu.title pack_name, tu.telCount pack_number_count, o.name dept_name FROM tel_upload tu LEFT JOIN organization o ON tu.organization_id = o.id) tu_i ON s.tel_upload_id = tu_i.tel_upload_id LEFT JOIN (SELECT h.id label_query_id, concat('B', h.id) pack_num, h.query_name pack_name, h.sms_count pack_number_count, o.name dept_name FROM label_query_his h LEFT JOIN organization o ON h.organization_id = o.id) lh_i ON s.label_query_id = lh_i.label_query_id WHERE substr(s.send_time, 1, 10) >= '${startSendDate}' AND substr(s.send_time, 1, 10) <= '${endSendDate}' AND if(tu_i.dept_name IS NOT NULL, tu_i.dept_name, lh_i.dept_name) IN ('${deptName}') AND sc.send_type = '${sendType}' ORDER BY s.id DESC) t WHERE t.pack_num = '${packNum}'\n ";
        sql = "SELECT * FROM T WHERE substr(send_time, 1, 10) >= '${startSendDate}' AND substr(send_time, 1, 10) <= '${endSendDate}' AND if(dept_name IS NOT NULL, dept_name, lh_i.dept_name) IN ('${deptName}') AND sc.send_type = '${sendType}' ";
        sql = "SELECT * FROM T WHERE substr(send_time, 1, 10) >= '${startSendDate}' AND substr(send_time, 1, 10) <= '${endSendDate}'";
        sql = " SELECT T1.dt, t1.mt, t1.yt, T1.channel_source_name, coalesce(t2.reg_cnt, 0) reg_cnt, coalesce(t3.apply_cnt, 0) apply_cnt, coalesce(t4.cmplt_t_cnt, 0) cmplt_t_cnt, coalesce(t4.cmplt_f_cnt, 0) cmplt_f_cnt, coalesce(t6.cred_amt, 0) cred_amt, coalesce(t5.pay_cust_cnt, 0) pay_cust_cnt, coalesce(t5.capital_limit, 0) capital_limit FROM cte3 t1 LEFT JOIN rpt_reg_rxh t2 ON t1.dt = TO_DATE(t2.dt, 'YYYYMMDD') AND t1.channel_source = t2.channel_source_name AND t2.dt != 'NULL' LEFT JOIN rpt_apply_rxh t3 ON t1.dt = TO_DATE(t3.dt, 'YYYYMMDD') AND t3.project_type = '${project_type}' AND t1.channel_source = t3.channel_source_name AND t3.dt != 'NULL' LEFT JOIN rpt_cmplt_rxh t4 ON t1.dt = TO_DATE(t4.dt, 'YYYYMMDD') AND t4.project_type = '${project_type}' AND t1.channel_source = t4.channel_source_name AND t4.dt != 'NULL' LEFT JOIN rpt_pay_rxh t5 ON t1.dt = TO_DATE(t5.dt, 'YYYYMMDD') AND t5.project_type = '${project_type}' AND t1.channel_source = t5.channel_source_name AND t5.dt != 'NULL' LEFT JOIN rpt_cred_rxh t6 ON t1.dt = TO_DATE(t6.dt, 'YYYYMMDD') AND t6.project_type = '${project_type}' AND t1.channel_source = t6.channel_source_name AND t6.dt != 'NULL'";
        sql = "select apply_dt,label,label2,`apply`,approved,payment from rpt_secondary_marketing_apply WHERE apply_dt>= ${startTime} AND apply_dt<= ${endTime} order by apply_dt desc ";
        sql = "SELECT * FROM f_evt_market_response_d WHERE dt BETWEEN '${startDate}' AND '${endDate}' AND PACKAGE_ID = '${package_id}' AND instr(title, '${title}') > 0 AND send_type = '${send_type}' AND credit_flag IN ('${credit_flag}') ORDER BY PACKAGE_ID DESC ";
        sql = "SELECT * FROM f_evt_market_response_d WHERE dt BETWEEN '${startDate}' AND '${endDate}' AND PACKAGE_ID = '${package_id}' AND instr(title, '${title}') > 0  ORDER BY PACKAGE_ID DESC ";
        //sql=" ";
        //sql=null;
        JsqlParserResult jsqlParserResult = JsqlParser.parserSql(sql, params);
        System.out.println(jsqlParserResult);
    }

    public static DataSource getDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://myj04:3306/label_test?useUnicode=true&useSSL=false&characterEncoding=utf8");
        dataSource.setUsername("root");
        dataSource.setPassword("Myron*312");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return dataSource;
    }

    @Test
    public void test2() throws JSQLParserException {
        DataSource dataSource = getDataSource();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List<Map<String, Object>> list = template.queryForList("select *  from ezrpt_meta_report_compose ");
        for (Map<String, Object> map : list) {
            String sqlText = map.get("sql_text").toString();
            if (StringUtils.isBlank(sqlText)) {
                continue;
            }
            System.out.println("============" + map.get("id").toString());

            //  System.out.println(sqlText);

            Map<String, Object> params = new HashMap<>();
            //  params.put("name","Myron");
            params.put("account", "account");
            //   params.put("province","province");
            params.put("ageLe", "ageLe");
            params.put("isDel", "isDel");
            JsqlParserResult jsqlParserResult = JsqlParser.parserSql(sqlText, params);
            System.out.println(jsqlParserResult);
            String newSql = jsqlParserResult.getNewSql();
            if (newSql.contains("${") || newSql.contains("#{")) {
                System.out.println("newSql:" + newSql);
                int a = 4 / 0;
                break;
            }
        }

    }

    @Test
    public void test3() throws Exception {
        DataSource dataSource = getDataSource();
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        String sql = "select id ,  ${a} as name , concat(id,name) as `concat`  from ezrpt_meta_report_compose where id=${id} and name like %${name}%";
        sql = "select id ,  ${a} as name , concat(id,name) as `concat`  from ezrpt_meta_report_compose where id in (${id}) or name like %${name}%";
        sql = "select id ,  ${a} as name , concat(id,name) as `concat`  from ezrpt_meta_report_compose where id in (${id}) or name like %${name}%";
        Map<String, Object> params = new HashMap<>();
        params.put("a", "Myron");
        params.put("id", "46");
        params.put("id", Arrays.asList(46,84));
        params.put("name", "天");
        List<Map<String, Object>> list = NamedParameterQuery.queryForList(jdbcTemplate, sql, params);
        System.out.println(list);
    }


    public static void test4() throws JSQLParserException {

    }

    public static void test5() throws JSQLParserException {

    }

    public static void test6() throws JSQLParserException {

    }


    public static void test7() {

    }


    public static void test8() {


    }
}
