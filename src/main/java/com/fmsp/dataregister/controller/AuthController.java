package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Rol;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.entity.dto.RegistroDTO;
import com.fmsp.dataregister.repository.RolRepository;
import com.fmsp.dataregister.service.IAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.util.List;

@Controller
public class AuthController {

    private final IAuthService iAuthService;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public AuthController(IAuthService iAuthService, PasswordEncoder passwordEncoder, RolRepository rolRepository) {
        this.iAuthService = iAuthService;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
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
        return iAuthService.mostrarPerfil(session, model);
    }

    @GetMapping("/perfil/editar")
    public String editarPerfil(HttpSession session, Model model) {
        return iAuthService.editarPerfil(session, model);
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@ModelAttribute Usuario datos, HttpSession session, RedirectAttributes redirect) {
        return iAuthService.actualizarPerfil(datos, session, redirect);
    }

    @GetMapping("/payment/info")
    public String mostrarInfoPublicaPago(Model model) {
        model.addAttribute("precio", 10);
        model.addAttribute("moneda", "USD");
        model.addAttribute("beneficios", List.of(
                "Registro y gestión de trabajos",
                "Control de clientes por grupo",
                "Reportes y estadísticas visuales",
                "Acceso multiusuario por empresa"
        ));

        return "payments/info";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(HttpSession session, Model model) {
       return iAuthService.mostrarFormularioRegistro(session, model);
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute RegistroDTO usuario, RedirectAttributes redirect) {
        return iAuthService.registrarUsuario(usuario, redirect);
    }

    @GetMapping("/encriptar")
    public String mostrarCodificador() {
        return "auth/encriptar-password";
    }

    @PostMapping("/encriptar")
    public String codificarPassword(@RequestParam String password, Model model) {
        String encoded = passwordEncoder.encode(password);
        model.addAttribute("encodedPassword", encoded);
        return "auth/encriptar-password";
    }

    @GetMapping("/auth/usuarios-bloqueados")
    public String listarUsuariosBloqueados(HttpSession session, Model model) {
        return iAuthService.listarUsuariosBloqueados(session, model);
    }

    @PostMapping("/auth/desbloquear/{id}")
    public String desbloquearUsuario(@PathVariable Long id, RedirectAttributes redirect) {
       return iAuthService.desbloquearUsuario(id, redirect);
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(HttpSession session, Model model) {
        return iAuthService.listarUsuarios(session, model);
    }

    @GetMapping("/usuarios/buscar")
    public String buscarUsuarios(@RequestParam("nombre") String filtro, HttpSession session, Model model) {
        return iAuthService.buscarUsuarios(filtro, session, model);
    }

    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEdicion(@PathVariable Long id, HttpSession session, Model model) {
        return iAuthService.mostrarFormularioEdicion(id, session, model);
    }

    @PostMapping("/usuarios/actualizar")
    public String actualizarUsuario(@ModelAttribute RegistroDTO usuario, RedirectAttributes redirect, HttpSession session) {
        return iAuthService.actualizarUsuario(usuario, redirect, session);
    }

    @GetMapping("/usuarios/resetear-password/{id}")
    public String mostrarResetPassword(@PathVariable Long id, Model model, HttpSession session) {
        return iAuthService.mostrarResetPassword(id, model, session);
    }

    @PostMapping("/usuarios/resetear-password")
    public String resetearPassword(@RequestParam Long id,
                                   @RequestParam String nuevaPassword,
                                   @RequestParam String confirmarPassword,
                                   RedirectAttributes redirect) {
        return iAuthService.resetearPassword(id, nuevaPassword, confirmarPassword, redirect);
    }

}
