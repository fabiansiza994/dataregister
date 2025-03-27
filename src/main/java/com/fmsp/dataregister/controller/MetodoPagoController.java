package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.FormaPago;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.repository.FormaPagoRepository;
import com.fmsp.dataregister.repository.TrabajoRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/metodos-pago")
public class MetodoPagoController {

    private final FormaPagoRepository formaPagoRepository;
    private final TrabajoRepository trabajoRepository;

    public MetodoPagoController(FormaPagoRepository formaPagoRepository, TrabajoRepository trabajoRepository) {
        this.formaPagoRepository = formaPagoRepository;
        this.trabajoRepository = trabajoRepository;
    }

    @GetMapping
    public String listarMetodosPago(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getRol().getNombre().equals("USER")) {
            return "redirect:auth/login";
        }

        Empresa empresaActual = usuario.getGrupo().getEmpresa();
        List<FormaPago> metodosPago = formaPagoRepository.findByEmpresaAndEstado(empresaActual, 1);

        model.addAttribute("metodosPago", metodosPago);
        return "metodos_pago/lista_metodos_pago";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("formaPago", new FormaPago());
        return "metodos_pago/registro_metodo_pago";
    }

    @PostMapping("/guardar")
    public String guardarMetodoPago(@RequestParam("formaPago") String formaPagoNombre, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getRol().getNombre().equals("USER")) {
            return "redirect:auth/login";
        }

        Empresa empresaActual = usuario.getGrupo().getEmpresa();
        if (empresaActual == null) {
            return "redirect:/metodos-pago?error=No tiene una empresa asociada";
        }

        FormaPago formaPago = new FormaPago();
        formaPago.setEstado(1);
        formaPago.setFormaPago(formaPagoNombre);
        formaPago.setEmpresa(empresaActual);  // Asignar la empresa

        formaPagoRepository.save(formaPago);
        return "redirect:/metodos-pago?success";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarMetodoPago(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (trabajoRepository.existsByFormaPagoId(id)) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el método de pago porque está asociado a trabajos.");
            return "redirect:/metodos-pago";
        }

        formaPagoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Método de pago eliminado correctamente.");
        return "redirect:/metodos-pago";
    }

    @Transactional
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstadoMetodoPago(@PathVariable Long id) {
        FormaPago formaPago = formaPagoRepository.findById(id).orElse(null);
        if (formaPago != null) {
            formaPago.setEstado(formaPago.getEstado() == 1 ? 0 : 1);
            formaPagoRepository.save(formaPago);
        }
        return "redirect:/metodos-pago?updated";
    }

}
