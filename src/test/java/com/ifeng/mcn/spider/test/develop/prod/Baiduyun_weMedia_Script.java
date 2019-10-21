package com.ifeng.mcn.spider.test.develop.prod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.common.utils.DateUtil;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 描述
 * @author: yingxj
 * @date: 2019/10/11 16:01
 */
public class Baiduyun_weMedia_Script extends CrawlerWorker {
    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    public static void main(String[] args) throws Exception{
        Baiduyun_weMedia_Script baiduyun_weMedia_script  = new Baiduyun_weMedia_Script() ;
        Map map =  Maps.newHashMap() ;
        baiduyun_weMedia_script.params.set(map);
        map.put("link","https://cpu.baidu.com/1033/b4e53502/profile/e4b889e6af9be7be8ee9a39fe8aeb0/video?from=list") ;
        map.put("mcnTaskId","test001") ;
        map.put("taskType","baiduyun_weMedia") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","baiduyun_http") ;
        map.put("mediaId","e4b889e6af9be7be8ee9a39fe8aeb0");
        map.put("mediaName","三毛美食记");
        System.out.println(JSON.toJSON(map));
        List<String>  result = baiduyun_weMedia_script.crawlerListPage(map);

        for (int i=0; i<result.size();i++) {
            baiduyun_weMedia_script.crawlerDetailPage(result.get(i), map);
        }
    }

    @Override
    public void setLog() {
        this.logger = LoggerFactory.getLogger(Baiduyun_weMedia_Script.class);
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        String pattern = "(?<=window.APP_STATE=).*?(?=</script)";
        String link = params.get("link").toString();
        Document parse = http(link).execute().parse();
        Elements elements = parse.getElementsByClass("video-layout news-wrapper").get(0).getElementsByTag("li");
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(parse.html());
        JSONObject jobj = null;
        if (m.find()) {
            jobj = JSON.parseObject(m.group());
        }
//        String username = jobj.getJSONObject("author").getStr("name");
        JSONArray jsonArray = jobj.getJSONArray("result");
        if(null==jsonArray||jsonArray.isEmpty()){
            return null;
        }
        List<String> result = new ArrayList<>();

        for (int i=0; i<jsonArray.size();i++){
            if(jsonArray.getJSONObject(i).size()<5)
                continue;
            JSONObject temp = jsonArray.getJSONObject(i);
            Element el = elements.get(i);
            temp.put("href","https://cpu.baidu.com" + el.getElementsByTag("a").attr("href"));
            result.add(temp.toString());
        }
        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        JSONObject jsonObject = JSON.parseObject(itemBody);
        JSONObject data = jsonObject.getJSONObject("data");
        String vid = data.getString("id");

        //判重
        String duplicateKey = params.get("taskType") + vid;
        duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;
        }

        String updateTime = data.getString("updateTime");
        long time = DateUtil.parseTime(updateTime).getTime();
        if (System.currentTimeMillis() - Long.valueOf(time) > TWO_DAY_M) {
            return null;
        }

        String title = data.getString("title");

//        String thumbUrl = data.getString("thumbUrl");
//        String ownerId = data.getString("ownerId");
//        String url = data.getString("url");
        Long playCount = data.getLong("playbackCount");

        McnContentBo mcnContentBo = new McnContentBo();
        mcnContentBo.setPlatId(vid);
        mcnContentBo.setTitle(title);
        mcnContentBo.setCreateTime(System.currentTimeMillis());
        mcnContentBo.setPublishTime(time);
        mcnContentBo.setMediaId(params.get("mediaId").toString());
        mcnContentBo.setMediaName(params.get("mediaName").toString());
        mcnContentBo.setFrom(mcnContentBo.getMediaName());
        mcnContentBo.setPageLink(jsonObject.getString("href"));
        mcnContentBo.setSourceLink(params.get("link").toString());
        mcnContentBo.setPlayCount(playCount);
        mcnContentBo.setShapeType("2");

        logger.info("百度云视频：{}",mcnContentBo.toStringRmContent());
        return mcnContentBo;
    }
}
