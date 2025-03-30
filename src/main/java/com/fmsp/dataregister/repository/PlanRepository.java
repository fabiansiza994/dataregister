package com.fmsp.dataregister.repository;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByFechaVigenciaBefore(LocalDate fecha);

    Plan findByEmpresa(Empresa empresa);

    Plan findTopByEmpresaOrderByFechaVigenciaDesc(Empresa empresa);
}
