/*
* CommonUtils.java
* Created on  2019/2/28 9:24
* Copyright © 2012 Phoenix New Media Limited All Rights Reserved
*/
package com.ifeng.mcn.log.client;

import cn.hutool.core.net.URLEncoder;
import com.google.common.collect.Maps;
import com.ifeng.mcn.log.client.common.Constants;
import com.xxl.glue.core.GlueFactory;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gengyl on 2019/2/28.
 */
public class CommonUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtils.class);


    public  static final String reg1 = "<video[^>]*>[\\s\\S]*?<\\/[^>]*video>|<video .*\\/>" ;
    public static Pattern VIDEO_PATTERN = Pattern.compile(reg1) ;
    public  static final String reg2 = "<audio[^>]*>[\\s\\S]*?<\\/[^>]*audio>|<audio .*\\/>" ;
    public static Pattern AUDIO_PATTERN = Pattern.compile(reg2) ;
    public static URLEncoder urlEncoder =  URLEncoder.DEFAULT ;
    static{
        urlEncoder.addSafeCharacter('?');
    }

    // 单个字符的正则表达式
    private static final String singlePattern = "[0-9|a-f|A-F]";
    // 4个字符的正则表达式
    private static final String pattern = singlePattern + singlePattern +
            singlePattern + singlePattern;

    /**
     * 将类似"5.6万"转换为数字“56000”
     * @param plainNumStr
     * @return
     */
    public static int parsePlainNum2RealNum(String plainNumStr) {
        int num = 0;
        if (StringUtils.isBlank(plainNumStr)) {
            return num;
        }
        try {
            if (plainNumStr.contains("亿")) {
                num = (int) (Double.parseDouble(plainNumStr.replace("亿", "")) * 100000000);
            } else if (plainNumStr.contains("千万")){
                num = (int) (Double.parseDouble(plainNumStr.replace("千万", "")) * 10000000);
            } else if (plainNumStr.contains("百万")){
                num = (int) (Double.parseDouble(plainNumStr.replace("百万", "")) * 1000000);
            } else if (plainNumStr.contains("十万")){
                num = (int) (Double.parseDouble(plainNumStr.replace("十万", "")) * 100000);
            } else if (plainNumStr.contains("万")){
                num = (int) (Double.parseDouble(plainNumStr.replace("万", "")) * 10000);
            } else if (plainNumStr.contains("千")){
                num = (int) (Double.parseDouble(plainNumStr.replace("千", "")) * 1000);
            } else if (plainNumStr.contains("百")){
                num = (int) (Double.parseDouble(plainNumStr.replace("百", "")) * 100);
            } else {
                num = Integer.parseInt(plainNumStr);
            }
        } catch (Exception e) {
            LOGGER.error("parsePlainNum2RealNum error, plainNumStr=" + plainNumStr, e);
        }

        return num;
    }




    public static String getExceptionInfo(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        String result = sw.getBuffer().toString();
        String[] bodys = result.split("\n");
        String key = e.getClass().getName() + ": " + e.getMessage() + " ";
        if (bodys.length > 1) {
            String body1 = bodys[1];
            try {
                key = key + body1.substring(body1.indexOf("("), body1.indexOf(")") + 1);
            } catch (Exception e1) {
                key = result;
            }
        } else {
            try{
                key = key + e.getCause().getMessage();
            }catch (Exception e1){
                e1.printStackTrace();
            }
        }
        return key;
    }



    public static String initTaskPush(String taskName , int count) throws  Exception{
       return  HttpClientUtils.getInstance().get(Constants.TASK_PUSH_URL + "/task/initPush?taskName=" + taskName+"&pushCount="+count);
    }



    /**
     * 字符串编码成Unicode编码
     */
    public static String encode(String src) throws Exception {
        char c;
        StringBuilder str = new StringBuilder();
        int intAsc;
        String strHex;
        for (int i = 0; i < src.length(); i++) {
            c = src.charAt(i);
            intAsc = (int) c;
            strHex = Integer.toHexString(intAsc);
            if (intAsc > 128)
                str.append("\\u" + strHex);
            else
                str.append("\\u00" + strHex); // 低位在前面补00
        }
        return str.toString();
    }

    /**
     * Unicode解码成字符串
     * @param src
     * @return
     */
    public static String decode(String src) {
        int t =  src.length() / 6;
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < t; i++) {
            String s = src.substring(i * 6, (i + 1) * 6); // 每6位描述一个字节
            // 高位需要补上00再转
            String s1 = s.substring(2, 4) + "00";
            // 低位直接转
            String s2 = s.substring(4);
            // 将16进制的string转为int
            int n = Integer.valueOf(s1, 16) + Integer.valueOf(s2, 16);
            // 将int转换为字符
            char[] chars = Character.toChars(n);
            str.append(new String(chars));
        }
        return str.toString();
    }

    public static String getConfigByGlue(String key)  throws Exception{
        Map map = Maps.newHashMap() ;
        map.put("config","1") ;

        Object glue = GlueFactory.glue("commons.CommonConfig", map);
        Map<String,String> configMap = (Map<String,String>)glue ;
        return configMap.get(key) ;

    }

    public static String getConfigVal(String key) {

        try {
            Map map = Maps.newHashMap() ;
            map.put("config","1") ;
            Object glue = GlueFactory.glue("commons.CommonConfig", map);
            Map<String,String> configMap = (Map<String,String>)glue ;
            return configMap.get(key) ;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }
        return null ;

    }


    /**
     * 获取 cpname 。
     * @param key
     * @return
     */
    public static String getSiteCodeCpVal(String key) {

        try {
            Map map = Maps.newHashMap() ;
            map.put("cpName","1") ;
            Object glue = GlueFactory.glue("commons.CommonConfig", map);
            Map<String,String> configMap = (Map<String,String>)glue ;
            return configMap.get(key) ;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }
        return null ;

    }





    /**
     * 获取 cpname 。
     * @param key
     * @return
     */
    public static String getConfigCodeVal(String configName, String key) {

        try {
            Map map = Maps.newHashMap() ;
            map.put(configName,"1") ;
            Object glue = GlueFactory.glue("commons.CommonConfig", map);
            Map<String,String> configMap = (Map<String,String>)glue ;
            return configMap.get(key) ;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }
        return null ;

    }


    /**
     * 获取cpname
     * @return
     */
    public static List<String>  getCpList() {

        try {
            Map map = Maps.newHashMap() ;
            map.put("cpList","1") ;
            Object glue = GlueFactory.glue("commons.CommonConfig", map);
            List<String> configMap = (List<String>)glue ;
            return configMap ;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }
        return null ;

    }



    /**
     * 是否 含有viedo  标签
     * @param c
     * @return
     */
    public static boolean hasVideo(String c){
        if(StringUtils.isNotBlank(c)){
            return VIDEO_PATTERN.matcher(c).find() ;
        }else{
            return  false ;
        }

    }

    /***
     * 是否还有   audio 标签
     * @param c
     * @return
     */
    public static boolean hasAudio(String c){

        if(StringUtils.isNotBlank(c)){
            return AUDIO_PATTERN.matcher(c).find() ;

        }else{
            return  false ;
        }
    }


    public static  String getFromByRegAuto(String content){
        content = content.replaceAll("r?n", " ");
        content = content.replace("<span>"," ").replace("</span>"," ") ;
        content  =Jsoup.clean(content,Whitelist.simpleText()) ;
        if (StringUtils.isEmpty(content))
            return content;
        //获取“来源”“稿源”后面的字符（空白符和|前面的字符）
        Pattern p = Pattern.compile("([来|稿]源[:|：].*)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = p.matcher(content);
        List matches;
        if (matcher.find() && matcher.groupCount() >= 1) {
            matches = new ArrayList();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String temp = matcher.group(i);
                matches.add(temp);
            }
        } else {
            matches = Collections.EMPTY_LIST;
        }

        if (matches.size() > 0) {
            String result = ((String) matches.get(0)).trim()
                    .replace("作者：", "")
                    .replace("作者:", "")
                    .replace("来源：", "")
                    .replace("来源:", "")
                    .replace("稿源：", "")
                    .replace("稿源:", "")
                    .replace("&nbsp;", "")
                    .replaceAll("^\\s", "").split(" ")[0];
            result = result.split("\\|")[0].trim();
            return result;
        } else {
            return "";
        }


    }


    /**
     *  获取md5 结果 去重用 --
     * @param md5Key
     * @return
     */

    public  static  String md5IdUrl(String md5Key){
        StringBuffer buf = new StringBuffer("");
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            try {
                md5.update(md5Key.getBytes("utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            byte[] byteDigest = md5.digest();
            int i;
            for (int offset = 0; offset < byteDigest.length; offset++) {
                i = byteDigest[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // 32位加密
        return buf.toString();
    }



    public  static  String md5ByUtf8(String md5Key) throws Exception{
        StringBuffer buf = new StringBuffer("");
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(md5Key.getBytes("utf-8"));
            byte[] byteDigest = md5.digest();
            int i;
            for (int offset = 0; offset < byteDigest.length; offset++) {
                i = byteDigest[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // 32位加密
        return buf.toString();
    }




    public static String unicodeToCn(final String str) {
        // 用于构建新的字符串
        StringBuilder sb = new StringBuilder();
        // 从左向右扫描字符串。tmpStr是还没有被扫描的剩余字符串。
        // 下面有两个判断分支：
        // 1. 如果剩余字符串是Unicode字符开头，就把Unicode转换成汉字，加到StringBuilder中。然后跳过这个Unicode字符。
        // 2.反之， 如果剩余字符串不是Unicode字符开头，把普通字符加入StringBuilder，向右跳过1.
        int length = str.length();
        for (int i = 0; i < length;) {
            String tmpStr = str.substring(i);
            if (isStartWithUnicode(tmpStr)) { // 分支1
                sb.append(ustartToCn(tmpStr));
                i += 6;
            } else { // 分支2
                sb.append(str.substring(i, i + 1));
                i++;
            }
        }
        return sb.toString();
    }




    /**
     * 把 \\u 开头的单字转成汉字，如 \\u6B65 ->　步
     * @param str
     * @return
     */
    private static String ustartToCn(final String str) {
        StringBuilder sb = new StringBuilder().append("0x")
                .append(str.substring(2, 6));
        Integer codeInteger = Integer.decode(sb.toString());
        int code = codeInteger.intValue();
        char c = (char)code;
        return String.valueOf(c);
    }


    public static String encodeUrl(String url ){
        return urlEncoder.encode(url,Charset.forName("UTF-8")) ;
    }


    /**
     * 字符串是否以Unicode字符开头。约定Unicode字符以 \\u开头。
     * @param str 字符串
     * @return true表示以Unicode字符开头.
     */
    private static boolean isStartWithUnicode(final String str) {
        if (null == str || str.length() == 0) {
            return false;
        }
        if (!str.startsWith("\\u")) {
            return false;
        }
        // \u6B65
        if (str.length() < 6) {
            return false;
        }
        String content = str.substring(2, 6);

        boolean isMatch = Pattern.matches(pattern, content);
        return isMatch;
    }




    public static void main(String[] args)  throws  Exception{

        Integer cd = null;

        try {
            cd++;
        }catch (Exception e){
            String exceptionInfo = getExceptionInfo(e);
            System.out.println(exceptionInfo);
        }



        /*try {
        try {
            int i  = 4/0 ;
        } catch (Exception e) {
            System.out.println(getExceptionInfo(e));
        }*/


       /* System.out.println(Jsoup.connect("http://www.yidianzixun.com/article/0MAvuPkJ").get().html().
                replace("<span>"," ").replace("</span>"," "));

        String a =getFromByRegAuto(Jsoup.connect("http://www.yidianzixun.com/article/0MAvuPkJ").get().html().toString()) ;

        System.out.println(a) ;
        String b = "   <span>来源：新摄影网撒<span>  <span>2019-01-03 15:29</span>   网友评论 10 条   " +
                "进入论坛sdsdsds" +
                "dsdsd," +
                "sdsdsd" +
                "sdsdsd" +
                "dsdsds" +
                "qwqwq" ;
        System.out.println(getFromByRegAuto(b));*/
    }

}
