package com.fmsp.dataregister.repository;

import com.fmsp.dataregister.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuario(String usuario);
    Optional<Usuario> findByEmail(String correo);
    List<Usuario> findByBloqueadoTrueAndGrupo_Empresa_Id(Long empresaId);
    List<Usuario> findByGrupo_Empresa_Id(Long empresaId);

    @Query("SELECT u FROM Usuario u WHERE u.grupo.empresa.id = :empresaId AND " +
            "(LOWER(u.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :filtro, '%')))")
    List<Usuario> buscarPorNombreOUsuario(@Param("empresaId") Long empresaId, @Param("filtro") String filtro);

}
