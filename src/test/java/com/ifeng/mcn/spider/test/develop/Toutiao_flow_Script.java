package com.ifeng.mcn.spider.test.develop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.base.ContentType;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.MySpider;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import java.util.List;
import java.util.Map;

/**
 * 头条 信息流 任务
 *
 * @author chenghao1
 * @create 2019/10/9
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */

public class Toutiao_flow_Script extends CrawlerWorker {

    private String cookie = "uuid=\"w:100b835531ee4a3fbbf98262cbb013d1\"; tt_webid=6535309099436738051; WEATHER_CITY=%E5%8C%97%E4%BA%AC; csrftoken=e838086ba7544dbd7fcd34110a2ce453; tt_webid=6535309099436738051; UM_distinctid=16a10a9968aa33-074f2ef6375ec-7a1437-1fa400-16a10a9968b7da; tt_track_id=afff81ad39b267311223a07a1f6d9896; __tasessionId=hcybe2y4s1562145259482; CNZZDATA1259612802=89339089-1521615038-%7C1562143287";

    // 测试
    public static void main(String[] args) throws Exception {



        Toutiao_flow_Script flow_script = new Toutiao_flow_Script();
        Connection connection = flow_script.http("https://www.toutiao.com/i6749075578839630350/", null);
        String s = connection.execute().parse().body().toString();
        System.out.println("-------"+s);
        Map map = Maps.newHashMap();
        flow_script.params.set(map);
        map.put("link", "https://www.toutiao.com/api/pc/feed/?category=news_tech");
        map.put("mcnTaskId", "test001");
        map.put("taskType", "toutiao_flow");
        map.put("crawlerType", "http");
        map.put("riskKeyPrefix", "toutiao_flow_http");
        List<String> list = flow_script.crawlerListPage(map);
        System.out.println(JSON.toJSONString(list));
        McnContentBo mcnContentBo = flow_script.crawlerDetailPage(list.get(0), map);
        System.out.println("size:" + list.size() + "--" + mcnContentBo.toString());

    }


    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {

       /* 未来 可能要新增的
        String internet = "https://www.toutiao.com/api/pc/feed/?category=internet";//&utm_source=toutiao&widen=1&max_behot_time=0&max_behot_time_tmp=0&tadrequire=true&as=A1B5BDA18C59B7E&cp=5D1C39BBA74E5E1&_signature=TWd0KAAAEHxEKw49rV3hiU1ndD
        String software = "https://www.toutiao.com/api/pc/feed/?category=software";//&utm_source=toutiao&widen=1&max_behot_time=0&max_behot_time_tmp=0&tadrequire=true&as=A195DD318C29B99&cp=5D1CF94BE9095E1&_signature=TYq2kQAAEJdExsyEFKkR1k2Kto
        String smart_home = "https://www.toutiao.com/api/pc/feed/?category=智能家居";//&utm_source=toutiao&widen=1&max_behot_time=0&max_behot_time_tmp=0&tadrequire=true&as=A155CDE1FC79BC7&cp=5D1CA99B2CA73E1&_signature=TdjsZgAAEMVElJZzGMetq03Y7H
        String new_baby = "https://www.toutiao.com/api/pc/feed/?category=news_baby";//&utm_source=toutiao&widen=1&max_behot_time=0&max_behot_time_tmp=0&tadrequire=true&as=A155EDD1BC59BE1&cp=5D1CD9AB9EF16E1&_signature=TcLdIwAAEN9Ejqc2MEWiXk3C3T
        String news_regimen = "https://www.toutiao.com/api/pc/feed/?category=news_regimen";//&utm_source=toutia*/
        /*ArrayList<String> strings = Lists.newArrayList("https://www.toutiao.com/api/pc/feed/?category=news_tech",
                "https://www.toutiao.com/api/pc/feed/?category=news_history",
                "https://www.toutiao.com/api/pc/feed/?category=news_finance"
        );*/
        logger.info("params:{}", JSON.toJSONString(params));
        Connection http = http((String) params.get("link"), null);
        //http.header("cookie", cookie).timeout(5000);
        Site site = Site.me().setUserAgent(MySpider.getRandomUa()).addHeader("cookie",cookie).setRetryTimes(6).setSleepTime(1000).setTimeOut(3000);
        String body = httpMySpider((String) params.get("link"),null).setSite(site).run().getRawText();
        //  logger.warn("flow_result:{}",body);
        JSONObject jsonObject = JSON.parseObject(body);
        JSONArray datas = jsonObject.getJSONArray("data");
        List<String> result = Lists.newArrayList();
        datas.forEach(x -> {
            JSONObject itemJson = null;
            itemJson = (JSONObject) x;
            String article_genre = itemJson.getString("article_genre");
            //  不要视频
            if ("video".equals(article_genre)) {
                return;
            }
            //  不要问答
            if ("悟空问答".equals(itemJson.getString("source"))) {
                return;
            }
            result.add(itemJson.toJSONString());
        });
        logger.warn("flow_result2 , size:{}", result.size());


        return result;
    }


    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        McnContentBo mcnContentBo = new McnContentBo();
        JSONObject itemJson = JSON.parseObject(itemBody);
        //去重  排除
        String media_url = itemJson.getString("media_url");

        String duplicateKey = CommonUtils.md5IdUrl(media_url);

        String behot_time = itemJson.getString("behot_time");

        Long publishTime = Long.valueOf(behot_time) * 1000;
        logger.warn("pubtime ----{}", publishTime);
        // 时间 排除
        boolean b = exceedPubTime(publishTime);
        if (b) {
            logger.info("exceed 2 days :{}", media_url);
            return null;
        }
        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;//重复key,取消抓取
        }
        String articleCategory = itemJson.getString("tag");
        String itemIdUrl = "https://www.toutiao.com/item/" + itemJson.get("item_id") + "/";
        //Connection http = http(itemIdUrl, null);
        //  http.header("cookie",cookie) ;
        logger.warn("url ----{}", itemIdUrl);
        //Document parse = http.execute().parse();
        MySpider mySpider = httpMySpider(itemIdUrl, null);
        Page run = mySpider.run();
        String article_genre = itemJson.getString("article_genre");

//        String content = article_genre.equals("gallery") ? ScriptUtil.getGalleryContext(Jsoup.parse(run.getRawText())) : ScriptUtil.getArticleContent(Jsoup.parse(run.getRawText()));
//        if (StringUtils.isBlank(content)) {
//            logger.error("toutiao;content:null;url:{}", itemIdUrl);
//            return null;
//        }

        String coverUrl = itemJson.getString("image_url");
        if (coverUrl != null && coverUrl.indexOf("list/") != -1) {
            coverUrl = coverUrl.replace("list/", "origin/");
            coverUrl = coverUrl.replaceAll("/\\d+x\\d+", "");
        }
        if (coverUrl != null && coverUrl.startsWith("//")) {

            coverUrl = "http:" + coverUrl;
        }
        String user_id = itemJson.getString("media_url");
        if (user_id == null) {
            logger.error("toutiaoflow;mediaIdUrl:null");
            return null;
        }
        user_id = user_id.replace("/c/user/", "").replace("/", "");
        mcnContentBo.setShapeType(ContentType.ARTICLE.getCode());
        mcnContentBo.setDuplicateKey(duplicateKey);
        mcnContentBo.setCatogery(articleCategory);
        mcnContentBo.setPublishTime(publishTime);
        mcnContentBo.setCover(coverUrl);
        mcnContentBo.setTitle(itemJson.getString("title"));
        mcnContentBo.setAbstractInfo(itemJson.getString("abstract"));
        mcnContentBo.setFrom(itemJson.getString("source"));
        mcnContentBo.setMediaId(user_id);
        mcnContentBo.setMediaName(itemJson.getString("source"));
        mcnContentBo.setPageLink(itemIdUrl);
//        mcnContentBo.setContent(content);
        logger.info("toutiaoloe;content:{}", JSON.toJSONString(mcnContentBo));
        return mcnContentBo;
    }
}
