package com.fmsp.dataregister.service;

import com.fmsp.dataregister.entity.Cliente;
import com.fmsp.dataregister.entity.dto.ClienteDTO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

public interface IClientService {
    String listarClientes(Model model, HttpSession session);
    String mostrarFormularioRegistro(Model model, HttpSession session);
    String guardarCliente(Cliente cliente, HttpSession session);
    String eliminarCliente(Integer id, RedirectAttributes redirectAttributes, HttpServletResponse response);
    String buscarClientes(String nombre,
                          Model model, HttpSession session);
    ClienteDTO guardarClienteAjax(Cliente cliente, HttpSession session);
    Page<ClienteDTO> listarClientesAjax(int page, HttpSession session);
    List<ClienteDTO> buscarClientesAjax(String nombre, HttpSession session);
    String editarCliente(Integer id, Model model);
    String actualizarCliente(Cliente cliente, HttpSession session, RedirectAttributes redirectAttributes);
}

