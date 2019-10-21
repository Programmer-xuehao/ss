package com.ifeng.mcn.spider.test.develop.prod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.base.ContentType;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.common.utils.DateUtil;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;

import java.util.List;
import java.util.Map;

/**
 * 网易号  自媒体 脚本
 *
 * @author chenghao1
 * @create 2019/10/8
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class Wangyi_weMedia_Script extends CrawlerWorker {

    private static Logger logger = LoggerFactory.getLogger(Wangyi_weMedia_Script.class);

    //  测试网易号抓取
    public static void main(String[] args) throws Exception{
        try {
            Wangyi_weMedia_Script weMedia_script  = new Wangyi_weMedia_Script() ;
            Map map =  Maps.newHashMap() ;
            weMedia_script.params.set(map);
            map.put("link","https://c.m.163.com/news/sub/T1494476571221.html?spss=newsapp") ;
            map.put("mcnTaskId","test001") ;
            map.put("taskType","wangyi_weMedia") ;
            map.put("crawlerType","http") ;
            map.put("riskKeyPrefix","wangyi_weMedia_http") ;
            List<String>  list = weMedia_script.crawlerListPage(map);
            McnContentBo mcnContentBo = weMedia_script.crawlerDetailPage(list.get(0), map);
            System.out.println("size:"+list.size()+"--"+mcnContentBo.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        logger.info("wangyi-script2324;params:{}", JSON.toJSONString(params));
        String crawlerUrl = params.get("link").toString();
        String body = "";
        Page page = null;

        try{
            Connection http = http(crawlerUrl);
//            http.header("Upgrade-Insecure-Requests","1").header("User-Agent","Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Mobile Safari/537.36")
//                    .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
//            http.header("Referer",crawlerUrl);
            body = http.execute().body();
        }catch (Exception e){
            logger.error("wangyiListErr:"+e.getMessage(),e);
        }
        Document  doc = Jsoup.parse(body);
        String userId = doc.select("html").attr("data-user-id");
        List result = Lists.newArrayList() ;
        // 说说 部分
        //String reqJsonUrl = "https://c.m.163.com/reader/api/recommend/list/v3?status=2&userId="+ userId +"&cursor=&from=H5";
        //String jsonStr = http(reqJsonUrl).execute().body();
        int index = crawlerUrl.lastIndexOf("/");
        int indexEnd = crawlerUrl.lastIndexOf(".html");
        String ariId = crawlerUrl.substring(index+1,indexEnd);
        String reqUrl = "https://c.m.163.com/nc/subscribe/list/"+ ariId +"/all/0-20.html";
        String docList = http(reqUrl).execute().body();
        if (StringUtils.isNotBlank(docList)) {
            JSONObject dataStr = JSON.parseObject(docList);
            JSONArray tabListObj = dataStr.getJSONArray("tab_list");
            if(tabListObj != null) {
                tabListObj.forEach(x->{
                    JSONObject liObj = (JSONObject) x;
                    String videoTopic = liObj.getString("videoTopic");
                    String publishTime = liObj.getString("ptime");
                    boolean b = exceedPubTime(formatYMDHMS(publishTime));
                    if(b){
                        return;
                    }
                    Map<String,Object> map = Maps.newHashMap() ;
                    if (videoTopic!= null && !videoTopic.equals("")) {
                        map.put("type",ContentType.VIDEO.getCode()) ; //  0 代表 video
                        map.put("userId",userId) ;

                    } else{
                        map.put("type",ContentType.ARTICLE.getCode()) ; //  1 代表 article
                    }
                    map.put("detail",liObj) ;
                    result.add(JSON.toJSONString(map)) ;
                });


            }
        }
        logger.info("wangyiListData:{}",result);
        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        DocumentContext parse = JsonPath.parse(itemBody);
        String read = (String)parse.read("$.type");
//        McnContentBo mcnContentBo   = new McnContentBo(params)  ;
        McnContentBo mcnContentBo   = new McnContentBo()  ;
        if(ContentType.VIDEO.getCode().equals(read)){
            String vid = parse.read("$.detail.videoID");
            String userName =parse.read("$.detail.source");
            String title = parse.read("$.detail.title");
            String publishTime = parse.read("$.detail.ptime");
            String posterUrl = parse.read("$.detail.imgsrc");
            String pageUrl = "https://c.m.163.com/news/v/"+ vid +".html";
            String uid =parse.read("$.userId") ;

            Document doc = http(pageUrl).execute().parse();
            String sourceLink = "http:" + doc.select("body > div.js-delegate > article > section.video-holder > div > video").attr("src");
            String duration = parse.read("$.detail.length")+"";
            //判重
            String duplicateKey = params.get("taskType") + vid;
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }
            mcnContentBo.setShapeType(read) ;
            mcnContentBo.setDuplicateKey(duplicateKey) ;
            mcnContentBo.setTitle(title) ;
            mcnContentBo.setVideoDuration(Integer.valueOf(duration)) ;
            mcnContentBo.setShareLink(pageUrl) ;
            mcnContentBo.setSourceLink(sourceLink) ;
            mcnContentBo.setMusicDownloadUrl(sourceLink) ;
            mcnContentBo.setPageLink(pageUrl) ;
            mcnContentBo.setMediaId(uid) ;
            mcnContentBo.setMediaName(userName)  ;
            mcnContentBo.setCover(posterUrl) ;
            mcnContentBo.setHaveViedo(2) ;
            mcnContentBo.setPublishTime(DateUtil.parse(publishTime,"yyyy-MM-dd HH:mm:ss").getTime()) ;

        }else{
            String docid = parse.read("$.detail.docid");
            String userName = parse.read("$.detail.source");
            String title = parse.read("$.detail.title");
            String publishTime = parse.read("$.detail.ptime");
            String downLoadUrl = "https://c.m.163.com/news/a/"+ docid +".html";
            String abstracts = parse.read("$.detail.digest");
            String duplicateKey = params.get("taskType") + docid;
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }
            Document doc = http(downLoadUrl).execute().parse();
            if(doc!=null){
                String contentHtml = doc.select("body > div.js-analysis.js-delegate > section.g-article.js-article > div.js-article-inner > article > main").html();
                mcnContentBo.setContent(contentHtml) ;
            }else{
                return null ;
            }
            mcnContentBo.setDuplicateKey(duplicateKey) ;
            mcnContentBo.setShapeType(ContentType.ARTICLE.getCode()) ;
            mcnContentBo.setPublishTime(DateUtil.parse(publishTime,"yyyy-MM-dd HH:mm:ss").getTime()) ;
            mcnContentBo.setTitle(title) ;
            mcnContentBo.setFrom(userName) ;
            mcnContentBo.setHaveViedo(1) ;
            mcnContentBo.setPageLink(downLoadUrl) ;
            mcnContentBo.setAbstractInfo(abstracts) ;
        }
        if(StringUtils.isNotBlank(mcnContentBo.getPageLink())&& StringUtils.isNotBlank(mcnContentBo.getTitle())){
            return mcnContentBo ;
        }else{
            return null ;
        }

    }
}
