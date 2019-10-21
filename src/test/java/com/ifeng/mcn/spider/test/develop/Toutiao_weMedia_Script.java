package com.ifeng.mcn.spider.test.develop;

import com.alibaba.fastjson.JSON;
import com.ifeng.mcn.common.base.TaskStatusEnum;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.ProxyUtils;
import com.ifeng.mcn.spider.utils.ScriptUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "https://www.365yg.com/c/user/article/?user_id=52029327939&count=30");
//        params.put("link", "https://www.365yg.com/c/user/article/?user_id=87865547577&count=30");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "qutoutiao_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "qutoutiao_http");
        params.put("mediaId", "30019305");

        Toutiao_weMedia_Script script = new Toutiao_weMedia_Script();
        script.params.set(params);


        List<String> list = script.crawlerListPage(params);
        for (String item : list) {
            script.crawlerDetailPage(item, params);
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
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
            mcnTaskDao.updateStatus(params.get("mcnTaskId") + "","4");
            logger.error("mid is error:{};taskId:{}",url,params.get("mcnTaskId"));
            return null ;
        }
        String crawlerUrl = "https://www.365yg.com/c/user/article/?count=30&user_id=" + mid;
        String rawText ="";
        String urlPhone = "https://i.snssdk.com/api/feed/profile/v6/?category=profile_all&visited_uid=%s&stream_api_version=82&offset=0&version_code=740&version_name=70400&request_source=1&active_tab=dongtai&device_id=65&app_name=news_article";
        // Site site = Site.me().setUserAgent(MySpider.getRandomUa()).addHeader("Referer","http://www.toutiao.com/").setRetryTimes(2).setSleepTime(1000).setTimeOut(2000);
        params.put("phone", "phone");
        try {
            rawText= http(String.format(urlPhone,mid)).timeout(6000).execute().body();
            //  rawText= http(crawlerUrl).execute().body();
            logger.info("365 rawText curl:{},365:{}",crawlerUrl,rawText);
            JSONArray read = JsonPath.parse(rawText)
                    .read("$.data.*", JSONArray.class);
            if(read.size()==0){
                //  没有数据   没有发表过文章  视频  暂停 抓取
                mcnTaskDao.updateStatus(params.get("mcnTaskId") + "",TaskStatusEnum.ACCOUNT.getCode());
            }
            return read.stream().map(obj -> JSON.toJSONString(obj))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("头条列表页走365链接异常!{},text:{},url:{}",crawlerUrl,rawText,url, e);
        }

        //手机端链接
        rawText= httpMySpider(String.format(urlPhone,mid),null).setDownloader(ProxyUtils.getProxyDownLoad()).setProxyFlag(true).run().getRawText();
        logger.info("url:{},phone:{}",String.format(urlPhone,mid),rawText);
        return JsonPath.parse(rawText)
                .read("$..content", JSONArray.class)
                .stream().map(Object::toString)
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {

        if (itemBody.contains("\"is_question_delete\"")) {
            return null;
        }
        DocumentContext item = JsonPath.parse(itemBody);


        //判重
        String duplicateKey = "toutiao" + item.read("$.item_id");
        duplicateKey = CommonUtils.md5IdUrl(duplicateKey);

        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;//重复key,取消抓取
        }

        McnContentBo contentBo = new McnContentBo();
        contentBo.setDuplicateKey(duplicateKey);

        if (!params.containsKey("phone")) {
            //跳过无效链接
            if (item.read("$.display_url").toString().startsWith("sslocal")) {
                return null;
            }

            String detailUrl = "https://www.toutiao.com" + item.read("$.source_url");
            Document doc = Jsoup.parse(httpMySpider(detailUrl,null).run().getRawText());

            Boolean hasVideo = item.read("$.has_video", Boolean.TYPE);
            //转换为McnContentBo
            contentBo.setShapeType(item.read("$.article_genre").toString().equals("video") ? "2" : "1");//1 文章  2 视频  3 ALL
            contentBo.setTitle(item.read("$.title"));
            contentBo.setOriginalTitle(item.read("$.title"));
            contentBo.setMediaName(item.read("$.source"));
            contentBo.setMediaId(item.read("$.media_url").toString().replaceAll("\\D", ""));
            contentBo.setHaveViedo(hasVideo != true ? 1 : 2);
            contentBo.setCommentCount(item.read("$.comments_count"));
            contentBo.setPlayCount(item.read("$.detail_play_effective_count", Long.class));
            contentBo.setSourceLink(detailUrl);
            contentBo.setPageLink(detailUrl);
            contentBo.setMusicDownloadUrl(detailUrl);
            //contentBo.setVideoDuration(item.read("video_duration_str"));"02:09",
            contentBo.setPublishTime(item.read("$.behot_time", Long.class) * 1000);
            contentBo.setAbstractInfo(item.read("$.abstract"));
            contentBo.setCover(item.read("$.image_url"));

            //任务中心下发的参数
            contentBo.setTaskId(params.get("mcnTaskId") + "");
            contentBo.setPlatId(item.read("$.item_id") + "");

            //判断页面是视频还是文章
            String article_genre = item.read("$.article_genre");//文章类型：article | video | gallery | ugc
            String content = article_genre.equals("gallery") ? ScriptUtil.getGalleryContext(doc) : ScriptUtil.getArticleContent(doc);
            contentBo.setContent(content);
        } else {
            //article_type 0:文章 2:西瓜视频
            Integer article_type = item.read("$.article_type", Integer.TYPE);

            contentBo.setAbstractInfo(item.read("$.abstract"));
            contentBo.setPlayCount(item.read("$.share_count", Long.TYPE));
            contentBo.setTitle(item.read("$.title"));
            String link = "https://www.toutiao.com/i" + item.read("$.item_id");
            logger.info("phone retry------{};body:{};userlink:{}",link,itemBody,params.get("link") + "");
            contentBo.setPageLink(link);
            contentBo.setMusicDownloadUrl(link);
            contentBo.setShareLink(item.read("$.share_url"));
            contentBo.setPublishTime(item.read("$.behot_time", Long.TYPE) * 1000);
            contentBo.setLikeCount(item.read("$.like_count", Integer.TYPE));
            contentBo.setCommentCount(item.read("$.comment_count", Integer.TYPE));
            contentBo.setOriginalFlag(item.read("$.is_original", Boolean.TYPE));
            contentBo.setLikeCount(item.read("$.like_count", Integer.TYPE));
            contentBo.setMediaName(item.read("$.source"));
            contentBo.setMediaName(item.read("$.user_info.user_id") + "");
            //文章
            if (article_type == 0) {
                contentBo.setShapeType("1");
                Document doc = Jsoup.parse(httpMySpider(link,null).run().getRawText());
//                Document doc = http(link).timeout(6000).execute().parse();
                String content = ScriptUtil.getArticleContent(doc);
                contentBo.setContent(content);
                contentBo.setCover(item.read("$.video_detail_info.detail_video_large_image.url"));
            }
            //视频
            if (article_type == 2) {
                //处理手机链接
                contentBo.setShapeType("2");
                contentBo.setCover(item.read("$.ugc_video_cover.url"));
            }
        }

        //按时间过滤
        if (exceedPubTime(contentBo.getPublishTime())) {
            return null;
        }


        if (params.containsKey("phone")){

            logger.warn("phone头条脚本抓取结果：{}", JSON.toJSONString(contentBo));
        }else{

            logger.warn("365头条脚本抓取结果：{}", JSON.toJSONString(contentBo));
        }
        return contentBo;
    }

}
