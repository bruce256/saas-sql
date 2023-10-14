package com.bruce.saas.sql.utils;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * @author lvsheng
 * @date 2022/3/10
 **/
public class LinkIdUtils {
	
	public static String LINK_ID = "link_id";
	
	/**
	 * 生成一个新的link_id
	 *
	 * @return
	 */
	public static String generate() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	/**
	 * 获取当前线程的link_id，如果没有，则新建
	 *
	 * @return
	 */
	public static String getLinkIdOfCurrentThread() {
		String linkId = MDC.get(LINK_ID);
		if (org.apache.commons.lang3.StringUtils.isBlank(linkId)) {
			linkId = LinkIdUtils.generate();
			MDC.put(LINK_ID, linkId);
		}
		return linkId;
	}
}
