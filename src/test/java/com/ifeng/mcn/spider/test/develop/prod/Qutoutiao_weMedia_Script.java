package com.ifeng.mcn.spider.test.develop.prod;

import com.alibaba.fastjson.JSON;
import com.ifeng.mcn.common.utils.CommonUtils;
import com.ifeng.mcn.data.api.bo.McnContentBo;
import com.ifeng.mcn.spider.script.CrawlerWorker;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 示例抓取任务脚本-趣头条
 * 不需要代理
 * @author ZFang
 */
@Service
public class Qutoutiao_weMedia_Script extends CrawlerWorker {


    public static void main(String[] args) throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", "977503");
        params.put("link", "http://api.1sapp.com/wemedia/content/videoList?token=&dtu=200&version=0&os=android&id=977503&page=1");
        params.put("mcnTaskId", "test001");
        params.put("taskType", "qutoutiao_weMedia");
        params.put("crawlerType", "http");
        params.put("riskKeyPrefix", "qutoutiao_http");

        Qutoutiao_weMedia_Script script = new Qutoutiao_weMedia_Script();
        script.params.set(params);

        List<String> list = script.crawlerListPage(params);
        list.forEach(System.out::println);

        for (String itemBody : list) {
            script.crawlerDetailPage(itemBody, params);
        }
    }


    @Override
    public List<String> crawlerListPage(Map<String, Object> params) throws Exception {
        //http://api.1sapp.com/wemedia/content/videoList?token=&dtu=200&version=0&os=android&id=977503&page=1
        String mid = params.get("link").toString().split("\\?id=")[1];
        String apiListUrl = String.format("http://api.1sapp.com/wemedia/content/videoList?token=&dtu=200&version=0&os=android&id=%s&page=1", mid);
        return JsonPath.parse(http(apiListUrl).execute().body())
                .read("$.data.list.*", JSONArray.class)
                .stream().map(JSON::toJSONString)
                .collect(Collectors.toList());
    }

    @Override
    public McnContentBo crawlerDetailPage(String itemBody, Map<String, Object> params) throws Exception {
        DocumentContext item = JsonPath.parse(itemBody);

        //判重
        String duplicateKey = "qutoutiao" + item.read("$.id");
        duplicateKey = CommonUtils.md5IdUrl(duplicateKey);

        //本地main方法测试时需要注掉
        //重复为true
        if (duplicateKeyDao.containsKey(duplicateKey)) {
            return null;//重复key,取消抓取
        }

        //转换为McnContentBo
        McnContentBo contentBo = new McnContentBo();
        contentBo.setDuplicateKey(duplicateKey);
        contentBo.setShapeType("2");//1 文章  2 视频  3 ALL
        contentBo.setTitle(item.read("$.title"));
        contentBo.setOriginalTitle(item.read("$.title"));
        contentBo.setCommentCount(item.read("$.comment_count"));
        contentBo.setPlayCount(item.read("$.read_count", Long.class));
        contentBo.setPageLink(item.read("$.url"));
        contentBo.setShareLink(item.read("$.share_url"));
        contentBo.setPublishTime(item.read("$.publish_time", Long.class));
        contentBo.setLikeCount(item.read("$.like_num", Integer.class));
        contentBo.setUnlikeCount(item.read("$.unlike_num", Integer.class));
        contentBo.setSourceLink(item.read("$.video_info.hd.url"));
        contentBo.setCommentCount(item.read("$.show_comment"));
        contentBo.setCover(item.read("$.cover[0]"));
        contentBo.setPlatId(item.read("$.id"));
        contentBo.setMusicDownloadUrl(item.read("$.video_info.hd.url"));
        contentBo.setMediaName(item.read("$.source"));
        contentBo.setMediaId(item.read("$.source_id"));

        //按时间过滤
        if (exceedPubTime(contentBo.getPublishTime())) {
            return null;
        }

        //任务中心下发的参数
        contentBo.setTaskId(params.get("mcnTaskId") + "");
        contentBo.setMediaId(params.get("mediaId") + "");
        contentBo.setMediaName(params.get("mediaName") + "");

        logger.warn("趣头条脚本抓取结果：{}", contentBo);
        return contentBo;
    }
}
