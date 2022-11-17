package com.gn.api.web.ctrip;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gn.api.utils.SibeServiceUtil;
import com.gn.config.SibeProperties;
import com.gn.ota.ctrip.model.CtripOrderRequest;
import com.gn.ota.ctrip.model.CtripOrderResponse;
import com.gn.ota.ctrip.transform.TransformCtripOrderRequest;
import com.gn.ota.site.SibeOrderRequest;
import com.gn.repository.entity.OtaSite;
import com.gn.sibe.KProductService;
import com.gn.sibe.SibeOrderService;
import com.gn.sibe.SibeSearchCommService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Objects;


/**
 * The type Order resource.
 */
@RestController
@RequestMapping("/order")
@Api(description = "携程机票直连API生单")
public class OrderCtripResource {

    private Logger LOGGER = LoggerFactory.getLogger(OrderCtripResource.class);
    @Autowired
    private SibeProperties sibeProperties;
    @Autowired
    private SibeOrderService sibeOrderService;
    @Autowired
    private SibeServiceUtil sibeServiceUtil;
    @Autowired
    private TransformCtripOrderRequest transformOrderRequest;
    @Autowired
    private KProductService kProductService;
    @Autowired
    private SibeSearchCommService sibeSearchCommService;


    @ApiOperation("生单")
    @RequestMapping(value = "/{otaSiteCode}")
    public ResponseEntity<String> order(@RequestBody String orderRequest, @PathVariable String otaSiteCode) throws Exception {

        OtaSite otaSite = sibeSearchCommService.findSiteCodeByOta(otaSiteCode.toUpperCase());
        if (Objects.isNull(otaSite)) {
            LOGGER.error("没有找到对应的站点,{}", otaSiteCode);
            throw new CustomSibeException(SibeConstants.RESPONSE_MSG_114, "请求没有找到对应的站点", "00000", "search");
        }

        //1.解密orderRequest
        ObjectMapper objectMapper = new ObjectMapper();
        String sKey = sibeProperties.getOta().getSkey();
        String decodeOrderRequest = null;

        try {
            decodeOrderRequest = AESUtils.jdk8decrypt(orderRequest, sKey);
        } catch (Exception e) {
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1, "请求参数错误", "000001", sKey, "UnKnow");
        }
        if (StringUtils.isEmpty(decodeOrderRequest)) {
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1, "请求参数错误", "000002", sKey, "UnKnow");
        }

        //2.LOGGER.志打印
        // LOGGER.debug("***order请求参数：" + decodeOrderRequest);

        //3.String转为OtaOrderRequestVM
        CtripOrderRequest ctripOrderRequest = null;
        try {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ctripOrderRequest = objectMapper.readValue(decodeOrderRequest, CtripOrderRequest.class);
        } catch (IOException e) {
            LOGGER.error("请求无法解析成json格式 JsonMappingException", e);
            throw new CustomSibeEncryptException(SibeConstants.RESPONSE_STATUS_1, "请求无法解析成json格式 IOException", "00000", sKey, "UnKnow");
        }

        //3.OtaVerifyRequestVM转为OtaVerifyRequest对象
        SibeOrderRequest sibeOrderRequest = transformOrderRequest.toOrderRequest(ctripOrderRequest, sibeServiceUtil.getSibeOrderRequest(sibeProperties), otaSite);
        LOGGER.info("uuid:" + sibeOrderRequest.getUuid() + " order请求参数：" + decodeOrderRequest);
        LogFileUtil.saveLogFile(sibeOrderRequest.getUuid(), "orderRequest", objectMapper, ctripOrderRequest);

        CtripOrderResponse ctripOrderResponse = null;
        //判断是否为K位产品，是则调用K位verify，否则调用正常verify
        String productType = sibeOrderRequest.getRouting().getSibeRoutingData().getSibePolicy().getProductType();
        if ("2".equals(productType)) {
            boolean permitKProductSign = false;
            try {
                permitKProductSign = kProductService.getKSeatSign(sibeOrderRequest.getSite());
            } catch (Exception e) {
                LOGGER.error("uuid:" + sibeOrderRequest.getUuid() + " Verify K位 获取开关失败");
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭-开关获取失败", sibeOrderRequest.getUuid(), "verify");
            }

            if (permitKProductSign) {
                ctripOrderResponse = (CtripOrderResponse) kProductService.order(sibeOrderRequest);
            } else {
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭", sibeOrderRequest.getUuid(), "verify");
            }
        } else {
            //不是K位产品，调用正常verify
            ctripOrderResponse = (CtripOrderResponse) sibeOrderService.order(sibeOrderRequest);
        }

        LogFileUtil.saveLogFile(sibeOrderRequest.getUuid(), "orderResponse", objectMapper, ctripOrderResponse);

        //加密变成字符串
        String strOrderResponse = objectMapper.writeValueAsString(ctripOrderResponse);
        String encryptResult = AESUtils.getInstance().jdk8encrypt(strOrderResponse, sKey);

        Long s = (System.currentTimeMillis() - sibeOrderRequest.getStartTime()) / (1000);
        LOGGER.info("uuid:" + sibeOrderRequest.getUuid() + " order返回消耗:" + s + "秒");

        //返回密文
        return ResponseEntity.ok()
                .body(encryptResult);
    }


    @ApiOperation("加密")
    @RequestMapping(value = "/encrypt")
    public String encrypt(@RequestBody String orderRequest) throws Exception {
        String sKey = sibeProperties.getOta().getSkey();
//        CtripOrderRequest decodeOrderRequest = JSON.parseObject(orderRequest,CtripOrderRequest.class);
        String encryptResult = AESUtils.getInstance().jdk8encrypt(orderRequest, sKey);
        return encryptResult;
    }


}
