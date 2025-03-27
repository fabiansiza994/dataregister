package com.fmsp.dataregister.service;

import com.fmsp.dataregister.entity.Grupo;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

public interface IGrupoService {

    String listarGrupos(Model model, HttpSession session);
    String formularioNuevoGrupo(Model model, HttpSession session);
    String guardarGrupo(Grupo grupo, HttpSession session);
    String formularioAsignarUsuarioAGrupo(Model model, HttpSession session);
    String asignarUsuarioAGrupo(Long usuarioId, Long grupoId, HttpSession session);
}
