///*
//* Wechat_OpenId_Script.java
//* Created on  2019/9/26 10:42
//* Copyright © 2012 Phoenix New Media Limited All Rights Reserved
//*/
//package com.ifeng.mcn.spider.test.develop;
//
//import com.ifeng.mcn.spider.domain.WechatOpenIdResult;
//import com.ifeng.mcn.spider.utils.MySpider;
//import com.xxl.glue.core.handler.GlueHandler;
//import org.apache.commons.lang3.StringUtils;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//import us.codecraft.webmagic.Page;
//import us.codecraft.webmagic.Site;
//import us.codecraft.webmagic.downloader.HttpClientDownloader;
//
//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
//import java.text.MessageFormat;
//import java.util.Map;
//
///**
// * 抓取微信openId脚本
// * Created by gengyl on 2019/9/26.
// */
//@Component
//public class Wechat_OpenId_Script implements GlueHandler {
//
//    private static final String WEIXIN_OPEN_ID_URL = "https://weixin.sogou.com/weixin?type=1&s_from=input&query={0}&ie=utf8&_sug_=n&_sug_type_=";
//    private static final String FIX_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36";
//    private static final int RETRY_TIMES = 0;
//    private static final Logger LOGGER = LoggerFactory.getLogger(Wechat_OpenId_Script.class);
//
//    @Override
//    public Object handle(Map<String, Object> params) {
//        try {
//            String weiXinHao = (String) params.get("weiXinHao");
//            return getOpenId(weiXinHao);
//        } catch (Exception e) {
//            LOGGER.error("Wechat_OpenId_Script handle error", e);
//        }
//        return null;
//    }
//
//    /**
//     * 抓取openId
//     * @param weixinHao
//     * @return
//     */
//    private WechatOpenIdResult getOpenId(String weixinHao) {
//        WechatOpenIdResult wechatOpenIdResult = new WechatOpenIdResult();
//        String url = null;
//        try {
//            url = MessageFormat.format(WEIXIN_OPEN_ID_URL, URLEncoder.encode(weixinHao.replace("\\u00A0", " "), "UTF-8"));
//        } catch (UnsupportedEncodingException e) {
//            LOGGER.error(e.getMessage(), e);
//        }
//        Site site = Site.me()
//                .setDomain("weixin.sogou.com")
//                .setUserAgent(FIX_UA)
////                .setUserAgent(MySpider.getRandomUa())
//                .setRetryTimes(RETRY_TIMES)
//                .addHeader("Referer", url);
//        HttpClientDownloader downloader = new HttpClientDownloader();
//
////        ProxyIp proxyIp = proxyService.getProxyIp();
////        downloader.setProxyProvider(SimpleProxyProvider.from(new Proxy(proxyIp.getIp(),proxyIp.getPort())));
//
//        Page page = MySpider.create()
//                .setRequest(url)
//                .setSite(site)
//                .setDownloader(downloader)
////                .setProxyFlag(true)
//                .run();
//
//        String openId = null;
//        if (null == page || !page.isDownloadSuccess()) {
//            LOGGER.error("页面抓取失败。account="+weixinHao);
////            proxyService.resetProxyStatus(proxyIp);
//            wechatOpenIdResult.setStatusMsg(WechatOpenIdResult.RESULT_STATUS.SPIDER_FAIL);
//        } else {
//            boolean handleFlag = Boolean.FALSE;
//            //尝试遍历5个搜索结果，比对名称，如果一致则表示是对应的账号
//            for (int i=0;i<5;i++) {
//                Elements elements = page.getHtml().getDocument().select("#sogou_vr_11002301_box_"+i);
//                if (null != elements && elements.size() > 0) {
//                    handleFlag = Boolean.TRUE;
//                    Element element =  elements.first();
//                    String pageWeixinHao = element.getElementsByAttributeValue("name", "em_weixinhao").first().text();
//                    if (weixinHao.equalsIgnoreCase(pageWeixinHao)) {
//                        openId = element.attr("d");
//                        wechatOpenIdResult.setOpenId(openId);
//                        wechatOpenIdResult.setStatusMsg(WechatOpenIdResult.RESULT_STATUS.SUCCESS);
//                        break;
//                    }
//                }
//            }
//
//            if (StringUtils.isBlank(openId)) {
//                if (page.getRawText().contains("请输入验证码")) {
//                    LOGGER.error("验证码页面出现，无法抓取。account="+weixinHao);
//                    wechatOpenIdResult.setStatusMsg(WechatOpenIdResult.RESULT_STATUS.VERIFICATION_CODE);
//                } else if (handleFlag || page.getRawText().contains("暂无与")) {
//                    wechatOpenIdResult.setStatusMsg(WechatOpenIdResult.RESULT_STATUS.NOT_FOUND);
//                    LOGGER.error("搜索不到，无法抓取。account="+weixinHao);
//                } else {
//                    wechatOpenIdResult.setStatusMsg(WechatOpenIdResult.RESULT_STATUS.OTHER_ERR);
//                    LOGGER.error("其他异常，无法抓取。account="+weixinHao);
//                }
//            }
//        }
//        return wechatOpenIdResult;
//    }
//
//    /*public static void main(String[] args) {
//        for (int i=0;i<1000;i++) {
//            Site site = Site.me()
//                    .setDomain("weixin.sogou.com")
//                    .setUserAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36")
//                    .setRetryTimes(1);
//            String cookieStr = "IPLOC=CN1100; SUID=EF07D4CB3921940A000000005CA2C023; SUV=00879A3ACBD407EF5CA576CC67829766; CXID=A14915CFD0EF67147962117741C87315; LSTMV=511%2C70; LCLKINT=4351; SMYUV=1565919414172972; UM_distinctid=16c9812091530f-0cc4f22c175946-3e38580a-1fa400-16c981209163d4; ABTEST=4|1568277903|v1; sct=167; JSESSIONID=aaaDuUwnwEd_rfkn3Qn1w; PHPSESSID=nfv06vftkn2eirjnkhsjq1ccm6; SNUID=1CF3273FF4F6609FC358435AF4F858FA; successCount=1|Thu, 26 Sep 2019 03:23:04 GMT";
//            for (String cookieKv : cookieStr.split("; ")) {
//                site.addCookie(cookieKv.split("=")[0], cookieKv.split("=")[1]);
//            }
//            site.addHeader("Referer", "https://weixin.sogou.com/antispider/?from=%2fweixin%3Fzhnss%3d1%26type%3d1%26ie%3dutf8%26query%3dfengyuhuangshan");
//            HttpClientDownloader downloader = new HttpClientDownloader();
//            Page page = MySpider.create()
//                    .setRequest("https://weixin.sogou.com/weixin?zhnss=1&type=1&ie=utf8&query=fengyuhuangshan")
//                    .setSite(site)
//                    .setDownloader(downloader)
////                .setProxyFlag(true)
//                    .run();
//            System.out.println(page.getRawText());
//        }
//    }*/
//}
