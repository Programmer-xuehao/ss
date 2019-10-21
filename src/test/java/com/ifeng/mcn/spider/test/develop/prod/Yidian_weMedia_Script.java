package com.ifeng.mcn.spider.test.develop.prod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.base.TaskStatusEnum;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.log.client.base.EventType;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import us.codecraft.webmagic.Page;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 描述
 * @author: yingxj
 * @date: 2019/10/8 15:51
 */
@Service
public class Yidian_weMedia_Script extends CrawlerWorker {

    private static Logger logger = LoggerFactory.getLogger(Yidian_weMedia_Script.class);;

    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    public static void main(String[] args) throws Exception{
        Yidian_weMedia_Script mongoGaoyuanYidianScript  = new Yidian_weMedia_Script() ;
        Map map =  Maps.newHashMap() ;
        mongoGaoyuanYidianScript.params.set(map);
        map.put("link","http://www.yidianzixun.com/channel/m1336381") ;
        map.put("mcnTaskId","test001") ;
        map.put("taskType","yidian_weMedia") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","yidian_weMedia_http") ;
        List<String>  result = mongoGaoyuanYidianScript.crawlerListPage(map);
        System.out.println(JSON.toJSON(result));
        mongoGaoyuanYidianScript.crawlerDetailPage(result.get(1), map);
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        logger.info("yidianzixun:{}",params);

        String crawlerUrl = params.get("link").toString();
        Page page = null;
        List result = new ArrayList();
        for(int i=0; i<5;i++){
            try{
                page = httpMySpider(crawlerUrl).run();
                if (page == null || !page.isDownloadSuccess()) {
                    page = httpMySpider(crawlerUrl).run();
                }
            } catch (Exception e){
                logger.error("yidianErr:"+e.getMessage(),e);
            }

            if (page == null) {
                logger.error("list page is null " + crawlerUrl);
                continue;
            }

            Elements scripts = page.getHtml().getDocument().select("script");
            String listPageJson = null;
            for (Element script : scripts) {
                if (script.toString().indexOf("yidian.docinfo = ") != -1) {
                    listPageJson = script.toString().split("yidian.docinfo = ")[1];
                    if (listPageJson.indexOf(";</script") != -1) {
                        listPageJson = listPageJson.replace(";</script>", "");
                    }
                    if (listPageJson.indexOf("</script") != -1) {
                        listPageJson = listPageJson.replace("</script>", "");
                    }
                    break;
                }
            }
            if (listPageJson == null) {
                logger.error(crawlerUrl+":一点资讯脚本解析失败:{}",http(crawlerUrl).execute().parse().text().substring(0,100));
                continue;
            }
            JSONObject jsonObject = JSON.parseObject(listPageJson);
            String fans = jsonObject.getString("bookcount").replace("人订阅","");
            if (Integer.valueOf(fans)<100){
                logger.info("yidian fans num less than 100 :{}",params);
                mcnTaskDao.updateStatus((String)params.get("mcnTaskId"), TaskStatusEnum.ACCOUNT.getCode());
                continue;
            }

            JSONArray dataList = JSON.parseObject(listPageJson).getJSONArray("result");
            if(dataList.size()<=2){
                continue;
            }
            for(Object data: dataList){
                result.add(data.toString());
            }
            break;
        }
        logger.info("yidianzixun result:{}",result);
        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        try {
            JSONObject articleItemJson = JSON.parseObject(itemBody);
            if (articleItemJson.get("docid") == null)
                return null;

            String publishTime = articleItemJson.getString("date");
            publishTime = getYidianDate(publishTime);
            Date publishDate = DateUtil.parseTime(publishTime);
            if (publishDate != null && System.currentTimeMillis() - publishDate.getTime() > TWO_DAY_M) {
                logger.info("mediaId:{},一点资讯发布时间过滤:{}", params.get("mediaId"), articleItemJson.get("docid"));
                return null;
            }
            String articleUrl = "http://www.yidianzixun.com/article/" + articleItemJson.get("docid");//文章页地址
//            //判重
            String duplicateKey = params.get("taskType") + articleItemJson.get("docid").toString();
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }
            McnContentBo mcnContentBo = new McnContentBo();
            mcnContentBo.setPlatId(articleItemJson.getString("itemid"));
            mcnContentBo.setDuplicateKey(duplicateKey);
            mcnContentBo.setPageLink(articleUrl);

            String content_type = articleItemJson.getString("content_type");
            if(content_type.equals("video")){
                mcnContentBo.setShapeType("2");
                mcnContentBo.setMusicDownloadUrl(articleItemJson.getString("video_url"));
            }else{
                mcnContentBo.setShapeType("1");
                getArticleContentUrl(articleUrl,mcnContentBo);
                if (StringUtils.isBlank(mcnContentBo.getContent())) {
                    return null;
                }
            }

            if (articleItemJson.get("image") != null && articleItemJson.getString("image").startsWith("//")) {
                String image = "http:" +articleItemJson.getString("image");
                mcnContentBo.setCover(image);
            }

            if (publishTime != null) {
                mcnContentBo.setPublishTime(publishDate.getTime());
            }else{
                mcnContentBo.setPublishTime(System.currentTimeMillis());
            }
            mcnContentBo.setTitle(articleItemJson.getString("title"));
            mcnContentBo.setSourceLink(params.get("link").toString());
            mcnContentBo.setCreateTime(System.currentTimeMillis());
            mcnContentBo.setFrom(articleItemJson.getString("source"));
            mcnContentBo.setAbstractInfo(articleItemJson.getString("summary"));
            mcnContentBo.setCatogery(articleItemJson.getString("category"));
            JSONObject wemediaInfo = articleItemJson.getJSONObject("wemedia_info");
            mcnContentBo.setMediaName(wemediaInfo.getString("name"));
            mcnContentBo.setMediaId(wemediaInfo.getString("channel_id"));

            logger.info("yidianDetailPage：{}",mcnContentBo.toStringRmContent());
            return mcnContentBo;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.error("getArticlePgeInfos:{}", sw);
        }

        return null;
    }

    /**
     * 获取文章的正文
     * @param articleUrl
     * @param mcnContentBo
     */
    private void getArticleContentUrl(String articleUrl, McnContentBo mcnContentBo) throws Exception {
        Page page = httpMySpider(articleUrl, getRiskInfo(articleUrl,articleUrl, EventType.detail)).run();
        if (page == null || !page.isDownloadSuccess()) {
            page = httpMySpider(articleUrl).run();
        }
        if (page == null || !page.isDownloadSuccess()) {
            logger.error("article page is null " + articleUrl);
            return;
        }
//        Elements scripts = page.getHtml().getDocument().select("script");
//        String pageJson = null;
//        for (Element script : scripts) {
//            if (script.toString().indexOf("yidian.docinfo = ") != -1) {
//                pageJson = script.toString().split("yidian.docinfo = ")[1];
//                if (pageJson.indexOf(";</script") != -1) {
//                    pageJson = pageJson.replace(";</script>", "");
//                }
//                if (pageJson.indexOf("</script") != -1) {
//                    pageJson = pageJson.replace("</script>", "");
//                }
//                break;
//            }
//        }

        Document document = page.getHtml().getDocument();
        try{
            Element elementById = document.getElementById("imedia-article");
            if(null==elementById){
                elementById = document.getElementById("js-article");
                if(null==elementById){
                    Elements content = document.select("content-bd");
                    if(null!=content&&!content.isEmpty()){
                        elementById = content.get(0);
                    }else{
                        content = document.getElementsByClass("content-bd");
                        if(null!=content&&!content.isEmpty()) {
                            elementById = content.get(0);
                        }
                    }
                }
            }
            if(null!=elementById){
                mcnContentBo.setContent(elementById.html());
            }else{
                logger.info("yidianGetHtml:{},{}",articleUrl,document.body().html());
            }
        } catch (Exception e){
            throw new Exception("yidianGetArticleContentUrlErr:{"+articleUrl+"},{"+e+"}");
        }
    }

    private String getYidianDate(String time) {
        if (StringUtils.isEmpty(time))
            return null;
        time = time.trim();
        if (time.equals(""))
            return null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (time.indexOf("天前") != -1) {
            Pattern pattern = Pattern.compile("(\\d+)天前");
            Matcher matcher = pattern.matcher(time);
            String days = null;
            if (matcher.find()) {
                days = matcher.group(1);
            } else
                return null;
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - Integer.parseInt(days));
            return simpleDateFormat.format(calendar.getTime()) + " 00:00:00";
        } else if ("昨天".equals(time) || time.endsWith("昨天")) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
            return simpleDateFormat.format(calendar.getTime()) + " 00:00:00";
        } else if (time.indexOf("分钟前") != -1) {
            Pattern pattern = Pattern.compile("(\\d+)分钟前");
            Matcher matcher = pattern.matcher(time);
            String mins = null;
            if (matcher.find()) {
                mins = matcher.group(1);
            } else {
                return null;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - Integer.parseInt(mins));
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
        } else if (time.indexOf("小时前") != -1) {
            Pattern pattern = Pattern.compile("(\\d+)小时前");
            Matcher matcher = pattern.matcher(time);
            String hours = null;
            if (matcher.find()) {
                hours = matcher.group(1);
            } else
                return null;
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - Integer.parseInt(hours));
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
        } else if (time.matches("\\d{4}\\.\\s*\\d+\\.\\s*\\d+")) {
            SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy.MM.dd");
            try {
                Date date = simpleDateFormat1.parse(time);
                return simpleDateFormat.format(date) + " 00:00:00";
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
