package com.ifeng.mcn.spider.exceptions;

/**
 * @Description:
 * @author: yingxj
 * @date: 2019/11/26 15:30
 */
public class CrawlerException extends Exception{

    private static final long serialVersionUID = 1L;

    public CrawlerException() {
        super();
    }

    public CrawlerException(String msg) {
        super(msg);
    }

    public CrawlerException(String format, Object... arguments){
        super(String.format(format,arguments));
    }

}
