package com.fmsp.dataregister.repository;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Grupo;
import com.fmsp.dataregister.entity.Trabajo;
import com.fmsp.dataregister.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrabajoRepository extends JpaRepository<Trabajo, Integer> {
    Optional<Trabajo> findById(Long id);
    boolean existsByClienteId(Integer id);

    boolean existsByFormaPagoId(Long id);

    List<Trabajo> findByUsuario(Usuario usuario);

    List<Trabajo> findByUsuario_Grupo_Empresa(Empresa empresa);

    // Filtrar trabajos por el grupo
    List<Trabajo> findByUsuario_Grupo(Grupo grupo);

    // Reporte para ADMIN: Sumar ventas de la empresa actual
    @Query("SELECT COALESCE(SUM(t.valorTotal), 0) FROM Trabajo t WHERE t.usuario.grupo.empresa = :empresa AND t.fecha BETWEEN :inicio AND :fin")
    BigDecimal sumarVentasPorEmpresaEntreFechas(@Param("empresa") Empresa empresa, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    // Reporte para ADMIN: Sumar ganancias de la empresa actual
    @Query("SELECT COALESCE(SUM(t.ganancias), 0) FROM Trabajo t WHERE t.usuario.grupo.empresa = :empresa AND t.fecha BETWEEN :inicio AND :fin")
    BigDecimal sumarGananciasPorEmpresaEntreFechas(@Param("empresa") Empresa empresa, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    // Reporte para usuarios normales: Sumar ventas por grupo
    @Query("SELECT COALESCE(SUM(t.valorTotal), 0) FROM Trabajo t WHERE t.usuario.grupo = :grupo AND t.fecha BETWEEN :inicio AND :fin")
    BigDecimal sumarVentasPorGrupoEntreFechas(@Param("grupo") Grupo grupo, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    // Reporte para usuarios normales: Sumar ganancias por grupo
    @Query("SELECT COALESCE(SUM(t.ganancias), 0) FROM Trabajo t WHERE t.usuario.grupo = :grupo AND t.fecha BETWEEN :inicio AND :fin")
    BigDecimal sumarGananciasPorGrupoEntreFechas(@Param("grupo") Grupo grupo, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    List<Trabajo> findByUsuario_Grupo_EmpresaAndFechaBetween(Empresa empresa, LocalDate fechaInicio, LocalDate fechaFin);

    List<Trabajo> findByUsuarioAndFechaBetween(Usuario usuario, LocalDate fechaInicio, LocalDate fechaFin);

}
