package com.bruce.saas.sql.mybatis;


/**
 * 存储当前线程里的merchantId信息，异步线程不用手工写入
 *
 * @author lvsheng
 * @date 2021/12/15
 **/
public class MerchantIdContext {
	
	private static InheritableThreadLocal<String> merchantThreadLocal = new InheritableThreadLocal<>();
	
	public static void put(String merchantId) {
		merchantThreadLocal.set(merchantId);
	}
	
	public static String get() {
		return merchantThreadLocal.get();
	}
	
	public static String getMerchantId() {
		return merchantThreadLocal.get();
		
	}
	
	/**
	 * 线程终止前要调用这个方法，防止内存泄漏
	 */
	public static void remove() {
		merchantThreadLocal.remove();
	}
}
