package com.bruce.saas.sql.mybatis;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Properties;


/**
 * mybatis拦截器，做sql拦截处理
 *
 * @author lvsheng
 * @date 2021/5/12 17:02
 * @Version 1.0
 */
@Slf4j
@Intercepts({
		@Signature(
				type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class
		})
})
public class SaasSqlInterceptor implements Interceptor {
	
	public static final String MERCHANT_ID_FIELD = "merchant_id";
	public static final String JOIN              = " join ";
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		//未捕获到租户id不做处理
		if (StringUtils.isBlank(MerchantIdContext.getMerchantId())) {
			return invocation.proceed();
		}
		
		StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
		MetaObject       metaObject       = MetaObject.forObject(statementHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
		MappedStatement  mappedStatement  = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
		
		if (!hasAnnotation(mappedStatement)) {
			//获取不到sql拦截的注解，直接放行
			return invocation.proceed();
		}
		
		//获取原始sql
		BoundSql boundSql = statementHandler.getBoundSql();
		String   newSql   = processSql(mappedStatement, boundSql);
		
		//基于反射修改sql语句
		Field field = boundSql.getClass().getDeclaredField("sql");
		field.setAccessible(true);
		field.set(boundSql, newSql);
		
		return invocation.proceed();
	}
	
	private String processSql(MappedStatement mappedStatement, BoundSql boundSql) {
		String sql = boundSql.getSql();
		log.info("to be intercepted sql : {}", sql);
		String newSql = sql;
		//sql类型 select、delete、insert、update
		SqlCommandType sqlType = mappedStatement.getSqlCommandType();
		//根据sql类型进行处理
		String merchantId = MerchantIdContext.getMerchantId();
		switch (sqlType) {
			case SELECT:
				newSql = processSelectSql(sql, merchantId);
				break;
			case INSERT:
				newSql = processInsertSql(sql, merchantId);
				break;
			
			// 删除或修改的处理方式一样
			case DELETE:
			case UPDATE:
				newSql = sql + " and " + MERCHANT_ID_FIELD + " = " + quotationField(merchantId);
				break;
			
			default:
				log.error(sqlType + " doesn't support!");
				break;
		}
		
		log.info("new sql : {}", newSql);
		return newSql;
	}
	
	private String processInsertSql(String sql, String merchantId) {
		StringBuilder sb = new StringBuilder();
		sb.append(sql.substring(0, sql.indexOf(")")))
		  .append(" ,")
		  .append(MERCHANT_ID_FIELD)
		  .append(")");
		
		String valuesClause = sql.substring(sql.indexOf(")") + 1);
		// 单行记录insert和多行记录insert都能写入
		String newValuesClause = StringUtils.replace(valuesClause, ")", "," + quotationField(merchantId) + ")");
		sb.append(newValuesClause);
		return sb.toString();
	}
	
	/**
	 * 查看类或方法上面有没有注解
	 *
	 * @param mappedStatement
	 * @return
	 * @throws ClassNotFoundException
	 */
	private boolean hasAnnotation(MappedStatement mappedStatement) throws ClassNotFoundException {
		String sqlId      = mappedStatement.getId();
		String className  = sqlId.substring(0, sqlId.lastIndexOf("."));
		String methodName = sqlId.substring(sqlId.lastIndexOf(".") + 1);
		
		// 如果有SelectKey那么它的SQLid是这样的 com.bruce.jr.invoice.front.dao.mapper.InvoiceOutInfoDetailsFrontDOMapper.save!selectKey
		if (methodName.contains("!")) {
			methodName = methodName.substring(0, methodName.indexOf('!'));
		}
		
		// 处理pageHelper导致的问题，如com.bruce.jr.invoice.mage.dao.mapper.InvoiceRedToConfirmMapper.listRedInvoiceToConfirm_COUNT
		if(methodName.contains("_")) {
			methodName = methodName.substring(0, methodName.indexOf('_'));
		}
		
		Class<?> classObj = Class.forName(className);
		
		// 类级别判断
		SaasSql classAnnotation = classObj.getAnnotation(SaasSql.class);
		if (classAnnotation != null) {
			return true;
		}
		Method[] declaredMethods = classObj.getDeclaredMethods();
		Method   method          = null;
		for (Method m : declaredMethods) {
			if (m.getName().equals(methodName)) {
				method = m;
				break;
			}
		}
		
		// 不排除一些框架对sqlId做了额数处理，找不到method这里直接放过
		if (method == null) {
			log.error("SaasSqlInterceptor: corresponding method can't be found, please contact lvsheng. sqlId: {}", sqlId);
			return false;
		}
		
		// 方法级别判断
		SaasSql saasSql = method.getAnnotation(SaasSql.class);
		if (saasSql != null) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}
	
	@Override
	public void setProperties(Properties properties) {
	
	}
	
	
	/**
	 * 获取修改后的select sql
	 *
	 * @param sql
	 * @param merchantId
	 * @return
	 */
	private String processSelectSql(String sql, String merchantId) {
		sql = sql.toLowerCase();
		
		int selectCount = StringUtils.countMatches(sql, "select");
		int whereCount  = StringUtils.countMatches(sql, "where");
		String newSql = sql;
		//单表的sql查询
		if (selectCount == 1 && !sql.contains(JOIN)) {
			if (selectCount == whereCount) {//最外层的sql有where
				newSql = sql.substring(0, sql.lastIndexOf("where") + 5) + "  " + MERCHANT_ID_FIELD + " = " + quotationField(merchantId) + " and " + sql.substring(sql.lastIndexOf("where") + 5);
			} else {//最外层的sql没有where 最外层的sql按照 group by 、 order by 、limit这种顺序写sql
				if (sql.contains("group by")) {
					newSql = sql.substring(0, sql.lastIndexOf("group by")) + " where " + MERCHANT_ID_FIELD + " = " + quotationField(merchantId) + " " + sql.substring(sql.lastIndexOf("group by"));
				} else if (!sql.contains("group by") && sql.contains("order by")) {
					newSql = sql.substring(0, sql.lastIndexOf("order by")) + " where " + MERCHANT_ID_FIELD + " = " + quotationField(merchantId) + " " + sql.substring(sql.lastIndexOf("order by"));
				} else if (!sql.contains("group by") && !sql.contains("order by") && sql.contains("limit")) {
					newSql = sql.substring(0, sql.lastIndexOf("limit")) + " where " + MERCHANT_ID_FIELD + " = " + quotationField(merchantId) + " " + sql.substring(sql.lastIndexOf("limit"));
				}
				
			}
		} else if (sql.contains(JOIN)) {
			//假如有子查询或者连表查询，原有sql需要做改造处理，查询字段必须查出merchant_id
			newSql = sql.substring(0, sql.indexOf("from")) + " from (" + sql + ")t where t." + MERCHANT_ID_FIELD + " = " + quotationField(merchantId);
		}
		
		return newSql;
	}
	
	/**
	 * 用双引号将merchantId括起来，避免数据库解析出错
	 *
	 * @param merchantId
	 * @return
	 */
	private String quotationField(String merchantId) {
		return new StringBuilder().append("\"")
								  .append(merchantId)
								  .append("\"").toString();
	}
}
