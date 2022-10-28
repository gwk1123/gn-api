package com.gn.api.web.ctrip;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gn.api.utils.SibeServiceUtil;
import com.gn.config.SibeProperties;
import com.gn.ota.ctrip.model.*;
import com.gn.ota.ctrip.transform.TransformCtripOrderRequest;
import com.gn.ota.ctrip.transform.TransformCtripPayRequest;
import com.gn.ota.ctrip.transform.TransformCtripVerifyRequest;
import com.gn.ota.site.SibeOrderRequest;
import com.gn.ota.site.SibePayRequest;
import com.gn.ota.site.SibeVerifyRequest;
import com.gn.sibe.KProductService;
import com.gn.sibe.SibeOrderService;
import com.gn.sibe.SibePayService;
import com.gn.sibe.SibeVerifyService;
import com.gn.utils.aes.AESUtils;
import com.gn.utils.constant.SibeConstants;
import com.gn.utils.exception.CustomSibeEncryptException;
import com.gn.utils.exception.CustomSibeException;
import com.gn.utils.logs.LogFileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


/**
 * The type Order resource.
 */
@RestController
@RequestMapping("/api")
@Api(description = "携程机票直连API生单")
public class OrderCtripResource {

    private Logger LOGGER = LoggerFactory.getLogger(OrderCtripResource.class);
    @Autowired
    private SibeProperties sibeProperties;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransformCtripVerifyRequest transformCtripVerifyRequest;
    @Autowired
    private SibeVerifyService sibeVerifyService;
    @Autowired
    private SibeOrderService sibeOrderService;
    @Autowired
    private SibeServiceUtil sibeServiceUtil;
    @Autowired
    private TransformCtripOrderRequest transformOrderRequest;
    @Autowired
    private TransformCtripPayRequest transformCtripPayRequest;
    @Autowired
    private KProductService kProductService;
    @Autowired
    private SibePayService sibePayService;

    /**
     * Verify response entity.
     */
    @ApiOperation("验价")
    @RequestMapping(value = "/verify")
    public ResponseEntity<CtripVerifyResponse> verify(@RequestBody String verifyRequest) throws Exception {
        //1.String转为OtaVerifyRequestVM
        //ObjectMapper objectMapper = new ObjectMapper();
        CtripVerifyRequest ctripVerifyRequest = null;
        try {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ctripVerifyRequest = objectMapper.readValue(verifyRequest, CtripVerifyRequest.class);
        } catch (IOException e) {
            LOGGER.error("请求无法解析成json格式 JsonMappingException", e);
            throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_1, "请求无法解析成json格式 IOException", "00000", "UnKnow");
        }
        SibeVerifyRequest sibeVerifyRequest = (SibeVerifyRequest) transformCtripVerifyRequest.toVerifyRequest(ctripVerifyRequest, sibeServiceUtil.getSibeVerifyRequest(sibeProperties));


        LOGGER.info("uuid:" + sibeVerifyRequest.getUuid() + " verify请求参数：" + verifyRequest);

        LogFileUtil.saveLogFile(sibeVerifyRequest.getUuid(), "verifyRequest", objectMapper, ctripVerifyRequest);

        CtripVerifyResponse ctripVerifyResponse = null;
        //判断是否为K位产品，是则调用K位verify，否则调用正常verify
        String productType = sibeVerifyRequest.getRouting().getSibeRoutingData().getSibePolicy().getProductType();
        if("2".equals(productType)){
            boolean permitKProductSign = false;
            try {
                permitKProductSign = kProductService.getKSeatSign(sibeVerifyRequest.getSite());
            } catch (Exception e) {
                LOGGER.error("uuid:"+sibeVerifyRequest.getUuid()+" Verify K位 获取开关失败");
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭-开关获取失败", sibeVerifyRequest.getUuid(),"verify");
            }

            if(permitKProductSign){
                ctripVerifyResponse  = (CtripVerifyResponse) kProductService.verify(sibeVerifyRequest);
            }else {
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭", sibeVerifyRequest.getUuid(),"verify");
            }
        }else {
            //不是K位产品，调用正常verify
            ctripVerifyResponse = (CtripVerifyResponse) sibeVerifyService.verify(sibeVerifyRequest);
        }

        LogFileUtil.saveLogFile(sibeVerifyRequest.getUuid(), "verifyResponse", objectMapper, ctripVerifyResponse);

        //ctripVerifyResponse.getRouting().setRule(null); //todo saveLogFile会变更对象的属性，暂时将被影响的属性赋值
        ctripVerifyResponse.getRule().setUseCtripRule(null);//todo verify不需要设置ctripRule
        ctripVerifyResponse.getRouting().setRule(null);

        Long s = (System.currentTimeMillis() - sibeVerifyRequest.getStartTime()) / (1000);
        LOGGER.info("uuid:" + sibeVerifyRequest.getUuid() + " verify返回消耗:" + s + "秒");

        return ResponseEntity.ok()
                .body(ctripVerifyResponse);
    }





    @ApiOperation("生单")
    @RequestMapping(value = "/order")
    public ResponseEntity<String> order(@RequestBody String orderRequest) throws Exception {
        //1.解密orderRequest
        ObjectMapper objectMapper = new ObjectMapper();
        String sKey =sibeProperties.getOta().getSkey();
        String decodeOrderRequest = null;

        try {
            decodeOrderRequest = AESUtils.jdk8decrypt(orderRequest, sKey);
        } catch (Exception e) {
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1,  "请求参数错误", "000001",sKey,"UnKnow");
        }
        if(StringUtils.isEmpty(decodeOrderRequest)){
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1,  "请求参数错误", "000002",sKey,"UnKnow");
        }

        //2.LOGGER.志打印
        // LOGGER.debug("***order请求参数：" + decodeOrderRequest);

        //3.String转为OtaOrderRequestVM
        CtripOrderRequest ctripOrderRequest=null;
        try {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ctripOrderRequest = objectMapper.readValue(decodeOrderRequest, CtripOrderRequest.class);
        }catch (IOException e) {
            LOGGER.error("请求无法解析成json格式 JsonMappingException",e);
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1,  "请求无法解析成json格式 IOException", "00000",sKey,"UnKnow");
        }

        //3.OtaVerifyRequestVM转为OtaVerifyRequest对象
        SibeOrderRequest sibeOrderRequest = transformOrderRequest.toOrderRequest(ctripOrderRequest,sibeServiceUtil.getSibeOrderRequest(sibeProperties));
        LOGGER.info("uuid:"+sibeOrderRequest.getUuid()+" order请求参数：" +decodeOrderRequest);
        LogFileUtil.saveLogFile(sibeOrderRequest.getUuid(),"orderRequest",objectMapper,ctripOrderRequest);

        CtripOrderResponse ctripOrderResponse = null;
        //判断是否为K位产品，是则调用K位verify，否则调用正常verify
        String productType = sibeOrderRequest.getRouting().getSibeRoutingData().getSibePolicy().getProductType();
        if("2".equals(productType)){
            boolean permitKProductSign = false;
            try {
                permitKProductSign = kProductService.getKSeatSign(sibeOrderRequest.getSite());
            } catch (Exception e) {
                LOGGER.error("uuid:"+sibeOrderRequest.getUuid()+" Verify K位 获取开关失败");
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭-开关获取失败", sibeOrderRequest.getUuid(),"verify");
            }

            if(permitKProductSign){
                ctripOrderResponse  = (CtripOrderResponse) kProductService.order(sibeOrderRequest);
            }else {
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭", sibeOrderRequest.getUuid(),"verify");
            }
        }else {
            //不是K位产品，调用正常verify
            ctripOrderResponse = (CtripOrderResponse) sibeOrderService.order(sibeOrderRequest);
        }

        LogFileUtil.saveLogFile(sibeOrderRequest.getUuid(),"orderResponse",objectMapper,ctripOrderResponse);

        //加密变成字符串
        String strOrderResponse= objectMapper.writeValueAsString(ctripOrderResponse);
        String encryptResult = AESUtils.getInstance().jdk8encrypt(strOrderResponse,sKey);

        Long s =(System.currentTimeMillis()-sibeOrderRequest.getStartTime())/(1000);
        LOGGER.info("uuid:"+sibeOrderRequest.getUuid() +" order返回消耗:"+ s +"秒");

        //返回密文
        return ResponseEntity.ok()
                .body(encryptResult);
    }


    @ApiOperation("付款校验")
    @RequestMapping(value = "/pay",
            method = RequestMethod.POST,
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> payValidate(@RequestBody String payRequest) throws Exception {
        String sKey =sibeProperties.getOta().getSkey();
        String decodePayRequest = null;
        try {
            decodePayRequest = AESUtils.jdk8decrypt(payRequest, sKey);
        } catch (Exception e) {
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1,  "请求参数错误", "000001",sKey,"UnKnow");
        }

        if(StringUtils.isEmpty(decodePayRequest)){
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1,  "请求参数错误", "000002",sKey,"UnKnow");
        }



        //3.String转为OtaOrderRequestVM
        CtripPayRequest ctripPayRequest = null;
        try {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ctripPayRequest = objectMapper.readValue(decodePayRequest, CtripPayRequest.class);
        }catch (JsonParseException e){
            LOGGER.error("请求无法解析成json格式 JsonMappingException",e);
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1,  "请求无法解析成json格式 JsonParseException", "00000",sKey,"UnKnow");
        } catch (JsonMappingException e) {
            LOGGER.error("请求无法解析成json格式 JsonMappingException",e);
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1,  "请求无法解析成json格式 JsonMappingException", "00000",sKey,"UnKnow");
        } catch (IOException e) {
            LOGGER.error("请求无法解析成json格式 JsonMappingException",e);
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1,  "请求无法解析成json格式 IOException", "00000",sKey,"UnKnow");
        }


        //Transform
        SibePayRequest sibePayRequest = (SibePayRequest)transformCtripPayRequest.toPayRequest(ctripPayRequest,sibeServiceUtil.getSibePayRequest(sibeProperties));

        LOGGER.info("uuid:"+sibePayRequest.getUuid()+" pay请求参数：" +decodePayRequest);
        //保存日志文件
        LogFileUtil.saveLogFile(sibePayRequest.getUuid(),"payRequest",objectMapper,ctripPayRequest);

        CtripPayResponse ctripPayResponse=null;
        String productType = sibePayRequest.getRouting().getSibeRoutingData().getSibePolicy().getProductType();
        if("2".equals(productType)){
            boolean permitKProductSign = false;
            try {
                permitKProductSign = kProductService.getKSeatSign(sibePayRequest.getSite());
            } catch (Exception e) {
                LOGGER.error("uuid:"+sibePayRequest.getUuid()+" pay K位 获取开关失败");
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭-开关获取失败", sibePayRequest.getUuid(),"pay");
            }

            if(permitKProductSign){
                ctripPayResponse  = (CtripPayResponse) kProductService.pay(sibePayRequest);
            }else {
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭", sibePayRequest.getUuid(),"pay");
            }
        }else {
            //发起pay核心请求
            ctripPayResponse = (CtripPayResponse) sibePayService.pay(sibePayRequest);
        }

        //保存日志文件
        LogFileUtil.saveLogFile(sibePayRequest.getUuid(),"payResponse",objectMapper,ctripPayResponse);

        //转字符并加密
        String strPayResponse= objectMapper.writeValueAsString(ctripPayResponse);
        String encryptResult = AESUtils.getInstance().jdk8encrypt(strPayResponse,sKey);
        return ResponseEntity.ok()
                .body(encryptResult) ;
    }





    @ApiOperation("加密")
    @RequestMapping(value = "/encrypt")
    public String encrypt(@RequestBody String orderRequest) throws Exception {
        String sKey =sibeProperties.getOta().getSkey();
//        CtripOrderRequest decodeOrderRequest = JSON.parseObject(orderRequest,CtripOrderRequest.class);
        String encryptResult = AESUtils.getInstance().jdk8encrypt(orderRequest,sKey);
        return encryptResult;
    }




}
