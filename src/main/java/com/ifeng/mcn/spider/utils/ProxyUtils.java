package com.ifeng.mcn.spider.utils;

import com.ifeng.mcn.spider.constant.Constants;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.SimpleProxyProvider;

/**
 * 代理  工具类
 *
 *
 * @author chenghao1
 * @create 2019/3/19
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class ProxyUtils {

    private final static String  requestUrl ="http://proxy.abuyun.com/switch-ip"   ;
    private final static String  headerKey = "Proxy-Switch-Ip" ;
    private final static String  headerVal ="yes" ;

    public static HttpClientDownloader getProxyDownLoad(){

        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(new Proxy(Constants.proxyHost, Constants.proxyPort,
                Constants.proxyUser, Constants.proxyPass)));

        return httpClientDownloader ;

    }

    /**
     * 1s  可以 切换一次ip
     * @return
     */
    public  static String   changeProxyIp(){
        Site site = Site.me().setDomain("www.baidu.cn").setUserAgent(MySpider.getRandomUa()).setRetryTimes(1);
        site.addHeader(headerKey,headerVal) ;
        Page page = MySpider.create().setRequest(requestUrl).setSite(site).setDownloader(getProxyDownLoad()).run();
        return page.getRawText() ;
    }


}
