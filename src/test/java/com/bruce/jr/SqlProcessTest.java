package com.bruce.jr;

import com.bruce.saas.sql.mybatis.SaasSqlInterceptor;
import org.junit.Test;

/**
 * @author LvSheng
 * @date 2023/10/14
 **/
public class SqlProcessTest {
	
	@Test
	public void simpleSql() {
		SaasSqlInterceptor saasSqlInterceptor = new SaasSqlInterceptor();
		String             sql                = saasSqlInterceptor.processSelectSql("select a, b, c from t where a = '1' and b = 3", "4000");
		System.out.println(sql);
	}
	
	@Test
	public void noWhere() {
		SaasSqlInterceptor saasSqlInterceptor = new SaasSqlInterceptor();
		String             sql                = saasSqlInterceptor.processSelectSql("select a, b, c from t ", "4000");
		System.out.println(sql);
	}
	
	@Test
	public void star() {
		SaasSqlInterceptor saasSqlInterceptor = new SaasSqlInterceptor();
		String             sql                = saasSqlInterceptor.processSelectSql("select * from t ", "4000");
		System.out.println(sql);
	}
	
	@Test
	public void groupBy() {
		SaasSqlInterceptor saasSqlInterceptor = new SaasSqlInterceptor();
		String             sql                = saasSqlInterceptor.processSelectSql("select a, b, c from t where a = '1' and b = 3 group by c", "4000");
		System.out.println(sql);
	}
	
	@Test
	public void orderBy() {
		SaasSqlInterceptor saasSqlInterceptor = new SaasSqlInterceptor();
		String             sql                = saasSqlInterceptor.processSelectSql("select a, b, c from t where a = '1' and b = 3 order by c", "4000");
		System.out.println(sql);
	}
	
}
