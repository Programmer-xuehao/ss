package com.ifeng.mcn.common.base;

/**
 * 任务状态
 * @author chenghao1
 * @create 2019/10/15
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public enum TaskStatusEnum {

    /**
     * 激活
     */
    ACTIVE("1"),
    /**
     * 初始化
     */
    INIT("0"),
    /**
     * 暂停抓取
     */
    PAUSE("2"),
    /**
     * 账号异常 注销
     */
    ACCOUNT("3"),
    /**
     * 停止更新  不更新  没有内容  半年以上
     */
    NOUPDATE("4"),
    /**
     * 错误链接
     */
    ERRORLINK("5"),
    /**
     * 没有数据
     */
    NODATA("6")
    ;
    private String  code  ;

    TaskStatusEnum(String type){
        this.code=type ;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
