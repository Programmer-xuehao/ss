package com.ifeng.mcn.spider.test.develop;

import com.alibaba.fastjson.JSON;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.MySpider;
import com.ifeng.mcn.spider.utils.ProxyUtils;
import com.ifeng.mcn.spider.utils.RegUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 示例抓取任务脚本-搜狗微信
 *
 * @author ZFang
 */
public class Wechat_weMedia_Script extends CrawlerWorker {

    static final String SOGOU_SEARCH_ARTICLE_URL = "https://weixin.sogou.com/weixin?query={0}&ft={1}&tsn=5&et={2}&interation=&type=2&wxid={3}&ie=utf8&usip={4}";
    static final String SOGOU_REFERER = "https://weixin.sogou.com/weixin?query={0}&_sug_type_=&s_from=input&_sug_=n&type=2&page={1}&ie=utf8";
    static final String QQ_MUSIC_URL = "http://open.music.qq.com/fcgi-bin/fcg_music_get_song_info_weixin.fcg?song_id={0}&mid={1}&format=json&app_id=1034002693&app_key=cjjDaueOyPYRJFeTqG&device_id=weixin&file_type=mp3&qqmusic_fromtag=50&callback=get_song_info_back";
    static final String KUGOU_MUSIC_URL = "https://mp.weixin.qq.com/mp/getkugousong?akey={0}&albumid={1}";
    static int TWO_DAYS_SECONDS = 2 * 24 * 60 * 60;

    public static void main(String[] args) throws Exception {

        String json = "{" +
                "\"articleUrl\":\"https://weixin.sogou.com/link?url=dn9a_-gY295K0Rci_xozVXfdMkSQTLW6cwJThYulHEtVjXrGTiVgS1Ns3S1mfAcicEaI0Vl4mh2MVAcB5OPMPFqXa8Fplpd9ZwoozPyUJP7U69P28nGAQyYCQKq62qWOdTTktlckshIum8M6Q4-ih5aVVbEav5OUmhNCLqT09SfMS8RKCT7zeqq0gjJqGi2c90PV78F3sOKOiKPi8NyObECJmOXmNGFqxvQxdRGIbG3jDCLfz4SsgROS2uqfkvrKR1iHRrAlbfn0OK9DLfgmPA..&type=2&query=%E6%9D%AD%E5%B7%9E%E5%B8%82%E4%B8%81%E5%85%B0%E7%AC%AC%E4%BA%8C%E5%B9%BC%E5%84%BF%E5%9B%AD&k=51&h=m\"," +
                "\"coverPic\":\"xxxxxx\"," +
                "\"title\":\"xxxxxxxx\"," +
                "\"publishTimeSecondStr\":\"5654\"," +
                "}";
        Wechat_weMedia_Script script = new Wechat_weMedia_Script();
        Map<String, Object> map = new HashMap<>();
        map.put("mediaName", "爱范儿");
        map.put("wechatOpenId", "oIWsFt3BMAX2LWmUkqQsJtCzWi1Y");
        map.put("mcnTaskId", "111");
        map.put("taskType", "");
        map.put("crawlerType", "");
        map.put("riskKeyPrefix", "wechat_weMedia_http");
                //  map.put("wechatOpenId", "磐石之心");
                List < String > list = script.crawlerListPage(map);
        System.out.println("----------" + JSON.toJSONString(list));
        script.params.set(map);

        //  script.crawlerDetailPage(json, map);
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        List<String> result = new ArrayList<>();

        String mediaName = (String) params.get("mediaName");
        String openId = (String) params.getOrDefault("wechatOpenId", "");
        if (openId == null) {
            openId = "";
        }
        //1.进行搜索，遍历2页内容
        for (int pageNo = 1; pageNo <= 1; pageNo++) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String searchUrl = MessageFormat.format(
                    SOGOU_SEARCH_ARTICLE_URL,
                    mediaName,
                    LocalDate.now().minusDays(1).format(dateTimeFormatter),
                    LocalDate.now().format(dateTimeFormatter),
                    openId,
                    mediaName)
                    .replace("wxid=&", "");//如果openId为空
            String refererUrl = MessageFormat.format(SOGOU_REFERER, mediaName, pageNo);
            //尝试抓取3次列表页
            String body = null;
            // 阿布云  全量 跑
            /*body =abuyunDownload(searchUrl,refererUrl)  ;

            if (body != null && body.contains("此验证码用于")) {
                throw new RuntimeException("出现验证码");
            }
            if (body != null && body.contains("429 Too Many Requests")) {
                throw new RuntimeException("429");
            }

            if (body != null && body.contains("429 Too Many Requests")) {
                throw new RuntimeException("429");
            }*/


            Connection.Response response = http(searchUrl)
                    .timeout(1000 * 20)
                    .execute();

            int statusCode = response.statusCode();
            body = response.body();
            if (body.contains("此验证码用于")) {
                throw new RuntimeException("出现验证码");
            }
            if (200 != statusCode) {
                throw new RuntimeException("状态码不争气");
            }

            if (null != body && body.contains("没有找到相关的微信公众号文章")) {
                System.err.println("没有找到相关的微信公众号文章");
                return result;
            }

            //2.解析页面数据
            List<Selectable> articleList = Html.create(body).xpath("//ul[@class='news-list']/li").nodes();
            if (null != articleList && articleList.size() > 0) {
                for (Selectable articleSelectable : articleList) {
                    String coverPic = articleSelectable.xpath("//div[@class='img-box']/a/img/@src").get();
                    if (StringUtils.isNotBlank(coverPic) && coverPic.startsWith("//")) {
                        coverPic = coverPic.replaceFirst("//", "https://");
                    }
                    List<Selectable> txtBoxList = articleSelectable.xpath("//div[@class='txt-box']").nodes();
                    if (null != txtBoxList && txtBoxList.size() > 0) {
                        Selectable txtBox = txtBoxList.get(0);
                        String title = Jsoup.parse(txtBox.xpath("//h3/a").get()).text().replace(" ", "");
                        String articleUrl = txtBox.xpath("//h3/a/@data-share").get();
                        String authorName = txtBox.xpath("//div[@class='s-p']/a/text()").get();
                        String publishTimeSecondStr = txtBox.xpath("//div[@class='s-p']/@t").get();
                        String _openId = txtBox.xpath("//a[@class='account']/@i").get();
                        HashMap<String, String> map = new HashMap<>();
                        map.put("title", title);
                        map.put("articleUrl", articleUrl);
                        map.put("authorName", authorName);
                        map.put("publishTimeSecondStr", publishTimeSecondStr);//Integer.parseInt(publishTimeSecondStr)
                        map.put("openId", _openId);
                        map.put("coverPic", coverPic);
                        // 判断文章  是否为   本人  ----
                        if (!authorName.equals(mediaName)) {
                            continue;
                        }

                        System.err.println("微信脚本抓取成功一个：" + map.toString());
                        //微信脚本抓取成功一个：{authorName=杭州市丁兰第二幼儿园, openId=oIWsFt7xk5H7MbMNanY9vgfQicVI, publishTimeSecondStr=1568873032, coverPic=https://img01.sogoucdn.com/net/a/04/link?appid=100520033&url=http://mmbiz.qpic.cn/mmbiz_jpg/WKVO81Fic8bBicQ942SpesrcmfpQFwnuZuYPZTPicBibS7U4d46CPjrwia4jFIQayeGgFVV3BGlO8iabiaqmMBKibic5SwA/0?wx_fmt=jpeg, title=丁兰二幼亲子乐园邀你来“玩”, articleUrl=http://weixin.sogou.com/api/share?timestamp=1569316746&signature=qIbwY*nI6KU9tBso4VCd8lYSesxOYgLcHX5tlbqlMR8N6flDHs4LLcFgRw7FjTAOzJssQmyPixrdB6-FwB2uKKagueCaG8k1J0qtfCarkxeaGzrff*27QUzIUt9o0*BAqufNECC5uwXLmCZ2pXUGJ8FcolShbG3AlS33DCzw*fq45nnpiwTFQbw8yO8-z*Kj4OfEJ9O6N6Q017cdw1Hh8woUENc32HUrAKkqhkZdtTk=}
                        result.add(JSON.toJSONString(map));
                    }
                }
            }
        }
        return result;
    }


    private String abuyunDownload(String url, String refererUrl) throws InterruptedException {

        HttpClientDownloader downloader = ProxyUtils.getProxyDownLoad();
        Site site = Site.me()
                .setTimeOut(10000)
                .setDomain("weixin.sogou.com")
                .addHeader("Referer", refererUrl)
//                .setUserAgent(FIX_UA)
                .setUserAgent(MySpider.getRandomUa())
                .setRetryTimes(6);

        MySpider mySpider = MySpider.create()
                .setRequest(url)
                .setSite(site)
                .setProxyFlag(true)
                .setDownloader(downloader);

        Page page = mySpider.run();

        return page.getRawText();

    }

    @Override
    public McnContentBo crawlerDetailPage(String itemJson, Map<String, Object> params) throws Exception {
        Map<String, String> map = JSON.parseObject(itemJson, Map.class);

        String duplicateKey = CommonUtils.md5IdUrl(params.get("mediaId") + map.get("title"));
        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;
        }

        String pageUrl = "";

        String body = "";
        Document document = null;
        Connection.Response response = http(map.get("articleUrl")).timeout(10000).execute();
        document = response.parse();
        body = document.html();
        System.err.println(body);
        pageUrl = response.url().toString();
        if (StringUtil.isBlank(body)) {
            throw new RuntimeException("重试5次还是失败");
        }
        //判断出现验证码
        if (body.contains("<br>此验证码用于确认这些请求是您的正常行为而不是自动程序发出的，需要您协助验证。</p>")) {
            System.err.println("详情页出现验证码");
            throw new RuntimeException("详情页出现验证码");
        }


        //判断特殊情况，文章内容是一个分享地址的
        Elements shares = document.select("#js_share_source");
        if (shares.size() > 0) {
            throw new RuntimeException("special detail page, url=");
        }
        if (body.contains("此内容因违规无法查看")) {
            throw new RuntimeException("此内容因违规无法查看");
        }

        //获取发布时间
        Long lts;
        int publishTimeSecondStr = Integer.parseInt(map.get("publishTimeSecondStr"));
        if (publishTimeSecondStr > 0) {
            lts = publishTimeSecondStr * 1000L;
        } else {
            String ts = null;
            List<String> tmlist = RegUtils.getSubUtil(body, "var ct = \"(.*?)\";");
            if (null != tmlist && tmlist.size() > 0) {
                ts = tmlist.get(0);
            } else {
                tmlist = RegUtils.getSubUtil(body, "d.ct = \"(.*?)\";");
                if (null != tmlist && tmlist.size() > 0) {
                    ts = tmlist.get(0);
                }
            }
            lts = Long.parseLong(ts) * 1000L;
            //判断是否是两天内文章
            if (lts < System.currentTimeMillis() - TWO_DAYS_SECONDS * 1000) {
                System.err.println("article publishTime before 2 days, url=");
            }
        }

        String title = document.select("#activity-name").text();

        //原创判断
        boolean originalArticleFlag = document.select("#copyright_logo").size() > 0;

        Elements oriElements = document.select("#js_content");
        //张林要求，过滤掉隐藏标签
        Elements pElements = oriElements.select("p");
        for (Element pElement : pElements) {
            if (pElement.attr("style").contains("text-indent: -9999px") || pElement.attr("style").contains("display:none")) {
                pElement.remove();
                logger.info("articleTitle={}, remove elements={}", title, pElement.toString());
            }
        }

        boolean haveAudio = Boolean.FALSE;
        //处理音频，微信
        Elements mpVoiceElements = oriElements.select("mpvoice");
        if (null != mpVoiceElements && mpVoiceElements.size() > 0) {
            haveAudio = Boolean.TRUE;
            for (Element voiceElement : mpVoiceElements) {
                String audioId = voiceElement.attr("voice_encode_fileid");
                String audioUrl = "http://res.wx.qq.com/voice/getvoice?mediaid=" + audioId;
                voiceElement.attr("src", audioUrl);
            }
        }
        //处理音频，qq音乐、酷狗
        Elements qqMusicElements = oriElements.select("qqmusic");
        if (null != qqMusicElements && qqMusicElements.size() > 0) {
            haveAudio = Boolean.TRUE;
            for (Element qqMusicElement : qqMusicElements) {
                String audioUrl = qqMusicElement.attr("audiourl");
                String mid = qqMusicElement.attr("mid");
                String finalAudioUrl = null;
                //qq音乐
                if (audioUrl.contains("qqmusic.qq.com/")) {
                    finalAudioUrl = MessageFormat.format(QQ_MUSIC_URL, qqMusicElement.attr("musicid"), mid);
                } else if (audioUrl.contains("http://fs.open.kugou.com/")) {
                    String akey = StringUtils.isBlank(qqMusicElement.attr("otherid")) ? mid : qqMusicElement.attr("otherid");
                    finalAudioUrl = MessageFormat.format(KUGOU_MUSIC_URL, akey, qqMusicElement.attr("albumid"));
                }
                if (StringUtils.isNotBlank(finalAudioUrl)) {
                    qqMusicElement.attr("src", finalAudioUrl);
                }
            }
        }
        //处理视频
        boolean haveVideo = Boolean.FALSE;
        Elements videoElements = new Elements();
        videoElements.addAll(oriElements.select("iframe[data-src~=https://v.qq.com/]"));
        videoElements.addAll(oriElements.select("iframe[data-src~=https://mp.weixin.qq.com/]"));
        videoElements.addAll(oriElements.select("iframe[src~=https://v.qq.com/]"));
        if (videoElements.size() > 0) {
            haveVideo = Boolean.TRUE;
            for (Element videoElement : videoElements) {
                String videoUrl = videoElement.attr("data-src");
                if (null != videoUrl && videoUrl.contains("https://v.qq.com")) {
                    videoUrl = RegUtils.replaceUrl(videoUrl);
                    videoUrl = videoUrl.replace("width=500&height=375&auto=0&", "");
                    videoElement.attr("src", videoUrl);
                }
                if (null != videoUrl && videoUrl.contains("https://mp.weixin.qq.com")) {
                    videoUrl = RegUtils.replaceUrl(videoUrl);
                    videoUrl = videoUrl.replace("width=500&height=375&auto=0&", "");
                    videoElement.attr("src", videoUrl);
                }
            }
        }

        //清洗文章
        String cleanContent = null;
        if ((null != mpVoiceElements && mpVoiceElements.size() > 0)
                || (null != qqMusicElements && qqMusicElements.size() > 0)
                || videoElements.size() > 0) {
            cleanContent = oriElements.html().replace("amp;", "")
                    .replace("<iframe", "<video").replace("iframe>", "video>")
                    .replace("<mpvoice", "<audio").replace("mpvoice>", "audio>")
                    .replace("<qqmusic", "<audio").replace("qqmusic>", "audio>");
            cleanContent = cleanContent.replace("<audio></audio>", "");
        } else {
            cleanContent = oriElements.html().replace("amp;", "");
        }
        cleanContent = cleanContent.replace("amp;", "");
        cleanContent = cleanContent.replace("amp;", "");
        cleanContent = RegUtils.replaceAnd(cleanContent);

        McnContentBo contentBo = new McnContentBo();
        contentBo.setOriginalFlag(originalArticleFlag);
        contentBo.setTitle(RegUtils.replaceUrl(title));
        contentBo.setCover(map.get("coverPic"));
        contentBo.setContent(cleanContent);
        contentBo.setHaveViedo(haveVideo ? 2 : 1);
        contentBo.setHaveAudio(haveAudio ? 2 : 1);
        contentBo.setPublishTime(lts);
        contentBo.setShapeType("1");//1 文章  2 视频  3 ALL
        contentBo.setPageLink(pageUrl);
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setMediaName(document.select("strong.profile_nickname").text());
        contentBo.setMediaId(params.get("mediaId") + "");

        //按时间过滤
        if (exceedPubTime(contentBo.getPublishTime())) {
            return null;
        }
        //判断是不是他自己的文章
        String weChatId = document.select("p.profile_meta:has(label:containsOwn(微信号)) span.profile_meta_value").text();
        if (!params.get("mediaId").toString().equals(weChatId)) {
            logger.warn("这文章不是这货发的！ Title = {}", contentBo.getTitle());
            return null;
        }

        logger.warn("微信脚本抓取结果：{}", JSON.toJSONString(contentBo));
        //返回
        return contentBo;
    }
}