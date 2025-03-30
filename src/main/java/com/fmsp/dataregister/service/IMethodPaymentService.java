package com.fmsp.dataregister.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public interface IMethodPaymentService {
    String listarMetodosPago(Model model, HttpSession session);
    String mostrarFormularioRegistro(Model model);
    String guardarMetodoPago(String formaPagoNombre, HttpSession session);
    String eliminarMetodoPago(Long id, RedirectAttributes redirectAttributes);
    String cambiarEstadoMetodoPago(@PathVariable Long id);
}
