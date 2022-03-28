package com.itranswarp.anno;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.itranswarp.enums.InputType;

@Retention(RUNTIME)
@Target(FIELD)
public @interface SettingInput {

    InputType value();

    int order();

    String description() default "";

}
