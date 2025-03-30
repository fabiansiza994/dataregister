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
public class ClientController {

    private final IClientService iClientService;
    private final ClienteRepository clienteRepository;

    public ClientController(IClientService iClientService, ClienteRepository clienteRepository) {
        this.iClientService = iClientService;
        this.clienteRepository = clienteRepository;
    }


    @GetMapping
    public String listClients(Model model, HttpSession session) {
        return iClientService.listarClientes(model, session);
    }

    @GetMapping("/nuevo")
    public String showRegisterForm(Model model, HttpSession session) {
        return iClientService.mostrarFormularioRegistro(model, session);
    }

    @PostMapping("/guardar")
    public String saveClient(@ModelAttribute Cliente client, HttpSession session) {
        return iClientService.guardarCliente(client, session);
    }


    @GetMapping("/eliminar/{id}")
    public String deleteClient(@PathVariable Integer id, RedirectAttributes redirectAttributes, HttpServletResponse response) {
        return iClientService.eliminarCliente(id, redirectAttributes, response);
    }

    @GetMapping("/buscar")
    public String searchClients(@RequestParam(value = "nombre", required = false) String name,
                                Model model, HttpSession session) {
        return iClientService.buscarClientes(name, model, session);
    }

    @PostMapping("/guardarAjax")
    @ResponseBody
    public ClienteDTO saveClientAjax(@RequestBody Cliente client, HttpSession session) {
        return iClientService.guardarClienteAjax(client, session);
    }

    @GetMapping("/listarAjax")
    @ResponseBody
    public Page<ClienteDTO> listClientsAjax(@RequestParam(defaultValue = "0") int page, HttpSession session) {
        return iClientService.listarClientesAjax(page, session);
    }

    @GetMapping("/buscarAjax")
    @ResponseBody
    public List<ClienteDTO> searchClientsAjax(@RequestParam("nombre") String name, HttpSession session) {
        return iClientService.buscarClientesAjax(name, session);
    }

    @GetMapping("/editar/{id}")
    public String editarCliente(@PathVariable Integer id, Model model) {
        return iClientService.editarCliente(id, model);
    }

    @PostMapping("/actualizar")
    public String updateClient(@ModelAttribute Cliente client, HttpSession session, RedirectAttributes redirectAttributes) {
        return iClientService.actualizarCliente(client, session, redirectAttributes);
    }

    @Transactional
    @PostMapping("/cambiar-estado/{id}")
    public String changeStatusClient(@PathVariable Integer id) {
        Optional<Cliente> client = clienteRepository.findById(id);
        if (client.isPresent()) {
            client.get().setEstado(Objects.equals(client.get().getEstado(), "ACTIVO") ? "INACTIVO" : "ACTIVO");
            clienteRepository.save(client.get());
        }
        return "redirect:/clientes";
    }
}
