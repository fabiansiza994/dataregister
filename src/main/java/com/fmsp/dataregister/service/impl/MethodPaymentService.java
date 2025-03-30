package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.FormaPago;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.repository.FormaPagoRepository;
import com.fmsp.dataregister.repository.TrabajoRepository;
import com.fmsp.dataregister.service.IMethodPaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Service
public class MethodPaymentService implements IMethodPaymentService {

    private final FormaPagoRepository formaPagoRepository;
    private final TrabajoRepository trabajoRepository;

    public MethodPaymentService(FormaPagoRepository formaPagoRepository, TrabajoRepository trabajoRepository) {
        this.formaPagoRepository = formaPagoRepository;
        this.trabajoRepository = trabajoRepository;
    }

    @Override
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

    @Override
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("formaPago", new FormaPago());
        return "metodos_pago/registro_metodo_pago";
    }

    @Override
    public String guardarMetodoPago(String formaPagoNombre, HttpSession session) {
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

    @Override
    public String eliminarMetodoPago(Long id, RedirectAttributes redirectAttributes) {
        if (trabajoRepository.existsByFormaPagoId(id)) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el método de pago porque está asociado a trabajos.");
            return "redirect:/metodos-pago";
        }

        formaPagoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Método de pago eliminado correctamente.");
        return "redirect:/metodos-pago";
    }

    @Override
    public String cambiarEstadoMetodoPago(Long id) {
        FormaPago formaPago = formaPagoRepository.findById(id).orElse(null);
        if (formaPago != null) {
            formaPago.setEstado(formaPago.getEstado() == 1 ? 0 : 1);
            formaPagoRepository.save(formaPago);
        }
        return "redirect:/metodos-pago?updated";
    }
}
