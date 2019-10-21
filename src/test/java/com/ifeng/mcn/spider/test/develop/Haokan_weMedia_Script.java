package com.ifeng.mcn.spider.test.develop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.DateUtil;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Description: 描述
 * @author: yingxj
 * @date: 2019/10/11 10:41
 */
public class Haokan_weMedia_Script extends CrawlerWorker {

    private static Logger logger = LoggerFactory.getLogger(Haokan_weMedia_Script.class);

    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    public static void main(String[] args) throws Exception{
        Haokan_weMedia_Script haokan_weMedia_script  = new Haokan_weMedia_Script();
        Map map =  Maps.newHashMap() ;
        haokan_weMedia_script.params.set(map);
        map.put("link","https://haokan.baidu.com/haokan/wiseauthor?app_id=1574408238031335") ;
        map.put("mcnTaskId","test001") ;
        map.put("taskType","haokan_weMedia") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","haokan_weMedia_http") ;
        map.put("mediaId","1574408238031335");
        map.put("mediaName","百度新闻直播");
        System.out.println(JSON.toJSON(map));
        List<String>  result = haokan_weMedia_script.crawlerListPage(map);
        System.out.println(JSON.toJSON(result));
        McnContentBo mcnContentBo = haokan_weMedia_script.crawlerDetailPage(result.get(0), map);
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        List<String> result = new ArrayList<>();
        try {
            String link = params.get("link").toString();
            Document d = http(link).execute().parse();
            String jsonText = d.select("#_page_data").get(0).toString();
            jsonText = jsonText.split("__PRELOADED_STATE__ =")[1].split(";         document")[0];
            JSONObject jsonObject = JSON.parseObject(jsonText);
            JSONObject videoResult= jsonObject.getJSONObject("video");
            JSONArray results = videoResult.getJSONArray("results");
            for(Object r: results){
                result.add(r.toString());
            }
            logger.info("haokan DataList:{}",result);
        } catch (Exception e){
            logger.error("haokanListPageErr:{}\n{}",e.getMessage(),e);
        }

        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        McnContentBo mcnContentBo = new McnContentBo();

        try{
            JSONObject jsonObject = JSON.parseObject(itemBody);
            String video_short_url =jsonObject.getJSONObject("content").getString("video_short_url");
            String title = jsonObject.getJSONObject("content").getString("title");
            String vid = jsonObject.getJSONObject("content").getString("vid");
            String publish_time = jsonObject.getJSONObject("content").getString("publish_time");
            long time = parseDate(publish_time).getTime();
            Long read_num = jsonObject.getJSONObject("content").getLong("read_num");
            String video_src = jsonObject.getJSONObject("content").getString("video_src");
            String cover_src = jsonObject.getJSONObject("content").getString("cover_src");
            Integer like_num = jsonObject.getJSONObject("content").getInteger("like_num");
            Integer commentNum = jsonObject.getJSONObject("content").getInteger("commentNum");

            //判重
            String duplicateKey = params.get("taskType") + vid;
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;
            }
            if (System.currentTimeMillis() - time > TWO_DAY_M) {
                return null;
            }

            mcnContentBo.setDuplicateKey(duplicateKey);
            mcnContentBo.setMusicDownloadUrl(video_src);
            mcnContentBo.setPlatId(vid);
            mcnContentBo.setShapeType("2");
            mcnContentBo.setCover(cover_src);
            mcnContentBo.setMediaName(params.get("mediaName").toString());
            mcnContentBo.setMediaId(params.get("mediaId").toString());
            mcnContentBo.setPageLink(video_short_url);
            mcnContentBo.setShareLink(video_short_url);
            mcnContentBo.setTitle(title);
            mcnContentBo.setCreateTime(System.currentTimeMillis());
            mcnContentBo.setPublishTime(time);
            mcnContentBo.setPlayCount(read_num);
            mcnContentBo.setLikeCount(like_num);
            mcnContentBo.setSourceLink(params.get("link").toString());
            mcnContentBo.setCommentCount(commentNum);
            mcnContentBo.setFrom(mcnContentBo.getMediaName());
        }catch (Exception e){
            logger.error("haokanDetailPageErr:{}",e);
        }
        logger.info("好看视频同步:{}" , mcnContentBo.toStringRmContent());

        return mcnContentBo;
    }

    private Date parseDate(String time){
        Date result = null;
        if(time.contains("年")&&time.contains("月")&&time.contains("日")){
            result = DateUtil.parseDateCn(time);
        }else if (time.contains("月")&&time.contains("日")){
            result = DateUtil.parseDateCn(Calendar.getInstance().get(Calendar.YEAR)+"年"+time);
        }else{
            result = DateUtil.parseTime(getHaokanTime(time));
        }
        return result;
    }

    private String getHaokanTime(String publish_time){
        if(publish_time==null){
            return null;
        }
        publish_time  = publish_time + "";
        //publish_time = publish_time.replace("/第(\\d+)楼/", "").trim();
        //logger.info("从评论获取创建时间为:"+publish_time);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance(); //得到日历
        calendar.setTime(date);//把当前时间赋给日历
        String dateStr = "";
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if(publish_time == "今天"){
            dateStr = simpleDate.format(date);
        }else if(publish_time.indexOf("昨天") != -1){
            calendar.add(Calendar.DATE, -1);
            Date zuotian = calendar.getTime();
            dateStr = simpleDate.format(zuotian);
        }else if(publish_time.indexOf("1天前") != -1){
            calendar.add(Calendar.DATE, -2);
            Date yitianqian = calendar.getTime();
            dateStr =simpleDate.format(yitianqian);
        }else if(publish_time.indexOf("2天前") != -1){
            calendar.add(Calendar.DATE, -3);
            Date liangtianqian = calendar.getTime();
            dateStr = simpleDate.format(liangtianqian);
        }else if(publish_time.indexOf("3天前") != -1){
            calendar.add(Calendar.DATE, -4);
            Date santianqian = calendar.getTime();
            dateStr = simpleDate.format(santianqian);
        }else if(publish_time.indexOf("4天前") != -1){
            calendar.add(Calendar.DATE, -5);
            Date sitianqian = calendar.getTime();
            dateStr = simpleDate.format(sitianqian);
        }else if(publish_time.indexOf("5天前") != -1){
            calendar.add(Calendar.DATE, -6);
            Date wutianqian = calendar.getTime();
            dateStr = simpleDate.format(wutianqian);
        }else if(publish_time.indexOf("6天前") != -1){
            calendar.add(Calendar.DATE, -7);
            Date liutianqian = calendar.getTime();
            dateStr = simpleDate.format(liutianqian);
        }else if(publish_time.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")){
            dateStr = publish_time;
        }
        return dateStr;
    }

}
