package com.gnng.annotation;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface MyRequestParam {

    /**
     * 表示参数的别名，必填
     * @return
     */
    String value();
}
