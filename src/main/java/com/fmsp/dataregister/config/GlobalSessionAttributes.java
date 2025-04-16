package com.fmsp.dataregister.config;

import com.fmsp.dataregister.entity.dto.UsuarioSesionDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalSessionAttributes {

    @ModelAttribute("usuario")
    public UsuarioSesionDTO usuarioSesion(HttpSession session) {
        return (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
    }
}
