package com.fmsp.dataregister.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class SesionFilter implements Filter {

    private static final Set<String> RUTAS_PUBLICAS = Set.of(
            "/", "/login", "/payment/info", "/payment/create", "/clientes/politica-datos", "/encriptar", "/descargar", "/downloads/app-release.apk", "/downloads"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        HttpSession session = req.getSession(false);
        boolean sesionActiva = (session != null && session.getAttribute("usuarioLogueado") != null);

        String uri = req.getRequestURI();
        boolean esRecursoEstatico = uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/");
        boolean esPaginaPublica = RUTAS_PUBLICAS.contains(uri);

        if (sesionActiva || esPaginaPublica || esRecursoEstatico) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            res.sendRedirect("/login");
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
