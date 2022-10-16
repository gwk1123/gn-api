package com.gn.api.web.model;

import lombok.Data;

@Data
public class SibeExceptionResponse {

    private String status;

    //响应信息
    private String msg;

    //UUID
    private String uuid;

    private String timeLapse;

    public SibeExceptionResponse(){}

    public SibeExceptionResponse(String status, String msg){
        this.status = status;
        this.msg = msg;
    }

    public SibeExceptionResponse(String status, String msg,String uuid,String timeLapse){
        this.status = status;
        this.msg = msg;
        this.uuid = uuid;
        this.timeLapse = timeLapse;
    }
}
