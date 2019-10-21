package com.ifeng.mcn.spider.test.develop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 描述
 * @author: yingxj
 * @date: 2019/10/11 14:40
 */
public class Douyin_weMedia_Script extends CrawlerWorker {
    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    public static void main(String[] args) throws Exception{
        Douyin_weMedia_Script douyin_weMedia_script  = new Douyin_weMedia_Script() ;
        Map map =  Maps.newHashMap() ;
        douyin_weMedia_script.params.set(map);
        map.put("link","https://www.iesdouyin.com/share/user/64269185197") ;
        map.put("mcnTaskId","test001") ;
        map.put("taskType","douyin_weMedia") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","douyin_http") ;
        map.put("mediaId","64269185197");
        map.put("mediaName","中国日报");
        System.out.println(JSON.toJSON(map));
        List<String>  result = douyin_weMedia_script.crawlerListPage(map);
        System.out.println(JSON.toJSON(result));

        for (int i=0; i<result.size();i++) {
            McnContentBo mcnContentBo = douyin_weMedia_script.crawlerDetailPage(result.get(i), map);
        }
    }

    @Override
    public void setLog() {
        this.logger = LoggerFactory.getLogger(Douyin_weMedia_Script.class);
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        String link = params.get("link").toString();
        Pattern pattern  = Pattern.compile("user/\\d+");
        Matcher matcher = pattern.matcher(link);
        String userId = "";
        if (!matcher.find()){
            return null;
        }
        userId = matcher.group().replace("user/","");
        String videoApi  ="https://www.iesdouyin.com/web/api/v2/aweme/post/?user_id="+userId+"&sec_uid=&count=21&max_cursor=0&aid=1128&_signature=ioOyAxAV19WDz8gWFDFnXYqDsh&dytk=2114da61f79778dbb301f9b37be9debe";
        String body = http(videoApi).execute().body();
        JSONObject pageDetail = JSON.parseObject(body);

        JSONArray aweme_list = pageDetail.getJSONArray("aweme_list");
        if(null==aweme_list||aweme_list.isEmpty()){
            return null;
        }
        List<String> result = new ArrayList<>();
        for(Object aweme : aweme_list){
            JSONObject videoInfo = (JSONObject) aweme;
            //判重
            String duplicateKey = params.get("taskType") + videoInfo.getJSONObject("video").getJSONObject("play_addr").getString("uri");
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                continue;
            }
            result.add(videoInfo.toJSONString());
        }

        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        McnContentBo mcnContentBo = new McnContentBo();

        JSONObject data = JSON.parseObject(itemBody);
        String title=data.getString("desc");
        Integer share_count=data.getJSONObject("statistics").getInteger("share_count");
        Integer comment_count=data.getJSONObject("statistics").getInteger("comment_count");
        Integer digg_count=data.getJSONObject("statistics").getInteger("digg_count");
//        String forward_count=data.getJSONObject("statistics").getString("forward_count");
        String cover=data.getJSONObject("video").getJSONObject("origin_cover").getJSONArray("url_list").getString(0);
//        String vid=data.getJSONObject("video").getJSONObject("play_addr").getString("uri");
//        String duration=data.getJSONObject("video").getString("duration");
        String play_addr=data.getJSONObject("video").getJSONObject("play_addr").getJSONArray("url_list").getString(0);
        String pageVid = data.getString("aweme_id");
        String pageUrl="https://www.iesdouyin.com/share/video/"+pageVid+"/?region=CN&mid="+pageVid;


        mcnContentBo.setTitle(title);
        mcnContentBo.setCreateTime(System.currentTimeMillis());
        mcnContentBo.setPublishTime(mcnContentBo.getCreateTime());
        mcnContentBo.setMediaId(params.get("mediaId").toString());
        mcnContentBo.setMediaName(params.get("mediaName").toString());
        mcnContentBo.setCover(cover);
        mcnContentBo.setCommentCount(comment_count);
        mcnContentBo.setLikeCount(digg_count);
        mcnContentBo.setShareCount(share_count);
        mcnContentBo.setSourceLink(play_addr);
        mcnContentBo.setPageLink(pageUrl);
        mcnContentBo.setSourceLink(params.get("link").toString());
        mcnContentBo.setFrom(mcnContentBo.getMediaName());

        logger.info("抖音:{}" ,mcnContentBo);



        return null;
    }
}
