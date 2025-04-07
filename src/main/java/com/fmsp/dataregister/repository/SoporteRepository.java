package com.fmsp.dataregister.repository;

import com.fmsp.dataregister.entity.Soporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoporteRepository extends JpaRepository<Soporte, Long> {
    List<Soporte> findByUsuario_Id(Long id);

    List<Soporte> findByUsuario_Grupo_Empresa_Id(Long empresaId);

    @Query("SELECT s FROM Soporte s " +
            "WHERE (:estado IS NULL OR s.estado = :estado) " +
            "AND (:usuario IS NULL OR LOWER(s.usuario.usuario) LIKE LOWER(CONCAT('%', :usuario, '%')))")
    List<Soporte> findByFiltros(@Param("estado") String estado, @Param("usuario") String usuario);
}
