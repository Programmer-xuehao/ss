package com.ifeng.mcn.spider.test.develop.travel;

import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.stream.Collectors;

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
 * 示例脚本-LvMama
 * 不需要代理
 * 不需要解析视频地址
 *
 * @author ZFang
 */
public class LvMama_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        Map map = Maps.newHashMap();
        map.put("link", "http://www.lvmama.com/trip/user/9182");
        map.put("mcnTaskId", "test001");
        map.put("taskType", " LvMama_weMedia");
        map.put("crawlerType", "http");
        map.put("riskKeyPrefix", " LvMama_http");
        LvMama_weMedia_Script douyin_weMedia_script = new LvMama_weMedia_Script();
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

        Document document = http(crawlerUrl).timeout(50000).execute().parse();
        String mediaId = crawlerUrl.split("com/trip/user/")[1];
        String mediaName = document.select("i.lydaren_name").text();
        params.put("mediaId", mediaId);
        params.put("mediaName", mediaName);
        return document.select("div.profile_travel_array")
                .stream().map(r -> r.html())
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemHtml, Map<String, Object> params) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//2018-07-23上传

        Element r = Jsoup.parse(itemHtml, "https://").selectFirst("body");
        String downloadUrl = r.select("div.pic_new>a").attr("href");
        String title = r.select("p").get(0).text();
        String tvid = r.select("div.pic_new>a").attr("href").split("/")[5];
        String abstracts = r.select("p.relative_path").text();
        String Time = r.select("p.updateInfo").text().split("发布")[0] + " 00:00:00";
        Integer commentCount = Integer.valueOf(r.select("p.tripViews>a").toString().split("</i>")[2].split("</a>")[0]);
        Integer likeCount = Integer.valueOf(r.select("div.iLike>p").text());
        String localCoverPic = r.select("div.pic_new>a>img").attr("to");
        Document document = http(downloadUrl).execute().parse();
        String oriContent = document.select("div.ebm-post").get(0).toString();
        //判重
        String duplicateKey = CommonUtils.md5IdUrl(params.get("taskType") + tvid);
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;//重复key,取消抓取
        }
        Long pushTime = df.parse(Time).getTime();
        McnContentBo contentBo = new McnContentBo();
        contentBo.setPlatId(tvid);
        contentBo.setMediaId(params.get("mediaId") + "");
        contentBo.setMediaName(params.get("mediaName") + "");
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setShapeType("1");//1 文章  2 视频  3 ALL
        contentBo.setMediaId(params.get("mediaId") + "");
        contentBo.setTitle(title);
        contentBo.setLikeCount(likeCount);
        contentBo.setCommentCount(commentCount);
        contentBo.setContent(oriContent);
        contentBo.setCover(localCoverPic);
        contentBo.setAbstractInfo(abstracts);
         //按时间过滤
        if (exceedPubTime(pushTime)) {
            return null;
        }
        contentBo.setPublishTime(pushTime);
        contentBo.setPageLink(downloadUrl);


        logger.warn("驴妈妈脚本抓取结果：{}", contentBo);
        //返回
        return contentBo;
    }
}
