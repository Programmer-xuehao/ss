package com.ifeng.mcn.spider.test.develop.car;


import com.alibaba.fastjson.JSON;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 示例脚本-车讯
 * 需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class CheXun_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {

        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "http://reg.chexun.com/ucdefaultx.aspx?userId=1096315");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "CheXun_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "CheXun_http");


        CheXun_weMedia_Script script = new CheXun_weMedia_Script();
        script.params.set(params);

        List<String> list = script.crawlerListPage(params);
        for (String item : list) {
            script.crawlerDetailPage(item, params);
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        String link = params.get("link").toString();
        String mid = link.replace("http://reg.chexun.com/ucdefaultx.aspx?userId=", "");
        Document document = http(link).execute().parse();
        String mediaId = mid;
        String mediaName = document.select("div.u-i-left>h2").text();
        params.put("mediaId", mediaId);
        params.put("mediaName", mediaName);
        String api = "https://api.chexun.com//pc/user/items?userId=" + mid + "&entityType=0&subType=0&pageNo=1&pageSize=10&myUserId=";
        //请求文章列表
        String body = http(api).execute().body();

        List<String> collect = JsonPath.parse(body).read("$.data.list", JSONArray.class)
                .stream()
                .map(JSON::toJSONString)
                .collect(Collectors.toList());
        return collect;

    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        DocumentContext context = JsonPath.parse(itemBody);
        McnContentBo contentBo = new McnContentBo();
        if (context.read("playUrl").toString().indexOf(".mp4") != -1) {
            //https://space.bilibili.com/ajax/member/getSubmitVideos?mid=30019305&page=1&pagesize=25
            //视频解析
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = context.read("$.createTime").toString().split("T")[0] + " " + context.read("$.createTime").toString().split("T")[1].split(".000")[0];
            Long publishTime = df.parse(time).getTime();
            String aid = context.read("$.id") + "";
            String duplicateKey = CommonUtils.md5IdUrl("url" + "");
            //手动判断重复，放在请求前面，避免发生多余的请求
            //重复为true

            contentBo.setPublishTime(publishTime);
            contentBo.setShapeType("2");//1文章  2 视频  3 ALL
            contentBo.setDuplicateKey(duplicateKey);
            contentBo.setPageLink("url" + "");
            contentBo.setCommentCount(context.read("$.commentCount", Integer.TYPE));
            contentBo.setLikeCount(context.read("thumbsCountsStr", Integer.TYPE));
            contentBo.setCover(context.read("$.videoCover") + "");
            contentBo.setPlatId(aid);
            contentBo.setMediaId(params.get("mediaId") + "");
            contentBo.setMediaName(params.get("mediaName") + "");
            contentBo.setMusicDownloadUrl(contentBo.getPageLink());
            contentBo.setTitle(context.read("$.title") + "");
            contentBo.setAbstractInfo(context.read("$.description") + "");
            contentBo.setPlayCount(context.read("$.uVContent", Long.TYPE));
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }
            System.err.println("视频抓取结果:" + contentBo);
        } else {
            //https://api.bilibili.com/x/space/article?mid=30019305&pn=1&ps=12&sort=publish_time&jsonp=jsonp
            //文章解析
            String aid = context.read("$.id") + "";
            String duplicateKey = CommonUtils.md5IdUrl("url" + "");
            //手动判断重复，放在请求前面，避免发生多余的请求
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = context.read("$.createTime").toString().split("T")[0] + " " + context.read("$.createTime").toString().split("T")[1].split(".000")[0];
            Long publishTime = df.parse(time).getTime();
            String curl = context.read("$.videoCover") + "";
            String content = http(context.read("$.url") + "").get().select("div.container>div.fl>div.article-detail").html();
            //按时间过滤

            contentBo.setPublishTime(publishTime);
            contentBo.setShapeType("1");//1 文章  2 视频  3 ALL
            contentBo.setTitle(context.read("$.title") + "");
            contentBo.setAbstractInfo(context.read("$.description") + "");
            contentBo.setCover(curl);
            contentBo.setShareCount(context.read("$.shareCount", Integer.TYPE));
            contentBo.setPlatId(aid);
            contentBo.setMediaId(params.get("mediaId") + "");
            contentBo.setMediaName(params.get("mediaName") + "");
            contentBo.setCommentCount(context.read("$.commentCount", Integer.TYPE));
            contentBo.setLikeCount(context.read("thumbsCountsStr", Integer.TYPE));
            contentBo.setCover(context.read("$.videoCover") + "");
            contentBo.setContent(content);
            if (exceedPubTime(contentBo.getPublishTime())) {
                return null;
            }
            System.err.println("文章抓取结果:" + contentBo);
        }
        logger.warn("车讯汽车抓取结果：{}", JSON.toJSONString(contentBo));
        return contentBo;
    }
}

