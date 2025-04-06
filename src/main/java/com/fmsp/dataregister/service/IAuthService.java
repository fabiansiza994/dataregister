package com.fmsp.dataregister.service;

import com.fmsp.dataregister.entity.Usuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public interface IAuthService {
    String mostrarLogin(HttpSession session);
    String procesarLogin(String usuario,
                         String password,
                         Model model,
                         HttpSession session);
    String cerrarSesion(HttpSession session);
    String mostrarHome(HttpSession session, Model model);
    String redireccionarSegunRol(Usuario usuario);

    String mostrarPerfil(HttpSession session, Model model);
    String editarPerfil(HttpSession session, Model model);
    String actualizarPerfil(@ModelAttribute Usuario datos, HttpSession session, RedirectAttributes redirect);

    Object obtenerTodosLosRoles();

    String registrarUsuario(Usuario usuario, RedirectAttributes redirect);
    String mostrarFormularioRegistro(Model model);
}
