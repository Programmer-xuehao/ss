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
        //本地测的时候只需要注意一下link即可
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
     * 抓取列表页
     *
     * 建议不加try，加了也请抛出，否则可能导致重试失效
     * @param params
     * @return 返回可能是URL/JSON/HTML字符串List
     * @throws Exception
     */
    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        logger.warn("梨视频抓取开始：", JSON.toJSONString(params));

        //设置列表页抓取最大执行几次，假设链接不正确，那么当前方法只会执行3次，一般不用设置
//        setListRetryCount(3);

        //http()这个方法就是封装了一下Jsoup，直接使用即可
        return http(params.get("link").toString())
                .get().select("#categoryList > li > div > a")
                .stream()
                .filter(Objects::nonNull)
                .map(a -> a.attr("abs:href"))
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemUrl, Map<String, Object> params) throws Exception {

        //设置详情页抓取最大执行几次，假设链接不正确，那么当前方法只会执行3次，一般不用设置
//        setDetailRetryCount(3);

        McnContentBo contentBo = new McnContentBo();
        //去重key拼接
        String duplicateKey = CommonUtils.md5IdUrl(itemUrl);
        //如果在详情抓取时不需要发请求，直接将contentBo.setDuplicateKey(duplicateKey);设置即可由父类自动去重
        //手动判断重复，放在请求前面，避免发生多余的请求
        //重复为true
//        if (duplicateKeyDao.containsKey(duplicateKey)) {
//            return null;//重复key,取消抓取
//        }

        Document document = http(itemUrl).get();
        long pushTime = DateUtil.parseTimeMinutes(document.select("#detailsbd div.date").text()).getTime();

        //按时间过滤，不是两天内的直接return null即可丢弃
        if (exceedPubTime(pushTime)) {
            return null;
        }

        //解析封装，尽量一行搞定，避免过多判断
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
