package com.bruce.saas.sql.mybatis;

import java.lang.annotation.*;

/**
 * 针对方法级别或类级别进行拦截
 *
 * @author lvsheng
 * @date 2021/5/17
 * @Version 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SaasSql {

}
