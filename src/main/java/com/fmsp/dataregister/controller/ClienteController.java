package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Cliente;
import com.fmsp.dataregister.entity.dto.ClienteDTO;
import com.fmsp.dataregister.repository.ClienteRepository;
import com.fmsp.dataregister.service.IClienteService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final IClienteService iClienteService;
    private final ClienteRepository clienteRepository;

    public ClienteController(IClienteService iClienteService, ClienteRepository clienteRepository) {
        this.iClienteService = iClienteService;
        this.clienteRepository = clienteRepository;
    }


    @GetMapping
    public String listarClientes(Model model, HttpSession session) {
        return iClienteService.listarClientes(model, session);
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioRegistro(Model model, HttpSession session) {
        return iClienteService.mostrarFormularioRegistro(model, session);
    }

    @PostMapping("/guardar")
    public String guardarCliente(@ModelAttribute Cliente cliente, HttpSession session) {
        return iClienteService.guardarCliente(cliente, session);
    }


    @GetMapping("/eliminar/{id}")
    public String eliminarCliente(@PathVariable Integer id, RedirectAttributes redirectAttributes, HttpServletResponse response) {
        return iClienteService.eliminarCliente(id, redirectAttributes, response);
    }

    @GetMapping("/buscar")
    public String buscarClientes(@RequestParam(value = "nombre", required = false) String nombre,
                                 Model model, HttpSession session) {
        return iClienteService.buscarClientes(nombre, model, session);
    }

    @PostMapping("/guardarAjax")
    @ResponseBody
    public ClienteDTO guardarClienteAjax(@RequestBody Cliente cliente, HttpSession session) {
        return iClienteService.guardarClienteAjax(cliente, session);
    }

    @GetMapping("/listarAjax")
    @ResponseBody
    public Page<ClienteDTO> listarClientesAjax(@RequestParam(defaultValue = "0") int page, HttpSession session) {
        return iClienteService.listarClientesAjax(page, session);
    }

    @GetMapping("/buscarAjax")
    @ResponseBody
    public List<ClienteDTO> buscarClientesAjax(@RequestParam("nombre") String nombre, HttpSession session) {
        return iClienteService.buscarClientesAjax(nombre, session);
    }

    @GetMapping("/editar/{id}")
    public String editarCliente(@PathVariable Integer id, Model model) {
        return iClienteService.editarCliente(id, model);
    }

    @PostMapping("/actualizar")
    public String actualizarCliente(@ModelAttribute Cliente cliente, HttpSession session, RedirectAttributes redirectAttributes) {
        return iClienteService.actualizarCliente(cliente, session, redirectAttributes);
    }

    @Transactional
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstadoCliente(@PathVariable Integer id) {
        Optional<Cliente> cliente = clienteRepository.findById(id);
        if (cliente.isPresent()) {
            cliente.get().setEstado(Objects.equals(cliente.get().getEstado(), "ACTIVO") ? "INACTIVO" : "ACTIVO");
            clienteRepository.save(cliente.get());
        }
        return "redirect:/clientes";
    }
}
