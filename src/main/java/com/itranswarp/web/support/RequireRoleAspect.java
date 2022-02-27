package com.itranswarp.web.support;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.itranswarp.web.filter.HttpContext;

@Aspect
@Component
public class RequireRoleAspect {

    @Around("@annotation(roleWith)")
    public Object checkSignIn(ProceedingJoinPoint joinPoint, RoleWith roleWith) throws Throwable {
        HttpContext.checkRole(roleWith.value());
        return joinPoint.proceed();
    }
}
