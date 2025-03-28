package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.repository.UsuarioRepository;
import com.fmsp.dataregister.service.IAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final IAuthService iAuthService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(IAuthService iAuthService, UsuarioRepository usuarioRepository) {
        this.iAuthService = iAuthService;
        this.usuarioRepository = usuarioRepository;
    }


    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        return iAuthService.mostrarLogin(session);
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String usuario,
                                @RequestParam String password,
                                Model model,
                                HttpSession session) {
       return iAuthService.procesarLogin(usuario, password, model, session);
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        return iAuthService.cerrarSesion(session);
    }

    @GetMapping("/")
    public String mostrarHome(HttpSession session, Model model) {
        return iAuthService.mostrarHome(session, model);
    }

    /**
     * Redirige al usuario según su rol.
     */
    private String redireccionarSegunRol(Usuario usuario) {
        return iAuthService.redireccionarSegunRol(usuario);
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:auth/login";
        }
        model.addAttribute("usuario", usuario);
        return "auth/perfil";
    }

    @GetMapping("/perfil/editar")
    public String editarPerfil(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";
        model.addAttribute("usuario", usuario);
        return "auth/editar-perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@ModelAttribute Usuario datos, HttpSession session, RedirectAttributes redirect) {
        Usuario actual = (Usuario) session.getAttribute("usuarioLogueado");
        if (actual == null) return "redirect:/login";

        // Validar si el correo ya está en uso por otro usuario
        Optional<Usuario> existente = usuarioRepository.findByEmail(datos.getEmail());
        if (existente.isPresent() && !existente.get().getId().equals(actual.getId())) {
            redirect.addFlashAttribute("errorCorreo", "Este correo ya está en uso por otro usuario.");
            return "redirect:/perfil/editar";
        }

        // Actualiza solo los campos permitidos
        actual.setNombre(datos.getNombre());
        actual.setApellido(datos.getApellido());
        actual.setEmail(datos.getEmail());

        usuarioRepository.save(actual);
        session.setAttribute("usuarioLogueado", actual);

        return "redirect:/perfil?success";
    }


}
