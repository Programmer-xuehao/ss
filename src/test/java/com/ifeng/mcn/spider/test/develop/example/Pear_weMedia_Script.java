package com.ifeng.mcn.spider.test.develop.example;

import com.alibaba.fastjson.JSON;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.DateUtil;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 示例脚本-梨视频
 * 不需要代理
 *
 * @author ZFang
 */
public class Pear_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "https://www.pearvideo.com/author_11549091");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "qutoutiao_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "qutoutiao_http");
        params.put("mediaId", "11549091");

        CrawlerWorker script = new Pear_weMedia_Script();
        script.params.set(params);

        List<String> list = script.crawlerListPage(params);
        for (String item : list) {
            System.err.println(item);
            script.crawlerDetailPage(item, params);
        }
    }

    /**
     * 返回一个URL
     *
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        logger.warn("梨视频抓取开始：", JSON.toJSONString(params));
        return http(params.get("link").toString())
                .get().select("#categoryList > li > div > a")
                .stream()
                .filter(Objects::nonNull)
                .map(a -> a.attr("abs:href"))
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemUrl, Map<String, Object> params) throws Exception {
        McnContentBo contentBo = new McnContentBo();
        String duplicateKey = CommonUtils.md5IdUrl(itemUrl);

        Document document = http(itemUrl).get();
        long pushTime = DateUtil.parseTimeMinutes(document.select("#detailsbd div.date").text()).getTime();
        //按时间过滤
        if (exceedPubTime(pushTime)) {
            return null;
        }
        String vUrl = document.html().split(",srcUrl=\"")[1].split("\",vdoUrl=")[0];
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setPublishTime(pushTime);
        contentBo.setTitle(document.select("#detailsbd div > div.box-left.clear-mar > h1").text());
        contentBo.setAbstractInfo(document.select("#detailsbd div.summary").text());
        contentBo.setCover(document.select("#poster > img").attr("src"));
        contentBo.setTag(document.select("div.tags > a > span").eachText());
        contentBo.setPageLink(itemUrl);
        contentBo.setShapeType("2");
        contentBo.setMusicDownloadUrl(vUrl);
        contentBo.setPlatId(document.select(".fav").attr("data-id"));
        logger.info("pearDetailResult：{}", JSON.toJSONString(contentBo));
        return contentBo;
    }
}