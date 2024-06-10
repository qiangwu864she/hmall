package com.hmall.gateway.filters;

import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LoginGolbalFilter implements GlobalFilter , Ordered {

    private final AuthProperties authProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtTool jwtTool;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取Request
        ServerHttpRequest request = exchange.getRequest();
        //2.判断当前请求是否需要被拦截
        if(isAllowPath(request)){
            //无需拦截，放行
            return chain.filter(exchange);
        }
        //3.获取Token
        String token = null;
        List<String> headers = request.getHeaders().get("authorization");
        if(headers != null){
            token = headers.get(0);
        }

        //4.解析token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
            System.out.println("userId = " + userId);
        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.valueOf(401));
            return response.setComplete();
        }

        //5.传递用户信息到下游服务--2023年11月11日
        String userInfo = userId.toString();
        ServerWebExchange webExchange = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();
        //6.放行
        return chain.filter(webExchange);
    }

    private boolean isAllowPath(ServerHttpRequest request) {
        boolean flag = false;
        //1.获取当前path
        String path = request.getPath().toString();
        //HttpMethod method = request.getMethod();
        //2.读取要放行的路径
        for (String excludePath : authProperties.getExcludePaths()) {
            boolean isMatch = pathMatcher.match(excludePath, path);
            if(isMatch){
                flag = true;
                break;
            }
        }
        return flag;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
