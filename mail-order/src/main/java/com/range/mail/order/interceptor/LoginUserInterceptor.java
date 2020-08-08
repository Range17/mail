package com.range.mail.order.interceptor;

import com.range.common.constant.AuthServerConstant;
import com.range.common.vo.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 进行登录拦截
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    /**
     * 本地变量
     */
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    /**
     * 获取登录用户
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        //请求匹配放行
        String requestURI = request.getRequestURI();
        AntPathMatcher matcher = new AntPathMatcher();
        boolean status = matcher.match("/order/order/status/**", requestURI);
        boolean payed = matcher.match("/payed/**", requestURI);
        if (status || payed) {
            return true;
        }

        MemberResponseVo attribute = (MemberResponseVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (!ObjectUtils.isEmpty(attribute)) {
            loginUser.set(attribute);
            return true;
        } else {
            request.getSession().setAttribute("msg", "请先进行登录");
            response.sendRedirect("http://auth.catmall.com/login.html");
            return false;
        }
    }
}
