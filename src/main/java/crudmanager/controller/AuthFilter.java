package crudmanager.controller;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/** Exige sessão autenticada em todas as rotas, exceto login e recursos estáticos. */
public class AuthFilter implements Filter {

    private static final String LOGIN_PATH = "/login";
    private static final List<String> PUBLIC_PREFIXES = List.of("/css/", "/js/", "/fonts/", "/images/");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (isPublic(WebUtil.route(req)) || isAuthenticated(req)) {
            chain.doFilter(request, response);
            return;
        }
        WebUtil.redirect(req, resp, LOGIN_PATH);
    }

    private static boolean isPublic(String path) {
        if (path == null) {
            return false;
        }
        return LOGIN_PATH.equals(path) || PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private static boolean isAuthenticated(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute(WebUtil.SESSION_USER) != null;
    }
}
