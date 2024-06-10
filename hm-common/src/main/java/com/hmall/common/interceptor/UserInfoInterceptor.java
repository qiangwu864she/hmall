package com.hmall.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//这个用户拦截器通过MvcConfig配置实现，其功能与网关拦截器不同，这里的功能是将从网关拦截器LoginGolbalFilter中获取的数据保存到context中，
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求头中的用户
        String userInfo = request.getHeader("user-info");

        //2.判断是否为空，不为空，存入ThreadLocal，即context上下文
        if(StrUtil.isNotBlank(userInfo)){
            UserContext.setUser(Long.valueOf(userInfo));
        }
        //3.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清除用户
        UserContext.removeUser();
    }
}
