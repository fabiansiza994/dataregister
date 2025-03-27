package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.service.IAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final IAuthService iAuthService;

    public AuthController(IAuthService iAuthService) {
        this.iAuthService = iAuthService;
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

}
