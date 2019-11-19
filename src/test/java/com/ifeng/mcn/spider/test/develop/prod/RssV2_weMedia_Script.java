package com.ifeng.mcn.spider.test.develop;

import cn.edu.hfut.dmic.contentextractor.ContentExtractor;
import cn.edu.hfut.dmic.contentextractor.News;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ifeng.mcn.common.base.ContentType;
import com.ifeng.mcn.common.utils.StringUtil;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.log.client.CommonUtils;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 迁移脚本-RSS
 * 不需要代理
 * <p>
 * RSS脚本务必每次从ide复制粘贴到后台保存
 * RSS脚本务必每次从ide复制粘贴到后台保存
 * RSS脚本务必每次从ide复制粘贴到后台保存
 * RSS脚本务必每次从ide复制粘贴到后台保存
 *
 * @author ZFang
 */
public class RssV2_weMedia_Script extends CrawlerWorker {
    private static final List<String> imgName = Arrays.asList(".jpg", ".png", ".jpeg", ".gif");
    //标题过滤关键词组
    static List<String> titFilter = Lists.newArrayList("发稿目录", "发稿总目录", "改稿目录", "改稿总目录", "港澳台要闻目录", "重要稿件预告", "重要高访报道预告", "公鉴", "截稿", "SG线路");
    private String[] localProxy = {"10.80.139.152:8888", "10.80.139.153:1081"};

    public static void main(String[] args) throws Exception {
        ///////////////

        //http://rss.huanqiu.com/topnews.xml
        //https://m.thepaper.cn/rss_fenghuang.jsp
        //https://zkapi.myzaker.com/rss/?f=dafeng&app_id=15702
        //https://rsshub.app/bilibili/user/video/2267573
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "http://www.thecover.cn/getOriginalRssForToutiao");
        params.put("link", "https://3w.huanqiu.com/rss?agt=86");
//        params.put("link", "http://weifang.dzwww.com/yl/yulerss/index.xml");
        params.put("link", "http://www.cankaoxiaoxi.com/xml/fenghuang.xml");
        params.put("link", "http://www.zqrb.cn/rss.xml");
//        params.put("link", "http://app.eeo.com.cn/?app=rss&controller=ifeng&action=index");
        params.put("link", "http://www.thecover.cn/getOriginalRssForToutiao");
        params.put("link", "http://module.iqilu.com/fhdfh/dfh");
        params.put("link", "http://www.thecover.cn/getOriginalRssForToutiao");
        params.put("link", "http://voice.hupu.com/generated/voice/news_nba.xml?name=ifeng");
        params.put("link", "https://www.dgtle.com/rss/dgtle.xml");
        params.put("link", "https://m.thepaper.cn/rss_fenghuang.jsp");
        params.put("link", "https://www.shobserver.com/fenghuang/rss.xml");
        params.put("link", "http://3g.sdchina.com/sinarss/news.xml");
        params.put("link", "http://app.iheima.com/rss/wyh.xml");
        params.put("link", "http://www.dsynews.cn/e/web/?type=rss2&order=0&orderby=0&classid=8|http://www.dsynews.cn/e/web/?type=rss2&order=0&orderby=0&classid=0\t");
        params.put("link", "http://rss.leju.com/rss/show/index?id=62");
        params.put("link", "http://www.investorchina.cn/console/rss/swcj/list2.xml");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "rss_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "rss_weMediao_http");

        RssV2_weMedia_Script script = new RssV2_weMedia_Script();
        script.params.set(params);
        List<String> list = script.crawlerListPage(params);
        list.forEach(er -> System.err.println(er + System.lineSeparator() + System.lineSeparator() + System.lineSeparator() + System.lineSeparator()));
        for (String item : list) {
            McnContentBo mcnContentBo = script.crawlerDetailPage(item, params);
            System.err.println(JSON.toJSONString(mcnContentBo));
        }

//        script.uploadAndRefresh();

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
        logger.info("rss list page params:{}", params);
        setListRetryCount(2);
        List<String> link = new ArrayList<>();
        logger.info("taskid:{};link：{}", params.get("mcnTaskId"), params.get("link"));
        /*try {*/
            /*MySpider link1 = httpMySpider((String)params.get("link"), null).setDownloader(ProxyUtils.getProxyDownLoad()).setProxyFlag(true);
            Page run = link1.run();
            logger.info("taskid:{}，detail：{}",(String)params.get("mcnTaskId"),run.getRawText());*/

        try {
            String body = http(params.get("link") + "").timeout(8000).get().toString();
            try {
                for (Node node : DocumentHelper.parseText(body).selectNodes("//rss/channel/item")) {
                    link.add(node.asXML());
                }
            } catch (DocumentException e) {
                if (params.get("link").toString().contains("www.investorchina.cn/console/rss")) {
                    Document document = Jsoup.parse(body, "", Parser.xmlParser());
                    Elements items = document.select("rss channel item");
                    for (Element item : items) {
                        link.add(item.toString());
                    }
                    return link;
                } else {
                    logger.error("RSS 转为dom4j Document时出错", e);
                    throw e;
                }
            }
//            link.addAll(nodes);
            /*} catch (Exception e) {
                logger.error("taskid:{}",(String)params.get("mcnTaskId"),e);
            }*/
            if (null == link || link.isEmpty()) {
                String proxy = localProxy[RandomUtils.nextInt(0, 2)];
                String[] proxys = proxy.split(":");
                body = http(params.get("link") + "").timeout(8000).proxy(proxys[0], Integer.parseInt(proxys[1])).get().toString();
                for (Node node : DocumentHelper.parseText(body).selectNodes("//rss/channel/item")) {
                    link.add(node.asXML());
                }

                logger.info("proxy  ---taskid:{},result:{}", params.get("mcnTaskId"), JSON.toJSON(link));
            }
            logger.info("taskid:{},result size:{}", params.get("mcnTaskId"), link.size());
        } catch (Exception e) {
            logger.error("rss crawler list Err:{}", params, e);
        }

        return link;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemHtml, Map<String, Object> params) throws Exception {
        Document item = Jsoup.parse(itemHtml, "", Parser.xmlParser());
        String duplicateKey = CommonUtils.md5IdUrl(params.get("mediaName") + item.select("title").text());

        //本地main方法测试时需要注掉
        //重复为true
        if (item.select("title").text().length() > 300) {
            File file = new File(UUID.randomUUID().toString() + ".txt");
            FileUtils.writeStringToFile(file, itemHtml + System.lineSeparator() + System.lineSeparator() + System.lineSeparator() + System.lineSeparator() + System.lineSeparator() + System.lineSeparator()
                    + params.toString()
            );
            logger.warn("大于300字的神仙标题：path = {} ", file.getAbsolutePath());
        }
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            logger.warn("taskId:{},title:{}", params.get("mcnTaskId"), item.select("title").text());
            return null;//重复key,取消抓取
        }

        boolean hasTitle = !item.select("title").isEmpty();
        boolean isXml = item.select("title").toString().contains("[CDATA[");

        String title = hasTitle && isXml ? item.selectFirst("title").data().trim() : item.select("title").text();

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
        Elements imageUrls = item.select("imageUrl");
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imageUrl = item.select("imageUrl").text();
        } else {
            Document parse = Jsoup.parse(content);
            Elements as = parse.select("a");
            out:
            for (Element a : as) {
                if (StringUtil.isNotBlank(a.attr("href"))) {
                    for (String n : imgName) {
                        if (a.attr("href").contains(n)) {
                            imageUrl = a.attr("href");
                            break out;
                        }
                    }
                }
            }
        }
//        if (StringUtil.isNotBlank(imageUrl)) {
////            content = "<img src='" + imageUrl + "'' />" + content;
//        } else {
//            logger.info("rss 文章不存在封面图:{},{}", link, videoUrl);
//            //return null;
//        }
        content = content.replaceAll("【如果您有新闻线索，欢迎向我们报料，一经采纳有费用酬谢。报料微信关注：\\w+，报料QQ：\\d+】", "");//特殊替换（封面新闻）

        //处理图集
        String contentType = "";
        if (item.select("content") != null)
            contentType = item.select("content").attr("type");
        ContentType articleType = null;
        if ("1".equals(contentType)) {//图集文章
            articleType = ContentType.GALLARY;
            content = getGalleryContent(content);
        }
//        if (item.text().length() < 15 && StringUtil.isBlank(videoUrl)) {
//        }
        if (item.select("video").size() > 0) {
            videoUrl = item.select("video").attr("src");
            if (StringUtil.isBlank(videoUrl)) {
                videoUrl = item.select("video").attr("data-src");
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
        contentBo.setShapeType(StringUtils.isBlank(videoUrl) ? "1" : "2");//1 文章  2 视频  3 图集
        if (articleType != null) {
            contentBo.setShapeType(ContentType.GALLARY.getCode());
        }
        //https://m.thecover.cn/video_details.html?from=web&id=2945767
        if (link.contains("thecover.cn/video_details.html")) {
            contentBo.setShapeType("2");
        }
        if (link.contains("thecover.cn")) {
            content = content.replace("您的浏览器不支持此视频格式</video>", "</video>");
        }
        if (link.contains("thepaper.cn") && StringUtil.isNotBlank(videoUrl)) {
            String videoStr = "<video src=\"%s\"></video>";
            content = String.format(videoStr, videoUrl) + content;
            System.err.println(content);
        }

        contentBo.setSourceLink(params.get("link") + "");
        contentBo.setTitle(title);
        String textDescription = description;
        if (StringUtil.isNotBlank(textDescription)) {
            textDescription = Jsoup.parse(textDescription).text();
        }
        contentBo.setAbstractInfo(textDescription);
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
        //需要走自动提取的链接
        ArrayList<String> autoCrawlerList = new ArrayList<>();
        autoCrawlerList.add("http://www.gs090.com/index.php?m=content&c=rss&rssid=49");
        autoCrawlerList.add("http://www.zui.ms/feed");
        autoCrawlerList.add("http://www.rmzs.net/rss.php");
        autoCrawlerList.add("http://www.kfarts.com/rss.php");
        autoCrawlerList.add("http://www.otcbeta.com/portal.php?mod=rss&catid=14");
        autoCrawlerList.add("http://www.dingbian.net/e/web/?");
        autoCrawlerList.add("https://www.fenxiangbe.com/feed");
        autoCrawlerList.add("https://hezibuluo.com/feed");
        autoCrawlerList.add("http://i.smartshe.com/feed/rss.php?mid=21&catid=5");
        autoCrawlerList.add("https://www.rugaow.cc/forum.php?mod=rss&fid=2");
        autoCrawlerList.add("https://www.ntcda.com/feed");
        autoCrawlerList.add("http://app.chinaz.com/RssHandler.ashx");
        autoCrawlerList.add("http://www.jfinfo.com/feed");
        autoCrawlerList.add("https://www.ruodian360.com/feed");
        autoCrawlerList.add("http://e.jznews.com.cn/GetData/BBSData.asmx/GetRSSData");
        autoCrawlerList.add("https://www.arcticray.com/e/web/?type=rss2&order=0&orderby=0&classid=2");
        autoCrawlerList.add("http://www.mhn24.com/feed");
        autoCrawlerList.add("https://getitfree.cn/feed/");
        autoCrawlerList.add("http://www.epx365.cn/rss.php");
        autoCrawlerList.add("http://www.dflit.com/newmedia/index.php?s=/Admin/Public/getdafrss/id/23");
        autoCrawlerList.add("http://app.bbtnews.com.cn/?app=rss&controller=index&action=feed&catid=260");
        autoCrawlerList.add("http://www.sydw.cc/portal.php?mod=rss&auth=0");
        autoCrawlerList.add("http://www.yuleq.com.cn/index.php?m=content&c=rss&rssid=6");
        autoCrawlerList.add("http://www.gl35w.com/feed/rss.php?mid=14");

        if (autoCrawlerList.contains(params.get("link"))) {
            autoCrawlerContent(contentBo, link);
        }

        //======自动抽取判断=======//
        if (link.contains("www.dsynews.cn") && item.select("content").isEmpty()) {
            autoCrawlerContent(contentBo, link);
        }
        if (link.contains("www.xyxww.com.cn") && item.select("content").isEmpty()) {
            autoCrawlerContent(contentBo, link);
        }
        if (link.contains("www.zgceo.cn") && item.select("content").isEmpty()) {
            autoCrawlerContent(contentBo, link);
        }
        if (link.contains("www.zqrb.cn") && item.select("content").isEmpty()) {
            autoCrawlerContent(contentBo, link);
        }
        if (link.contains("www.chinanews.com") && item.select("content").isEmpty()) {
            autoCrawlerContent(contentBo, link);
        }
        //详情为空时走一次自动提取
        if (StringUtil.isBlank(content) || content.length() < 50) {
            autoCrawlerContent(contentBo, link);
        }

        if (contentBo.getPageLink().split(" ").length == 2) {
            logger.warn("貌似找到一个标题超长的：{}", JSON.toJSONString(contentBo));
            return null;
        }

        logger.warn("RSS抓取结果{}", contentBo.toStringRmContent());
        return contentBo;
    }

    /**
     * 走一次自动抽取
     *
     * @param contentBo
     * @param link
     */
    public void autoCrawlerContent(McnContentBo contentBo, String link) {
        try {
            String body = http(link).get().html();
            News contentByHtml = ContentExtractor.getNewsByHtml(body);
            contentBo.setContent(contentByHtml.getContentElement().toString());
        } catch (Exception e) {
            logger.error("RSS走自动提取异常", e);
        }
    }
}
