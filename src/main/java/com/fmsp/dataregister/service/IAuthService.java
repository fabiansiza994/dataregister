package com.fmsp.dataregister.service;

import com.fmsp.dataregister.entity.Usuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

public interface IAuthService {
    String mostrarLogin(HttpSession session);
    String procesarLogin(String usuario,
                         String password,
                         Model model,
                         HttpSession session);
    String cerrarSesion(HttpSession session);
    String mostrarHome(HttpSession session, Model model);
    String redireccionarSegunRol(Usuario usuario);
}
