package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Grupo;
import com.fmsp.dataregister.service.IGrupoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/grupos")
public class GrupoController {

    private final IGrupoService grupoService;

    public GrupoController(IGrupoService grupoService) {
        this.grupoService = grupoService;
    }

    @GetMapping
    public String listarGrupos(Model model, HttpSession session) {
        return grupoService.listarGrupos(model, session);
    }

    @GetMapping("/nuevo")
    public String formularioNuevoGrupo(Model model, HttpSession session) {
       return grupoService.formularioNuevoGrupo(model, session);
    }

    @PostMapping("/guardar")
    public String guardarGrupo(@ModelAttribute Grupo grupo, HttpSession session) {
        return grupoService.guardarGrupo(grupo, session);
    }


    @GetMapping("/asignar")
    public String formularioAsignarUsuarioAGrupo(Model model, HttpSession session) {
        return grupoService.formularioAsignarUsuarioAGrupo(model, session);
    }

    @PostMapping("/asignar-usuario")
    public String asignarUsuarioAGrupo(@RequestParam Long usuarioId, @RequestParam Long grupoId, HttpSession session) {
        return grupoService.asignarUsuarioAGrupo(usuarioId, grupoId, session);
    }


}
