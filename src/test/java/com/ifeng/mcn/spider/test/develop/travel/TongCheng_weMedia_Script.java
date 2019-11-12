package com.ifeng.mcn.spider.test.develop.travel;


import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 示例脚本-同程
 * 需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class TongCheng_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        Map map = Maps.newHashMap();
        map.put("id", "977503");
        map.put("link", "https://www.ly.com/travels/member/person.html?uid=ce2234221eaa3a9800fa7ea82ef01bac");
        map.put("mcnTaskId", "test001");
        map.put("taskType", "TongCheng_weMedia");
        map.put("crawlerType", "http");
        map.put("riskKeyPrefix", "TongCheng_http");


        TongCheng_weMedia_Script douyin_weMedia_script = new TongCheng_weMedia_Script();
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
        String mediaName = document.selectFirst("div.namecontent").text();
        String mediaId = crawlerUrl.split("uid")[1];
        params.put("mediaId", mediaId);
        params.put("mediaName", mediaName);
        return document.select("div.content_right")
                .stream().map(r -> r.html())
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemHtml, Map<String, Object> params) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//2018-07-23上传

        Element r = Jsoup.parse(itemHtml, "https://").selectFirst("body");
        Element a = r.selectFirst("p");
        String url = "https://www.ly.com" + a.selectFirst("a").attr("href");
        Document document = http(url).execute().parse();
        String tvid = url.split("ls/")[1].split(".html").toString();
        String time = r.select("span").text().split("发表于 ")[1] + ":00";
        String content = document.select("div.content").toString();
        String cover = "http:" + document.select("div.content>div.imgwith>img").attr("data-img-src");
        String AbstractInfo = r.select("a").get(1).text();
        Long pushTime = df.parse(time).getTime();
        //判重
        String duplicateKey = CommonUtils.md5IdUrl(params.get("taskType") + url);
        McnContentBo contentBo = new McnContentBo();
        contentBo.setPublishTime(pushTime);
        contentBo.setContent(content);
        contentBo.setCover(cover);
        contentBo.setPlatId(tvid);
        contentBo.setAbstractInfo(AbstractInfo);
        contentBo.setMediaId(params.get("mediaId") + "");
        contentBo.setMediaName(params.get("mediaName") + "");
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setShapeType("1");//1 文章  2 视频  3 ALL
        contentBo.setTitle(a.attr("data-title"));
        //按时间过滤
        if (exceedPubTime(pushTime)) {
            return null;
        }
        contentBo.setPublishTime(pushTime);
        contentBo.setPageLink(url);


        logger.warn("同程旅游抓取结果：{}", contentBo);
        //返回
        return contentBo;
    }
}