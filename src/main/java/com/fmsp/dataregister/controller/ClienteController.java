package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.Cliente;
import com.fmsp.dataregister.entity.dto.ClienteDTO;
import com.fmsp.dataregister.repository.ClienteRepository;
import com.fmsp.dataregister.service.IClientService;
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

    private final IClientService iClientService;
    private final ClienteRepository clienteRepository;

    public ClienteController(IClientService iClientService, ClienteRepository clienteRepository) {
        this.iClientService = iClientService;
        this.clienteRepository = clienteRepository;
    }


    @GetMapping
    public String listarClients(Model model, HttpSession session) {
        return iClientService.listarClientes(model, session);
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioRegistro(Model model, HttpSession session) {
        return iClientService.mostrarFormularioRegistro(model, session);
    }

    @PostMapping("/guardar")
    public String guardarCliente(@ModelAttribute Cliente client, HttpSession session, RedirectAttributes redirectAttributes) {
        return iClientService.guardarCliente(client, session, redirectAttributes);
    }


    @PostMapping("/eliminar/{id}")
    public String eliminarCliente(@PathVariable Integer id, RedirectAttributes redirectAttributes, HttpServletResponse response) {
        return iClientService.eliminarCliente(id, redirectAttributes, response);
    }

    @GetMapping("/buscar")
    public String buscarClientes(@RequestParam(value = "nombre", required = false) String name,
                                Model model, HttpSession session) {
        return iClientService.buscarClientes(name, model, session);
    }

    @PostMapping("/guardarAjax")
    @ResponseBody
    public ClienteDTO guardarClienteAjax(@RequestBody Cliente client, HttpSession session) {
        return iClientService.guardarClienteAjax(client, session);
    }

    @GetMapping("/listarAjax")
    @ResponseBody
    public Page<ClienteDTO> listarClientesAjax(@RequestParam(defaultValue = "0") int page, HttpSession session) {
        return iClientService.listarClientesAjax(page, session);
    }

    @GetMapping("/buscarAjax")
    @ResponseBody
    public List<ClienteDTO> buscarClientesAjax(@RequestParam("nombre") String name, HttpSession session) {
        return iClientService.buscarClientesAjax(name, session);
    }

    @GetMapping("/editar/{id}")
    public String editarCliente(@PathVariable Integer id, Model model) {
        return iClientService.editarCliente(id, model);
    }

    @PostMapping("/actualizar")
    public String actualizarCliente(@ModelAttribute Cliente client, HttpSession session, RedirectAttributes redirectAttributes) {
        return iClientService.actualizarCliente(client, session, redirectAttributes);
    }

    @Transactional
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstadoCliente(@PathVariable Integer id) {
        Optional<Cliente> client = clienteRepository.findById(id);
        if (client.isPresent()) {
            client.get().setEstado(Objects.equals(client.get().getEstado(), "ACTIVO") ? "INACTIVO" : "ACTIVO");
            clienteRepository.save(client.get());
        }
        return "redirect:/clientes";
    }

    @GetMapping("/politica-datos")
    public String politicaDatos() {
        return "politica-datos";
    }
}