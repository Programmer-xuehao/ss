package com.ifeng.mcn.spider.test.develop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 描述
 * @author: yingxj
 * @date: 2019/10/10 12:40
 */
public class TTkuaibao_weMedia_Script extends CrawlerWorker {

    private static Logger logger = LoggerFactory.getLogger(TTkuaibao_weMedia_Script.class);

    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    public static void main(String[] args) throws Exception{
        TTkuaibao_weMedia_Script tTkuaibao_weMedia_script  = new TTkuaibao_weMedia_Script() ;
        Map map =  Maps.newHashMap() ;
        tTkuaibao_weMedia_script.params.set(map);
        map.put("link","https://kuaibao.qq.com/s/MEDIANEWSLIST?chlid=5074512") ;
        map.put("mcnTaskId","test001") ;
        map.put("taskType","tiantiankuaibao_weMedia") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","tiantiankuaibao_weMedia_http") ;

        List<String>  result = tTkuaibao_weMedia_script.crawlerListPage(map);
//        System.out.println(JSON.toJSON(list));

        McnContentBo mcnContentBo = tTkuaibao_weMedia_script.crawlerDetailPage(result.get(0), map);
        System.out.println(JSON.toJSON(mcnContentBo));
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        logger.info("startTTkuaibao");
        String link = params.get("link").toString();
        Pattern compile = Pattern.compile("chlid=\\d+");
        Matcher matcher = compile.matcher(link);
        String pageLink = "";
        if(matcher.find()){
            pageLink = "https://kuaibao.qq.com/getMediaCardInfo?chlid="+matcher.group().replace("chlid=","");
        }
        logger.info("ttkuaibao,pageLink:{}"+pageLink);
        List<String> result = new ArrayList<>();
        try {
            String body = httpMySpider(pageLink).run().getRawText();
            JSONObject infoJson = JSON.parseObject(body).getJSONObject("info");

            JSONObject cardInfo = infoJson.getJSONObject("cardInfo");
            String chlname = cardInfo.getString("chlname");
            String chlid = cardInfo.getString("chlid");
            this.params.get().put("mediaName",chlname);
            this.params.get().put("mediaId",chlid);

            // 文章
            List newsList = infoJson.getJSONArray("newsList");
            // 视频
//            List videoList = infoJson.getJSONArray("videoList");
//            if(null!=videoList&&!videoList.isEmpty()){
//                newsList.addAll(videoList);
//            }

            if(null!=newsList&&!newsList.isEmpty()){
                for(Object data : newsList){
                    JSONObject jsonObject = JSON.parseObject(data.toString());
                    if(jsonObject.containsKey("time")) {
                        Long time = DateUtil.parseTime(jsonObject.getString("time")).getTime();
                        if(null!=time && System.currentTimeMillis()-time>TWO_DAY_M){
                            logger.info("mediaId:{},天天快报时间过滤:{}", params.get("mediaId"), jsonObject.get("id"));
                            continue;
                        }
                    }
                    result.add(data.toString());
                }
            }
            logger.info("TTkuaibao listData:{}",result);
        } catch (Exception e){
            logger.error("tt ListErr"+e.getMessage()+":{}",e);
        }
        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        JSONObject dataJson = JSON.parseObject(itemBody);

        //判重
        String duplicateKey = params.get("taskType") + dataJson.getString("id");
        duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;//重复key,取消抓取
        }

        String pageUrl = "https://kuaibao.qq.com/getSubNewsContent?id="+dataJson.getString("id");
        String page = http(pageUrl, null).execute().body();
        JSONObject pageJson = JSON.parseObject(page);
        McnContentBo mcnContentBo = new McnContentBo();
        mcnContentBo.setSourceLink(params.get("link").toString());
//        if(dataJson.containsKey("playcount")){
//            String videoRelateUrl = "https://kuaibao.qq.com/getVideoRelate?id="+dataJson.getString("id");
//            String videoRelateStr = http(videoRelateUrl, null).execute().body();
//            JSONObject jsonObject = JSON.parseObject(videoRelateStr).getJSONObject("videoinfo");
//            if(jsonObject.containsKey("pubTime")){
//                mcnContentBo.setPublishTime(DateUtil.parseTime(jsonObject.getString("pubTime")).getTime());
//            }
//            mcnContentBo.setShapeType("2");
//            mcnContentBo.setPlayCount(dataJson.getLong("playcount"));
//        } else{
            mcnContentBo.setShapeType("1");
            mcnContentBo.setContent(pageJson.getJSONObject("content").getString("text"));
//        }

        mcnContentBo.setDuplicateKey(duplicateKey);
        mcnContentBo.setPlatId(dataJson.getString("id"));
        mcnContentBo.setMediaName(params.get("mediaName").toString());
        mcnContentBo.setMediaId(params.get("mediaId").toString());
        mcnContentBo.setFrom(mcnContentBo.getMediaName());
        if(dataJson.containsKey("title")){
            mcnContentBo.setTitle(dataJson.getString("title"));
            mcnContentBo.setOriginalTitle(dataJson.getString("title"));
        }
        if(dataJson.containsKey("url")){
            mcnContentBo.setPageLink(dataJson.getString("url"));
        }
        if(dataJson.containsKey("thumbnails_qqnews")){
            mcnContentBo.setCover(dataJson.getJSONArray("thumbnails_qqnews").getString(0));
        }
        if(dataJson.containsKey("thumbnails")){
            mcnContentBo.setMusicCoverUrl(dataJson.getJSONArray("thumbnails").get(0).toString());
        }
        if(dataJson.containsKey("time")){
            mcnContentBo.setPublishTime(DateUtil.parseTime(dataJson.getString("time")).getTime());
        }
        mcnContentBo.setCreateTime(System.currentTimeMillis());

        logger.info("TTkuaibao data:{}",mcnContentBo.toStringRmContent());
        return mcnContentBo;
    }
}
