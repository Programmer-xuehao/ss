package com.ifeng.mcn.spider.test.develop.prod;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.ifeng.mcn.spider.utils.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
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
 * @date: 2019/10/11 16:55
 */
public class Souhu_weMedia_Script extends CrawlerWorker {

    private static Logger logger = LoggerFactory.getLogger(Souhu_weMedia_Script.class);

    private static final Long TWO_DAY_M = 2*24*60*60*1000L;

    private static String articleUrlRegex = "https:\\/\\/www.sohu.com\\/(a\\/\\d+_\\d+|picture\\/\\d+)";

    public static void main(String[] args) {
        try{
            Souhu_weMedia_Script souhu_weMedia_script  = new Souhu_weMedia_Script() ;
            Map<String,Object> map =  Maps.newHashMap() ;
            souhu_weMedia_script.params.set(map);
            map.put("link","https://mp.sohu.com/profile?xpt=Z2hfN2Y0YjMwNWU4MGVjQHNvaHUuY29t&_f=index_pagemp_1&spm=smpc.content.author.1.1571412048682yMa6ixJ") ;
            map.put("mcnTaskId","test001") ;
            map.put("taskType","souhu_weMedia") ;
            map.put("crawlerType","http") ;
            map.put("riskKeyPrefix","souhu_weMedia_http") ;
            map.put("mediaId","Z2hfN2Y0YjMwNWU4MGVjQHNvaHUuY29t");
            map.put("mediaName","潘家园街道潘家园社区");
            System.out.println(JSON.toJSON(map));
            List<String>  result = souhu_weMedia_script.crawlerListPage(map);
            System.out.println(JSON.toJSON(result));
            McnContentBo mcnContentBo = souhu_weMedia_script.crawlerDetailPage(result.get(1), map);
            System.out.println(JSON.toJSON(mcnContentBo));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        String link = params.get("link").toString();
        Connection http = http(link, null);
        String query = http.request().url().getQuery();
        List<String> result = new ArrayList<String>();

        try{
            Document document = http.execute().parse();
            Elements one = document.select("div.article-box").select("article.one-pic-mode");
            Elements video = document.select("div.article-box").select("article.video-pic-mode");
            Elements article = document.select("div.article-box").select("article.multi-pic-mode");
            one.addAll(video);
            one.addAll(article);
            if(null==one||one.isEmpty()){
                return null;
            }
            for(Element e : one){
                result.add(e.html());
            }

            String[] split = query.split("&");
            for(String s : split){
                if(s.contains("xpt=")){
                    this.params.get().put("mediaId",s.replace("xpt=",""));
                    break;
                }
            }

            this.params.get().put("mediaName",document.getElementsByClass("author-info_name").get(0).text());
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        logger.info("sohu DataList:{}，{}",params.get("link"),result);
        return result;
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        Document e = Jsoup.parse(itemBody);
        String id = e.select("div > span.comment").attr("data-state-loading");
        //判重
        String duplicateKey = params.get("taskType") + id;
        duplicateKey = CommonUtils.md5IdUrl(duplicateKey);
        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;
        }

        String u = "http:"+e.select("a").attr("href");
//        String cover = "http:" + e.select("a > img").attr("src");
        String cover = "http:"+e.getElementsByTag("img").get(0).attr("original");

        int idxBegin = u.indexOf("?");
        if (idxBegin > 0){
            u = u.substring(0,idxBegin);
        }
        Document d = http(u,null).execute().parse();

        Long lts = null;

        Element time = d.getElementById("news-time");
        if(null!=time){
            lts = DateUtil.parseTimeMinutes(time.text()).getTime();
        }
        if (System.currentTimeMillis() - lts > TWO_DAY_M) {
            return null;
        }

        String title = d.select("#article-container > div.left.main > div:nth-child(1) > div.text > div.text-title > h1").text();
        if(StringUtils.isBlank(title)){
            title = d.select("body > div.content.area > div.article-box.l > h3").text();
            if(StringUtils.isBlank(title)){
                title = d.select("#article-container > div.left.main > div:nth-child(1) > div.text > div.text-title > h1 > span.title-info-title").text();
                if(StringUtils.isBlank(title)){
                    title = d.select("#article-container > div.left.main > div:nth-child(1) > div.text > div.text-title > h1").text();
                }
            }
        }

        McnContentBo mcnContentBo = new McnContentBo();

        List<String> strTags = new ArrayList<>();
        Elements tags = e.getElementsByClass("tags");
        if(tags!=null&&!tags.isEmpty()){
            Elements as = tags.get(0).getElementsByTag("a");
            if(as!=null&&!as.isEmpty()){
                for(Element a: as){
                    strTags.add(a.text());
                }
                mcnContentBo.setTag(strTags);
            }
        }


        String content = "";
        Elements articleHtml = d.getElementsByClass("article-text");
        if(articleHtml!=null && !articleHtml.isEmpty()){
            content = articleHtml.get(0).html();
        } else{
            Element editor = d.getElementById("mp-editor");
            if(null!=editor){
                content = editor.html();
            }
            if(StringUtil.isBlank(content)){
                logger.info("sohu content isnull :{}",u);
                return null;
            }
        }


        mcnContentBo.setMediaId(params.get("mediaId").toString());
        mcnContentBo.setMediaName(params.get("mediaName").toString());
        mcnContentBo.setDuplicateKey(duplicateKey);
        mcnContentBo.setPageLink(u);
        mcnContentBo.setTitle(title);
        mcnContentBo.setContent(content);
        mcnContentBo.setShapeType("1");
        mcnContentBo.setCreateTime(System.currentTimeMillis());
        mcnContentBo.setSourceLink(params.get("link").toString());
        mcnContentBo.setPublishTime(lts);
        mcnContentBo.setCover(cover);
        String text = d.getElementsByClass("read-num").get(0).text();
        Pattern compile = Pattern.compile("阅读(\\([^\\)]\\d+\\))");
        Matcher matcher = compile.matcher(text);
        if(matcher.find()){
            mcnContentBo.setPlayCount(Long.valueOf(matcher.group().replace("阅读(","").replace(")","")));
        }
        logger.info("sohu data:{}",mcnContentBo.toStringRmContent());

        return mcnContentBo;
    }
}
