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
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 西瓜视频任务
 *
 * @author chenghao1
 * @create 2019/10/9
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class Xigua_weMedia_Script  extends CrawlerWorker {


    public String vpfengUrl = "http://vpfeng.5233game.com/parse_pc.php?url=";

    //  全是数字的部分
    private Pattern regex=Pattern.compile("([0-9]\\d*)");
    //  测试 西瓜视频 抓取
    public static void main(String[] args)   throws Exception{
        Xigua_weMedia_Script flow_script  = new Xigua_weMedia_Script() ;
        Map map =  Maps.newHashMap() ;
        flow_script.params.set(map);
        map.put("link","https://www.ixigua.com/home/67475256535/") ;
        map.put("mcnTaskId","test001") ;
        map.put("taskType","xigua_weMedia") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","toutiao_weMedia_http") ;
        List<String>  list = flow_script.crawlerListPage(map);
        McnContentBo mcnContentBo = flow_script.crawlerDetailPage(list.get(0), map);
        System.out.println("size:"+list.size()+"--"+mcnContentBo.toString());

    }


    @Override
    public void setLog() {
        this.logger = LoggerFactory.getLogger(Xigua_weMedia_Script.class);
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        logger.info("xigua params:{}", JSON.toJSONString(params));
        List<String>  result =Lists.newArrayList() ;
        String linkUrl = (String) params.get("link")  ;
        Matcher matcher=regex.matcher(linkUrl);
        matcher.find() ;
        String userId=matcher.group(1);
        String accountUrl="https://www.365yg.com/c/user/article/?user_id="+userId+"&count=15";

        try {
            Connection http = http(accountUrl,null);
            http.header("Referer",accountUrl).timeout(5000) ;
            String body = http.execute().body();
            JSONObject listJson = JSON.parseObject(body);
            JSONArray datas = listJson.getJSONArray("data");//
            result = datas.stream().map(x-> JSON.toJSONString(x)).collect(Collectors.toList()) ;
            return result ;
        } catch (HttpStatusException e) {
            logger.warn("365 接口异常，phone");

            //手机端链接
            String urlPhone = "https://i.snssdk.com/api/feed/profile/v1/?category=profile_video&visited_uid=%s&stream_api_version=82&offset=0&version_code=740&version_name=70400&request_source=1&active_tab=dongtai&device_id=65&app_name=news_article";
            params.put("phone", "phone");
            return JsonPath.parse(http(String.format(urlPhone, userId), null).timeout(6000).execute().body())
                    .read("$..content", net.minidev.json.JSONArray.class)
                    .stream().map(Object::toString)
                    .collect(Collectors.toList());
        }

    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {

        McnContentBo mcnContentBo   =new McnContentBo() ;
        if(params.containsKey("phone")){
            DocumentContext item = JsonPath.parse(itemBody);

            //判重
            String duplicateKey = "xiguashort" + item.read("$.item_id");
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);

            if(duplicateKeyDao.containsKey(duplicateKey)){
                logger.warn("xiguashort;info:duplicate;link:{}",(String)item.read("$.display_url"));
                return null ;
            }

            //article_type 0:文章 2:西瓜视频
            Integer article_type = item.read("$.article_type", Integer.TYPE);
            mcnContentBo.setDuplicateKey(duplicateKey) ;
            mcnContentBo.setAbstractInfo(item.read("$.abstract"));
            mcnContentBo.setPlayCount(item.read("$.read_count", Long.TYPE));
            mcnContentBo.setTitle(item.read("$.title"));
            //String link = "https://www.toutiao.com/" + item.read("$.item_id");
            String link = item.read("$.display_url");
            mcnContentBo.setPageLink(link);
            mcnContentBo.setMusicDownloadUrl(link);
            mcnContentBo.setCover(item.read("$.ugc_video_cover.url")) ;
            mcnContentBo.setShareLink(item.read("$.share_url"));
            mcnContentBo.setPublishTime(item.read("$.behot_time", Long.TYPE) * 1000);
          //  mcnContentBo.setLikeCount(item.read("$.like_count", Integer.TYPE));
            mcnContentBo.setCommentCount(item.read("$.comment_count", Integer.TYPE));
       //     mcnContentBo.setOriginalFlag(item.read("$.is_original", Boolean.TYPE));
      //      mcnContentBo.setLikeCount(item.read("$.like_count", Integer.TYPE));
            //处理手机链接
            mcnContentBo.setShapeType("2");
        }else{

            JSONObject articleItemJson = JSON.parseObject(itemBody);
            String vid=articleItemJson.getString("item_id");
            logger.info(articleItemJson.toJSONString());
            if(vid==null){
                return null ;
            }
            String articleUrl = "https://www.toutiao.com" + articleItemJson.get("source_url");//toutiao文章页地址 https://www.toutiao.com/i6704443617911505415/
            // 1 时间过滤
            String behot_time = articleItemJson.getString("behot_time");
            Long publishTime = Long.valueOf(behot_time) * 1000;
            if(exceedPubTime(publishTime)){
                logger.warn("xiguashort;info:exceet 2 days;link:{}",articleUrl);
                return null ;
            }
            // 2 下载去重
            String duplicateKey = "xiguashort" + vid;
            if(duplicateKeyDao.containsKey(duplicateKey)){
                logger.warn("xiguashort;info:duplicate;link:{}",articleUrl);
                return null ;
            }


            // 3 字段设置
            String article_genre = articleItemJson.getString("article_genre");//文章类型：video
            String title = articleItemJson.getString("title");
            logger.info(title + " article_genre :" + article_genre);
            if (article_genre == null ) {
                article_genre = "video";
            }
            if(!article_genre.equals("video"))
            {
                // 不要非文章类
                return null;
            } else{

                String vpUrl = vpfengUrl + articleUrl;
                String body = http(vpUrl,null).execute().body();
                JSONObject jsonobj= JSONObject.parseObject(body);
                if(body != null &&body.contains("该视频无法播放")) {
                    logger.warn(articleUrl + "视频无法播放");
                    return null;
                }
                if (jsonobj.get("V") == null) {
                    logger.info(articleUrl + "解析视频失败");
                    return null;
                }
                //否则正常解析出视频的url,即videoUrl.
                JSONObject urlJson = (JSONObject) jsonobj.getJSONArray("V").get(0);
                String videoUrl = urlJson.getString("U");
                String imgUrl="" +articleItemJson.get("image_url");//海报地址\
                String newImgUrl="";
                if (imgUrl != null && imgUrl.indexOf("/") != -1) {//https://p3.pstatp.com/list/190x124/4ae70000d217e84c5602
                    if (imgUrl.indexOf("list/") != -1) {
                        newImgUrl = imgUrl.replace("list/", "origin/");
                        newImgUrl = newImgUrl.replaceAll("/\\d+x\\d+", "");
                    }
                }
                mcnContentBo.setShapeType(ContentType.VIDEO.getCode()) ;
                mcnContentBo.setTitle(title) ;
                mcnContentBo.setPageLink(articleUrl) ;
                mcnContentBo.setDuplicateKey(duplicateKey) ;
                mcnContentBo.setCover(newImgUrl) ;
                mcnContentBo.setPublishTime(publishTime) ;
                mcnContentBo.setPlayCount(articleItemJson.getLong("detail_play_effective_count")) ;
                mcnContentBo.setPlayCount(articleItemJson.getLong("comments_count")) ;
                mcnContentBo.setPlatId(vid) ;
                mcnContentBo.setShareLink(articleItemJson.getString("display_url")) ;
                mcnContentBo.setAbstractInfo(articleItemJson.getString("abstract")) ;
                mcnContentBo.setPageLink(articleItemJson.getString("display_url")) ;
                mcnContentBo.setMusicDownloadUrl(videoUrl) ;
                mcnContentBo.setHaveViedo(2) ;
                mcnContentBo.setTag(Lists.newArrayList(articleItemJson.getString("tag"),(String)articleItemJson.get("chinese_tag"))) ;

            }
        }
        if(mcnContentBo.dataReflag(mcnContentBo)){
            logger.warn("xigua----{}", JSON.toJSONString(mcnContentBo));
            return mcnContentBo ;
        }else{
            return null ;
        }

    }

}
