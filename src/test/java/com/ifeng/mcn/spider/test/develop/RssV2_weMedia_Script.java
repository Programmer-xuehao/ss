package com.ifeng.mcn.spider.test.develop;

import cn.edu.hfut.dmic.contentextractor.ContentExtractor;
import cn.edu.hfut.dmic.contentextractor.News;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 迁移脚本-RSS
 * 不需要代理
 *
 * @author ZFang
 */
public class RssV2_weMedia_Script extends CrawlerWorker {
    //标题过滤关键词组
    static List<String> titFilter = Lists.newArrayList("发稿目录", "发稿总目录", "改稿目录", "改稿总目录", "港澳台要闻目录", "重要稿件预告", "重要高访报道预告", "公鉴", "截稿", "SG线路");


    public static void main(String[] args) throws Exception {
        //http://rss.huanqiu.com/topnews.xml
        //https://m.thepaper.cn/rss_fenghuang.jsp
        //https://zkapi.myzaker.com/rss/?f=dafeng&app_id=15702
        //https://rsshub.app/bilibili/user/video/2267573
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "https://rss-api.fotomore.com/dafenghao/rss?channel=ent&pass=fj3AkqLUXgazmnR51yPwmXpjY98SNG97");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "qutoutiao_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "qutoutiao_http");

        RssV2_weMedia_Script script = new RssV2_weMedia_Script();
        script.params.set(params);
        List<String> list = script.crawlerListPage(params);
        list.forEach(er -> System.err.println(er));
        for (String item : list) {
            McnContentBo mcnContentBo = script.crawlerDetailPage(item, params);
            System.out.println(JSON.toJSONString(mcnContentBo));
        }

    }

    private String getPubDate(String itemPubDate) {
        if (StringUtils.isBlank(itemPubDate))
            return null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        itemPubDate = itemPubDate.replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
                .replace("时", ":")
                .replace("分", ":")
                .replace("秒", "")
                .replace("/", "-");

        try {
            if (itemPubDate.matches("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}")) {
                return simpleDateFormat.format(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(itemPubDate));
            }
            if (itemPubDate.matches("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}")) {
                return simpleDateFormat.format(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(itemPubDate));
            }
            if (itemPubDate.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                return simpleDateFormat.format(new SimpleDateFormat("yyyy-MM-dd").parse(itemPubDate));
            }

            if (itemPubDate.matches("\\d{2}-\\d{2}")) {
                return Calendar.getInstance().get(Calendar.YEAR) + '-' + itemPubDate + " 00:00:00";
            }

            TimeZone china = TimeZone.getTimeZone("Asia/Shanghai");
            simpleDateFormat.setTimeZone(china);
            Date date = new Date(itemPubDate);
            try {
                itemPubDate = simpleDateFormat.format(date);
            } catch (Exception e) {
                itemPubDate = java.time.LocalDateTime.parse(itemPubDate, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
//            e.printStackTrace();
            logger.error("getPubDate", e);
        }
        return itemPubDate;
    }

    private String replaceRSSContent(String content) {
        content = content.replace("<iframe", "<ｉｆｒａｍｅ")
                .replace("</iframe>", "</ｉｆｒａｍｅ>");
        content = content.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("<p></p>", "")
                .replace("<p><br></p>", "")
                .replace("<p><br/></p>", "")
                .replace("您的浏览器不支持播放本视频", "")
                .replaceAll("<p>\\s+</p>", "");
        return content;
    }

    //图集的正文改成固定的格式
    private String getGalleryContent(String content) {
        try {
            Whitelist imageSimplewhitelist = Whitelist.simpleText()
                    .removeTags("b", "em", "i", "strong", "u")
                    .addAttributes("img", "src", "data-src", "real_src", "real-src", "zoomfile", "file", "data-original", "alt_src", "original-src");
            content = Jsoup.clean(content, imageSimplewhitelist);
//            content = content.replace("&amp;", "&");
        } catch (Exception e) {
//            e.printStackTrace();
            logger.error("rss getGalleryContent", e);
        }
        return content;
    }

    private String getPublishTime(Element item) {
        String dateStr = item.select("pubDate").text();
        if (StringUtils.isBlank(dateStr) && item.select("pubdate") != null)
            dateStr = item.select("pubdate").text();
        if (StringUtils.isBlank(dateStr))
            return null;
        String itemPubDate = dateStr.replace("Asia/Shanghai", "") + "";
        itemPubDate = itemPubDate.replace("+0000", "+0800");
        try {
            itemPubDate = getPubDate(itemPubDate);
        } catch (Exception e) {
            // itemPubDate = java.time.LocalDateTime.parse(itemPubDate, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return itemPubDate;
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        setListRetryCount(2);
        List<String> link = null;
        logger.info("taskid:{};link：{}", (String) params.get("mcnTaskId"), params.get("link"));
        /*try {*/
            /*MySpider link1 = httpMySpider((String)params.get("link"), null).setDownloader(ProxyUtils.getProxyDownLoad()).setProxyFlag(true);
            Page run = link1.run();
            logger.info("taskid:{}，detail：{}",(String)params.get("mcnTaskId"),run.getRawText());*/
        link = http(params.get("link") + "").timeout(6000)
                .parser(Parser.xmlParser())
                .get().select("rss>channel>item").stream()
                .map(Element::html)
                .collect(Collectors.toList());
        /*} catch (Exception e) {
            logger.error("taskid:{}",(String)params.get("mcnTaskId"),e);
        }*/

        logger.info("taskid:{},result:{}", (String) params.get("mcnTaskId"), JSON.toJSON(link));

        return link;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemHtml, Map<String, Object> params) throws Exception {
        Document item = Jsoup.parse(itemHtml, "", Parser.xmlParser());
        String duplicateKey = CommonUtils.md5IdUrl(params.get("mediaId") + item.select("title").text());

        //本地main方法测试时需要注掉
        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;//重复key,取消抓取
        }

        String title = item.select("title").text();
        //过滤
        boolean anyMatch = titFilter.stream().anyMatch(kw -> title.contains(kw));
        if (anyMatch) {
            logger.info("标题关键词过滤：{}", title);
            return null;
        }
        if (title.startsWith("深度资讯 |") && "36氪".equals(params.get("params"))) {
            logger.info("36氪深度资讯过滤：{}", title);
            return null;
        }
        //处理文章和视频
        String link = item.select("link").text();
        String videoUrl = item.select("videoUrl").text();
        String content = item.select("content").text();
        String description = item.select("description").text();
        content = replaceRSSContent(StringUtils.isBlank(content) ? description : content);
        String imageUrl = "";
        if (item.select("imageUrl") != null) {
            imageUrl = item.select("imageUrl").text();
        }
        if (!StringUtils.isBlank(imageUrl)) {
            content = "<img src='" + imageUrl + "'' />" + content;
        }
        content = content.replaceAll("【如果您有新闻线索，欢迎向我们报料，一经采纳有费用酬谢。报料微信关注：\\w+，报料QQ：\\d+】", "");//特殊替换（封面新闻）

        //处理图集
        String contentType = "";
        if (item.select("content") != null)
            contentType = item.select("content").attr("type");
        String articleType = "article";
        if ("1".equals(contentType)) {//图集文章
            articleType = "ydslide";
            content = getGalleryContent(content);
        }
        if (item.text().length() < 15 && StringUtil.isBlank(videoUrl)) {
            if (item.select("video").size() > 0) {
                videoUrl = item.select("video").attr("abs:href");
            }
        }

        int haveVideo = 1;
        int haveAudio = 1;
        if (content.indexOf("<video") != -1)
            haveVideo = 2;
        if (content.indexOf("<audio") != -1)
            haveAudio = 2;

        McnContentBo contentBo = new McnContentBo();
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setShapeType(StringUtil.isBlank(videoUrl) ? "1" : "2");//1 文章  2 视频  3 ALL
        contentBo.setTitle(title);
        contentBo.setAbstractInfo(description);
        contentBo.setContent(content);
        contentBo.setPageLink(link);
        contentBo.setCover(imageUrl);
        contentBo.setHaveViedo(haveVideo);
        contentBo.setHaveAudio(haveAudio);
        contentBo.setMusicDownloadUrl(StringUtil.isBlank(videoUrl) ? link : videoUrl);

        try {
            long pushDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(getPublishTime(item)).getTime();
            contentBo.setPublishTime(pushDate);
            //按时间过滤
            if (exceedPubTime(pushDate)) {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //没解析到时间就按当前时间
        if (contentBo.getPublishTime() == null || contentBo.getPublishTime() <= 0) {
            contentBo.setPublishTime(System.currentTimeMillis());
        }

        //详情为空时走一次自动提取
        if (StringUtil.isBlank(content)) {
            try {
                String body = http(link).execute().body();
                News contentByHtml = ContentExtractor.getNewsByHtml(body);
                contentBo.setContent(contentByHtml.getContentElement().toString());
            } catch (Exception e) {
                logger.error("RSS走自动提取异常", e);
            }
        }

        logger.warn("RSS抓取结果{}", contentBo);
        return contentBo;
    }
}
