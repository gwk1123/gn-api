package com.gn.api.web.ctrip;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gn.api.utils.SibeServiceUtil;
import com.gn.config.SibeProperties;
import com.gn.ota.ctrip.model.CtripVerifyRequest;
import com.gn.ota.ctrip.model.CtripVerifyResponse;
import com.gn.ota.ctrip.transform.TransformCtripVerifyRequest;
import com.gn.ota.site.SibeVerifyRequest;
import com.gn.repository.entity.OtaSite;
import com.gn.sibe.KProductService;
import com.gn.sibe.SibeSearchCommService;
import com.gn.sibe.SibeVerifyService;
import com.gn.utils.constant.SibeConstants;
import com.gn.utils.exception.CustomSibeException;
import com.gn.utils.logs.LogFileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@RequestMapping("/ctrip")
@Api(description = "携程机票直连API生单")
public class VerifyCtripResource {

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
    private SibeServiceUtil sibeServiceUtil;
    @Autowired
    private KProductService kProductService;
    @Autowired
    private SibeSearchCommService sibeSearchCommService;

    /**
     * Verify response entity.
     */
    @ApiOperation("验价")
    @RequestMapping(value = "/verify/{otaSiteCode}")
    public ResponseEntity<CtripVerifyResponse> verify(@RequestBody String verifyRequest, @PathVariable String otaSiteCode) {

        OtaSite otaSite = sibeSearchCommService.findSiteCodeByOta(otaSiteCode.toUpperCase());
        if (Objects.isNull(otaSite)) {
            LOGGER.error("没有找到对应的站点,{}", otaSiteCode);
            throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_114, "请求没有找到对应的站点", "00000", "search");
        }
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
        SibeVerifyRequest sibeVerifyRequest = (SibeVerifyRequest) transformCtripVerifyRequest.toVerifyRequest(ctripVerifyRequest, sibeServiceUtil.getSibeVerifyRequest(sibeProperties), otaSite);

        LOGGER.info("uuid:" + sibeVerifyRequest.getUuid() + " verify请求参数：" + verifyRequest);
        LogFileUtil.saveLogFile(sibeVerifyRequest.getUuid(), "verifyRequest", objectMapper, ctripVerifyRequest);

        CtripVerifyResponse ctripVerifyResponse = null;
        //判断是否为K位产品，是则调用K位verify，否则调用正常verify
        String productType = sibeVerifyRequest.getRouting().getSibeRoutingData().getSibePolicy().getProductType();
        if ("2".equals(productType)) {
            boolean permitKProductSign = false;
            try {
                permitKProductSign = kProductService.getKSeatSign(sibeVerifyRequest.getSite());
            } catch (Exception e) {
                LOGGER.error("uuid:" + sibeVerifyRequest.getUuid() + " Verify K位 获取开关失败");
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭-开关获取失败", sibeVerifyRequest.getUuid(), "verify");
            }

            if (permitKProductSign) {
                ctripVerifyResponse = (CtripVerifyResponse) kProductService.verify(sibeVerifyRequest);
            } else {
                throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_999,
                        "该产品已经关闭", sibeVerifyRequest.getUuid(), "verify");
            }
        } else {
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
}
