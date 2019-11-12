package com.ifeng.mcn.spider.test.develop.car;


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
 * 示例脚本-WangTongShe
 * 不需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class WangTongShe_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        Map map = Maps.newHashMap();
        map.put("link", "http://auto.news18a.com/editor/31968/");
        map.put("mcnTaskId", "test001");
        map.put("taskType", "WangTongShe_weMedia");
        map.put("crawlerType", "http");
        map.put("riskKeyPrefix", "WangTongShe_http");
        map.put("mediaId", "64269185197");
        map.put("mediaName", "中国日报");

        WangTongShe_weMedia_Script douyin_weMedia_script = new WangTongShe_weMedia_Script();
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
        String mediaId = crawlerUrl.split("editor/")[1].split("/")[0];
        String mediaName = document.select("p").get(2).select("a").get(2).text();
        params.put("mediaId", mediaId);
        params.put("mediaName", mediaName);
        return document.select("div.ina_list>dl.ina_wenzhang")
                .stream().map(r -> r.html())
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemHtml, Map<String, Object> params) throws Exception {

        Element r = Jsoup.parse(itemHtml, "https://").selectFirst("body");
        String downloadUrl = "http:" + r.select("div.ina_dongtai_bt>h3>a").attr("href");
        String tvid = downloadUrl.split("storys_")[1].split(".html")[0];
        Document document = http(downloadUrl).execute().parse();
        String title = r.select("div.ina_dongtai_bt>h3>a").text();
        String abstracts = r.select("a").get(2).text();
        String time = document.select("p>span.ina_data").text() + ":00";
        Integer commentCount = Integer.valueOf(document.select("div.ina_plbt").attr("data-counts"));
        String localCoverPic = "http:" + r.select("dt>a>img").attr("data-original");
        String oriContent = document.select("div.ina_content").toString();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long pushTime = df.parse(time).getTime();
        String duplicateKey = CommonUtils.md5IdUrl(params.get("taskType") + tvid);
        McnContentBo contentBo = new McnContentBo();
        contentBo.setPlatId(tvid);
        contentBo.setTitle(title);
        contentBo.setAbstractInfo(abstracts);
        contentBo.setPublishTime(pushTime);
        contentBo.setMediaId(params.get("mediaId") + "");
        contentBo.setMediaName(params.get("mediaName") + "");
        contentBo.setCover(localCoverPic);
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setShapeType("1");//1 文章  2 视频  3 ALL
        contentBo.setContent(oriContent);
        contentBo.setPageLink(downloadUrl);

        //按时间过滤
        if (exceedPubTime(pushTime)) {
            return null;
        }
        logger.warn("网通社抓取结果：{}", contentBo);
        //返回
        return contentBo;
    }
}