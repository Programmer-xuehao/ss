package com.ifeng.mcn.spider.test.develop.prod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 描述
 * @author: yingxj
 * @date: 2019/10/10 15:59
 */
public class Qutoutiao_Category_weMedia_Script extends CrawlerWorker {

    private static Logger logger = LoggerFactory.getLogger(Qutoutiao_Category_weMedia_Script.class);

    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    public static void main(String[] args) throws Exception{
        Qutoutiao_Category_weMedia_Script qutoutiao_category_weMedia_script  = new Qutoutiao_Category_weMedia_Script() ;
        Map map =  Maps.newHashMap() ;
        map.put("link","https://api.1sapp.com/content/getListV2?qdata=MDNFRTI2Q0M1RUIzNkExQ0Q2MDQwRjgxMjFCMzM5MzguY0dGeVlXMGZOak01TWpkbVl6TXRNalJoTWkwME5tRm1MVGd3TmpVdE1qUmtNVGN6WWpoak5UZG1IblpsY25OcGIyNGZNaDV3YkdGMFptOXliUjloYm1SeWIybGsuQFHwf%2F%2BXW0%2FsEFWINStjRnM8BHAMsC1uSdm112uV55Fr40tMLBr6xIIuwc7tXhr5o2cTR0tcxltdRqlfiXpcKEc%2BuQisg3zFAyUXfMsp9IDt4OdGdQuLgY2cfNVHKMBpSsFi4EmftkhecGt7l4akjQYmfac9ZNtJpK7AEIBUba%2Fg8n4EIU2TN5uK2rPLB0nAkjaUtm0MTDTgie7jufSUk2ONbswqGK0t4aPnfi7QLq%2BfQf66rQxHWjebEUEfPOIZ2SG8zVLKa8cEPRQOGfCCc6hUer27o3wKSBAVeBS2kXz2YlZleD2D7WAxRz8sFH%2FuQ4hjEs65XedZBvxIq1AkNfNzGYYwQyGAc64Li1OIRJ0vzneU8RjY%2FloRPu9dqu5THxQx8prjeBLX1UW0Mg3tH4JJQgUJOqaDx2SMa0kX2x1aYHiPS65sezoNc%2FSVRqJuEh8sufJFNNxoEZWr4A4BGu9QaOtqVcCMRMLJnJfU3r3RNORwawfNwwc%2BmAFXD5TjHbRhOt6s4BeY43yhayKSkZUR3TJ%2BgS6tjFGH7c3r3HNou%2Fg0r9xiCeZlE5umMFAzQpB5BG02wFdTs%2BWHUIVoy03Hn87fVlkYBkybVPbuaAkz") ;
        map.put("mcnTaskId","test001") ;
        map.put("taskType","qutoutiao_infoFlow") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","qutoutiao_infoFlow_http") ;
        qutoutiao_category_weMedia_script.params.set(map);

        List<String>  result = qutoutiao_category_weMedia_script.crawlerListPage(map);
        McnContentBo mcnContentBo = qutoutiao_category_weMedia_script.crawlerDetailPage(result.get(0), map);
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {

        List<String> result = new ArrayList<>();
        String link = params.get("link").toString();
        Document document = getTheirsData(link);
        String content = document.text();
        JSONObject jsonObject = JSON.parseObject(content);
        JSONArray data = jsonObject.getJSONObject("data").getJSONArray("data");
        for (Object item : data) {
            JSONObject itemJson = (JSONObject) item;
            String tips = itemJson.getString("tips");
            if ("广告".equalsIgnoreCase(tips) || "推广".equalsIgnoreCase(tips))
                continue;

            String id = itemJson.getString("id");
            String duplicateKey = params.get("taskType") + id;
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
            //重复为true
//            if (duplicateKeyDao.containsKey(duplicateKey)) {
//                continue ;//重复key,取消抓取
//            }
            logger.info("title " + itemJson.getString("title") + " time " + itemJson.get("publish_time"));
            if (System.currentTimeMillis() - itemJson.getLong("publish_time") > TWO_DAY_M) {
                continue;
            }
            Map temp = new HashMap();
            temp.put("itemJson",itemJson);
            if ("视频".equalsIgnoreCase(tips)) {
                temp.put("type","video");
            } else {
                temp.put("type","article");
            }
            result.add(JSON.toJSONString(temp));
        }

        logger.info("qutoutiao_infoFlowList:{}",result);
        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        Map map = JSON.parseObject(itemBody, Map.class);
        String type = (String) map.get("type");
        McnContentBo mcnContentBo = new McnContentBo();
        JSONObject itemJson = (JSONObject) map.get("itemJson");
        mcnContentBo.setDuplicateKey(params.get("taskType") + itemJson.getString("id"));
        if(type.equals("video")){
            handlerVideo(itemJson, mcnContentBo);
        } else{
            handlerArticle(itemJson, mcnContentBo);
        }

        logger.info("qutoutiao_infoFlowDetail:{}",mcnContentBo.toStringRmContent());
        return mcnContentBo;
    }

    public void  handlerVideo(JSONObject itemJson, McnContentBo mcnContentBo){
        mcnContentBo.setTitle(itemJson.getString("title"));
        mcnContentBo.setPageLink(itemJson.getString("url"));
        mcnContentBo.setPlatId(itemJson.getString("id"));
        JSONArray covers = itemJson.getJSONArray("cover");
        if(null!=covers&&!covers.isEmpty()){
            mcnContentBo.setCover(covers.get(0).toString());
        }
        if(itemJson.containsKey("video_info")){
            JSONObject video_info = itemJson.getJSONObject("video_info");
            if (video_info.containsKey("hd")){
                mcnContentBo.setMusicDownloadUrl(video_info.getJSONObject("hd").getString("url"));
            } else if (video_info.containsKey("ld")){
                mcnContentBo.setMusicDownloadUrl(video_info.getJSONObject("ld").getString("url"));
            }
        }

        mcnContentBo.setPublishTime(itemJson.getLong("publish_time"));
        mcnContentBo.setLikeCount(itemJson.getInteger("like_num"));
        mcnContentBo.setPlayCount(itemJson.getLong("read_count"));
        mcnContentBo.setCommentCount(itemJson.getInteger("comment_count"));
        mcnContentBo.setTag((List)itemJson.getJSONArray("tag"));
        mcnContentBo.setUnlikeCount(itemJson.getInteger("unlike_num"));
        mcnContentBo.setShareCount(itemJson.getInteger("share_count"));
        mcnContentBo.setCreateTime(System.currentTimeMillis());
        mcnContentBo.setMediaName(itemJson.getString("source_name"));
        mcnContentBo.setMediaId(itemJson.getString("source_id"));
        mcnContentBo.setFrom(itemJson.getString("nickname"));
        mcnContentBo.setSourceLink(params.get().get("url").toString());
        mcnContentBo.setShapeType("2");

        logger.info("趣头条视频数据：{}",mcnContentBo.toStringRmContent());
    }
    public void  handlerArticle(JSONObject itemJson, McnContentBo mcnContentBo){
        String source_name = itemJson.getString("source_name");
        String sourceLink = itemJson.getString("url");
        String title = itemJson.getString("title");
        String content = itemJson.getString("detail");

        List keyword = itemJson.getJSONArray("tag");
        Integer haveVideo = itemJson.getInteger("has_video");
        JSONArray cover = itemJson.getJSONArray("cover");
        String coverURL = null;
        if(null!=cover&&!cover.isEmpty()){
            coverURL = itemJson.getJSONArray("cover").getString(0).replace("\\/ ","/");
        }
        Long read_count = itemJson.getLong("read_count");
        Integer share_count = itemJson.getInteger("share_count");
        Integer comment_count = itemJson.getInteger("comment_count");
        Integer like_num = itemJson.getInteger("like_num");
        Integer unlike_num = itemJson.getInteger("unlike_num");
        String source_id = itemJson.getString("source_id");

        mcnContentBo.setShapeType("1");
        mcnContentBo.setFrom(source_name);
        mcnContentBo.setPageLink(sourceLink);
        mcnContentBo.setTitle(title);
        mcnContentBo.setCreateTime(System.currentTimeMillis());
        mcnContentBo.setPublishTime(itemJson.getLong("publish_time"));
        mcnContentBo.setKeyword(keyword);
        mcnContentBo.setSourceLink("https://h5ssl3.1sapp.com/qukan_new2/dest/zmt_home/read/zmt_home/index.html?id="+source_id);
        mcnContentBo.setHaveViedo(haveVideo);
        mcnContentBo.setCover(coverURL);
        mcnContentBo.setMediaName(itemJson.getString("source_name"));
        mcnContentBo.setMediaId(itemJson.getString("source_id"));
        mcnContentBo.setPlayCount(read_count);
        mcnContentBo.setShareCount(share_count);
        mcnContentBo.setCommentCount(comment_count);
        mcnContentBo.setLikeCount(like_num);
        mcnContentBo.setUnlikeCount(unlike_num);
        mcnContentBo.setContent(content);
        logger.info("趣头条文章数据：{}",mcnContentBo.toStringRmContent());
    }

    private Document getTheirsData(String url){
        Document result = null;
        // 可能会报超时，重试一次
        for(int i=0 ; i<2; i++){
            try {
                result = http(url).execute().parse();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(result!=null){
                break;
            }
        }
        return result;
    }
}
