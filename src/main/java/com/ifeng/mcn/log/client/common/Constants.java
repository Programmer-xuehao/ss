package com.ifeng.mcn.log.client.common;

/**
 *  全局变量
 *
 * @author chenghao1
 * @create 2019/2/21
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class Constants {

    //获取需要监控的 queue 队列
    public static  final String QUEEN_CONFIG = "spider_script.spider_queue_config" ;
    //  计算配置
    public static  final String CAL_CONFIG = "spider_script.spider_cal_config" ;
    // 监控队列 前缀
    public static  final String QUEEN_PREFIX = "spider:{0}:queue" ;

    public static  final String SPLIT_TAG="ifeng:split:tag";

    public static final  String  SCRIPT_CONFIG="spider_script.sspider_script_config" ;

    public static  final String NLP_LIST="spider:nlp:lists" ;

    public static  final String NLP_LY_LIST="spider:lynlp:lists" ;




    public static final  String  _CONFIG="spider_script.sspider_script_config" ;

    public static final  String  BAIDU_LIMIT="spider:baidu:limit" ;

    public static final  String  ZMT_LINK="http://local.Integration.fhh.ifengidc.com/stream/spider/article?__token=spiderandtrancode" ;






    public static final String ARTICLE_MAPPING="{\"properties\":{\"areaStr\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"articletags\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"classificationCode\":{\"type\":\"keyword\"},\"commentNum\":{\"type\":\"long\"},\"createTime\":{\"type\":\"date\",\"format\":\"epoch_millis\"},\"downloadUrl\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"forwardNum\":{\"type\":\"long\"},\"hasOriginal\":{\"type\":\"boolean\"},\"imgList\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"introduction\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"isGallery\":{\"type\":\"boolean\"},\"isOthenLink\":{\"type\":\"boolean\"},\"likeNum\":{\"type\":\"long\"},\"mediaAccountId\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"mediaAccountName\":{\"type\":\"keyword\"},\"oriUrl\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"pubDateStr\":{\"type\":\"keyword\"},\"publishTime\":{\"type\":\"date\",\"format\":\"epoch_millis\"},\"readNum\":{\"type\":\"long\"},\"score\":{\"type\":\"float\"},\"shape\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"siteCode\":{\"type\":\"keyword\"},\"store\":{\"type\":\"long\"},\"title\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"type\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"updateTime\":{\"type\":\"date\",\"format\":\"epoch_millis\"},\"vertical\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"videoPage\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"viedoUrls\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}},\"wemediaIdcId\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"ignore_above\":256}}}}}"  ;

    public static final String ACCOUNT_MAPPING="{\n" +
            "\t\"properties\": {\n" +
            "\n" +
            "\t\t\"type\": {\n" +
            "\t\t\t\"type\": \"keyword\"\n" +
            "\t\t},\n" +
            "\t\t\"verticle\": {\n" +
            "\t\t\t\"type\": \"keyword\"\n" +
            "\t\t},\n" +
            "\t\t\"shape\": {\n" +
            "\t\t\t\"type\": \"keyword\"\n" +
            "\t\t},\n" +
            "\t\t\"insertTime\": {\n" +
            "\t\t\t\"type\": \"date\",\n" +
            "\t\t\t\"format\": \"epoch_second\"\n" +
            "\t\t},\n" +
            "\t\t\"updateTime\": {\n" +
            "\t\t\t\"type\": \"date\",\n" +
            "\t\t\t\"format\": \"epoch_second\"\n" +
            "\t\t},\n" +
            "\t\t\"lastPublishTime\": {\n" +
            "\t\t\t\"type\": \"date\",\n" +
            "\t\t\t\"format\": \"epoch_second\"\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}" ;



    // 代理服务器
    public final static  String proxyHost = "http-pro.abuyun.com";
    public final static Integer proxyPort = 9010;

    // 代理隧道验证信息
    public final static String proxyUser = "H0ACZ4092DT9T3KP";
    public final static String proxyPass = "DD35428A12203C95";

    public static final String MAX_SCORE="spider:score:{0}" ;

    public static String TASK_PUSH_URL="" ;

    public static final String  DAY_FORMAT="yyyyMMdd" ;
    public static final String  LOF_DAY_FORMAT="yyyyMMdd" ;
    public static final String  MONTH_FORMAT="yyyyMM" ;

    public static final String ES_PUT_URL="http://10.95.0.40:9200/{0}" ;






    public final static String   TARS_CONFIG="<tars>\n" +
            "  <application>\n" +
            "\tenableset                      = N\n" +
            "\tsetdivision                    = NULL\n" +
            "    <client>\n" +
            "        locator                     = tars.tarsregistry.QueryObj@tcp -h 10.80.90.150 -p 17890\n" +
            "        connect-timeout             = 3000\n" +
            "        connections                 = 4\n" +
            "        sync-invoke-timeout         = 30000\n" +
            "        async-invoke-timeout        = 5000\n" +
            "        refresh-endpoint-interval   = 60000\n" +
            "        stat                        = tars.tarsstat.StatObj\n" +
            "        property                    = tars.tarsproperty.PropertyObj\n" +
            "        #report time interval\n" +
            "        report-interval             = 60000\n" +
            "    </client>\n" +
            "  </application>\n" +
            "</tars>" ;






}
