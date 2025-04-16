package com.fmsp.dataregister.config;

import com.fmsp.dataregister.entity.dto.UsuarioSesionDTO;
import com.fmsp.dataregister.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Component
@ControllerAdvice
public class UsuarioGlobalAdvice {

    private final UsuarioRepository usuarioRepository;

    public UsuarioGlobalAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute
    public void agregarUsuarioCompleto(Model model, HttpSession session) {
        Object sesion = session.getAttribute("usuarioLogueado");
        if (sesion instanceof UsuarioSesionDTO dto) {
            usuarioRepository.findById(dto.getId()).ifPresent(usuario -> model.addAttribute("usuario", usuario));
        }
    }
}
