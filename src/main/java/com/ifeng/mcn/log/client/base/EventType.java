package com.ifeng.mcn.log.client.base;

/**
 * 记录事件类型
 *
 * @author chenghao1
 * @create 2019/9/16
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public enum EventType {


    arrive(Constants.EVENT_TYPE_SPIDER_ARRIAVE, "获取任务加入线程池"),
    invoke(Constants.EVENT_TYPE_SPIDER_START, "执行任务方法:GlueFactory.glue()"),
    normal(Constants.EVENT_TYPE_NORMAL, "正常入库日志"),
    list(Constants.EVENT_TYPE_SPIDER_LIST, "列表页下载"),
    detail(Constants.EVENT_TYPE_SPIDER_DETAIL, "详情页下载"),
    listrisk(Constants.EVENT_TYPE_SPIDER_RISK, "列表风控调用"),
    detailrisk(Constants.EVENT_TYPE_SPIDER_DETAIL_RISK, "列表风控调用")
    ;


    private String symbol;

    private String label;

    private EventType(String symbol, String label) {
        this.symbol = symbol;
        this.label = label;
    }

    public String symbol() {
        return this.symbol;
    }

    public String label() {
        return this.label;
    }




}



