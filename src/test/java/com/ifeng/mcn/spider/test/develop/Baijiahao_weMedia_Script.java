//package com.ifeng.mcn.spider.test.develop;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.google.common.collect.Maps;
//import com.ifeng.mcn.common.base.TaskStatusEnum;
//import com.ifeng.mcn.data.api.bo.McnContentBo;
//import com.ifeng.mcn.log.client.CommonUtils;
//import com.ifeng.mcn.log.client.StringUtil;
//import com.ifeng.mcn.log.client.base.EventType;
//import com.ifeng.mcn.spider.script.CrawlerWorker;
//import com.ifeng.mcn.spider.utils.MySpider;
//import com.ifeng.mcn.spider.utils.RegUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import us.codecraft.webmagic.Page;
//import us.codecraft.webmagic.Site;
//import us.codecraft.webmagic.downloader.HttpClientDownloader;
//import us.codecraft.webmagic.selector.Html;
//
//import javax.annotation.Resource;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
///**
// * @Description: 描述
// * @author: yingxj
// * @date: 2019/10/8 17:24
// */
//public class Baijiahao_weMedia_Script extends CrawlerWorker {
//
//    private static Logger logger = LoggerFactory.getLogger(Baijiahao_weMedia_Script.class);
//
//    private static final Long TWO_DAY_M = 2*24*60*60*1000L;
//
//    @Resource
//    private InterProcessSemaphoreV2 abuyunDistributeSemaphore;
//    @Resource
//
//    private static Site site = Site.me().setUserAgent(MySpider.getRandomUa()).setRetryTimes(3).setSleepTime(1000).setTimeOut(5000);
//
//    public static void main(String[] args) throws Exception{
//        Baijiahao_weMedia_Script mongoGaoyuanBaijiahaoScript  = new Baijiahao_weMedia_Script() ;
//        Map map =  Maps.newHashMap() ;
//        mongoGaoyuanBaijiahaoScript.params.set(map);
//        map.put("link","http://author.baidu.com/home/1577028297281460?from=dusite_sresults") ;
//        map.put("mcnTaskId","test001") ;
//        map.put("taskType","baijiahao_weMedia") ;
//        map.put("crawlerType","http") ;
//        map.put("riskKeyPrefix","baijiahao_weMedia_http") ;
//        map.put("mediaId","1577028297281460");
//        map.put("mediaName","河南老马");
//        System.out.println(JSON.toJSON(map));
//        List<String>  result = mongoGaoyuanBaijiahaoScript.crawlerListPage(map);
//        System.out.println(JSON.toJSON(result));
//        for (int i=0; i<result.size();i++) {
//            McnContentBo mcnContentBo = mongoGaoyuanBaijiahaoScript.crawlerDetailPage(result.get(i), map);
//            System.out.println(JSON.toJSON(mcnContentBo));
//        }
//    }
//
//    @Override
//    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
//        String url = params.get("link").toString();
//
//        Page page = httpMySpider(url).run();
//        String runtimeDataStr = "";
//        Document html = page.getHtml().getDocument();
//        html.select("s-user-fans->s-user-num");
//        Elements scripts = page.getHtml().getDocument().select("script");
//        for(Element script : scripts){
//            if(script.toString().contains("window.runtime")){
//                runtimeDataStr = script.toString().split("<script>window.runtime= ")[1].split(",window.runtime.pageType")[0];
//                break;
//            }
//        }
//        if (StringUtil.isBlank(runtimeDataStr))
//            return null;
//        JSONObject runtimeDataJson = JSON.parseObject(runtimeDataStr);
//        JSONObject userJson = runtimeDataJson.getJSONObject("user");
//        Integer fans = userJson.getInteger("fans");
//        if(fans!=null&&fans<100){
//            logger.info("baijia fans num less than 100 :{}",params);
//            // 粉丝号小于100的过滤掉，避免抓取无用号，调用停止任务接口，后续有检测机制
//            // 暂停抓取 这种账号 量级 比较大而且基本没有内容
//            mcnTaskDao.updateStatus((String)params.get("mcnTaskId"),TaskStatusEnum.ACCOUNT.getCode());
//            return null;
//        }
//        this.params.get().put("mediaId",userJson.getString("bjh_id"));
//        this.params.get().put("mediaName",userJson.getString("display_name"));
//
//        String uk = userJson.getString("uk");
//        // type video 视频  article 文章 smallVideo 小视频
//
////        Map<String,String> type = new HashMap();
////        type.put("article","pc");
////        type.put("video","app");
//////        type.put("smallVideo","app");
////
////        for(String key : type.keySet()){
////            String webpageUrl = "https://mbd.baidu.com/webpage?tab="+key+"&num=6&uk="+uk+"&type=newhome&action=dynamic&format=jsonp";
////            Document webpageData = getHtmlWithoutProxy(webpageUrl);
////        }
//
//        // 暂时先分开获取，后续修改成多类型抓取，把小视频也加进去 ↑
//        String articleUrl = "https://mbd.baidu.com/webpage?tab=article&num=10&uk="+uk+"&type=newhome&action=dynamic&format=jsonp";
//        Document articleData = getHtmlWithoutProxy(articleUrl);
//        String callbackData = articleData.text().replace("callback(", "");
//        String data = callbackData.substring(0, callbackData.length() - 1);
//        List articleList = JSON.parseObject(data).getJSONObject("data").getJSONArray("list");
//
//        String videoUrl = "https://mbd.baidu.com/webpage?tab=video&num=10&uk="+uk+"&type=newhome&action=dynamic&format=jsonp";
//        Document videoData = getHtmlWithoutProxy(videoUrl);
//        String videocallbackData = videoData.text().replace("callback(", "");
//        String vData = videocallbackData.substring(0, videocallbackData.length() - 1);
//        List videoList = JSON.parseObject(vData).getJSONObject("data").getJSONArray("list");
//
//        List result = new ArrayList();
//        for(int i=0; i<(articleList.size()<=videoList.size() ? videoList.size() : articleList.size()) ; i++){
//            Map temp = new HashMap();
//            if(i<articleList.size()){
//                temp.put("article",articleList.get(i));
//            }
//            if(i<videoList.size()){
//                temp.put("video",videoList.get(i));
//            }
//            result.add(JSON.toJSONString(temp));
//        }
//        return result;
//    }
//
//    @Override
//    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
//        Map map = JSON.parseObject(itemBody, Map.class);
////        Object video = map.get("video");
//        Object article = map.get("article");
//
//        McnContentBo mcnContentBo = parseArticleStr(article.toString());
////        parseVideoElement(video.toString(),mcnContentBo);
//        return mcnContentBo;
//    }
//
//    public void parseVideoElement(String videoStr, McnContentBo mcnContentBo){
//        Element vda = JSON.parseObject(videoStr,Element.class);
//        String sourceUrl = vda.select("div > div.largevideo-wrapper > div.largevideo-box").attr("data-src");
//        mcnContentBo.setSourceLink(sourceUrl);
//        if(mcnContentBo.getShapeType()!=null){
//            mcnContentBo.setShapeType("3");
//        }
//    }
//
//    public McnContentBo parseArticleStr(String articleStr) throws Exception{
//        McnContentBo mcnContentBo = new McnContentBo();
//        JSONObject json = JSON.parseObject(articleStr);
//        JSONObject itemData = json.getJSONObject("itemData");
//        String srcLink = itemData.getString("url");
//        Page detailPage1 = httpMySpider(srcLink).run();
////        Page detailPage1 = MySpider.create().setDownloader(new HttpClientDownloader()).setSite(site).setRequest(srcLink).run();
//
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        long publishTime = 0L;
//        publishTime = simpleDateFormat.parse(detailPage1.getHtml().getDocument().select("meta[ itemprop=\"dateUpdate\"]").get(0).attr("content")).getTime();
//
//        String title = itemData.getString("title");
//        String content = detailPage1.getHtml().getDocument().select(".article-content").toString();
//        detailPage1.getHtml().getDocument().getElementsByClass("author-name");
//
//        //判重
//        String duplicateKey = params.get().get("taskType") + itemData.getString("article_id");
//        duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
//        //重复为true
//        if (duplicateKeyDao.containsKey(duplicateKey)) {
//            return null;
//        }
//        if (System.currentTimeMillis() - Long.valueOf(publishTime) > TWO_DAY_M) {
//            return null;
//        }
//        mcnContentBo.setDuplicateKey(duplicateKey);
//        mcnContentBo.setTitle(title);
//        mcnContentBo.setMediaId(params.get().get("mediaId").toString());
//        mcnContentBo.setContent(content);
//        mcnContentBo.setPageLink(srcLink);
//        mcnContentBo.setCreateTime(System.currentTimeMillis());
//        mcnContentBo.setPublishTime(publishTime);
//        mcnContentBo.setMediaName(params.get().get("mediaName").toString());
//        mcnContentBo.setShapeType("1");
//        logger.info("百家号文章：{}",mcnContentBo.toStringRmContent());
//        return mcnContentBo;
//    }
//
//    private List<String> getVideoElements(Page page) throws Exception{
//        System.out.println(page.getJson());
//        JSONObject js = JSON.parseObject(page.getJson().toString().split("BigPipe.onPageletArrive")[1].substring(1,page.getJson().toString().split("BigPipe.onPageletArrive")[1].length()-2));
//        System.out.println(js);
//        String uk = js.getJSONArray("scripts").get(0).toString().split("other\":\"")[1].split("\"}")[0];
//        String videoUrl = "https://author.baidu.com/pipe?tab=9&uk="+uk+"&defaultTab=article&pagelets[]=video";
////        page = MySpider.create().setDownloader(new HttpClientDownloader()).setSite(site).setRequest(videoUrl).run();
//        page = httpMySpider(videoUrl).run();
//
//        JSONObject s2 = JSON.parseObject(page.getJson().toString().split("BigPipe.onPageletArrive")[1].substring(1,page.getJson().toString().split("BigPipe.onPageletArrive")[1].length()-2));
//        Document doc = Jsoup.parse(s2.getString("html"));
//        Elements els = doc.getElementsByTag("ul").get(0).getElementsByTag("li");
//
//        List<String> result = new ArrayList<>();
//        if(els!=null&&!els.isEmpty()){
//            for(Element el : els){
//                result.add(el.toString());
//            }
//        }
//        return result;
//    }
//
//    public List<String> getArticleJsons(Page page) throws Exception{
//        String s = page.getRawText().split("BigPipe.onPageletArrive")[1];
//        String ss=s.substring(1,s.length()-2);
//        JSONObject jsonObject = JSON.parseObject(ss);
//        String uk = jsonObject.getJSONArray("scripts").get(1).toString().split("other\":\"")[1].split("\"}")[0];
//        // https://mbd.baidu.com/webpage?tab=article&num=6&uk=R_Ru7LtatDqxLNEXGuOHxA&type=newhome&action=dynamic&format=jsonp&callback=__jsonp01565687023837
//        String aurl = "https://mbd.baidu.com/webpage?tab=article&num=6&uk="+uk+"&type=newhome&action=dynamic&format=jsonp&callback=__jsonp01565687023837";
//        Document document = Jsoup.parse(jsonObject.getString("html"));
//        String from = document.select("div.name-item > div").text();
//        Page page1 = httpMySpider(aurl).run();
//
//        JSONObject s2 = JSON.parseObject(page1.getJson().toString().split("__jsonp01565687023837")[1].substring(1,page1.getJson().toString().split("__jsonp01565687023837")[1].length()-1));
//        JSONArray dataList = s2.getJSONObject("data").getJSONArray("list");
//
//        List<String> result = new ArrayList<>();
//        if(dataList!=null&&!dataList.isEmpty()){
//            for(Object data : dataList){
//                result.add(data.toString());
//            }
//        }
//        return result;
//    }
//
//    static String getAppId(String url) throws Exception {
//        String pageUrl = url + "?";
//        String app_id = null;
//        List<String> app_ids = RegUtils.getSubUtil(pageUrl,"app_id%22%3A%22(.*?)%22");
//        if (null == app_ids || app_ids.size() <= 0){
//            app_ids = RegUtils.getSubUtil(pageUrl,"home/(.*?)\\?");
//        }
//        if (null != app_ids && app_ids.size() > 0){
//            app_id = app_ids.get(0);
//        }
//        if (StringUtils.isBlank(app_id)){
//            URL serverUrl = new URL(url);
//            HttpURLConnection conn = (HttpURLConnection) serverUrl
//                    .openConnection();
//            conn.setRequestMethod("GET");
//            conn.setInstanceFollowRedirects(false);
//            conn.addRequestProperty("Accept-Charset", "UTF-8;");
//            conn.addRequestProperty("User-Agent",fixUa);
//            conn.connect();
//            String location = conn.getHeaderField("Location");
//            app_ids = RegUtils.getSubUtil(location,"app_id%22%3A%22(.*?)%22");
//            if (null != app_ids && app_ids.size() > 0){
//                app_id = app_ids.get(0);
//            }
//        }
//        if (StringUtils.isBlank(app_id)){
//            return null;
//        }
//        return app_id;
//    }
//
//    static String getUrlHot(String app_id) throws Exception {
//        JSONObject obj = new JSONObject();
//        obj.put("from","ugc_share");
//        obj.put("app_id",app_id);
//        String json = URLEncoder.encode(obj.toString(), "UTF-8");
//        return "https://author.baidu.com/profile?context="+json+"&cmdType=&pagelets[]=root&reqID=0&ispeed=1";
//    }
//
//
//    static String getUrlReal(String app_id) throws Exception {
//        return "https://author.baidu.com/pipe?tab=2&app_id="+app_id+"&num=6&pagelets[]=article&reqID=1&ispeed=1";
//    }
//
//    static final String fixUa = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36";
//
//    Document getHtml(String url) throws Exception {
//        Site site = Site.me()
//                .setDomain("author.baidu.com")
//                .setUserAgent(fixUa)
//                .setRetryTimes(2)
//                .setCharset("UTF-8").setTimeOut(5000);
//        //RANDOM PROXY
//        Random r = new Random(System.currentTimeMillis());
//        int p = r.nextInt(4);
////        MySpider mySpider = MySpider.create().setRequest(url).setSite(site).setProxyFlag(true).setDownloader(getHttpClientDownloader(url, null, EventType.list));
//        Page page = null;
////        Lease lease = null;
//        try {
////            lease = abuyunDistributeSemaphore.acquire();
////            page = mySpider.run();
//            page = httpMySpider(url).run();
//        } finally {
////            if (null != lease) {
////                abuyunDistributeSemaphore.returnLease(lease);
////            }
//        }
//        page = ifnullPage(page,url,site);
//        Html html = page.getHtml();
//        if (html.getDocument().html().contains("PLEASE TRY AGAIN!") || page.getRawText().contains("哎呀迷路了...")) {
//            String strCookie = page.getHeaders().get("Set-Cookie").get(0);
//            site.addHeader("Cookie", strCookie);
////            mySpider = MySpider.create().setRequest(url).setSite(site).setProxyFlag(true).setDownloader(getHttpClientDownloader(url, null, EventType.list));
//            try {
////                lease = abuyunDistributeSemaphore.acquire();
////                page = mySpider.run();
//                page = httpMySpider(url).run();
//            } finally {
////                if (null != lease) {
////                    abuyunDistributeSemaphore.returnLease(lease);
////                }
//            }
//        }
//        page = ifnullPage(page,url,site);
//        checkPage(page);
//
//        String x = "quot;";         //for mysql conversion
//        String h = page.getHtml().getDocument().html().replaceAll("\\\\&" + x,"");
//        return Jsoup.parse(h);
//    }
//
//    private Page ifnullPage(Page page,String url,Site site) throws Exception {
//        if (null != page && !page.getHtml().getDocument().html().contains("<html> <head></head> <body> </body> </html>")){
//            return page;
//        }
//        HttpClientDownloader downloaderLocal = new HttpClientDownloader();
////        page = MySpider.create().setRequest(url).setSite(site).setDownloader(downloaderLocal).run();//
//        page = httpMySpider(url).run();
//
//        if (null == page || page.getHtml().getDocument().html().contains("<html> <head></head> <body> </body> </html>")){
////            Thread.sleep(2*1000);
//            throw new RuntimeException("getHtml page null " + url);
//        }
//        return page;
//    }
//
//    static void checkPage(Page page) {
//        if (null == page) {
//            throw new RuntimeException("page is null");
//        }
//        if (page.getRawText().contains("PLEASE TRY AGAIN!")) {
//            throw new RuntimeException("try again page");
//        }
//        if (page.getRawText().contains("哎呀迷路了...")) {
//            throw new RuntimeException("lost page");
//        }
//        if (page.getRawText().contains("<html> <head></head> <body> </body> </html>")) {
//            throw new RuntimeException("empty html page");
//        }
//    }
//
//    public Document getHtmlWithoutProxy(String url) throws Exception {
//        Page page = httpMySpider(url,getRiskInfo(url,url, EventType.detail)).run();
//
//        boolean needRetry = Boolean.FALSE;
//        if (null == page || !page.isDownloadSuccess()) {
//            needRetry = Boolean.TRUE;
//        } else if (page.getRawText().contains("PLEASE TRY AGAIN!")
//                || page.getRawText().contains("哎呀迷路了...")
//                || page.getRawText().contains("<html> <head></head> <body> </body> </html>")) {
//            String strCookie = page.getHeaders().get("Set-Cookie").get(0);
//            site.addHeader("Cookie",strCookie);
//            needRetry = Boolean.TRUE;
//        }
//        //重试
//        if (needRetry) {
////            page = MySpider.create().setRequest(url).setSite(site).setProxyFlag(false).setDownloader(downloader).run();
//            page = httpMySpider(url).run();
//            System.out.println(page.getRawText());
//        }
//        if (null == page || !page.isDownloadSuccess()
//                || page.getRawText().contains("PLEASE TRY AGAIN!")
//                || page.getRawText().contains("哎呀迷路了...")
//                || page.getRawText().contains("<html> <head></head> <body> </body> </html>")) {
//            throw new RuntimeException("Baijiahao download page without proxy error");
//        }
//        String x = "quot;";         //for mysql conversion
//        String h = page.getHtml().getDocument().html().replaceAll("\\\\&" + x,"");
//        return Jsoup.parse(h);
//    }
//
//}
