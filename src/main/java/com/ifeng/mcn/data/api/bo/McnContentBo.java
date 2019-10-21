package com.ifeng.mcn.data.api.bo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * mcn  内容表转换
 *
 * @author chenghao1
 * @create 2019/9/10
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
@Data
@Accessors(chain=true)
@NoArgsConstructor
public class McnContentBo implements Serializable {


//    public McnContentBo(Map<String, Object> params) {
//
//        String taskType = (String) params.get("taskType");
//        this.setFrom((String) params.get("from"));
//        this.setTaskId(params.get("mcnTaskId") + "");
//        this.setFrom(params.get("from") + "");
//        this.setMediaId((String) params.get("mediaId"));
//        this.setMediaName(params.get("mediaName") + "");
//        this.setTaskType(taskType);
//        this.setSiteCode(taskType.split("_")[0]);
//        this.setSourceLink(params.get("link") + "");
//
//
//    }

    /***
     *  数据是否可返回
     * @param mcnContentBo
     * @return
     */
    public boolean dataReflag(McnContentBo mcnContentBo) {
        return StringUtils.isNotBlank(mcnContentBo.getTitle()) && StringUtils.isNotBlank(mcnContentBo.getPageLink());
    }


    private String id;
    private String ucmsId;
    /**
     * 抓取内容站点对应文章或视频唯一
     */
    private String platId;
    private String title;
    private String originalTitle;
    //  内容类型
    private String shapeType;
    private String content;
    private Integer processStatus;
    private String siteCode;
    private List<String> keyword;
    private String taskId;
    private List<String> tag;
    private String mediaName;
    private String mediaId;
    private String abstractInfo;
    private String from;  //  来源
    private String cover; // 封面图
    private Integer haveViedo;  // 1 有 2 没有
    private Integer haveAudio;  // 1   有 2  没有
    private Integer commentCount;// 评论数
    private Long playCount;//播放次数

    private Integer storeCount;//收藏数
    private Integer likeCount;// 点赞数
    private Integer shareCount;//分享数
    private Integer unlikeCount;// 踩数
    private Integer delay;  //延迟 分钟
    private String sourceLink;  // list  来源页面
    private String shareLink; //  分享页面
    private String pageLink; //  当前页面地址
    private String nlpResult;
    private List<String> lv1Catogery;
    private List<String> lv2Catogery;
    private String province;  // 省
    private String city;  // 市
    private String region;  // 区
    private String catogery;  //  原平台 分类
    private Integer videoDuration;
    private String videoCity;  // 视频发布城市
    private String videoDynamicCover; // 视频动态图
    private String videoTopic; // 视频话题
    private String musicAuthor; // 视频音乐作者
    private String musicDownloadUrl; // 视频音乐下载地址,视频源地址，非详情地址
    private String musicDescription; // 视频音乐描述信息
    private String musicCoverUrl; //  封面
    private Integer score;   //  评分
    private Integer isLong;  //  是否长效
    private String ifengCoverUrl; //本地化 封面
    private String videoKeywords; //智能识别分类
    private String videoKeywordsFormat; //智能识别 带权重分类
    private String ifengMediaFiles; //视频 现在完毕信息
    private String videoAspect; //视频 比例
    private String tomanyImg; //  视频连图
    private String videoText; //  语音 识别结果信息
    private Integer status;  //  内容状态
    private Long publishTime;  //  原始发布时间
    private Long createTime;  //  抓取时间
    private Long updateTime;   //  更新时间
    private Boolean originalFlag;//是否为原创

    //  add  字段 时间范围查询
    private Long gtCreate;  // 大于
    private Long ltCreate; // 小于

    // 判重key
    private String duplicateKey;

    // 删除时间key
    private Date delkey;

    // 话题
    private List<String> topic;

    /**
     * 是否重复，0：不重复，1：重复，2：字数过短无法判断
     */
    private Integer repeat;

    /**
     * 本地化时传入的source参数，枚举值
     */
    private String localParamSource;

    private String pageProcess;
    /**
     * 同步多个采用英文,分开
     */
    private String syncSys;

    /**
     * 张林清洗回调带回的del字段，=1则表示不要同步出去
     */
    private Integer cleanDelStatus;

    /**
     * 本地化是否成功标志
     */
    private Boolean localFlag;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 对应账户表的唯一id
     */
    private String sourceId;

    /**
     * 日志打印使用，已经内容替换成是否有内容
     * @return
     */
    public String toStringRmContent() {
        String o = JSON.toJSONString(this);
        JSONObject jsonObject = JSON.parseObject(o);
        jsonObject.put("content",content==null ? false:true);
        return jsonObject.toJSONString();
    }
}