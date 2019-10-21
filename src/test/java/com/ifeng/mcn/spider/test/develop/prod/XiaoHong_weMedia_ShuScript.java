package com.ifeng.mcn.spider.test.develop.prod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.DateUtil;
import com.ifeng.mcn.spider.utils.MySpider;
import com.ifeng.mcn.spider.utils.ProxyUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 描述
 * @author: yingxj
 * @date: 2019/10/18 14:17
 */
public class XiaoHong_weMedia_ShuScript extends CrawlerWorker {

    private static Logger logger = LoggerFactory.getLogger(XiaoHong_weMedia_ShuScript.class);

    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    public static void main(String[] args) throws Exception{
        XiaoHong_weMedia_ShuScript xiaoHong_weMedia_ShuScript  = new XiaoHong_weMedia_ShuScript() ;
        Map map =  Maps.newHashMap() ;
        map.put("link","https://www.xiaohongshu.com/user/profile/572c0cbd5e87e70c4bf3fd56");
        map.put("mcnTaskId","test001") ;
        map.put("taskType","xiaohongshu_weMedia") ;
        map.put("crawlerType","http") ;
        map.put("riskKeyPrefix","xiaohongshu_weMedia_http") ;
//        String ss = "{\"id\":\"5da8054961ab1a80be0b910e\",\"mcnTaskId\":\"15712924890050\",\"shapeType\":3,\"taskLevel\":1,\"taskPlan\":[\"2019-10-19 11:16\",\"2019-10-19 19:16\",\"2019-10-19 09:14\",\"2019-10-20 03:16\"],\"status\":1,\"taskType\":\"xiaohongshu_weMedia\",\"link\":\"https://www.xiaohongshu.com/user/profile/5a8572884eacab422b37bcc2?xhsshare=CopyLink&appuid=5993037f50c4b45b18a4fc85&apptime=1550561246\",\"mediaId\":\"\",\"mediaName\":\"长沙野生张条条\",\"from\":\"人工\",\"crawlerType\":\"http\",\"riskKeyPrefix\":\"xiaohongshu_weMedia_http\",\"isSpiderHistory\":2,\"scriptName\":\"spider_script.xiaohongshu_weMedia_Script\",\"remark\":\"\",\"insertTime\":\"2019-02-21 11:14:16\",\"insertMinuteOfDay\":675,\"sourceId\":\"6564803619529429061\",\"flowSpiderInterval\":0,\"pageProcess\":\"PersistProcess,NlpProcess,RepeatProcess,LocalProcess,CleanProcess,DistributeProcess\",\"mcnExecuteTaskId\":\"15712924890050_2019-10-19 09:14\",\"taskCreateTime\":1571447643800,\"scriptNameByConf\":\"spider_script.XiaoHong_weMedia_ShuScript\"}";
//        map = JSON.parseObject(ss,Map.class);
        xiaoHong_weMedia_ShuScript.params.set(map);

        List<String>  result = xiaoHong_weMedia_ShuScript.crawlerListPage(map);
        for(String s : result){
            McnContentBo mcnContentBo = xiaoHong_weMedia_ShuScript.crawlerDetailPage(s, map);
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        List<String>  result = new ArrayList<>();
        String link = params.get("link").toString();

        logger.info("小红书任务执行参数:{}", JSON.toJSONString(params));
        String res = "";
        try{
            Html html = doHttp(link).getHtml();

            Elements elements = html.getDocument().select("body > script");
            for(Element e : elements){
                if(e.html().contains("__INITIAL_SSR_STATE__")){
                    res = e.html().replace("window.__INITIAL_SSR_STATE__=","");
                    logger.info("小红书列表json获取,{}",e.html());
                    res = JSON.parseObject(res).getJSONObject("Main").getString("notesDetail");
                }
            }

//            if(link.contains("discovery/item")){
//                Document document = Jsoup.parse(res);
//                String el = document.select("body > script").get(0).html();
//                el = el.replace("window.__INITIAL_SSR_STATE__=","");
//                JSONObject jsonObject = JSON.parseObject(el);
//                JSONObject userJson = jsonObject.getJSONObject("NoteView").getJSONObject("commentInfo").getJSONObject("targetNote").getJSONObject("user");
//                link = "https://www.xiaohongshu.com/user/profile/" + userJson.getString("id");
//                res = doHttp(link);
//                if (StringUtil.isBlank(res)) {
//                    return result;
//                }
//                this.params.get().put("link",link);
//            }

            JSONArray resArr = JSON.parseArray(res);
            if (resArr == null || resArr.size() == 0) {
                logger.info("小红书列表获取不到结果：{},[{}]",res,params);
                return result;
            }

            for(Object s : resArr){
                result.add(s.toString());
            }
            logger.info("xiaohongshuListPage12:{}",result);
        } catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.error("小红书列表抓取异常:{},{},{}",res,e,sw);
        }
        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        JSONObject job = JSON.parseObject(itemBody);
        logger.info("xiaohongshuCrawlerDetailPage:{}",itemBody);
        McnContentBo mcnContentBo = new McnContentBo();
        try {
            String duplicateKey = params.get("taskType") + job.getString("id");
            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
            //重复为true
            if (duplicateKeyDao.containsKey(duplicateKey)) {
                return null;
            }

            mcnContentBo.setPlatId(job.getString("id"));
            String link = "https://www.xiaohongshu.com/discovery/item/" + job.getString("id");
            String cover = job.getString("cover");
            JSONObject coverJs = JSONObject.parseObject(cover);
            String coverUrl = coverJs.getString("original");
            String userInfo = job.getString("user");
            JSONObject userInfoJs = JSONObject.parseObject(userInfo);
            String userId = userInfoJs.getString("id");
            String nickName = userInfoJs.getString("nickname");

            String html = doHttp(link).getHtml().toString();

            Document document = Jsoup.parse(html);

            String el = document.select("body > script").get(0).html();
            el = el.replace("window.__INITIAL_SSR_STATE__=","");
            logger.info("xiaohonshuInfo:{}",el);
            JSONObject obj = JSONObject.parseObject(el);
            JSONObject obj3 = obj.getJSONObject("NoteView").getJSONObject("noteInfo");
            // 发布时间
            String pubDate = obj3.getString("time");
            if (System.currentTimeMillis() - DateUtil.parseTimeMinutes(pubDate).getTime() > TWO_DAY_M) {
                return null;
            }

            Long playCount = obj3.getLong("collects");
            // 点赞数
            Integer likeCount = obj3.getInteger("likes");
            // 评论数
            Integer commentCount = obj3.getInteger("comments");
            //文章内容content,如果是视频就是标题
            String content = obj3.getString("desc");
            //获取视频的评论数。喜欢数、和发布时间
            Elements elements = document.getElementsByClass("operation-block").get(0).getElementsByTag("span");
            elements.forEach(s->{
                if(s.html().contains("star")){
                    mcnContentBo.setStoreCount(Integer.valueOf(s.text()));
                }
            });
            mcnContentBo.setPublishTime(DateUtil.parseTimeMinutes(pubDate).getTime());
            mcnContentBo.setPlayCount(playCount);
            mcnContentBo.setLikeCount(likeCount);
            mcnContentBo.setCommentCount(commentCount);

            if(obj3.getString("type").equals("video")){
                String video = obj3.getString("video");
                mcnContentBo.setMusicDownloadUrl(video);
                mcnContentBo.setTitle(content);
                mcnContentBo.setShapeType("2");
            } else{
                mcnContentBo.setTitle(obj3.getString("name"));
                mcnContentBo.setContent(content);
                mcnContentBo.setShapeType("1");
            }


            mcnContentBo.setPageLink(link);
            mcnContentBo.setCover(coverUrl);
            mcnContentBo.setMediaId(userId);
            mcnContentBo.setMediaName(nickName);

            mcnContentBo.setDuplicateKey(duplicateKey);
            mcnContentBo.setCreateTime(System.currentTimeMillis());
            mcnContentBo.setSourceLink(params.get("link").toString());
        }catch (Exception e){
            logger.error("xiaohonshuDataResult:{}",e);
        }
        logger.info("xiaohongshuDetailPage:{}",mcnContentBo.toStringRmContent());

        return mcnContentBo;
    }

    Site site = Site.me().setUserAgent(MySpider.getRandomUa()).setRetryTimes(3).setTimeOut(10000);

    /***
     * 获取html
     * @param url
     * @return
     */
    private Page doHttp(String url) throws Exception {
        Map map = new HashMap<>();

        map.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        map.put("Accept-Encoding", "gzip, deflate, br");
        map.put("Accept-Language", "zh-CN,zh;q=0.9");
        map.put("Cache-Control", "max-age=0");
        map.put("Cookie", "xhsTrackerId=5a243d39-023b-46bf-c6a3-a505d471024a; Hm_lvt_9df7d19786b04345ae62033bd17f6278=1559015267,1559036745; Hm_lvt_d0ae755ac51e3c5ff9b1596b0c09c826=1559015267,1559036745; extra_exp_ids=; xhs_spses.5dde=*; Hm_lpvt_9df7d19786b04345ae62033bd17f6278=1560133306; Hm_lpvt_d0ae755ac51e3c5ff9b1596b0c09c826=1560133306; xhs_spid.5dde=6bbeee19ea1493d7.1559015268.4.1560133309.1559036748.98312260-5a1e-4dc6-ba04-d1248c1d10d6");
        map.put("Upgrade-Insecure-Requests", "1");
        map.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");

        for(Object key : map.keySet()){
            site.addHeader(key.toString(),map.get(key).toString());
        }
        return httpMySpider(url).setDownloader(ProxyUtils.getProxyDownLoad()).setProxyFlag(true).setSite(site).run();
    }
}
