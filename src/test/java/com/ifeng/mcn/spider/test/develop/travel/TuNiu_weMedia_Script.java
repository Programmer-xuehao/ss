package com.ifeng.mcn.spider.test.develop.travel;

import com.alibaba.fastjson.JSON;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.HttpUtils;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.jsoup.nodes.Document;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 示例脚本-途牛
 * 需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class TuNiu_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {

        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "http://www.tuniu.com/person/72EE78524021E31BC3176B56D2266801");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "TuNiu_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "TuNiu_http");

        TuNiu_weMedia_Script script = new TuNiu_weMedia_Script();
        script.params.set(params);

        List<String> list = script.crawlerListPage(params);
        for (String item : list) {
            script.crawlerDetailPage(item, params);
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        String link = params.get("link").toString();
        String mid = link.replace("http://www.tuniu.com/person/", "");
        String userId = HttpUtils.get(link).split("PC:个人主页:")[1].split("'")[0];
        String api = "http://www.tuniu.com/web-personal/api/person/getList?oId=" + userId + "&types%5B%5D=0&page=1&limit=10";
        //请求文章列表
        String body = http(api).execute().body();
        List<String> collect = JsonPath.parse(body).read("$.data.list.*", JSONArray.class)
                .stream()
                .map(JSON::toJSONString)
                .collect(Collectors.toList());
        //请求视频列表

        return collect;

    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        DocumentContext context = JsonPath.parse(itemBody);
        McnContentBo contentBo = new McnContentBo();
        String pd = context.read("$.commonModule").toString();
        if (pd.indexOf("videoSourceFileUrl") == -1) {
            String aid = context.read("$.contentId").toString();
            String time = context.read("$.publishTime");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Long pushTime = df.parse(time).getTime();
            String url = context.read("$.pcUrl");
            Document document = http(url).execute().parse();
            contentBo.setContent(document.select("div.wrapper>div.main-content") + document.select("div.container>div.fl>div.article-detail").toString() + document.select("div.detai-content>div.detail-main>div.content-left").toString());
            contentBo.setShapeType("1");
            contentBo.setPageLink(url);
            contentBo.setPublishTime(pushTime);
            contentBo.setPlatId(aid);
            contentBo.setCommentCount(context.read("$.commonModule.commentCnt", Integer.TYPE));
            contentBo.setLikeCount(context.read("$.commonModule.praiseCnt", Integer.TYPE));
            contentBo.setCover(context.read("$.commonModule.coverImageUrl") + "");
            contentBo.setAbstractInfo(context.read("$.commonModule.description") + "");
            contentBo.setTitle(context.read("$.commonModule.title") + "");
            String duplicateKey = CommonUtils.md5IdUrl(url + aid);
            //手动判断重复，放在请求前面，避免发生多余的请求
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }


            System.err.println("视频抓取结果:" + contentBo);
        } else {
            String sourceLink = context.read("commonModule.videoSourceFileUrl");
            String aid = context.read("$.contentId");
            String time = context.read("$.publishTime");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Long pushTime = df.parse(time).getTime();
            String url = context.read("$.pcurl");
            Document document = http(url).execute().parse();
            contentBo.setContent(document.select("div.wrapper>div.main-content") + document.select("div.container>div.fl>div.article-detail").toString() + document.select("div.detai-content>div.detail-main>div.content-left").toString());
            contentBo.setShapeType("2");
            contentBo.setPageLink(url);
            contentBo.setPublishTime(pushTime);
            contentBo.setPlatId(aid);
            contentBo.setCommentCount(context.read("$.commonModule.commentCnt", Integer.TYPE));
            contentBo.setLikeCount(context.read("$.commonModule.praiseCnt", Integer.TYPE));
            contentBo.setCover(context.read("$.commonModule.coverImageUrl"));
            contentBo.setAbstractInfo("$.commonModule.description");
            contentBo.setTitle(context.read("$.commonModule.title") + "");
            contentBo.setMusicDownloadUrl(sourceLink);
            String duplicateKey = CommonUtils.md5IdUrl(url + aid);
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }


        }
        logger.warn("途牛抓取结果：{}", JSON.toJSONString(contentBo));
        return contentBo;
    }
}
