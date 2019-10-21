package com.ifeng.mcn.common.base;

/**
 * 任务状态
 * @author chenghao1
 * @create 2019/10/15
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public enum TaskStatusEnum {

    ACTIVE("1"),
    INIT("0"),
    PAUSE("2"),
    ACCOUNT("3"),
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
