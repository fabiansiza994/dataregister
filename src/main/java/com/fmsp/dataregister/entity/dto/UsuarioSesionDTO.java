package com.fmsp.dataregister.entity.dto;

import com.fmsp.dataregister.entity.Grupo;

public class UsuarioSesionDTO {
    private Long id;
    private String username;
    private String rol;
    private Long empresaId;
    private Long grupoId;;

    public UsuarioSesionDTO(Long id, String username, String rol, Long empresaId, Long grupoId) {
        this.id = id;
        this.username = username;
        this.rol = rol;
        this.empresaId = empresaId;
        this.grupoId = grupoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(Long grupoId) {
        this.grupoId = grupoId;
    }
}
