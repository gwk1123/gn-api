package com.gn.api.util.constants;

public class SibeConstants {

    public static final String RESPONSE_STATUS_SUCCESS = "0";
    public static final String RESPONSE_STATUS_1 = "1";
    public static final String RESPONSE_STATUS_2 = "2";
    public static final String RESPONSE_STATUS_3 = "3";
    public static final String RESPONSE_STATUS_4 = "4";
    public static final String RESPONSE_STATUS_5 = "5";
    public static final String RESPONSE_STATUS_6 = "6";
    public static final String RESPONSE_STATUS_7 = "7";
    public static final String RESPONSE_STATUS_8 = "8";
    public static final String RESPONSE_STATUS_9 = "9";
    public static final String RESPONSE_STATUS_10 = "10";
    public static final String RESPONSE_STATUS_11 = "11";
    public static final String RESPONSE_STATUS_100 = "100";
    public static final String RESPONSE_STATUS_101 = "101";
    public static final String RESPONSE_STATUS_102 = "102";
    public static final String RESPONSE_STATUS_103 = "103";
    public static final String RESPONSE_STATUS_104 = "104";
    public static final String RESPONSE_STATUS_105 = "105";
    public static final String RESPONSE_STATUS_106 = "106";
    public static final String RESPONSE_STATUS_107 = "107";
    public static final String RESPONSE_STATUS_108 = "108";
    public static final String RESPONSE_STATUS_109 = "109";
    public static final String RESPONSE_STATUS_110 = "110";
    public static final String RESPONSE_STATUS_111 = "111";
    public static final String RESPONSE_STATUS_112 = "112";
    public static final String RESPONSE_STATUS_113 = "113";
    public static final String RESPONSE_STATUS_999 = "99";

    public static final String RESPONSE_MSG_SUCCESS = "成功";
    public static final String RESPONSE_MSG_1 = "请求参数错误";
    public static final String RESPONSE_MSG_2 = "程序异常";
    public static final String RESPONSE_MSG_3 = "舱位数不足，舱位已售完";
    public static final String RESPONSE_MSG_4 = "查无价格";
    public static final String RESPONSE_MSG_5 = "无法获取税费";
    public static final String RESPONSE_MSG_6 = "退改签异常";
    public static final String RESPONSE_MSG_7 = "网络异常";
    public static final String RESPONSE_MSG_8 = "价格变化";
    public static final String RESPONSE_MSG_9 = "order某航段预定失败并且cancel该PNR成功";
    public static final String RESPONSE_MSG_10 = "order某航段预定失败并且cancel该PNR失败";
    public static final String RESPONSE_MSG_11 = "GDS返回异常";
    public static final String RESPONSE_MSG_100 = "GDS查询无航班数据";
    public static final String RESPONSE_MSG_101 = "客户重复预订";
    public static final String RESPONSE_MSG_102 = "OTA-航线白名单限制";
    public static final String RESPONSE_MSG_103 = "GDS-航线黑名单限制";
    public static final String RESPONSE_MSG_104 = "OTA-限制多程,包括缺口程限制";
    public static final String RESPONSE_MSG_105 = "OTA-限制旅行日期范围限制";
    public static final String RESPONSE_MSG_106 = "出发地目的地不合法";
    public static final String RESPONSE_MSG_107 = "不销售单独儿童票";
    public static final String RESPONSE_MSG_108 = "order失败";
    public static final String RESPONSE_MSG_109 = "order后PNR retrieve异常";
    public static final String RESPONSE_MSG_110 = "order后PNR retrieve 发现PNR状态异常";
    public static final String RESPONSE_MSG_111 = "lcc order 生成信用卡失败";
    public static final String RESPONSE_MSG_112 = "lcc payOrder失败";
    public static final String RESPONSE_MSG_113 = "lcc 订单状态无法确认，等待人工确认";
    public static final String RESPONSE_MSG_999 = "其他失败原因";


    //匹配优先级定义
    public static final int MP_TRIP_TYPE = 1;  //行程类型
    public static final int MP_PRICE_TYPE = 2; //价格类型
    public static final int MP_SEAT_GRADE= 3; //适用舱等
    public static final int MP_SEAT_CABIN = 4; //适用舱位
    public static final int MP_FLIGHT_AVAILABLE= 5; //可售航班
    public static final int MP_FLIGHT_PROHIBITED= 6; //禁售航班
    public static final int MP_TRANSFER_TYPE = 7; //是否允许中转
    public static final int MP_TRANSFER_POINT= 8; //指定转机点
    public static final int MP_INTERLINE_TYPE = 9; //是否混合航司
    public static final int MP_CODE_SHARE_TYPE = 10; //是否代码共享
    public static final int MP_INTERLINE_AIRLINE = 11; //适用联运航司
    public static final int MP_INTERLINE_AIRLINE_EXCEPT = 12; //除外联运航司
    public static final int MP_OUTBOUND_DATE = 13;//去程旅行日期
    public static final int MP_INBOUND_DATE = 14; //回程旅行日期
    public static final int MP_OUTBOUND_DATE_EXCEPT = 15;//去程除外旅行日期
    public static final int MP_INBOUND_DATE_EXCEPT = 16; //回程除外旅行日期
    public static final int MP_OUTBOUND_DAY_TIME= 17; //去程班期
    public static final int MP_INBOUND_DAY_TIME= 18; //回程班期
    public static final int MP_SALE_DATE= 19; //销售日期
    public static final int MP_ADVANCE_SALE_DAY= 20; //提前销售天数
    public static final int MP_CODE_SHARE_AIRLINE = 21; //适用共享航司
    public static final int MP_CODE_SHARE_AIRLINE_EXCEPT = 22; //除外共享航司
    public static final int MP_SEAT_CABIN_EXCEPT = 23;//适用舱位除外
    public static final int MP_BOOK_GDS_CHANNEL = 24;//GDS类型
    public static final int MP_RET_MIN_AND_MAX_TIME = 25;//最小和最大停留时间（分）
    public static final String DEP_ARR_UNLIMITED = "UNLIMITED";//不限制


}
