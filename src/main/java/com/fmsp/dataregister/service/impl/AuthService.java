package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.repository.UsuarioRepository;
import com.fmsp.dataregister.service.IAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.Optional;

@Service
public class AuthService implements IAuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public String mostrarLogin(HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado != null) {
            return redireccionarSegunRol(usuarioLogueado);
        }
        return "auth/login";
    }

    @Override
    public String procesarLogin(String usuario, String password, Model model, HttpSession session) {
        Optional<Usuario> user = usuarioRepository.findByUsuario(usuario);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            session.setAttribute("usuarioLogueado", user.get());

            if (user.get().getGrupo() == null) {
                return "redirect:/grupos/nuevo?msg=Debe crear o unirse a un grupo";
            }

            if (user.get().getGrupo().getEmpresa() == null) {
                return "redirect:/empresas/nueva?msg=Debe crear una empresa antes de continuar";
            }

            // Verificar el estado de la empresa
            if ("PENDIENTE".equals(user.get().getGrupo().getEmpresa().getEstado())) {
                model.addAttribute("alertaPago", "Por favor, realice el pago de la suscripción.");
            }

            return "home"; // Se redirige a la vista `home.html`
        } else {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }
    }


    @Override
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:auth/login";
    }

    @Override
    public String mostrarHome(HttpSession session, Model model) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return "redirect:auth/login";
        }

        Empresa empresa = usuarioLogueado.getGrupo().getEmpresa();

        if ("PENDIENTE".equals(empresa.getEstado())) {
            model.addAttribute("alertaPago", "Por favor, realice el pago de la suscripción.");
        }

        if ("INACTIVO".equals(empresa.getEstado())) {
            return "payments/index";
        }
        return redireccionarSegunRol(usuarioLogueado);
    }

    @Override
    public String redireccionarSegunRol(Usuario usuario) {
        if ("ADMIN".equals(usuario.getRol().getNombre())) {
            return "home";
        } else {
            return "home";
        }
    }
}
