package com.fmsp.dataregister.repository;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Integer> {
    Reporte findTopByEmpresa(Empresa empresa);
}
