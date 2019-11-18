package com.ifeng.mcn.spider.test.develop.travel;


import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 示例脚本-去哪儿
 * 不需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class QuNar_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        Map map = Maps.newHashMap();
        map.put("link", "https://travel.qunar.com/space/165158952@qunar");
        map.put("mcnTaskId", "test001");
        map.put("taskType", "QuNar_weMedia");
        map.put("crawlerType", "http");
        map.put("riskKeyPrefix", "QuNar_http");

        QuNar_weMedia_Script douyin_weMedia_script = new QuNar_weMedia_Script();
        douyin_weMedia_script.params.set(map);
        List<String> result = douyin_weMedia_script.crawlerListPage(map);
        for (String item : result) {
            douyin_weMedia_script.crawlerDetailPage(item, map);
        }
    }

    /**
     * 返回一段HTML
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        String crawlerUrl = params.get("link").toString();
        Document document = http(crawlerUrl).execute().parse();
        String mediaId = crawlerUrl.split("space/")[1].split("@")[0];
        String mediaName = document.select("h3>span").get(0).text();
        params.put("mediaId", mediaId);
        params.put("mediaName", mediaName);
        return document.select("ul.timeline-ct>li")
                .stream().map(r -> r.html())
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemHtml, Map<String, Object> params) throws Exception {

        Element r = Jsoup.parse(itemHtml, "https://").selectFirst("body");
        String a1 = r.select("div.timeline-ico").toString().split("class=\"")[1].split("\"")[0];


        if (a1.equals("timeline-ico note-ico")) {
            String downloadUrl = "http:" + r.select("a").attr("href");
            String tvid = downloadUrl.split("youji/")[1];
            Document document = http(downloadUrl).execute().parse();
            String title = r.select("div.timeline-content>dl>dd.title").text();
            String abstracts = r.select("dl.noteitem>dd.intro").text();
            String Time = r.select("div.cdate").text().split("出发")[0];
            String time = Time + " 00:00:00";
            Integer commentCount = Integer.valueOf(r.select("div.stat>ul>li.comment").text());
            Integer likeCount = Integer.valueOf(r.select("div.stat>ul>li.zan").text());
            String localCoverPic = r.select("dl.noteitem>dd.pics>a>img").attr("data-original");
            String oriContent = document.select("div.main_leftbox").toString();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Long pushTime = df.parse(time).getTime();
            //判重
            String duplicateKey = CommonUtils.md5IdUrl(params.get("taskType") + tvid);
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }
            Calendar instance = Calendar.getInstance();
            McnContentBo contentBo = new McnContentBo();
            contentBo.setPlatId(tvid);
            contentBo.setMediaId(params.get("mediaId") + "");
            contentBo.setTitle(title);
            contentBo.setLikeCount(likeCount);
            contentBo.setCommentCount(commentCount);
            contentBo.setContent(oriContent);
            contentBo.setCover(localCoverPic);
            contentBo.setAbstractInfo(abstracts);
            contentBo.setMediaName(params.get("mediaName") + "");
            contentBo.setDuplicateKey(duplicateKey);
            contentBo.setShapeType("1");//1 文章  2 视频  3 ALL
            //按时间过滤
            if (exceedPubTime(pushTime)) {
                return null;
            }
            contentBo.setPublishTime(pushTime);
            contentBo.setPageLink(downloadUrl);
            logger.warn("去哪儿脚本抓取结果：{}", contentBo);
            //返回
            return contentBo;
        } else {
            return null;
        }

    }

}
