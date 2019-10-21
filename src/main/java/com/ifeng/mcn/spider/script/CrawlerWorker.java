package com.ifeng.mcn.spider.script;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.log.client.base.EventType;
import com.ifeng.mcn.spider.dao.DuplicateKeyDao;
import com.ifeng.mcn.spider.dao.McnTaskDao;
import com.ifeng.mcn.spider.utils.DateUtil;
import com.ifeng.mcn.spider.utils.MySpider;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.xxl.glue.core.handler.GlueHandler;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.SimpleProxyProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 脚本公共父类
 * <p>
 * 注意事项 ：
 * // haveVideo 1 没有 2 有    haveAudio  1 没有 2 有
 * 视频不需要解析的：bilibili，头条，微信，爱奇艺，优酷，土豆，搜狐，秒拍，acfun，乐视   设置到
 * shape 1  文章   2  视频   视频类都要设置 该字段
 * mcnContentBo.setMusicDownloadUrl(sourceLink) ;
 */
public abstract class CrawlerWorker implements GlueHandler {

    public Logger logger = LoggerFactory.getLogger(CrawlerWorker.class);
    public ThreadLocal<Map<String, Object>> params = new ThreadLocal<>();
    public ThreadLocal<Integer> reTryCountD = new ThreadLocal<>();//详情页重试次数
    //    @Autowired
//每次从任务中心拿到的任务
//public Map<String, Object> params;
//////////main方法执行不管用//////////////
//    @Autowired
    public DuplicateKeyDao duplicateKeyDao = new DuplicateKeyDao();
    public ThreadLocal<Integer> reTryCountL = new ThreadLocal<>();//列表页重试次数
    //    @Autowired
    public McnTaskDao mcnTaskDao = new McnTaskDao();
    //////////main方法执行不管用//////////////
    private Configuration configuration = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
    //只打日志用,子类貌似用不到
    private String origin = "";
    private Site site = Site.me().setUserAgent(MySpider.getRandomUa()).setRetryTimes(3).setSleepTime(1000).setTimeOut(3000);

    public CrawlerWorker() {
        setLog();
    }

    @Override
    public Object handle(Map<String, Object> params) {
        //预编译时params==null
        if (params == null) return null;
        this.params.set(params);
        //设置默认重试次数
        this.reTryCountL.set(1);
        this.reTryCountD.set(1);
        //详情页成功失败数
        int detailSuccessCount = 0;
        int detailFailureCount = 0;

        //本次任务成功失败
        boolean isSuccess = true;

        //抓取列表页
        List<String> detailUrlList = new ArrayList<>();
        for (int i = 0; i < this.reTryCountL.get(); i++) {//重试机制
            long startTime = System.currentTimeMillis();
            try {
                detailUrlList = crawlerListPage(params);
                if (detailUrlList == null) detailUrlList = new ArrayList<>();
//                logSpiderEvent(params, startTime, null, EventType.list);
//                logSpiderStatistics(params, null, EventType.list, true, origin);
                isSuccess = true;
                break;
            } catch (Exception e) {
                isSuccess = false;
//                logSpiderEvent(params, startTime, e, EventType.list);
//                logSpiderStatistics(params, e, EventType.list, false, origin);
                this.logger.error("列表页抓取异常{}      URL:", e.getMessage(), params.get("link"), e);
            }
        }

        //抓取详情页
        List<McnContentBo> contentBoList = new ArrayList<>();
        for (String url : detailUrlList) {
            for (int i = 0; i < this.reTryCountD.get(); i++) {//重试机制
                long startTime = System.currentTimeMillis();
                try {
                    McnContentBo contentBo = crawlerDetailPage(url, params);
                    if (contentBo != null) {
                        //统一去重处理
                        //重复为true
                        if (duplicateKeyDao.containsKey(contentBo.getDuplicateKey())) {
                            break;
                        }
                        detailSuccessCount += 1;
                        contentBoList.add(contentBo);
//                        logSpiderEvent(params, startTime, null, EventType.detail);
//                        logSpiderStatistics(params, null, EventType.detail, true, origin);
                    }
                    break;
                } catch (Exception e) {
                    isSuccess = false;
                    detailFailureCount += 1;
//                    logSpiderEvent(params, startTime, e, EventType.detail);
//                    logSpiderStatistics(params, e, EventType.detail, false, origin);
                    this.logger.error(e.getMessage(), e);

                }
            }
        }
        //统一字段处理
        contentBoList = contentBoList.stream().map(mcnContentBo -> {
            //统一添加CreateTime
            mcnContentBo.setCreateTime(System.currentTimeMillis());
            //防止误加ID导致存库错误
            mcnContentBo.setId(null);
            return mcnContentBo;
        }).collect(Collectors.toList());
        //返回结果,后续将推送Kafka,回调任务中心及存库
//        return new TaskResult(detailUrlList != null, isSuccess, detailSuccessCount, detailFailureCount, contentBoList);

        logger.info("" +
                "||||||||||||||||||||||||脚本执行完毕|||||||||||||||||||||||||||：{}" + System.lineSeparator() +
                "" + JSON.toJSONString(contentBoList) +
                "||||||||||||||||||||||||脚本执行完毕|||||||||||||||||||||||||||：{}" + System.lineSeparator() + "" +
                "");
        return null;
    }


    protected boolean exceedPubTime(long pubTime) {
        return System.currentTimeMillis() - pubTime >= 2 * 24 * 60 * 60 * 1000;
    }


    protected long formatYMDHMS(String date) {
        return DateUtil.parse(date, "yyyy-MM-dd HH:mm:ss").getTime();
    }

    protected long formatYMD(String date) {
        return DateUtil.parse(date, "yyyy-MM-dd").getTime();
    }


    /**
     * 子类设置重试次数
     *
     * @param listRetryCount
     */
    protected void setListRetryCount(int listRetryCount) {
        if (listRetryCount > 0) {
            reTryCountL.set(listRetryCount);
        }
    }

    /**
     * 子类设置重试次数
     *
     * @param detailRetryCount
     */
    protected void setDetailRetryCount(int detailRetryCount) {
        if (detailRetryCount > 0) {
            reTryCountD.set(detailRetryCount);
        }
    }

    /**
     * 抓取列表页
     *
     * @return
     */
    public abstract List<String> crawlerListPage(Map<String, Object> params) throws Exception;

    /***
     * 后续 修改 为 抽象方法 子类 必须 实现该log 方法 。 方便日志 排查。
     */
    public void setLog() {

    }

    /**
     * 抓取详情页
     *
     * @param itemBody
     * @return
     * @throws Exception
     */
    public abstract McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception;

    /**
     * 获取风控信息
     * 请把异常信息抛出,crawlerListPage或crawlerDetailPage中处理
     * 列表页无sourceUrl
     *
     * @return
     * @throws Exception
     */
    public JSONObject getRiskInfo(String crawlerUrl, String sourceUrl, EventType eventType) throws Exception {
        return null;
//        long startTime = System.currentTimeMillis();
//        try {
//            //System.out.println(params.get().get("riskKeyPrefix"));
//            Connection.Response response = NacosUtil.getRiskInfo()
//                    .data("configKey", params.get().get("riskKeyPrefix") + "_" + eventType)
//                    .data("crawlerUrl", crawlerUrl)
//                    .data("sourceUrl", sourceUrl == null ? "" : sourceUrl)
//                    .method(Connection.Method.POST)
//                    .execute();
//            String body = response.body();
//            this.logger.warn("{}风控接口返回：{}  configKey={}   crawlerUrl{}    sourceUrl{}", params.get().get("riskKeyPrefix"), body, params.get().get("riskKeyPrefix") + "_" + eventType, crawlerUrl, sourceUrl);
//            DocumentContext context = JsonPath.parse(body, configuration);
//            if (context.read("$.data") == null) {
//                this.logger.error("风控接口返回不正常!请检查：{}", body);
//                //  如果没有 名字 默认 不需要 风控规则   add by chenghao1  20191009
//                return null;
//            }
//            //将这俩取出，用于打日志
//            origin = context.read("$.data.proxy.origin") + "";
//            //转为对应的风控类型
//            switch (eventType) {
//                case list:
//                    eventType = EventType.listrisk;
//                    break;
//                case detail:
//                    eventType = EventType.detailrisk;
//                    break;
//            }
//            logSpiderEvent(params.get(), startTime, null, eventType);
//            //{"msg":{"code":900,"msg":"操作成功","success":true},"data":{"proxy":{"proxy":"58.218.213.116:10758","origin":"ZDAYE"},"headers":{"Referer":"www.baidu.com","User-Agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:63.0) Gecko/20100101 Firefox/63.0","Host":"www.baidu.com"}}}
//            //{"msg":{"code":900,"msg":"操作成功","success":true},"data":{"proxy":null,"headers":{"Referer":"www.baidu.com","User-Agent":"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36","Host":"www.baidu.com"}}}
//            return JSON.parseObject(body);
//        } catch (Exception e) {
//            logSpiderEvent(params.get(), startTime, e, eventType);
//            throw e;
//        }
    }

    /**
     * @param crawlerUrl
     * @param sourceUrl
     * @param eventType
     * @return
     */
    public HttpClientDownloader getHttpClientDownloader(String crawlerUrl, String sourceUrl, Object eventType) {
        JSONObject riskInfo = null;
        try {
//            riskInfo = getRiskInfo(crawlerUrl, null, eventType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Boolean success = riskInfo.getJSONObject("msg").getBoolean("success");
        if (!success) {
            this.logger.info("获取不到风控数据");
            return new HttpClientDownloader();
        }

        JSONObject proxyJson = riskInfo.getJSONObject("data").getJSONObject("proxy");
        if (proxyJson == null) {
            return new HttpClientDownloader();
        }
        String[] split = riskInfo.getJSONObject("data").getJSONObject("proxy").getString("proxy").split(":");
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(new Proxy(split[0], Integer.valueOf(split[1]))));
        return httpClientDownloader;
    }

    /**
     * 自动获取风控规则并组装请求
     *
     * @param url
     * @return
     * @throws Exception
     */
    protected Connection http(String url) throws Exception {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (method.startsWith("crawlerListPage")) {//绝对不能equals
//            return http(url, getRiskInfo(url, null, EventType.list));
            return http(url, null);
        } else if (method.startsWith("crawlerDetailPage")) {//绝对不能equals
//            return http(url, getRiskInfo(url, null, EventType.detail));
            return http(url, null);
        }
        // 默认超时时间 5秒
        return http(url, null).timeout(5000);
    }

    /**
     * 根据url及风控规则组装请求
     *
     * @param url
     * @param riskInfo
     * @return
     */
    protected Connection http(String url, JSONObject riskInfo) {
        Connection connection = Jsoup.connect(url).validateTLSCertificates(false).ignoreContentType(true).timeout(3000);
        if (riskInfo == null) {
            System.err.println("当前未使用风控!");
            return connection;
        }
        System.err.println("风控返回::" + riskInfo.toJSONString());
        DocumentContext context = JsonPath.parse(riskInfo.toJSONString(), configuration);
        Map headers = context.read("$.data.headers", Map.class);
        Map cookies = context.read("$.data.cookies", Map.class);
        connection.headers(headers);
        if (cookies != null) {
            connection.cookies(cookies);
        }
        if (context.read("$.data.proxy.proxy") != null) {
            String[] proxy = context.read("$.data.proxy.proxy").toString().split(":");
            connection.proxy(proxy[0], Integer.parseInt(proxy[1]));
        }
        return connection;
    }

    protected MySpider httpMySpider(String url) throws Exception {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (method.startsWith("crawlerListPage")) {
//            return httpMySpider(url, getRiskInfo(url, null, EventType.list));
            return httpMySpider(url, null);
        } else if (method.startsWith("crawlerDetailPage")) {
//            return httpMySpider(url, getRiskInfo(url, null, EventType.detail));
            return httpMySpider(url, null);
        }
        return httpMySpider(url, null);
    }

    protected MySpider httpMySpider(String url, JSONObject riskInfo) {
        if (null == riskInfo) {
            return MySpider.create().setRequest(url).setSite(site).setDownloader(new HttpClientDownloader());
        }
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        if (null != riskInfo) {
            DocumentContext context = JsonPath.parse(riskInfo.toJSONString(), configuration);
            Map headers = context.read("$.data.headers", Map.class);
            Map cookies = context.read("$.data.cookies", Map.class);
            if (null != cookies && !cookies.isEmpty()) {
                for (Object key : cookies.keySet()) {
                    site.addCookie(key.toString(), cookies.get(key).toString());
                }
            }
            // 自动填充风控获取的header，包含cookie
            if (null != headers && !headers.isEmpty()) {
                for (Object key : headers.keySet()) {
                    site.addHeader(key.toString(), headers.get(key).toString());
                }
            }
            if (context.read("$.data.proxy.proxy") != null) {
                String[] proxy = context.read("$.data.proxy.proxy").toString().split(":");
                httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(new Proxy(proxy[0], Integer.valueOf(proxy[1]))));
            }
        }

        return MySpider.create().setRequest(url).setSite(site).setDownloader(httpClientDownloader);
    }

}
