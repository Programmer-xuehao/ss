package com.ifeng.mcn.spider.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.common.utils.StringUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脚本  公共方法 提取
 *
 *
 * @author chenghao1
 * @create 2019/10/9
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class ScriptUtil {

    /***
     * 获取 头条 图集
     * @param document
     * @return
     */
    public static  String getGalleryContext(Document document) {
        String s = document.html().split("JSON\\.parse\\(\"")[1].split("\"\\),\n" +
                "    siblingList:")[0];
        s= s.replace("\\\"", "\"");
        System.err.println(s);

        String context="";
        DocumentContext parse = JsonPath.parse(s);
        JSONArray url = parse.read("$.sub_images.*.url", JSONArray.class);
        JSONArray abs = parse.read("$.sub_abstracts.*", JSONArray.class);
        for (int i = 0; i < url.size(); i++) {
            context += "<img src=" + url.get(i) + ">";
            context += abs.get(i);
        }
        context = context.replaceAll("\\\\", "");
        return context;
    }


    /**
     * 获取 头条文章
     * @param document
     * @return
     */
    public static  String getArticleContent(Document document) {
        Elements elements = document.select("script");
        String docStr = null;
        for (int i = 0; i < elements.size(); i++) {
            docStr = elements.get(i).html();
            if (docStr.indexOf("BASE_DATA") != -1) {
                docStr = docStr.replace("var BASEa_DATA = ", "");
                docStr = docStr.substring(0, docStr.length() - 1);
                break;
            }
        }
        String regex = "content: '(?<content>.*?)'";
        Pattern pattern = Pattern.compile(regex);
        String content = "";

        Matcher matcher = pattern.matcher(docStr);
        if (matcher.find()) {
            //var title  = matcher.group("title");
            String con = matcher.group("content").replace("\\&quot;", "\"")
                    .replace("&#x3D;", "=")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "");
            content = con;
        }

        if (content.indexOf("pgc-card") != -1) {
            Document doc = Jsoup.parse(content);
            doc.select("style").remove();
            Elements divs = doc.select("div");
            for (Element element : divs) {
                if ("pgc-card".equals(element.attr("id"))) {
                    element.remove();
                    break;
                }
            }
            content = doc.body().html();
        }
        if (!StringUtil.isBlank(content) && content.indexOf("\\u") > -1){
            content = CommonUtils.unicodeToCn(content);
        }
        return content;
    }





    public static String decodeUnicode(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len; ) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed   \\uxxxx   encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';

                    else if (aChar == 'f')

                        aChar = '\f';

                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }

//    public static void main(String[] args) throws IOException {
//        Document document = Jsoup.connect("https://www.toutiao.com/a6749110608710337037").get();
//        String s = document.html().split("JSON\\.parse\\(\"")[1].split("\"\\),\n" +
//                "    siblingList:")[0];
//        s= s.replace("\\\"", "\"");
//        System.err.println(s);
//
//        String context="";
//        DocumentContext parse = JsonPath.parse(s);
//        JSONArray url = parse.read("$.sub_images.*.url", JSONArray.class);
//        JSONArray abs = parse.read("$.sub_abstracts.*", JSONArray.class);
//        for (int i = 0; i < url.size(); i++) {
//            context += "<img src=" + url.get(i) + ">";
//            context += abs.get(i);
//        }
//        context = context.replaceAll("\\\\", "");
//        System.err.println(context);
//
//    }
}
