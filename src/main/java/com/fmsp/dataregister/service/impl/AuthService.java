package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Plan;
import com.fmsp.dataregister.entity.Usuario;
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
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PlanRepository planRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.planRepository = planRepository;
        this.rolRepository = rolRepository;
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
    public String registrarUsuario(Usuario usuario, RedirectAttributes redirect) {
        if (usuarioRepository.findByUsuario(usuario.getUsuario()).isPresent()) {
            redirect.addFlashAttribute("error", "El usuario ya existe.");
            return "redirect:/registro";
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol(rolRepository.findById(usuario.getRol().getId()).orElseThrow());
        usuarioRepository.save(usuario);
        redirect.addFlashAttribute("success", "Usuario registrado exitosamente.");
        return "redirect:/login";
    }

    @Override
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", obtenerTodosLosRoles());
        return "auth/registro";
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


}
