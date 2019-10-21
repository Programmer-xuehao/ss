package com.ifeng.mcn.spider.test.develop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
 * @date: 2019/10/10 18:17
 */
public class Huosaon_weMedia_Script extends CrawlerWorker {

    private static Logger logger = LoggerFactory.getLogger(Huosaon_weMedia_Script.class);

    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    public static void main(String[] args) throws Exception{
        Huosaon_weMedia_Script huosoon_weMedia_script  = new Huosaon_weMedia_Script() ;
        Map map =  Maps.newHashMap() ;
        huosoon_weMedia_script.params.set(map);
        map.put("link","https://reflow.huoshan.com/share/user/89879772942/") ;
        map.put("mcnTaskId","test001") ;
        map.put("taskType","huosan_weMedia") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","huosan_weMedia_http") ;

        List  result = huosoon_weMedia_script.crawlerListPage(map);
        McnContentBo mcnContentBo = huosoon_weMedia_script.crawlerDetailPage(result.get(0).toString(), map);
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        String link = params.get("link").toString();

        List<String> items = new ArrayList<>();
        try{
            Document html = http(link).execute().parse();
            String nickname = html.getElementsByClass("nickname").get(0).text();
            this.params.get().put("mediaName",nickname);
            Elements ps = html.getElementsByClass("intro").get(0).getElementsByTag("p");
            if(null==ps || ps.isEmpty()){
                return null;
            }

            for(Element p : ps ){
                Pattern compile = Pattern.compile("火山号：\\d+");
                Matcher matcher = compile.matcher(p.text());
                if(matcher.find()){
                    this.params.get().put("mediaId",matcher.group().replace("火山号：",""));
                }
            }

            String accountId=link.split("/")[5];
            // 一次最多获取30条记录
            String accountUrl="https://reflow.huoshan.com/share/load_videos/?offset=0&count=30&user_id="+accountId;

            String body = http(accountUrl).execute().body();
            JSONArray jsonArray = JSON.parseObject(body).getJSONObject("data").getJSONArray("items");
            if(null!=jsonArray&&!jsonArray.isEmpty()){
                for (Object o : jsonArray){
                    items.add(o.toString());
                }
            }
        }catch (Exception e){
            logger.error("huoshanListPage:{}",e);
        }

        logger.info("huoshanListData:{}",items);
        return items;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        logger.info("huoshanDataDetailPage");
        McnContentBo mcnContentBo = new McnContentBo();
        try{
            JSONObject articleItemJson = JSON.parseObject(itemBody);
            String duplicateKey = params.get("taskType") + articleItemJson.getString("id");
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;//重复key,取消抓取
            }
            if (System.currentTimeMillis() - articleItemJson.getLong("create_time")*1000 > TWO_DAY_M) {
                logger.info("huoshanTimeout:{}",articleItemJson.getLong("create_time"));
                return null;
            }
            mcnContentBo.setDuplicateKey(duplicateKey);
            mcnContentBo.setCreateTime(System.currentTimeMillis());
            mcnContentBo.setPlatId(articleItemJson.getString("id"));
            JSONObject video = articleItemJson.getJSONObject("video");
            JSONArray download_url = video.getJSONArray("download_url");
            if(null!=download_url&&!download_url.isEmpty()){
                mcnContentBo.setMusicDownloadUrl(download_url.getString(0));
            }
            mcnContentBo.setPublishTime(articleItemJson.getLong("create_time")*1000);
            mcnContentBo.setMediaName(params.get("mediaName").toString());
            mcnContentBo.setMediaId(params.get("mediaId").toString());
            mcnContentBo.setShapeType("2");
            if(articleItemJson.getJSONObject("stats").getString("play_count")!=null)
                mcnContentBo.setPlayCount(articleItemJson.getJSONObject("stats").getLong("play_count"));
            if(articleItemJson.getJSONObject("stats").getString("share_count")!=null)
                mcnContentBo.setShareCount(articleItemJson.getJSONObject("stats").getInteger("share_count"));

            JSONArray covers = articleItemJson.getJSONObject("video").getJSONObject("cover").getJSONArray("url_list");
            if(covers!=null&&!covers.isEmpty())
                mcnContentBo.setCover(covers.getString(0));

            String playurl = articleItemJson.getJSONObject("video").getJSONArray("url_list").get(0).toString();
            mcnContentBo.setSourceLink(params.get("link").toString());
            mcnContentBo.setPageLink(playurl);
            // double 小数可能被磨掉
            mcnContentBo.setVideoDuration(articleItemJson.getJSONObject("video").getInteger("duration"));
            mcnContentBo.setFrom(mcnContentBo.getMediaName());

            logger.info("huoshanData:{}",mcnContentBo.toStringRmContent());
        } catch (Exception e){
            logger.error("huoshanDataErr:{}",e.getMessage());
        }
        return mcnContentBo;
    }
}
