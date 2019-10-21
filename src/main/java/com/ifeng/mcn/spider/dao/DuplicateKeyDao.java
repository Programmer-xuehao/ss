package com.ifeng.mcn.spider.dao;

import org.springframework.stereotype.Component;

/**
 * @author: ZFang
 */
@Component
public class DuplicateKeyDao {

    /**
     * 为方便本地编写脚本所以直接返回false
     *
     * @param duplicateKey
     * @return
     */
    public boolean containsKey(String duplicateKey) {
        return false;
    }

}
