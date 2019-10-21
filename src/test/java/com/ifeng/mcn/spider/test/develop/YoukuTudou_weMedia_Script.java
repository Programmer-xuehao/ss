package com.ifeng.mcn.spider.test.develop;

import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 迁移脚本-优酷土豆
 * 优酷跟土豆公用
 * 不需要代理
 * 不需要解析视频地址
 * @author ZFang
 */
public class YoukuTudou_weMedia_Script extends CrawlerWorker {

    public static void main(String[] args) throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "https://id.tudou.com/i/UNTk2NjE0MDM4NA==/videos");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "qutoutiao_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "qutoutiao_http");
        params.put("mediaId", "UNTk2NjE0MDM4NA==");

        CrawlerWorker script = new YoukuTudou_weMedia_Script();
        script.params.set(params);

        List<String> list = script.crawlerListPage(params);
        for (String item : list) {
            System.err.println(item);
            script.crawlerDetailPage(item, params);
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        //https://id.tudou.com/i/UNTk2NjE0MDM4NA==/videos

        //从url中获取mid，优酷跟土豆都可以
        String link = params.get("link").toString();
        String mid = link.split("com/[a-z]/")[1].replaceAll("/videos", "");
        if (mid.contains("?")) {
            mid = mid.split("\\?")[0];
        }

        return http(String.format("https://id.tudou.com/i/%s/videos", mid))
                .get().select("div.items > div .v-link a")
                .stream().map(e -> e.attr("abs:href"))
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemUrl, Map<String, Object> params) throws Exception {
        String duplicateKey = CommonUtils.md5IdUrl(itemUrl);
        //本地main方法测试时需要注掉
        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;//重复key,取消抓取
        }
        //https://video.tudou.com/v/XNDM4NDY0Mjc2NA==.html
        String json = http(itemUrl).execute().body()
                .split("window.__INITIAL_STATE__= ")[1]
                .split(";</script>")[0];
        DocumentContext context = JsonPath.parse(json);

        String published_time = context.read("$.videoDesc.detail.published_time") + "";//"2019-10-03 09:38:30"

        McnContentBo contentBo = new McnContentBo();
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setPageLink(itemUrl);
        contentBo.setShapeType("2");//1 文章  2 视频  3 ALL
        contentBo.setTitle(context.read("$.videoDesc.detail.title"));
        contentBo.setCover(context.read("$.videoDesc.detail.img"));
        contentBo.setCommentCount(context.read("$.videoDesc.detail.total_comment", Integer.TYPE));
        contentBo.setPlayCount(context.read("$.videoDesc.detail.total_vv", Long.TYPE));
        contentBo.setPublishTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(published_time).getTime());
        contentBo.setPlatId(context.read("videoId"));
        contentBo.setMusicDownloadUrl(itemUrl);
        contentBo.setMediaId(context.read("$.channelDetail.userid"));
        contentBo.setMediaName(context.read("$.channelDetail.username"));

        //按时间过滤
        if (exceedPubTime(contentBo.getPublishTime())) {
            return null;
        }

        logger.info("土豆脚本抓取结果：{}" + contentBo);
        return contentBo;
    }
}
