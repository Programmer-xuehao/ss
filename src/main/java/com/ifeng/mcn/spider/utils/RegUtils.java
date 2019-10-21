package com.ifeng.mcn.spider.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则 清洗 集合
 *
 * @author chenghao1
 * @create 2019/3/1
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class RegUtils {

    public static List<String> replaceUrl(List<String> urls) {
        List<String> rt = new ArrayList<String>();
        for (String u : urls){
            rt.add(replaceUrl(u));
        }
        return rt;
    }

    public static String replaceUrl(String url) {
        url = url.replaceAll("&#39;","'");
        url = url.replaceAll("&quot;","\"");
        url = url.replaceAll("&nbsp;"," ");
        url = url.replaceAll("&gt;",">");
        url = url.replaceAll("&lt;","<");
        url = url.replaceAll("&yen;","¥");
        url = url.replaceAll("&amp;amp;", "&");
        url = url.replaceAll("&amp;", "&");
        return url;
    }

    public static String getEscapeXpath(String rule){
        if(StringUtils.isNotBlank(rule)){
            //  对  rule 进行转义"& #40; ( "& #41;  )    & #39;    ''
            rule = rule.replace("& #39;","'").replace("&#39;","'").
                    replace("& #40;","(").replace("&#10;","(").
                    replace("& #41;",")"). replace("&#41;",")") ;
            return rule ;
        }else{
            return null ;
        }
    }

    static public List<String> getSubUtil(String soap, String rgex){
        List<String> list = new ArrayList<String>();
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            int i = 1;
            list.add(m.group(i));
            i++;
        }
        return list;
    }

    static public String  escapeDollar(String msg,String replaceMent){
        if(StringUtils.isBlank(msg)){
            return "" ;
        }
        return msg.replaceAll("\\$",replaceMent) ;
    }

    public static String replaceAnd(String content) {
        if (StringUtils.isBlank(content)){
            return content;
        }
        return content.replace("&amp;", "&");
    }
}
