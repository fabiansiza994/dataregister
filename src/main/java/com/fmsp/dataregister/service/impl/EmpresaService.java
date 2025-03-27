package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.repository.EmpresaRepository;
import com.fmsp.dataregister.repository.GrupoRepository;
import com.fmsp.dataregister.service.IEmpresaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class EmpresaService implements IEmpresaService {

    private final EmpresaRepository empresaRepository;
    private final GrupoRepository grupoRepository;

    public EmpresaService(EmpresaRepository empresaRepository, GrupoRepository grupoRepository) {
        this.empresaRepository = empresaRepository;
        this.grupoRepository = grupoRepository;
    }


    @Override
    public String formularioNuevaEmpresa(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getGrupo() == null) {
            return "redirect:/grupos/nuevo";
        }

        model.addAttribute("empresa", new Empresa());
        return "empresas/formulario_empresa";
    }

    @Override
    public String guardarEmpresa(Empresa empresa, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getGrupo() == null) {
            return "redirect:/grupos/nuevo";
        }

        Empresa nuevaEmpresa = empresaRepository.save(empresa);

        var grupo = usuario.getGrupo();
        grupo.setEmpresa(nuevaEmpresa);
        grupoRepository.save(grupo);

        return "redirect:/";
    }
}
