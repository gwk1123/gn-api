package com.gn.api.web.book;

import com.baomidou.mybatisplus.core.toolkit.SystemClock;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gn.ota.book.model.search.BookSearchRequest;
import com.gn.ota.book.model.search.BookSearchResponse;
import com.gn.ota.book.transform.TransformBookSearchRequest;
import com.gn.ota.ctrip.model.CtripSearchResponse;
import com.gn.ota.site.SibeSearchRequest;
import com.gn.repository.entity.OtaSite;
import com.gn.sibe.SibeSearchCommService;
import com.gn.sibe.SibeSearchService;
import com.gn.utils.constant.SibeConstants;
import com.gn.utils.exception.CustomSibeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Objects;

@Controller
@RequestMapping(value = "/book")
public class SearchBookResource {

    private Logger logger = LoggerFactory.getLogger(SearchBookResource.class);
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransformBookSearchRequest transformBookSearchRequest;
    @Autowired
    private SibeSearchService sibeSearchService;
    @Autowired
    private SibeSearchCommService sibeSearchCommService;

    @ResponseBody
    @RequestMapping(value = "/search/{otaSiteCode}")
    public String searchCtrip(@RequestBody String request, @PathVariable String otaSiteCode) throws Exception {
        BookSearchRequest bookSearchRequest = null;

        OtaSite otaSite = sibeSearchCommService.findSiteCodeByOta(otaSiteCode.toUpperCase());
        if (Objects.isNull(otaSite)) {
            logger.error("没有找到对应的站点,{}", otaSiteCode);
            throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_114, "请求没有找到对应的站点", "00000", "search");
        }
        try {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            bookSearchRequest = objectMapper.readValue(request, BookSearchRequest.class);
        } catch (Exception e) {
            logger.error("请求无法解析成json格式 Exception", e);
            throw new CustomSibeException(SibeConstants.RESPONSE_STATUS_1, "请求无法解析成json格式 JsonParseException", "00000", "search");
        }
        SibeSearchRequest sibeSearchRequest = transformBookSearchRequest.toSearchRequest(bookSearchRequest, otaSite);
        logger.info("uuid:" + sibeSearchRequest.getUuid() + " search请求:"
                + "站点" + sibeSearchRequest.getSite()
                + " 渠道" + sibeSearchRequest.getChannel()
                + " " + sibeSearchRequest.getTripType()
                + "出发地" + sibeSearchRequest.getFromCity()
                + "-" + sibeSearchRequest.getToCity()
                + "出发时间" + sibeSearchRequest.getFromDate()
                + "-" + sibeSearchRequest.getRetDate());

        BookSearchResponse bookSearchResponse = (BookSearchResponse) sibeSearchService.search(sibeSearchRequest);

        Long s = (SystemClock.now() - sibeSearchRequest.getStartTime()) / (1000);
        logger.info("uuid:" + sibeSearchRequest.getUuid() + " search返回消耗:" + s + "秒");
        return objectMapper.writeValueAsString(bookSearchResponse);
    }

}
