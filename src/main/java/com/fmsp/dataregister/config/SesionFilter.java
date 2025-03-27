package com.fmsp.dataregister.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SesionFilter implements Filter {


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        // Obtener la sesión actual
        HttpSession session = req.getSession(false);
        boolean sesionActiva = (session != null && session.getAttribute("usuarioLogueado") != null);

        // Rutas permitidas sin autenticación
        String uri = req.getRequestURI();
        boolean esRecursoEstatico = uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/");
        boolean esPaginaPublica = uri.equals("/login") || uri.equals("/") || uri.startsWith("/public");

        // Permitir acceso sin autenticación a recursos públicos
        if (sesionActiva || esPaginaPublica || esRecursoEstatico) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            res.sendRedirect("/login"); // Redirigir al login si no hay sesión
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
