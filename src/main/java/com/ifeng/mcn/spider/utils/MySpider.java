package com.ifeng.mcn.spider.utils;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.downloader.HttpClientDownloader;

import java.util.List;
import java.util.Random;

//import com.ifeng.spider.crawler.webmagic.downloader.SpiderHttpClientDownloader;
//import us.codecraft.webmagic.proxy.Proxy;
//import us.codecraft.webmagic.proxy.SimpleProxyProvider;

/**
 *  改写 webmagic  Spider 类  使用 分布式 抓取
 *  只负责 单页面下载
 *
 * @author chenghao1
 * @create 2019/2/21
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class MySpider implements Task {


    private static Logger logger = LoggerFactory.getLogger(MySpider.class);

    //站点设置
    private Site site;

    private String uuid ;
    protected Downloader downloader;
    public  static List<String> uaList = Lists.newArrayList();
    private String url ;
    private  Request request ;
    private   int retryTimes =0;
    private boolean proxyFlag  = false;

    static {
        // chrome
        uaList.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36");
        uaList.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
        uaList.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.648.133 Safari/534.16");
        // firefox
        uaList.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0 ");
        uaList.add("Mozilla/5.0 (X11; U; Linux x86_64; zh-CN; rv:1.9.2.10) Gecko/20100922 Ubuntu/10.10 (maverick) Firefox/3.6.10");
        // Safari
        uaList.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2") ;
        //360
        uaList.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36") ;
    }




    public static MySpider create() {
        return new MySpider();
    }

    // 设置下载器  下载 html页面
    public MySpider setDownloader(Downloader downloader) {
        this.downloader = downloader;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getUUID() {
        return this.getUUID();
    }

    @Override
    public Site getSite() {
        return this.site;
    }

    public MySpider setSite(Site site){
        this.site =site ;
        return this ;
    }


    public Request getRequest() {
        return request;
    }

    public MySpider setRequest(String  request) {
        this.request = new Request(request);
        return this ;
    }


    public MySpider setRequest(Request  request) {
        this.request = request ;
        return this ;
    }


    public boolean isProxyFlag() {
        return proxyFlag;
    }

    public MySpider setProxyFlag(boolean proxyFlag) {
        this.proxyFlag = proxyFlag;
        return this  ;
    }


    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public Page run() {
        // 默认downloader 下载器
        if (this.downloader == null) {
            this.downloader = new HttpClientDownloader();
        }
        Page page = this.downloader.download(request, this);
        if (page!=null&&page.isDownloadSuccess()) {
            //  记录日志
            if(this.proxyFlag){
                if(page.getHtml().get().contains("503 Service Unavailable")&& this.getSite().getRetryTimes()>this.retryTimes){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //  ip  切换
//                    ProxyUtils.changeProxyIp() ;
                    logger.info("代理异常-----{}",page.getRawText()) ;
                    this.retryTimes++ ;

                    page=this.run() ;
                }else if (page.getHtml().get().contains("429 Too Many Requests")&&this.getSite().getRetryTimes()>this.retryTimes){
                    try {
                        Thread.sleep(RandomUtils.nextInt(1000,3000));
                        logger.info("代理异常-----{}",page.getRawText()) ;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.retryTimes++ ;
                    page = this.run() ;
                }else{

                    if(this.retryTimes>0){
                        logger.info("重复抓取 url{},{}次成功,返回result{}--------",this.getRequest().getUrl().substring(0,10),this.retryTimes,page.getRawText()) ;
                    }
                }
            }
            return page ;
        } else {
            // 记录日志
            // 查看 retry 次数 重新下载。
            logger.info("抓取失败 url{},download  error retryTimes{}",this.getRequest().getUrl(),this.retryTimes) ;
            if(this.getSite().getRetryTimes()>this.retryTimes){
                this.retryTimes++ ;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                page = this.run() ;
                return page ;
            }
            return page ;

        }
    }





    public static String getRandomUa(){

        return  uaList.get(new Random().nextInt(uaList.size())) ;

    }

//    // 测试示范 抓取单页文章。
//    public static void main(String[] args) {
//        // 还可以 设置 其他header cookie  。
//        Site  site = Site.me().setDomain("www.baidu.com").setUserAgent(getRandomUa()).setRetryTimes(2) ;
//
//        //String url = "http://is.snssdk.com/api/feed/profile/v1/?category=profile_article&visited_uid=144759449719645&stream_api_version=88&count=30&offset=0&ac=wifi&channel=tengxun2&aid=13&app_name=news_article&version_code=717&version_name=7.1.7&device_platform=android&ab_feature=102749%2C94563&ssmix=a&device_type=MI+NOTE+LTE&device_brand=Xiaomi&language=zh&os_api=23&os_version=6.0.1&uuid=326561400841790&manifest_version_code=717&resolution=1080*1920&dpi=440&update_version_code=71712&_rticket=1,554,894,593,977&fp=2lTrJ2UWFrUIFlcWJ2U1FzmSLl4r&tma_jssdk_version=1.13.5.0&rom_version=miui_v9_v9.2.1.0.mxecnek&plugin=18766&ts=1554894593";
//        String url = "https://www.ip.cn";
//       // Map map = ToutiaoParamsUtil.getAsCp();
//       // url = MessageFormat.format(url, map.get("as"), map.get("cp"));
//        //HttpClientDownloader  可以  设置 下载代理  。
//
//        for (int i = 0; i <1 ; i++) {
//            MySpider mySpider = MySpider.create();
//            SpiderHttpClientDownloader httpClientDownloader  = new SpiderHttpClientDownloader() ;
//            httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(new Proxy("221.229.162.64", 11460)));
//            Page page = mySpider.setRequest(url).setSite(site).setDownloader(httpClientDownloader).
//                    setProxyFlag(false).run();
//            if(mySpider.getRetryTimes()>0){
//
//                System.out.println(mySpider.getRetryTimes()+"-----"+page.getRawText());
//            }
//        }
//
//    }

}
