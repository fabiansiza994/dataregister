package com.fmsp.dataregister.service;

import com.fmsp.dataregister.entity.Empresa;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

public interface IEmpresaService {
    String formularioNuevaEmpresa(Model model, HttpSession session);
    String guardarEmpresa(@ModelAttribute Empresa empresa, HttpSession session);
}
