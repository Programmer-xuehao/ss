package com.ifeng.mcn.spider.test.develop.prod;

import cn.edu.hfut.dmic.contentextractor.ContentExtractor;
import cn.edu.hfut.dmic.contentextractor.News;
import cn.hutool.core.text.UnicodeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ifeng.mcn.common.base.TaskStatusEnum;
import com.ifeng.mcn.common.utils.StringUtil;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.log.client.CommonUtils;
import com.ifeng.mcn.log.client.base.EventType;
import com.ifeng.mcn.spider.exceptions.CrawlerException;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.MySpider;
import com.ifeng.mcn.spider.utils.ProxyUtils;
import com.ifeng.mcn.spider.utils.ScriptUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 示例抓取任务脚本-头条
 * 不需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
@Service
public class Toutiao_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        System.out.println(UnicodeUtil.toString("\\u002F"));

        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "https://www.toutiao.com/c/user/84914772586/#mid=1589012569581572");
        //   params.put("link", "https://i.snssdk.com/api/feed/profile/v6/?category=profile_all&visited_uid=86028962958&stream_api_version=82&offset=0&version_code=740&version_name=70400&request_source=1&active_tab=dongtai&device_id=65&app_name=news_article");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "toutiao_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "toutiao_weMedia_http");
        params.put("mediaId", "106828227125");
        params.put("phone","");

//
        Toutiao_weMedia_Script script = new Toutiao_weMedia_Script();
        script.params.set(params);
//        Document doc = script.httpMySpider("https://www.toutiao.com/item/6750864854308880907/",null).run().getHtml().getDocument();
//        String content = ScriptUtil.getGalleryContext(doc);
//        System.out.println("-----"+content);
        // System.out.println(script.http("https://irb.cfbond.com/api/v1/rss/rss.xml?appid=55091843835488552247",null).execute().parse().html());
//        script.params.set(params);
//
        List<String> list = script.crawlerListPage(params);
        for (String item : list) {
            McnContentBo mcnContentBo = script.crawlerDetailPage(item, params);
            System.out.println(JSON.toJSONString(mcnContentBo));
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        setListRetryCount(2);
        logger.info("toutiao weMedia list page params:{}",params);
        //https://www.toutiao.com/c/user/58634896950/#mid=1563080908028929
        //https://www.365yg.com/c/user/article/?user_id=6919713042&count=30
        //头条任务可能有两种链接，所以拿mediaId去拼
        String mid = "";
        String url = params.get("link") + "";
        if (url.startsWith("https://www.365yg.com")) {
            mid = url.split("user_id=")[1].split("&")[0];
        }
        if (url.startsWith("https://www.toutiao.com")) {
            mid = url.replace("https://www.toutiao.com/c/user/", "").split("/")[0];
        }
        if (url.startsWith("http://www.toutiao.com")) {
            mid = url.replace("http://www.toutiao.com/c/user/", "").split("/")[0];
        }
        // 链接异常
        if(StringUtils.isBlank(mid)){
            mcnTaskDao.updateStatus(params.get("mcnTaskId") + "", TaskStatusEnum.ERRORLINK.getCode());
            logger.error("mid is error:{};taskId:{}",url,params.get("mcnTaskId"));
            throw new CrawlerException("mid is error:%s;taskId:%s",url,params.get("mcnTaskId"));
        }
        String crawlerUrl = "https://www.365yg.com/c/user/article/?count=20&user_id=" + mid;
        //String urlPhone = "https://i.snssdk.com/api/feed/profile/v6/?category=profile_all&visited_uid=%s&stream_api_version=82&offset=0&version_code=740&version_name=70400&request_source=1&active_tab=dongtai&device_id=65&app_name=news_article";
        String urlPhone = "https://i.snssdk.com/api/feed/profile/v6/?category=profile_all&visited_uid=%s&stream_api_version=82&count=15&offset=0&version_code=740&version_name=70400&request_source=1&active_tab=dongtai&device_id=65&app_name=news_article";
        urlPhone = String.format(urlPhone,mid)  ;
//        try{
//            rawText = httpMySpider(crawlerUrl,null).run().getRawText();
        List<String> result = getUrl(crawlerUrl);
//            if(null==result){
//                result = getUrl(urlPhone);
//            }
       /*     if(result!=null && result.isEmpty()){
                // 停止抓取没有数据的
                mcnTaskDao.updateStatus((String)params.get("mcnTaskId"),TaskStatusEnum.NODATA.getCode());
            }*/

        logger.info("toutiao list result size:{}",result.size());
        return result;
//        }catch (Exception e){
//            logger.error("toutiao list page Err url:{},Err:{}",crawlerUrl,e);
//        }
//        return null;
    }

    private List<String> getUrl(String crawlerUrl) throws Exception{
        JSONObject jsonObject = null;
        String rawText = new String();
        MySpider mySpider = httpMySpider(crawlerUrl, getRiskInfo(null, null, EventType.list));
        mySpider.setDownloader(ProxyUtils.getProxyDownLoad()).setProxyFlag(true);
        rawText = mySpider.run().getRawText();
        if (StringUtil.isBlank(rawText)){
            logger.info("get toutiao list data is null,url:{},params:{}",crawlerUrl,params);
            throw new CrawlerException("get toutiao list data is null,url:%s,params:%s",crawlerUrl,params);
        }
        jsonObject = JSON.parseObject(rawText);
        if(null==jsonObject){
            logger.info("parse toutiao list data is null,url:{},params:{}",crawlerUrl,params);
            throw new CrawlerException("parse toutiao list data is null,url:%s,params:%s",crawlerUrl,params);
        }
        JSONArray read = jsonObject.getJSONArray("data");
        if(read == null || read.isEmpty()){
            logger.info("toutiao List data is null,url:{},params:{}",crawlerUrl,params);
            throw new CrawlerException("toutiao List data is null,url:%s,params:%s",crawlerUrl,params);
        }
        return read.stream().map(obj -> JSON.toJSONString(obj))
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {

        if (itemBody.contains("\"is_question_delete\"")) {
            return null;
        }

        JSONObject item = JSON.parseObject(itemBody);

        //判重
        String duplicateKey = "toutiao";
        McnContentBo contentBo = new McnContentBo();

        try{
            String itemId = item.getString("item_id");
            if(StringUtils.isBlank(itemId)){
                itemId = item.getString("group_id");
            }
            if(StringUtils.isBlank(itemId)){
                Pattern c = Pattern.compile("group/\\d+");
                Matcher m = c.matcher(item.getString("display_url"));
                if(m.find()){
                    itemId = m.group().replace("group/","");
                }
            }

            if (!item.containsKey("content")) {
                duplicateKey = duplicateKey + itemId;
                duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
                //重复为true
                if (duplicateKeyDao.containsKey(duplicateKey)) {
                    return null;//重复key,取消抓取
                }

                contentBo.setDuplicateKey(duplicateKey);
                //跳过无效链接
                if (item.getString("display_url").startsWith("sslocal")) {
                    return null;
                }
                contentBo.setPublishTime(item.getLong("behot_time") * 1000);
                if (exceedPubTime(contentBo.getPublishTime())) {
                    return null;
                }

                String detailUrl = "https://www.toutiao.com" + item.getString("source_url");
                Document doc = Jsoup.parse(httpMySpider(detailUrl,null).run().getRawText());

                Boolean hasVideo = item.getBoolean("has_video");
                //转换为McnContentBo
                String article_genre = item.getString("article_genre");
                contentBo.setShapeType(article_genre.equals("video") ? "2" : "1");//1 文章  2 视频  3 ALL
                contentBo.setTitle(item.getString("title"));
                contentBo.setOriginalTitle(item.getString("title"));
                contentBo.setMediaName(item.getString("source"));
                contentBo.setMediaId(item.getString("media_url").toString().replaceAll("\\D", ""));
                contentBo.setHaveViedo(hasVideo != true ? 1 : 2);
                contentBo.setCommentCount(item.getInteger("comments_count"));
                contentBo.setPlayCount(item.getLong("detail_play_effective_count"));
                contentBo.setSourceLink(params.get("link").toString());
                contentBo.setPageLink(detailUrl);
                contentBo.setMusicDownloadUrl(detailUrl);
                contentBo.setDuplicateKey(duplicateKey);
                //contentBo.setVideoDuration(item.read("video_duration_str"));"02:09",

                contentBo.setAbstractInfo(item.getString("abstract"));
                contentBo.setCover(item.getString("image_url"));
                if(contentBo.getShapeType().equals("2")){
                    Elements scripts = doc.select("script");
                    String listPageJson = null;
                    for (Element script : scripts) {
                        if("SSR_HYDRATED_DATA".equals(script.attr("id"))){
                            listPageJson = script.toString().split("<script type=\"application/json\" id=\"SSR_HYDRATED_DATA\">")[1];
                            if (listPageJson.indexOf(";</script") != -1) {
                                listPageJson = listPageJson.replace(";</script>", "");
                            }
                            if (listPageJson.indexOf("</script") != -1) {
                                listPageJson = listPageJson.replace("</script>", "");
                            }
                        }
                    }
                    if(StringUtil.isNotBlank(listPageJson)){
                        JSONObject data = JSON.parseObject(listPageJson);
                        JSONObject video = data.getJSONObject("Projection").getJSONObject("video");
                        String posterUri = video.getString("poster_uri");
                        if(StringUtil.isNotBlank(posterUri)){
                            posterUri = "https://sf6-xgcdn-tos.pstatp.com/img/"+UnicodeUtil.toString(posterUri)+"~tplv-noop.jpeg";
                            contentBo.setCover(posterUri);
                        }
                    }
                }
                contentBo.setCreateTime(System.currentTimeMillis());
                //任务中心下发的参数
                contentBo.setTaskId(params.get("mcnTaskId") + "");
                contentBo.setPlatId(itemId);

                //文章类型：article | video | gallery | ugc
                //判断页面是视频还是文章
                String content = article_genre.equals("gallery") ? ScriptUtil.getGalleryContext(doc) : ScriptUtil.getArticleContent(doc);
                if(article_genre.equals("gallery")){
                    contentBo.setShapeType("3");
                }
                contentBo.setContent(content);
                //详情为空并且是文章类型时走一次自动提取
                if(StringUtil.isBlank(content) && contentBo.getShapeType().equals("1")){
                    retry(contentBo,detailUrl);
                }
                logger.info("365 parse toutiao detail page info:{}",contentBo.toStringRmContent());
                if(StringUtil.isBlank(contentBo.getContent())){
                    logger.info("parse 365 detail page content is null:{}",contentBo.toStringRmContent());
                }
            } else {
                item = item.getJSONObject("content");
//                logger.info("phone parse toutiao detail dage info:{}",contentBo.toStringRmContent());
                //article_type 0:文章 2:西瓜视频
                System.out.println(item);
                contentBo.setPublishTime(item.getLong("behot_time") * 1000);
                //按时间过滤
                if (exceedPubTime(contentBo.getPublishTime())) {
                    return null;
                }
                duplicateKey = duplicateKey + itemId;
                duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
                //重复为true
                if (duplicateKeyDao.containsKey(duplicateKey)) {
                    return null;//重复key,取消抓取
                }
                Integer article_type = item.getInteger("article_type");
                contentBo.setAbstractInfo(item.getString("abstract"));
                contentBo.setPlayCount(item.getLong("share_count"));
                contentBo.setTitle(item.getString("title"));
                String link = "https://www.toutiao.com/i" + itemId;
                //            logger.info("phone retry------{};body:{};userlink:{}",link,itemBody,params.get("link") + "");
                contentBo.setSourceLink(params.get("link").toString());
                contentBo.setPageLink(link);
                contentBo.setMusicDownloadUrl(link);
                contentBo.setShareLink(item.getString("share_url"));
                contentBo.setLikeCount(item.getInteger("like_count"));
                contentBo.setCommentCount(item.getInteger("comment_count"));
                contentBo.setOriginalFlag(item.getBoolean("is_original"));
                contentBo.setLikeCount(item.getInteger("like_count"));
                contentBo.setMediaName(item.getString("source"));
                contentBo.setMediaId(item.getJSONObject("user_info").getString("user_id"));
                contentBo.setDuplicateKey(duplicateKey);

                //文章
                if (article_type == 0) {
                    contentBo.setShapeType("1");
                    Document doc = httpMySpider(link,null).run().getHtml().getDocument();
                    String content = ScriptUtil.getArticleContent(doc);
                    if (StringUtil.isBlank(content)) {
                        try {
                            String body = http(link).execute().body();
                            News contentByHtml = ContentExtractor.getNewsByHtml(body);
                            contentBo.setContent(contentByHtml.getContentElement().toString());
                        } catch (Exception e) {
                            logger.error("RSS走自动提取异常", e);
                        }
                    }
                    contentBo.setContent(content);
                    if(item.containsKey("video_detail_info")){
                        contentBo.setCover(item.getJSONObject("video_detail_info").getJSONObject("detail_video_large_image").getString("url"));
                    }
                }
                //视频
                if (article_type == 2) {
                    //处理手机链接
                    contentBo.setShapeType("2");
                    if(item.containsKey("ugc_video_cover")){
                        contentBo.setCover(item.getJSONObject("ugc_video_cover").getString("url"));
                    }
                }
                logger.info("phone parse toutiao detail page info:{}",contentBo.toStringRmContent());
            }
        }catch (Exception e){
            logger.error("toutiao parse detail page err :{},content:{}",duplicateKey,e);
        }

        return contentBo;
    }

    /**
     * 内容为空重试一次抓取-----外链
     * @param contentBo
     * @param url
     * @return
     */
    private McnContentBo retry(McnContentBo contentBo,String url){
        try {
            String body = http(url).execute().body();
            News contentByHtml = ContentExtractor.getNewsByHtml(body);
            contentBo.setContent(contentByHtml.getContentElement().toString());
        } catch (Exception e) {
            logger.error("RSS走自动提取异常", e);
        }
        return contentBo;
    }

}
