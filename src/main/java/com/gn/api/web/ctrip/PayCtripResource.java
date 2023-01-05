package com.gn.api.web.ctrip;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gn.api.utils.SibeServiceUtil;
import com.gn.config.SibeProperties;
import com.gn.ota.ctrip.model.CtripPayRequest;
import com.gn.ota.ctrip.model.CtripPayResponse;
import com.gn.ota.ctrip.transform.TransformCtripPayRequest;
import com.gn.ota.site.SibePayRequest;
import com.gn.repository.entity.OtaSite;
import com.gn.service.transform.TransformByOta;
import com.gn.sibe.KProductService;
import com.gn.sibe.SibePayService;
import com.gn.sibe.SibeSearchCommService;
import com.gn.utils.aes.AESUtils;
import com.gn.utils.constant.SibeConstants;
import com.gn.utils.exception.CustomSibeEncryptException;
import com.gn.utils.exception.CustomSibeException;
import com.gn.utils.logs.LogFileUtil;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/ctrip")
public class PayCtripResource {

    private Logger LOGGER = LoggerFactory.getLogger(OrderCtripResource.class);
    @Autowired
    private SibeProperties sibeProperties;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SibeServiceUtil sibeServiceUtil;
    @Autowired
    private TransformCtripPayRequest transformCtripPayRequest;
    @Autowired
    private KProductService kProductService;
    @Autowired
    private SibePayService sibePayService;
    @Autowired
    private SibeSearchCommService sibeSearchCommService;


    @ApiOperation("付款校验")
    @RequestMapping(value = "/pay/{otaSiteCode}",
            method = RequestMethod.POST,
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> payValidate(@RequestBody String payRequest, @PathVariable String otaSiteCode) throws Exception {

        OtaSite otaSite = sibeSearchCommService.findSiteCodeByOta(otaSiteCode.toUpperCase());
        if (Objects.isNull(otaSite)) {
            LOGGER.error("没有找到对应的站点,{}", otaSiteCode);
            throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_114, "请求没有找到对应的站点", "00000", "search");
        }

        String sKey = TransformByOta.getSkey(otaSite.getOtaCode(),sibeProperties);
        String decodePayRequest = null;
        try {
            decodePayRequest = AESUtils.jdk8decrypt(payRequest, sKey);
        } catch (Exception e) {
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1, "请求参数错误", "000001", sKey, "UnKnow");
        }

        if (StringUtils.isEmpty(decodePayRequest)) {
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1, "请求参数错误", "000002", sKey, "UnKnow");
        }

        //3.String转为OtaOrderRequestVM
        CtripPayRequest ctripPayRequest = null;
        try {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ctripPayRequest = objectMapper.readValue(decodePayRequest, CtripPayRequest.class);
        } catch (JsonParseException e) {
            LOGGER.error("请求无法解析成json格式 JsonMappingException", e);
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1, "请求无法解析成json格式 JsonParseException", "00000", sKey, "UnKnow");
        } catch (JsonMappingException e) {
            LOGGER.error("请求无法解析成json格式 JsonMappingException", e);
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1, "请求无法解析成json格式 JsonMappingException", "00000", sKey, "UnKnow");
        } catch (IOException e) {
            LOGGER.error("请求无法解析成json格式 JsonMappingException", e);
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1, "请求无法解析成json格式 IOException", "00000", sKey, "UnKnow");
        }

        //Transform
        SibePayRequest sibePayRequest = (SibePayRequest) transformCtripPayRequest.toPayRequest(ctripPayRequest, sibeServiceUtil.getSibePayRequest(sibeProperties), otaSite);
        LOGGER.info("uuid:" + sibePayRequest.getUuid() + " pay请求参数：" + decodePayRequest);
        //保存日志文件
        LogFileUtil.saveLogFile(sibePayRequest.getUuid(), "payRequest", objectMapper, ctripPayRequest);
        CtripPayResponse ctripPayResponse = null;
        String productType = sibePayRequest.getRouting().getSibeRoutingData().getSibePolicy().getProductType();
        if ("2".equals(productType)) {
            boolean permitKProductSign = false;
            try {
                permitKProductSign = kProductService.getKSeatSign(sibePayRequest.getSite());
            } catch (Exception e) {
                LOGGER.error("uuid:" + sibePayRequest.getUuid() + " pay K位 获取开关失败");
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭-开关获取失败", sibePayRequest.getUuid(), "pay");
            }
            if (permitKProductSign) {
                ctripPayResponse = (CtripPayResponse) kProductService.pay(sibePayRequest);
            } else {
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999, "该产品已经关闭", sibePayRequest.getUuid(), "pay");
            }
        } else {
            //发起pay核心请求
            ctripPayResponse = (CtripPayResponse) sibePayService.pay(sibePayRequest);
        }
        //保存日志文件
        LogFileUtil.saveLogFile(sibePayRequest.getUuid(), "payResponse", objectMapper, ctripPayResponse);
        //转字符并加密
        String strPayResponse = objectMapper.writeValueAsString(ctripPayResponse);
        String encryptResult = AESUtils.getInstance().jdk8encrypt(strPayResponse, sKey);
        return ResponseEntity.ok().body(encryptResult);
    }


}
