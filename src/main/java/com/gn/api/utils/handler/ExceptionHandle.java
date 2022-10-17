package com.gn.api.utils.handler;

import com.gn.api.web.model.SibeExceptionResponse;
import com.gn.utils.exception.CustomSibeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ExceptionHandle {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandle.class);

    /**
     * 异常处理
     * @param e 异常信息
     * @return 返回类是我自定义的接口返回类，参数是返回码和返回结果，异常的返回结果为空字符串
     */

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public SibeExceptionResponse handle(Exception e) {

        //自定义异常返回对应编码
        if (e instanceof CustomSibeException) {
            CustomSibeException ex = (CustomSibeException) e;
            return new SibeExceptionResponse(ex.getStatus(),ex.getMsg());
        } else {  //其他异常报对应的信息
            logger.error("[系统异常]{}", e.getMessage(), e);
            return new SibeExceptionResponse("-1", "fail");
        }
    }

}
