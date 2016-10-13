package com.tweather.app.util;

/**
 * Created by DADOU on 2016/10/13.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);

}
