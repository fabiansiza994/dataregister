package com.fmsp.dataregister.repository;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Integer> {
    List<Grupo> findByEmpresa(Empresa empresaActual);
}
