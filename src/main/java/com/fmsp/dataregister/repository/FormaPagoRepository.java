package com.fmsp.dataregister.repository;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.FormaPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormaPagoRepository extends JpaRepository<FormaPago, Long> {
    List<FormaPago> findByEmpresaAndEstado(Empresa empresa, int estado);
}
