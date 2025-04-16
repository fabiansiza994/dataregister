package com.fmsp.dataregister.service.impl;

import com.fmsp.dataregister.entity.Cliente;
import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.entity.dto.ClienteDTO;
import com.fmsp.dataregister.entity.dto.UsuarioSesionDTO;
import com.fmsp.dataregister.repository.ClienteRepository;
import com.fmsp.dataregister.repository.TrabajoRepository;
import com.fmsp.dataregister.repository.UsuarioRepository;
import com.fmsp.dataregister.service.IClientService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService implements IClientService {

    private final ClienteRepository clienteRepository;
    private final TrabajoRepository trabajoRepository;
    private final UsuarioRepository usuarioRepository;

    public ClientService(ClienteRepository clienteRepository, TrabajoRepository trabajoRepository, UsuarioRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.trabajoRepository = trabajoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public String listarClientes(Model model, HttpSession session) {
        UsuarioSesionDTO usuario = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:auth/login";
        }

        Usuario user = usuarioRepository.findById(usuario.getId()).orElseThrow();

        List<Cliente> clientes;
        Empresa empresaActual = user.getGrupo().getEmpresa();

        if ("INACTIVO".equals(empresaActual.getEstado())) {
            return "payments/index";
        }

        if (user.getRol().getNombre().equals("ADMIN")) {
            clientes = clienteRepository.findByEmpresa(empresaActual);  // ADMIN ve solo los clientes de su empresa
        } else {
            clientes = clienteRepository.findByGrupo(user.getGrupo()); // Usuario normal solo ve los de su grupo
        }

        model.addAttribute("clientes", clientes);
        return "clientes/lista";
    }

    @Override
    public String mostrarFormularioRegistro(Model model, HttpSession session) {
        UsuarioSesionDTO usuario = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:auth/login";  // Redirigir si no está autenticado
        }

        Usuario user = usuarioRepository.findById(usuario.getId()).orElseThrow();

        Empresa empresaActual = user.getGrupo().getEmpresa();

        if ("INACTIVO".equals(empresaActual.getEstado())) {
            return "payments/index";
        }

        model.addAttribute("cliente", new Cliente());
        return "clientes/crear_cliente";
    }

    @Override
    public String guardarCliente(Cliente cliente, HttpSession session, RedirectAttributes redirectAttributes) {
        UsuarioSesionDTO usuario = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:auth/login";
        }

        Usuario user = usuarioRepository.findById(usuario.getId()).orElseThrow();

        cliente.setUsuario(user);

        if (user.getGrupo() != null && user.getGrupo().getEmpresa() != null) {
            cliente.setEmpresa(user.getGrupo().getEmpresa());
        } else {
            return "redirect:/clientes?error=Debe pertenecer a una empresa para registrar clientes";
        }

        clienteRepository.save(cliente);
        redirectAttributes.addFlashAttribute("success", "¡Cliente registrado con éxito!");
        return "redirect:/clientes?success";
    }

    @Override
    public String eliminarCliente(Integer id, RedirectAttributes redirectAttributes, HttpServletResponse response) {
        if (trabajoRepository.existsByClienteId(Long.valueOf(id))) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el cliente porque tiene trabajos asociados.");
            return "redirect:/clientes";
        }
        clienteRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Cliente eliminado correctamente.");
        return "redirect:/clientes";
    }



    @Override
    public String buscarClientes(String nombre, Model model, HttpSession session) {
        UsuarioSesionDTO usuario = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:auth/login";
        }

        Usuario user = usuarioRepository.findById(usuario.getId()).orElseThrow();

        Empresa empresaActual = user.getGrupo().getEmpresa(); // Obtener la empresa del usuario
        List<Cliente> clientes;

        if (nombre == null || nombre.trim().isEmpty()) {
            clientes = clienteRepository.findByEmpresa(empresaActual);
        } else {
            clientes = clienteRepository.findByEmpresaAndNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(empresaActual, nombre, nombre);
        }

        model.addAttribute("clientes", clientes);
        model.addAttribute("nombreBuscado", nombre); // Mantener el estado del input de búsqueda
        return "clientes/lista";
    }

    @Override
    public ClienteDTO guardarClienteAjax(Cliente cliente, HttpSession session) {
        UsuarioSesionDTO usuario = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            throw new RuntimeException("Usuario no autenticado");
        }

        Usuario user = usuarioRepository.findById(usuario.getId()).orElseThrow();

        Empresa empresaActual = user.getGrupo().getEmpresa();

        cliente.setUsuario(user);
        cliente.setEmpresa(empresaActual);

        Cliente nuevoCliente = clienteRepository.save(cliente);
        return new ClienteDTO(nuevoCliente.getId(), nuevoCliente.getNombre(), nuevoCliente.getApellido(),
                nuevoCliente.getDireccion(), nuevoCliente.getTelefono());
    }

    @Override
    public Page<ClienteDTO> listarClientesAjax(int page, HttpSession session) {
        UsuarioSesionDTO usuario = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            throw new RuntimeException("Usuario no autenticado");
        }

        Usuario user = usuarioRepository.findById(usuario.getId()).orElseThrow();

        Empresa empresaActual = user.getGrupo().getEmpresa();
        Page<Cliente> clientesPage = clienteRepository.findByEmpresaAndEstado(empresaActual, "ACTIVO", PageRequest.of(page, 10));

        List<ClienteDTO> clientesDTO = clientesPage.getContent().stream()
                .map(cliente -> new ClienteDTO(cliente.getId(), cliente.getNombre(), cliente.getApellido(), cliente.getDireccion(), cliente.getTelefono()))
                .collect(Collectors.toList());

        return new PageImpl<>(clientesDTO, clientesPage.getPageable(), clientesPage.getTotalElements());
    }

    @Override
    public List<ClienteDTO> buscarClientesAjax(String nombre, HttpSession session) {
        UsuarioSesionDTO usuario = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            throw new RuntimeException("Usuario no autenticado");
        }

        Usuario user = usuarioRepository.findById(usuario.getId()).orElseThrow();
        Empresa empresaActual = user.getGrupo().getEmpresa();
        List<Cliente> clientes = clienteRepository.findByNombreContainingIgnoreCaseAndEmpresa(nombre, empresaActual);

        return clientes.stream()
                .map(cliente -> new ClienteDTO(cliente.getId(), cliente.getNombre(), cliente.getApellido(),
                        cliente.getDireccion(), cliente.getTelefono()))
                .toList();
    }

    @Override
    public String editarCliente(Integer id, Model model) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        model.addAttribute("cliente", cliente); // Correcto: cliente en singular
        model.addAttribute("usuario", cliente.getUsuario()); // Añadir el usuario asociado al cliente

        return "clientes/editar";
    }

    @Override
    public String actualizarCliente(Cliente cliente, HttpSession session, RedirectAttributes redirectAttributes) {
        UsuarioSesionDTO usuario = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:auth/login";
        }
        var clienteId = cliente.getId();
        Cliente clienteExistente = clienteRepository.findById(Math.toIntExact(clienteId))
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        cliente.setUsuario(clienteExistente.getUsuario());

        clienteRepository.save(cliente);
        redirectAttributes.addFlashAttribute("success", "Cliente actualizado correctamente.");
        return "redirect:/clientes";
    }
}