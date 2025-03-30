package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.service.IMethodPaymentService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/metodos-pago")
public class MetodoPagoController {

    private final IMethodPaymentService methodPaymentService;

    public MetodoPagoController(IMethodPaymentService methodPaymentService) {
        this.methodPaymentService = methodPaymentService;
    }


    @GetMapping
    public String listarMetodosPago(Model model, HttpSession session) {
        return methodPaymentService.listarMetodosPago(model, session);
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioRegistro(Model model) {
        return methodPaymentService.mostrarFormularioRegistro(model);
    }

    @PostMapping("/guardar")
    public String guardarMetodoPago(@RequestParam("formaPago") String formaPagoNombre, HttpSession session) {
        return guardarMetodoPago(formaPagoNombre, session);
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarMetodoPago(@PathVariable Long id, RedirectAttributes redirectAttributes) {
       return methodPaymentService.eliminarMetodoPago(id, redirectAttributes);
    }

    @Transactional
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstadoMetodoPago(@PathVariable Long id) {
        return methodPaymentService.cambiarEstadoMetodoPago(id);
    }

}
