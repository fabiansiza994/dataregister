package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.service.IEmpresaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/empresas")
public class EmpresaController {

    private final IEmpresaService iEmpresaService;

    public EmpresaController(IEmpresaService iEmpresaService) {
        this.iEmpresaService = iEmpresaService;
    }

    @GetMapping("/nueva")
    public String formularioNuevaEmpresa(Model model, HttpSession session) {
        return iEmpresaService.formularioNuevaEmpresa(model, session);
    }

    @PostMapping("/guardar")
    public String guardarEmpresa(@ModelAttribute Empresa empresa, HttpSession session) {
        empresa.setEstado("ACTIVO");
        return iEmpresaService.guardarEmpresa(empresa, session);
    }
}
