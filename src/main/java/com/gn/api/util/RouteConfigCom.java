package com.gn.api.util;//package com.site.api.util;
//
//public class RouteConfigCom {
//
//    public void findRouteConfig(){
//
//
//        String formCity= search.getFromCity();
//        String toCity  = search.getToCity();
//		List<String> arrays = getDepartureOrder(formCity, toCity);
//		// 获取所有的pcc
//		List<BasicGdsPcc> gsdPccList = basicGdsPccCacheService.getHashMap(DirectConstant.GDS_PCC_KEY);
//		Map<String, RoutePcc> routePccMap = new HashMap<>();
//		for (int i = 0; i < arrays.size(); i++) {
//
//			String[] trem = arrays.get(i).split("/");
//			Optional<BasicRouteConfig> routeConfig = findRouteConfigCache().stream().filter(Objects::nonNull)
//					.filter(basicRouteConfig -> (bothWaysFlag(basicRouteConfig, trem))).findFirst();
//			if (routeConfig.isPresent()) {
//				// return routeConfig.get();
//				String[] searchOffices = StringUtils.split(routeConfig.get().getSearchOfficeNo(), "/");
//				String[] orderOffices = StringUtils.split(routeConfig.get().getOrderOfficeNo(), "/");
//				List<String> srearchPcc = Arrays.asList(searchOffices);
//				List<String> orderPcc = Arrays.asList(orderOffices);
//				for (int j = 0; j < srearchPcc.size(); j++) {
//					String[] searchOffisc = srearchPcc.get(j).split("-");
//					String[] orderOffice = orderPcc.get(j).split("-");
//					RoutePcc routePcc = new RoutePcc();
//					BasicGdsPcc srearchOff = null;
//					BasicGdsPcc ordreOff = null;
//					for (BasicGdsPcc pcc : gsdPccList) {
//						if (pcc != null) {
//							// 获取查询pcc
//							if (pcc.getPccCode().equals(searchOffisc[1])) {
//								srearchOff = pcc;
//							}
//							// 获取生单pcc
//							if (pcc.getPccCode().equals(orderOffice[1])) {
//								ordreOff = pcc;
//							}
//							if (srearchOff != null && ordreOff != null) {
//								GdsPcc srearchGdsPcc=new GdsPcc();
//								GdsPcc orderGdsPcc=new GdsPcc();
//								BeanUtils.copyProperties(srearchOff,srearchGdsPcc);
//								BeanUtils.copyProperties(ordreOff,orderGdsPcc);
//								routePcc.setSearchPcc(srearchGdsPcc);
//								routePcc.setOrderPcc(orderGdsPcc);
//								routePccMap.put(srearchPcc.get(j), routePcc);
//								break;
//							}
//						}
//					}
//				}
//				break;
//			}
//
//		}
//
//        if(routePccMap == null || routePccMap.entrySet().size()==0) {
//            throw new CustomException(ROUTE_CONFIG_ERROR,"出发地:"+search.getFromCity()+",目的地:"+search.getToCity());
//        }
//        search.setRoutePccMap(routePccMap);
//    }
//
//
//    /**
//     * 根据出发地进行排序
//     * @param formCity
//     * @param toCity
//     * @return
//     */
//    public  List<String> getDepartureOrder(String formCity,String toCity){
//        List<String> regionalList=new ArrayList<String>();
//        Airport formAirport = allAirportsService.getByCCode(formCity);
//        Airport toAirport = allAirportsService.getByCCode(toCity);
//        if(formAirport == null) {
//            formAirport= allAirportsService.getByCode(formCity);
//        }
//        if(toAirport == null) {
//            toAirport = allAirportsService.getByCode(toCity);
//        }
//        Assert.isTrue(formAirport != null && toAirport != null,"出发地城市、目的地城市为空");
//
//        regionalList.add(formAirport.getCcode()+"/"+toAirport.getCcode());//城市与城市
//        regionalList.add(formAirport.getCcode()+"/"+toAirport.getGcode());//城市与国家
//        regionalList.add(formAirport.getCcode()+"/"+toAirport.getQcode());//城市与区域
//        regionalList.add(formAirport.getCcode()+"/"+DirectConstant.ADDRESS_UNLIMITED);//城市与不限
//
//        regionalList.add(formAirport.getGcode()+"/"+toAirport.getCcode());//国家与城市
//        regionalList.add(formAirport.getGcode()+"/"+toAirport.getGcode());//国家与国家
//        regionalList.add(formAirport.getGcode()+"/"+toAirport.getQcode());//国家与区域
//        regionalList.add(formAirport.getGcode()+"/"+DirectConstant.ADDRESS_UNLIMITED);//国家与不限
//
//        regionalList.add(formAirport.getQcode()+"/"+toAirport.getCcode());//区域与城市
//        regionalList.add(formAirport.getQcode()+"/"+toAirport.getGcode());//区域与国家
//        regionalList.add(formAirport.getQcode()+"/"+toAirport.getQcode());//区域与区域
//        regionalList.add(formAirport.getQcode()+"/"+DirectConstant.ADDRESS_UNLIMITED);//区域与不限
//
//        regionalList.add(DirectConstant.ADDRESS_UNLIMITED+"/"+toAirport.getCcode());//不限与城市
//        regionalList.add(DirectConstant.ADDRESS_UNLIMITED+"/"+toAirport.getGcode());//不限与国家
//        regionalList.add(DirectConstant.ADDRESS_UNLIMITED+"/"+toAirport.getQcode());//不限与区域
//        regionalList.add(DirectConstant.ADDRESS_UNLIMITED+"/"+DirectConstant.ADDRESS_UNLIMITED);//不限与不限
//        return regionalList;
//    }
//
//
//    /**
//     * 根据航线路由获取GDS缓存，删除不在航线路由中的GDS缓存，以及重新刷新航线路由中存在而GDS缓存中不存在的pcc配置
//     * @param searchVo
//     * @param gdsKey
//     * @return
//     */
//    public List<GdsRouteVo> getGdsCacheByRouteConfig(GdsSearchRequestVo searchVo,String gdsKey,NumberUnit numberUnit){
//        Set<String> gdsKeys= (Set<String>) cacheService.findAllByKey(gdsKey);
//        if(CollectionUtils.isEmpty(gdsKeys)) {
//            return null;
//        }
//        List<SearchGdsResponse> gdsCacheList=new ArrayList<>();
//        //获取刷新的路由
//        Map<String,RoutePcc> refreshPccMap =new HashMap<>();
//        searchVo.getRoutePccMap().keySet().stream().filter(Objects::nonNull).forEach(e ->{
//            if(!gdsKeys.contains(e)) {
//                refreshPccMap.put(e,searchVo.getRoutePccMap().get(e));
//            }
//        });
//        logger.info("GDS缓存key{}",JSON.toJSONString(gdsKeys));
//        logger.info("刷新路由refreshPccMap{}",JSON.toJSONString(refreshPccMap));
//        searchVo.setRefreshPccMap(refreshPccMap);
//        for(String key:gdsKeys) {
//            if(searchVo.getRoutePccMap().get(key) == null) {
//                //删除GDS缓存
//                gdsAsyncService.deleteGdsCache(gdsKey,key);
//                logger.info("GDS删除key{}",key);
//            }else {
//                try {
//                    SearchGdsResponse searchGdsResponse=(SearchGdsResponse) cacheService.getHash(gdsKey, key);
//                    gdsCacheList.add(searchGdsResponse);
//                    logger.info("GDS数据key{}",key);
//                    logger.info("GDS数据长度{}",searchGdsResponse.getGdsRouteVo().size());
//                } catch (CustomException e) {
//                    logger.error("获取缓存异常:{}",e);
//                    return null;
//                }
//            }
//        }
//        //根据refreshPccMap是否有值发起异步请求
//        if(searchVo.getRefreshPccMap() != null && searchVo.getRefreshPccMap().size() >0) {
//            gdsAsyncService.requestGDSAsync(searchVo, numberUnit);
//            logger.info("异步请求{}",JSON.toJSONString(searchVo));
//        }
//        gdsCacheList=gdsCacheList.stream().filter(Objects::nonNull).collect(Collectors.toList());
//        return mergeResultGds(gdsCacheList);
//    }
//
//
//}
