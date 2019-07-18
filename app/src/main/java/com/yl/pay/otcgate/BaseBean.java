package com.yl.pay.otcgate;

/**
 * Created by lu on 2019/7/18.
 */

class BaseBean<T> {
    String msg;
    String code;
    T data;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
