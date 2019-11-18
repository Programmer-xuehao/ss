package com.ifeng.mcn.common.base;

/**
 * 〈一句话功能简述〉<br>
 *
 * @author chenghao1
 * @create 2019/10/9
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public enum ContentType {
    //shape//1 文章  2 视频  3 ALL
    ARTICLE("1"),
    VIDEO("2"),
    GALLARY("3"),
    ALL("4") ;
    private String  code  ;

    ContentType(String type){
        this.code=type ;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
