package com.fmsp.dataregister.entity.dto;

import java.math.BigDecimal;

public class TrabajoDTO {
    private int id;
    private String fecha;
    private String cliente; // 👈 Solo el nombre
    private BigDecimal valorTotal;
    private String usuario;

    public TrabajoDTO(int id, String fecha, String cliente, BigDecimal valorTotal, String usuario) {
        this.id = id;
        this.fecha = fecha;
        this.cliente = cliente;
        this.valorTotal = valorTotal;
        this.usuario = usuario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}