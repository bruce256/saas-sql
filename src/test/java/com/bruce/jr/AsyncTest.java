package com.bruce.jr;

import com.bruce.saas.sql.mybatis.MerchantIdContext;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class AsyncTest {
	
	@Test
	public void async() {
		String merchantId = "jlasdjflkasd";
		MerchantIdContext.put(merchantId);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				System.out.println(MerchantIdContext.getMerchantId());
				Assert.assertTrue(StringUtils.equals(merchantId, MerchantIdContext.getMerchantId()));
			}
		}).start();
	}
}
