package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Soporte;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.repository.SoporteRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/soporte")
public class SoporteController {

    private final SoporteRepository soporteRepository;

    public SoporteController(SoporteRepository soporteRepository) {
        this.soporteRepository = soporteRepository;
    }

    @GetMapping
    public String mostrarFormulario(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        if ("ADMIN".equals(usuario.getRol().getNombre())) {
            // Admin ve los mensajes de su empresa
            Long empresaId = usuario.getGrupo().getEmpresa().getId();
            List<Soporte> mensajes = soporteRepository.findByUsuario_Grupo_Empresa_Id(empresaId);
            model.addAttribute("mensajes", mensajes);
            return "soporte/admin";
        } else {
            // Usuario normal ve solo los suyos
            model.addAttribute("soporte", new Soporte());
            List<Soporte> mensajes = soporteRepository.findByUsuario_Id(usuario.getId());
            model.addAttribute("mensajes", mensajes);
            return "soporte/formulario";
        }
    }



    @PostMapping("/enviar")
    public String enviarFormulario(@ModelAttribute Soporte soporte, HttpSession session, RedirectAttributes redirect) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime ultima = (LocalDateTime) session.getAttribute("ultimaSolicitudSoporte");

        if (ultima != null && ultima.plusSeconds(30).isAfter(ahora)) {
            redirect.addFlashAttribute("error", "Por favor espera unos segundos antes de enviar otro mensaje.");
            return "redirect:/soporte";
        }

        soporte.setFecha(LocalDateTime.now());
        soporte.setUsuario(usuario);
        soporteRepository.save(soporte);

        session.setAttribute("ultimaSolicitudSoporte", ahora);
        redirect.addFlashAttribute("success", "Tu mensaje ha sido enviado. ¡Gracias por tu aporte!");
        return "redirect:/soporte";
    }

    @PostMapping("/eliminar")
    public String eliminarMensaje(Long id, HttpSession session, RedirectAttributes redirect) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        soporteRepository.findById(id).ifPresent(mensaje -> {
            if (mensaje.getUsuario().getId().equals(usuario.getId())) {
                soporteRepository.delete(mensaje);
                redirect.addFlashAttribute("success", "Mensaje eliminado correctamente.");
            } else {
                redirect.addFlashAttribute("error", "No estás autorizado para eliminar este mensaje.");
            }
        });

        return "redirect:/soporte";
    }

    @PostMapping("/actualizar-estado")
    public String actualizarEstado(@RequestParam Long id, @RequestParam String estado, RedirectAttributes redirect) {
        Soporte soporte = soporteRepository.findById(id).orElse(null);
        if (soporte != null) {
            soporte.setEstado(estado);
            soporteRepository.save(soporte);
            redirect.addFlashAttribute("success", "Estado actualizado.");
        }
        return "redirect:/soporte/admin";
    }

    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Long id, Model model, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        if (admin == null || !"ADMIN".equals(admin.getRol().getNombre())) {
            return "redirect:/login";
        }

        Soporte soporte = soporteRepository.findById(id).orElse(null);
        if (soporte == null || !soporte.getUsuario().getGrupo().getEmpresa().getId().equals(admin.getGrupo().getEmpresa().getId())) {
            return "redirect:/soporte/admin?error=Mensaje no encontrado o no autorizado.";
        }

        model.addAttribute("soporte", soporte);
        return "soporte/detalle";
    }

    @GetMapping("/admin")
    public String verMensajesAdmin(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String usuario,
            Model model) {

        if ("__null__".equals(estado)) estado = null;

        List<Soporte> mensajes;
        if ((estado != null && !estado.isEmpty()) || (usuario != null && !usuario.isEmpty())) {
            mensajes = soporteRepository.findByFiltros(
                    estado != null && !estado.isEmpty() ? estado : null,
                    usuario != null && !usuario.isEmpty() ? usuario : null
            );
        } else {
            mensajes = soporteRepository.findAll();
        }

        model.addAttribute("mensajes", mensajes);
        model.addAttribute("estado", estado);
        model.addAttribute("usuario", usuario);
        return "soporte/admin";
    }


}
