package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Grupo;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.repository.GrupoRepository;
import com.fmsp.dataregister.repository.UsuarioRepository;
import com.fmsp.dataregister.service.IGrupoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

@Service
public class GrupoService implements IGrupoService {

    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;

    public GrupoService(GrupoRepository grupoRepository, UsuarioRepository usuarioRepository) {
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public String listarGrupos(Model model, HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/";
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || usuario.getGrupo() == null || usuario.getGrupo().getEmpresa() == null) {
            return "redirect:/grupos?error=Debe pertenecer a una empresa";
        }

        Empresa empresaActual = usuario.getGrupo().getEmpresa();

        List<Grupo> grupos = grupoRepository.findByEmpresa(empresaActual);
        model.addAttribute("grupos", grupos);
        return "grupos/listar_grupos";
    }

    @Override
    public String formularioNuevoGrupo(Model model, HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/";
        }
        model.addAttribute("grupo", new Grupo());
        return "grupos/formulario_grupo";
    }

    @Override
    public String guardarGrupo(Grupo grupo, HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/";
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || usuario.getGrupo() == null || usuario.getGrupo().getEmpresa() == null) {
            return "redirect:/grupos?error=Debe pertenecer a una empresa";
        }

        grupo.setEmpresa(usuario.getGrupo().getEmpresa());

        grupoRepository.save(grupo);

        return "redirect:/grupos?success";
    }

    @Override
    public String formularioAsignarUsuarioAGrupo(Model model, HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/";
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || usuario.getGrupo() == null || usuario.getGrupo().getEmpresa() == null) {
            return "redirect:/grupos?error=Debe pertenecer a una empresa";
        }

        Empresa empresaActual = usuario.getGrupo().getEmpresa();

        List<Grupo> grupos = grupoRepository.findByEmpresa(empresaActual);

        model.addAttribute("grupos", grupos);
        model.addAttribute("usuarios", usuarioRepository.findAll());

        return "grupos/asignar_usuario_grupo";
    }

    @Override
    public String asignarUsuarioAGrupo(Long usuarioId, Long grupoId, HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        Optional<Grupo> grupoOpt = grupoRepository.findById(Math.toIntExact(grupoId));

        if (usuarioOpt.isPresent() && grupoOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setGrupo(grupoOpt.get());
            usuarioRepository.save(usuario);
        }

        return "redirect:/grupos/asignar?success=usuario-asignado";
    }

    private boolean esAdmin(HttpSession session) {
        var usuario = session.getAttribute("usuarioLogueado");
        return usuario != null && ((Usuario) usuario).getRol().getNombre().equals("ADMIN");
    }
}
