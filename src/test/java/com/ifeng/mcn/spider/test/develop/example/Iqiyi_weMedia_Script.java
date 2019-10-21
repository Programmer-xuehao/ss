package com.ifeng.mcn.spider.test.develop.example;

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
 * 示例脚本-爱奇艺
 * 不需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class Iqiyi_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        Map map = Maps.newHashMap();
        map.put("link", "http://www.iqiyi.com/u/1479459391/v");
        map.put("mcnTaskId", "test001");
        map.put("taskType", "douyin_weMedia");
        map.put("crawlerType", "http");
        map.put("riskKeyPrefix", "douyin_http");
        map.put("mediaId", "64269185197");
        map.put("mediaName", "中国日报");

        Iqiyi_weMedia_Script douyin_weMedia_script = new Iqiyi_weMedia_Script();
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
        if (!crawlerUrl.endsWith("/v")) {
            crawlerUrl += "/v";
        }
        Document document = http(crawlerUrl).execute().parse();
        String mediaId = document.selectFirst("[data-userid]").attr("data-userid");
        String mediaName = document.selectFirst("div.pf_username > span").text();
        params.put("mediaId", mediaId);
        params.put("mediaName", mediaName);
        return document.select("li:has(.site-piclist_pic)")
                .stream().map(r -> r.html())
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemHtml, Map<String, Object> params) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd上传");//2018-07-23上传

        Element r = Jsoup.parse(itemHtml, "https://").selectFirst("body");
        Element a = r.selectFirst(".site-piclist_pic a");
        String tvid = a.attr("data-tvid");

        //判重
        String duplicateKey = CommonUtils.md5IdUrl(params.get("taskType") + tvid);

        String tl = r.select("span.playTimes_status.tl").text();

        Calendar instance = Calendar.getInstance();
        long pushTime = 0L;
        if (tl.matches("\\d+小时前上传")) {//18小时前上传
            instance.set(Calendar.HOUR, instance.get(Calendar.HOUR) - Integer.parseInt(tl.replaceAll("\\D", "")));
            pushTime = instance.getTimeInMillis();
        } else if (tl.equals("昨日上传")) {//昨日上传
            instance.set(Calendar.DAY_OF_MONTH, instance.get(Calendar.DAY_OF_MONTH - 1));
            pushTime = instance.getTimeInMillis();
        } else if (tl.matches("\\d{4}-\\d{2}-\\d{2}上传")) {//2018-07-23上传
            pushTime = dateFormat.parse(tl).getTime();
        }


        McnContentBo contentBo = new McnContentBo();
        contentBo.setPlatId(tvid);
        contentBo.setMediaId(params.get("mediaId") + "");
        contentBo.setMediaName(params.get("mediaName") + "");
        contentBo.setMusicDownloadUrl(a.attr("abs:href"));
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setShapeType("2");//1 文章  2 视频  3 ALL
        contentBo.setTitle(a.attr("data-title"));

        //按时间过滤
        if (exceedPubTime(pushTime)) {
            return null;
        }
        contentBo.setPublishTime(pushTime);
        contentBo.setPageLink(a.attr("abs:href"));
        contentBo.setCover(a.select("img").attr("abs:src"));

        logger.warn("爱奇艺脚本抓取结果：{}", contentBo);
        //返回
        return contentBo;
    }
}