package com.ifeng.mcn.spider.test.develop.car;


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

import com.google.common.collect.Maps;


/**
 * 示例脚本-优酷
 * 不需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class YoukuQiche_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        Map map = Maps.newHashMap();
        map.put("link", "https://i.youku.com/i/UNTg1NDg4NTY=?spm=a2hzp.8244740.0.0");
        map.put("mcnTaskId", "test001");
        map.put("taskType", "YoukuQiche_weMedia");
        map.put("crawlerType", "http");
        map.put("riskKeyPrefix", "YoukuQiche_http");
        map.put("mediaId", "64269185197");
        map.put("mediaName", "优酷视频");

        YoukuQiche_weMedia_Script douyin_weMedia_script = new YoukuQiche_weMedia_Script();
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
        String crawlerUrl = (String) params.get("link");
        Document document = http(crawlerUrl).execute().parse();
        String mediaId = document.selectFirst("div.head-avatar>a").attr("href").split("i/")[1].split("=")[0];
        String mediaName = document.selectFirst("div.head-avatar>a").attr("title");
        params.put("mediaId", mediaId);
        params.put("mediaName", mediaName);
        return document.select("div.v")
                .stream().map(r -> r.html())
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemHtml, Map<String, Object> params) throws Exception {
        Element r = Jsoup.parse(itemHtml, "https://").selectFirst("body");

        //判重
        String url = "http:" + r.selectFirst("a").attr("href");
        Document document = http(url).execute().parse();
        String publishTime = document.select("meta").get(21).attr("content");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String duplicateKey = CommonUtils.md5IdUrl(params.get("taskType") + url);
        String AbstractInfo = document.toString().split("视频内容简介:")[1].split("\">")[0];
        String title = r.selectFirst("a").attr("title");
        String tvid = url.split("id_")[1].split("==")[0];
        long pushTime = df.parse(publishTime).getTime();
        String curl = r.select("div").get(0).select("img").attr("src");
        McnContentBo contentBo = new McnContentBo();
        contentBo.setPlatId(tvid);
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setShapeType("2");//1 文章  2 视频  3 ALL
        contentBo.setPublishTime(pushTime);
        contentBo.setTitle(title);
        contentBo.setCover(curl);
        contentBo.setAbstractInfo(AbstractInfo);
        contentBo.setMediaId(params.get("mediaId") + "");
        contentBo.setMediaName(params.get("mediaName") + "");
        //按时间过滤
        if (exceedPubTime(pushTime)) {
            return null;
        }
        contentBo.setPageLink(url);


        logger.warn("优酷脚本抓取结果：{}", contentBo);
        //返回
        return contentBo;
    }
}