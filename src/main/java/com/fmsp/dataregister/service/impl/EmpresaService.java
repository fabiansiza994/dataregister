package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.entity.dto.UsuarioSesionDTO;
import com.fmsp.dataregister.repository.EmpresaRepository;
import com.fmsp.dataregister.repository.GrupoRepository;
import com.fmsp.dataregister.repository.UsuarioRepository;
import com.fmsp.dataregister.service.IEmpresaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class EmpresaService implements IEmpresaService {

    private final EmpresaRepository empresaRepository;
    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;

    public EmpresaService(EmpresaRepository empresaRepository, GrupoRepository grupoRepository, UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
    }


    @Override
    public String formularioNuevaEmpresa(Model model, HttpSession session) {
        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null || usuarioDto.getGrupoId() == null) {
            return "redirect:/grupos/nuevo";
        }

        model.addAttribute("empresa", new Empresa());
        return "empresas/formulario_empresa";
    }

    @Override
    public String guardarEmpresa(Empresa empresa, HttpSession session) {
        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null || usuarioDto.getGrupoId() == null) {
            return "redirect:/grupos/nuevo";
        }

        Usuario usuario = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();
        Empresa nuevaEmpresa = empresaRepository.save(empresa);

        var grupo = usuario.getGrupo();
        grupo.setEmpresa(nuevaEmpresa);
        grupoRepository.save(grupo);

        return "redirect:/";
    }
}
