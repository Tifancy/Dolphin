package com.dianping.paas.service;

import com.dianping.paas.entity.AppEntity;

import java.util.List;

/**
 * chao.yu@dianping.com
 * Created by yuchao on 15/11/3.
 */
public interface AppService {
    List<AppEntity> getAll();
}