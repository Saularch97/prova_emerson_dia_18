package crudmanager.controller;

import crudmanager.exception.NotFoundException;
import crudmanager.model.Page;
import crudmanager.service.Pagination;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;

public final class WebUtil {

    public static final String SESSION_USER = "loggedUser";
    public static final String MESSAGE_KEY = "message";
    public static final String ALERT_TYPE_KEY = "alertType";
    public static final int ALERT_ERROR = 0;
    public static final int ALERT_SUCCESS = 1;
    public static final String DB_ERROR_MESSAGE = "Não foi possível concluir a operação. Tente novamente mais tarde.";

    private WebUtil() {
    }

    public static void forward(HttpServletRequest req, HttpServletResponse resp, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher(jsp).forward(req, resp);
    }

    public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    public static void success(HttpServletRequest req, String message) {
        flash(req, message, ALERT_SUCCESS);
    }

    public static void error(HttpServletRequest req, String message) {
        flash(req, message, ALERT_ERROR);
    }

    private static void flash(HttpServletRequest req, String message, int type) {
        HttpSession session = req.getSession();
        session.setAttribute(MESSAGE_KEY, message);
        session.setAttribute(ALERT_TYPE_KEY, type);
    }

    public static void transferFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return;
        }
        Object message = session.getAttribute(MESSAGE_KEY);
        Object type = session.getAttribute(ALERT_TYPE_KEY);
        if (message != null) {
            req.setAttribute(MESSAGE_KEY, message);
            session.removeAttribute(MESSAGE_KEY);
        }
        if (type != null) {
            req.setAttribute(ALERT_TYPE_KEY, type);
            session.removeAttribute(ALERT_TYPE_KEY);
        }
    }

    public static int parsePage(HttpServletRequest req) {
        return Pagination.parse(req.getParameter("page"));
    }

    public static int parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new NotFoundException("Registro não encontrado.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new NotFoundException("Registro não encontrado.");
        }
    }

    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.trim());
    }

    public static String route(HttpServletRequest req) {
        return req.getServletPath();
    }

    public static void pageData(HttpServletRequest req, Page<?> page, String baseUrl) {
        req.setAttribute("pageData", page);
        req.setAttribute("pageBaseUrl", baseUrl);
    }
}
