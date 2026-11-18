package crudmanager.controller;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CharacterEncodingFilterTest {

    @Test
    @DisplayName("EDGE-3 define UTF-8 na request e na response antes de seguir a cadeia")
    void setsUtf8() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new CharacterEncodingFilter().doFilter(req, resp, chain);

        InOrder order = inOrder(req, resp, chain);
        order.verify(req).setCharacterEncoding("UTF-8");
        order.verify(resp).setCharacterEncoding("UTF-8");
        order.verify(chain).doFilter(req, resp);
    }
}
