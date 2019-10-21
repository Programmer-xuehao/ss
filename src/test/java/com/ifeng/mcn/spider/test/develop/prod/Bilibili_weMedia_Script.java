package com.ifeng.mcn.spider.test.develop.prod;

import com.alibaba.fastjson.JSON;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.jsoup.helper.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 迁移脚本-B站
 * 需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class Bilibili_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {

        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "http://rss.huanqiu.com/topnews.xml");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "qutoutiao_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "qutoutiao_http");
        params.put("mediaId", "30019305");

        Bilibili_weMedia_Script script = new Bilibili_weMedia_Script();
        script.params.set(params);

        List<String> list = script.crawlerListPage(params);
        for (String item : list) {
            System.err.println(item);
            script.crawlerDetailPage(item, params);
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        String link = params.get("link").toString();
        String mid = link.replace("https://space.bilibili.com/", "");
        if (mid.contains("?")) {
            mid = mid.split("\\?")[0];
        }
        if (mid.contains("/")) {
            mid = mid.split("/")[0];
        }

        String api = "https://api.bilibili.com/x/space/article?mid=%s&pn=1&ps=12&sort=publish_time&jsonp=jsonp";
        //请求文章列表
        String body = http(String.format(api, mid)).execute().body();
        List<String> collect = JsonPath.parse(body).read("$.data.articles.*", JSONArray.class)
                .stream()
                .map(JSON::toJSONString)
                .collect(Collectors.toList());
        //请求视频列表
        api = "https://space.bilibili.com/ajax/member/getSubmitVideos?mid=%s&page=1&pagesize=25";
        body = http(String.format(api, mid)).execute().body();
        JsonPath.parse(body).read("$.data.vlist.*", JSONArray.class)
                .stream()
                .map(JSON::toJSONString)
                .forEach(collect::add);
        return collect;

    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        DocumentContext context = JsonPath.parse(itemBody);
        McnContentBo contentBo = new McnContentBo();
        if (!context.read("$.[?(@.play)]", JSONArray.class).isEmpty()) {
            //https://space.bilibili.com/ajax/member/getSubmitVideos?mid=30019305&page=1&pagesize=25
            //视频解析
            contentBo.setShapeType("2");//1 文章  2 视频  3 ALL
            contentBo.setPublishTime(context.read("$.created", Long.TYPE) * 1000);
            String aid = context.read("$.aid") + "";

            String duplicateKey = CommonUtils.md5IdUrl("https://www.bilibili.com/video/av" + aid);
            //本地main方法测试时需要注掉
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }

            contentBo.setDuplicateKey(duplicateKey);
            contentBo.setShapeType("2");
            contentBo.setPageLink("https://www.bilibili.com/video/av" + aid);
            contentBo.setCommentCount(context.read("$.comment", Integer.TYPE));
            contentBo.setLikeCount(context.read("$.favorites", Integer.TYPE));
            contentBo.setCover("https:" + context.read("$.pic"));
            contentBo.setPlatId(aid);
            contentBo.setMusicDownloadUrl(contentBo.getPageLink());
            contentBo.setTitle(context.read("$.title") + "");
            contentBo.setAbstractInfo(context.read("$.description") + "");
            contentBo.setPlayCount(context.read("$.play", Long.TYPE));
            System.err.println("视频抓取结果:" + contentBo);
        } else {
            //https://api.bilibili.com/x/space/article?mid=30019305&pn=1&ps=12&sort=publish_time&jsonp=jsonp
            //文章解析
            String id = context.read("$.id") + "";

            String duplicateKey = CommonUtils.md5IdUrl("https://www.bilibili.com/read/cv" + id);
            //本地main方法测试时需要注掉
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }

            contentBo.setShapeType("1");//1 文章  2 视频  3 ALL
            contentBo.setTitle(context.read("$.title") + "");
            contentBo.setAbstractInfo(context.read("$.summary") + "");
            contentBo.setPublishTime(context.read("$.publish_time", Long.TYPE) * 1000);
            //按时间过滤
            if (exceedPubTime(contentBo.getPublishTime())) {
                return null;
            }
            String curl = context.read("$.banner_url") + "";
            if (StringUtil.isBlank(curl)) {
                curl = context.read("$.image_urls[0]") + "";
            }
            contentBo.setCover(curl);
            String categories = context.read("$.categories.*.name", JSONArray.class)
                    .stream().map(Object::toString)
                    .collect(Collectors.joining(","));
            contentBo.setCatogery(categories);
            String content = http("https://www.bilibili.com/read/cv" + id).get().select(".article-holder").html();
            contentBo.setContent(content);
            System.err.println("文章抓取结果:" + contentBo);
        }
        logger.warn("bilibili抓取结果：{}", JSON.toJSONString(contentBo));
        return contentBo;
    }
}
