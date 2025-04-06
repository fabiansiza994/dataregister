package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.*;
import com.fmsp.dataregister.entity.dto.RegistroDTO;
import com.fmsp.dataregister.repository.GrupoRepository;
import com.fmsp.dataregister.repository.PlanRepository;
import com.fmsp.dataregister.repository.RolRepository;
import com.fmsp.dataregister.repository.UsuarioRepository;
import com.fmsp.dataregister.service.IAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService implements IAuthService {

    private final UsuarioRepository usuarioRepository;
    private final PlanRepository planRepository;
    private final RolRepository rolRepository;
    private final GrupoRepository grupoRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PlanRepository planRepository, RolRepository rolRepository, GrupoRepository grupoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.planRepository = planRepository;
        this.rolRepository = rolRepository;
        this.grupoRepository = grupoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String mostrarLogin(HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado != null) {
            return redireccionarSegunRol(usuarioLogueado);
        }
        return "auth/login";
    }

    @Override
    public String procesarLogin(String usuario, String password, Model model, HttpSession session) {
        Optional<Usuario> userOpt = usuarioRepository.findByUsuario(usuario);

        if (userOpt.isPresent()) {
            Usuario user = userOpt.get();

            // Verificar si está bloqueado
            if (user.isBloqueado()) {
                model.addAttribute("error", "Tu cuenta está bloqueada por múltiples intentos fallidos.");
                return "auth/login";
            }

            // Verificar contraseña
            if (passwordEncoder.matches(password, user.getPassword())) {
                user.setIntentosFallidos(0); // ✅ reinicia los intentos
                usuarioRepository.save(user);

                session.setAttribute("usuarioLogueado", user);

                if (user.getGrupo() == null) {
                    return "redirect:/grupos/nuevo?msg=Debe crear o unirse a un grupo";
                }

                if (user.getGrupo().getEmpresa() == null) {
                    return "redirect:/empresas/nueva?msg=Debe crear una empresa antes de continuar";
                }

                if ("PENDIENTE".equals(user.getGrupo().getEmpresa().getEstado())) {
                    model.addAttribute("alertaPago", "Por favor, realice el pago de la suscripción.");
                }

                return "home";

            } else {
                // Contraseña incorrecta → incrementar intentos
                int intentos = user.getIntentosFallidos() + 1;
                user.setIntentosFallidos(intentos);

                if (intentos >= 5) {
                    user.setBloqueado(true);
                    model.addAttribute("error", "Has excedido el número de intentos permitidos. Tu cuenta ha sido bloqueada.");
                } else {
                    model.addAttribute("error", "Usuario o contraseña incorrectos. Intento " + intentos + " de 5.");
                }

                usuarioRepository.save(user);
                return "auth/login";
            }

        } else {
            model.addAttribute("error", "Usuario o contraseña incorrectos.");
            return "auth/login";
        }
    }


    @Override
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:auth/login";
    }

    @Override
    public String mostrarHome(HttpSession session, Model model) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return "redirect:auth/login";
        }

        Empresa empresa = usuarioLogueado.getGrupo().getEmpresa();

        if ("PENDIENTE".equals(empresa.getEstado())) {
            model.addAttribute("alertaPago", "Por favor, realice el pago de la suscripción.");
        }

        if ("INACTIVO".equals(empresa.getEstado())) {
            return "payments/index";
        }
        return redireccionarSegunRol(usuarioLogueado);
    }

    @Override
    public String redireccionarSegunRol(Usuario usuario) {
        if ("ADMIN".equals(usuario.getRol().getNombre())) {
            return "home";
        } else {
            return "home";
        }
    }

    @Override
    public String mostrarPerfil(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:auth/login";
        }

        Empresa empresa = usuario.getGrupo().getEmpresa();
        Plan plan = planRepository.findTopByEmpresaOrderByFechaVigenciaDesc(empresa);
        boolean proximoAVencer = plan.getFechaVigencia().isBefore(LocalDate.now().plusDays(10));

        model.addAttribute("proximoAVencer", proximoAVencer);
        model.addAttribute("plan", plan);
        model.addAttribute("usuario", usuario);
        return "auth/perfil";
    }

    @Override
    public String editarPerfil(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/login";

        model.addAttribute("usuario", usuario);
        return "auth/editar-perfil";
    }

    @Override
    public String actualizarPerfil(Usuario datos, HttpSession session, RedirectAttributes redirect) {
        Usuario actual = (Usuario) session.getAttribute("usuarioLogueado");
        if (actual == null) return "redirect:/login";

        Optional<Usuario> existente = usuarioRepository.findByEmail(datos.getEmail());
        if (existente.isPresent() && !existente.get().getId().equals(actual.getId())) {
            redirect.addFlashAttribute("errorCorreo", "Este correo ya está en uso por otro usuario.");
            return "redirect:/perfil/editar";
        }

        actual.setNombre(datos.getNombre());
        actual.setApellido(datos.getApellido());
        actual.setEmail(datos.getEmail());

        usuarioRepository.save(actual);
        session.setAttribute("usuarioLogueado", actual);

        return "redirect:/perfil?success";
    }

    @Override
    public Object obtenerTodosLosRoles() {
        return rolRepository.findAll();
    }

    @Override
    public String registrarUsuario(RegistroDTO usuarioDTO, RedirectAttributes redirect) {
        if (usuarioRepository.findByUsuario(usuarioDTO.getUsuario()).isPresent()) {
            redirect.addFlashAttribute("error", "El usuario ya existe.");
            return "redirect:/registro";
        }

        // Buscar entidades
        Rol rol = rolRepository.findById(usuarioDTO.getRol().getId()).orElseThrow();
        Grupo grupo = grupoRepository.findById(usuarioDTO.getGrupo().getId()).orElseThrow();

        // Crear entidad Usuario
        Usuario usuario = new Usuario();
        usuario.setUsuario(usuarioDTO.getUsuario());
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setApellido(usuarioDTO.getApellido());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        usuario.setRol(rol);
        usuario.setGrupo(grupo);

        usuarioRepository.save(usuario);
        redirect.addFlashAttribute("success", "Usuario registrado exitosamente.");
        return "redirect:/login";
    }


    @Override
    public String mostrarFormularioRegistro(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        Empresa empresaActual = usuario.getGrupo().getEmpresa();

        model.addAttribute("usuario", new Usuario());
        model.addAttribute("grupos", obtenerTodosLosGrupos(empresaActual));
        model.addAttribute("roles", obtenerTodosLosRoles());
        return "auth/registro";
    }

    private List<Grupo> obtenerTodosLosGrupos(Empresa empresa) {
        return grupoRepository.findByEmpresa(empresa);
    }

    @Override
    public String listarUsuariosBloqueados(HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null) {
            return "redirect:/login";
        }

        Long empresaId = logueado.getGrupo().getEmpresa().getId();

        List<Usuario> bloqueados = usuarioRepository.findByBloqueadoTrueAndGrupo_Empresa_Id(empresaId);
        model.addAttribute("usuariosBloqueados", bloqueados);

        return "auth/usuarios-bloqueados";
    }

    @Override
    public String desbloquearUsuario(Long id, RedirectAttributes redirect) {
        Optional<Usuario> userOpt = usuarioRepository.findById(id);
        if (userOpt.isPresent()) {
            Usuario user = userOpt.get();
            user.setIntentosFallidos(0);
            user.setBloqueado(false);
            usuarioRepository.save(user);
            redirect.addFlashAttribute("success", "Usuario desbloqueado exitosamente.");
        } else {
            redirect.addFlashAttribute("error", "Usuario no encontrado.");
        }
        return "redirect:/auth/usuarios-bloqueados";
    }

    @Override
    public String listarUsuarios(HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null) {
            return "redirect:/login";
        }

        Long empresaId = logueado.getGrupo().getEmpresa().getId();
        List<Usuario> usuarios = usuarioRepository.findByGrupo_Empresa_Id(empresaId);

        model.addAttribute("usuarios", usuarios);
        return "auth/listar-usuarios";
    }

    @Override
    public String buscarUsuarios(String filtro, HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null) {
            return "redirect:/login";
        }

        Long empresaId = logueado.getGrupo().getEmpresa().getId();
        List<Usuario> usuarios = usuarioRepository.buscarPorNombreOUsuario(empresaId, filtro);

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("nombreBuscado", filtro);

        return "auth/listar-usuarios";
    }

    @Override
    public String mostrarFormularioEdicion(Long id, HttpSession session, Model model) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null) return "redirect:/login";

        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getGrupo().getEmpresa().getId().equals(logueado.getGrupo().getEmpresa().getId()))
                .orElse(null);

        if (usuario == null) return "redirect:/usuarios?error=Usuario no encontrado o no autorizado.";

        model.addAttribute("usuario", usuario);
        model.addAttribute("grupos", grupoRepository.findByEmpresa(logueado.getGrupo().getEmpresa()));
        model.addAttribute("roles", rolRepository.findAll());

        return "auth/editar-usuario";
    }


    @Override
    public String actualizarUsuario(RegistroDTO datos, RedirectAttributes redirect, HttpSession session) {
        Usuario actual = usuarioRepository.findById(datos.getId()).orElse(null);
        if (actual == null) {
            redirect.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/usuarios";
        }

        actual.setNombre(datos.getNombre());
        actual.setApellido(datos.getApellido());
        actual.setEmail(datos.getEmail());
        actual.setGrupo(grupoRepository.findById(datos.getGrupo().getId()).orElse(null));
        actual.setRol(rolRepository.findById(datos.getRol().getId()).orElse(null));

        usuarioRepository.save(actual);
        redirect.addFlashAttribute("success", "Usuario actualizado correctamente.");
        return "redirect:/usuarios";
    }

    @Override
    public String mostrarResetPassword(Long id, Model model, HttpSession session) {
        Usuario logueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (logueado == null) return "redirect:/login";

        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getGrupo().getEmpresa().getId().equals(logueado.getGrupo().getEmpresa().getId()))
                .orElse(null);

        if (usuario == null) {
            model.addAttribute("error", "Usuario no encontrado.");
            return "redirect:/usuarios";
        }

        model.addAttribute("usuario", usuario);
        return "auth/reset-password";
    }


    @Override
    public String resetearPassword(Long id, String nuevaPassword, String confirmarPassword, RedirectAttributes redirect) {
        if (!nuevaPassword.equals(confirmarPassword)) {
            redirect.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/usuarios/resetear-password/" + id;
        }

        Optional<Usuario> userOpt = usuarioRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirect.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/usuarios";
        }

        Usuario user = userOpt.get();
        user.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(user);

        redirect.addFlashAttribute("success", "Contraseña actualizada exitosamente.");
        return "redirect:/usuarios";
    }

}
