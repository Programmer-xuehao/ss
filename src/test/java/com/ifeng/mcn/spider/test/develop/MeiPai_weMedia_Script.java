//package com.ifeng.mcn.spider.test.develop;
//
//import com.alibaba.fastjson.JSONObject;
//import com.google.common.collect.Maps;
//import com.ifeng.mcn.common.utils.CommonUtils;
//import com.ifeng.mcn.common.utils.StringUtil;
//import com.ifeng.mcn.data.api.bo.McnContentBo;
//import com.ifeng.mcn.log.client.base.EventType;
//import com.ifeng.mcn.spider.script.CrawlerWorker;
//import com.ifeng.mcn.spider.utils.DateUtil;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import sun.misc.BASE64Decoder;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * @Description: 描述
// * @author: yingxj
// * @date: 2019/10/10 19:22
// */
//public class MeiPai_weMedia_Script extends CrawlerWorker {
//
//    private static Logger logger = LoggerFactory.getLogger(MeiPai_weMedia_Script.class);
//
//    private static final Long TWO_DAY_M = 2*24*60*60*1000L;
//
//    private static Pattern compile = Pattern.compile("user/\\d+");
//
//    public static void main(String[] args) throws Exception{
//        MeiPai_weMedia_Script meiPai_weMedia_script  = new MeiPai_weMedia_Script() ;
//        Map map =  Maps.newHashMap() ;
//        meiPai_weMedia_script.params.set(map);
//        map.put("link","https://www.meipai.com/user/1461556760") ;
//        map.put("mcnTaskId","test001") ;
//        map.put("taskType","meipai_weMedia") ;
//        map.put("crawlerType","http") ;
//        map.put("riskKeyPrefix","meipai_weMedia_http") ;
//
//        List<String>  result = meiPai_weMedia_script.crawlerListPage(map);
//        McnContentBo mcnContentBo = meiPai_weMedia_script.crawlerDetailPage(result.get(0), map);
//    }
//
//    @Override
//    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
//        logger.info("meipaiScriptStart");
//        String link = params.get("link").toString();
//        Document pageDoc = http(link).execute().parse();
//
//        //获取到的视频列表集合mediaList
//        Elements videoEles = pageDoc.select("div.feed-content.clearfix>div.feed-left.fl>ul#mediasList");
//
//        if(videoEles.size() == 0 || videoEles == null){
//            return null;
//        }
//        Element videoUL =  videoEles.get(0);
//        //获取到每条视频所在的LI元素
//        Elements videoLis = videoUL.select("li.feed-item.pr");
//        if(videoEles.size() == 0 || videoLis == null){
//            return null;
//        }
//        List<String> result = new ArrayList<>();
//        for(Element v : videoLis){
//            result.add(v.html());
//        }
//        String userName = pageDoc.getElementsByClass("user-name").get(0).getElementsByTag("a").get(0).text();
//        if(StringUtil.isNotBlank(userName)){
//            this.params.get().put("mediaName",userName);
//        }
//
//        Matcher matcher = compile.matcher(link);
//        if(matcher.find()){
//            this.params.get().put("mediaId",matcher.group().replace("user/",""));
//        }
//        logger.info("meipaiDataList:{}",result);
//        return result;
//    }
//
//    @Override
//    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
//        McnContentBo mcnContentBo = new McnContentBo();
////        Element vEle = JSON.parseObject(itemBody,Element.class);
//        try{
//            Document vEle = Jsoup.parse(itemBody);
//
//            //获取文章发布时间
//            Element timeEle = vEle.select("div.feed-user.pr>span.feed-time.pa").get(0);
//            Element dataEle = vEle.select("div.feed-user.pr>meta").get(1);
//            String dateText = dataEle.attr("content");
//            String timeText = timeEle.text();
//            //视频发布时间
//            timeText = getVideoTime(dateText, timeText);
//            long time = DateUtil.parseTimeMinutes2(timeText).getTime();
//            if (System.currentTimeMillis() - time > TWO_DAY_M) {
//                return null;
//            }
//
//            //获取视频封面图片
//            Element imgCoverEle = vEle.select("div.feed-v-wrap.pr.loading > img").get(0);
//            String posterUrl = imgCoverEle.attr("src");
//            posterUrl = getVideoCover(posterUrl);
//
//            Element videoUrlEle = vEle.select("a.feed-description.break").get(0);
//            String videoHref = videoUrlEle.attr("href");
//
//            //视频Id
//            String vIdStr = videoHref.substring(videoHref.lastIndexOf("/")+1);
//            if (StringUtil.isBlank(vIdStr))
//                return null;
//
//            //排重
//            String duplicateKey = params.get("taskType") + vIdStr;
//            duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
//            //重复为true
////            if (duplicateKeyDao.containsKey(duplicateKey)) {
////                return null;//重复key,取消抓取
////            }
//
//            //获取视频页详情地址
//            videoHref = getVideoPageUrl(videoHref);
//
//            //获取单个视频的详情页
//            Document videoPageDoc = doHttp(videoHref);
//            Elements videoDetails = videoPageDoc.select("div.detail-content.clearfix");
//
//            Elements metas = videoPageDoc.getElementsByTag("meta");
//            if(metas==null || metas.isEmpty())
//                return null;
//
//            Element meta = null;
//            for(Element m : metas){
//                if("og:video:url".equals(m.attr("property"))){
//                    meta = m;
//                    break;
//                }
//            }
//            if(null==meta || videoDetails.size() == 0 || videoDetails == null)
//                return null;
//
//            String videoSource =  meta.attr("content");
//            Map<String, String> map1 = getHex(videoSource);
//            Map<String,String[]> map2 = getDecimal(map1.get("hex"));
//            String str4 = substr(map1.get("str"), map2.get("pre"));
//            BASE64Decoder base64 = new BASE64Decoder();
//            byte[] url = base64.decodeBuffer(substr(str4, getPosition(str4, map2.get("tail"))));
//            // 视频真实的url地址
//            String sourceLink = new String(url);
//            mcnContentBo.setMusicDownloadUrl("http:"+sourceLink);
//
//
//            List<String> catogerys = new ArrayList<>();
//            List<String> tags = new ArrayList<>();
//            //获取到视频的分类类型(比如搞笑、吃秀之类的)
//            Elements description = videoPageDoc.getElementsByClass("detail-description break js-detail-desc");
//            if(null != description && !description.isEmpty()){
//                Elements as = description.get(0).getElementsByTag("a");
//                if(null!=as && !as.isEmpty()){
//                    for(Element a: as){
//                        //获取视频标签、分类（##中间的部分）
//                        if(a.attr("href").contains("square")){
//                            catogerys.add(a.text().replace("#",""));
//                        }
//                        //获取视频标签、话题（##中间的部分）
//                        if(a.attr("href").contains("topic")){
//                            tags.add(a.text().replace("#",""));
//                        }
//                    }
//                }
//            }
//
//            Element divDetail = videoDetails.get(0);
//            //获取视频播放量
//            Element detailInfo = divDetail.select(" div.detail-left.fl.pr > div.detail-info.pr").get(0);
//            Element playCountEle = detailInfo.select("div.detail-location").get(0);
//            String playCountText = playCountEle.text();
//            playCountText = playCountText.replace("播放", "");
//
//            //获取视频描述
//            Element descEle = detailInfo.select(" h1.detail-description.break.js-detail-desc").get(0);
//            String desc = descEle.text();
//
//            //获取视频标题
//            String title = "";
//            Elements titleEles = detailInfo.select("h1.detail-cover-title.break");
//            if(titleEles != null && titleEles.size() >0) {
//                title = titleEles.get(0).text();
//            }else{
//                title = desc;
//            }
//
//            //获取点赞、评论、分享等
//
//            Elements countEles = detailInfo.select("div.detail-count.no-select");
//            String likeText = countEles.select("div.detail-count.no-select > span.detail-like.dbl.pr.cp > span").get(0).text();
//            int likeCount = getIntVal(likeText);
//
//            String commentText = countEles.select("#commentCount").get(0).text();
//            int commentCount = getIntVal(commentText);
//
//            String shareText = detailInfo.select("#shareMediaBtn > span").get(0).text();
//            int shareCount = getIntVal(shareText);
//
//            mcnContentBo.setPlayCount(Long.valueOf(playCountText));
//            mcnContentBo.setDuplicateKey(duplicateKey);
//            mcnContentBo.setPlatId(vIdStr);
//            mcnContentBo.setShapeType("2");
//            mcnContentBo.setTitle(title);
//            mcnContentBo.setCreateTime(System.currentTimeMillis());
//            mcnContentBo.setPublishTime(time);
//            mcnContentBo.setMediaName(params.get("mediaName").toString());
//            mcnContentBo.setMediaId(params.get("mediaId").toString());
//            mcnContentBo.setCover(posterUrl);
//            mcnContentBo.setPageLink(videoHref);
//            mcnContentBo.setSourceLink(params.get("link").toString());
//            mcnContentBo.setFrom(mcnContentBo.getMediaName());
//            mcnContentBo.setLikeCount(likeCount);
//            mcnContentBo.setCommentCount(commentCount);
//            mcnContentBo.setShareCount(shareCount);
//        }catch (Exception e){
//            logger.error("meipaiDetailPage:{}",e);
//        }
//        logger.info("meipaiData:{}" , mcnContentBo.toStringRmContent());
//        return mcnContentBo;
//    }
//
//    /***
//     * 获取视频的标签描述
//     * @param title
//     * @param desc
//     * @return
//     */
//    private String getVideoTags(String title,String desc ){
//        String tags = null;
//        tags = getTags(title) + getTags(desc);
//        return tags;
//    }
//    public static  String  getTags(String originalString) {
//        Pattern hashtagPattern = Pattern.compile("#[^#]+#");
//        String tagText = "";
//
//        if (originalString.indexOf("【") != -1 && originalString.indexOf("】") != -1) {
//            tagText = originalString.substring(originalString.indexOf("【"));
//            tagText = tagText.substring(1, tagText.indexOf("】"));
//        }
//        Matcher matcher = hashtagPattern.matcher(originalString);
//        while (matcher.find()) {
//            int matchStart = matcher.start();
//            int matchEnd = matcher.end();
//            String tmpHashtag = originalString.substring(matchStart, matchEnd);
//            tagText = tagText +" "+originalString.substring(matchStart+1, matchEnd-1);
//            originalString = originalString.replace(tmpHashtag, "");
//            matcher = hashtagPattern.matcher(originalString);
//        }
//        return tagText;
//    }
//    private int getIntVal(String text){
//        int val = 0;
//        int index = text.indexOf("万");
//        if(index == -1){
//            if(text.indexOf("分享") != -1){
//                return val;
//            }
//            val = Integer.parseInt(text);
//        }else{
//            val = (new Double(text.substring(0,index)).intValue())*10000;
//        }
//        return val;
//    }
//
//    private Document doHttp(String url) {
//        try {
//            JSONObject riskInfo = getRiskInfo(url, url, EventType.detail);
//            Document parse = http(url, riskInfo).execute().parse();
//            return parse;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    /***
//     * 获取视频封面地址
//     * @param url
//     * @return
//     */
//    private String getVideoCover(String url){
//        String imgUrl = "";
//        if (!"".equals(url)) {
//            imgUrl = url.startsWith("//") ? "http:" + url : url;
//        }
//        return imgUrl;
//    }
//
//    /***
//     * 获取视频创建时间
//     * @param createAt  样例数据：06-07  12:00
//     * @return
//     */
//    private String getVideoTime(String dateText,String createAt){
//        if(createAt == null || dateText == null){
//            return null;
//        }
//        String dateStr = "";
//        String[] arr = createAt.split(" ");
//        if(arr.length == 2){
//            dateStr = dateText +" "+ arr[1] + ":00";
//        }else{
//            dateStr = dateText +" "+createAt.replaceAll("[\\u4e00-\\u9fa5]","") + ":00";
//        }
//
//        return dateStr;
//    }
//
//    /***
//     * 获取视频页详情地址
//     * @param url
//     * @return
//     */
//    private String getVideoPageUrl(String url){
//        String pageUrl = "";
//        if(url.indexOf("http") == -1){
//            pageUrl = "https://www.meipai.com" + url;
//        }
//        return pageUrl;
//    }
//
//    /***
//     * 解析美拍视频链接
//     */
//
//    // 获得16进制数，该数用来分割字符串
//    public Map<String,String> getHex(String param1){
//        Map dict1 = new HashMap<String, String>();
//        String cstr =  param1.substring(4);//str
//        String[] splitStr = param1.substring(0,4).split("");
//        String hex = "";
//        for (int i=3; i >= 0; i--){
//            hex = hex + splitStr[i];
//        }
//        dict1.put("str", cstr);
//        dict1.put("hex", hex);
//        return dict1;
//    }
//    // 获取正确的字符串,解析16进制数
//    public Map<String, String[]> getDecimal(String param1){
//        Map dict2 = new HashMap<String, String[]>();
//        // loc2是用来分割字符串的索引标识，转换16进制
//        String loc2 = String.valueOf(Integer.parseInt(param1,16));
//        String[] pre = loc2.substring(0,2).split("");//dict1.put("loc2", loc2.substring(0,2));
//        String[] tail = loc2.substring(2).split("");
//        dict2.put("pre", pre);
//        dict2.put("tail", tail);
//        return dict2;
//    }
//    // 分割字符串
//    public String substr(String param1, String[] param2) {
//        String loc3 = param1.substring(0, Integer.parseInt(param2[0]));//param2 = pu.getDec(pa2).get("pre")
//        String loc4 = param1.substring(Integer.parseInt(param2[0]), Integer.parseInt(param2[0])+Integer.parseInt(param2[1]));
//        return loc3 + param1.substring(Integer.parseInt(param2[0])).replace(loc4, "");
//    }
//    // 获取分割的位置
//    public String[] getPosition(String param1, String[] param2){
//        param2[0] = String.valueOf(param1.length() - Integer.parseInt(param2[0]) - Integer.parseInt(param2[1]));
//        return param2;
//    }
//}
