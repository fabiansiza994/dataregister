package com.fmsp.dataregister.repository;

import com.fmsp.dataregister.entity.Cliente;
import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Grupo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    @Query("SELECT c FROM Cliente c WHERE c.usuario.grupo = :grupo")
    List<Cliente> findByGrupo(@Param("grupo") Grupo grupo);
    List<Cliente> findByEmpresa(Empresa empresa);
    boolean existsByUsuario_Grupo_Empresa(Empresa empresa);
    boolean existsByUsuario_Grupo(Grupo grupo);
    List<Cliente> findByEmpresaAndNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(Empresa empresa, String nombre, String apellido);
    Page<Cliente> findByEmpresa(Empresa empresa, Pageable pageable);
    Page<Cliente> findByEmpresaAndEstado(Empresa empresa, String estado, Pageable pageable);
    List<Cliente> findByNombreContainingIgnoreCaseAndEmpresa(String nombre, Empresa empresaActual);
}
