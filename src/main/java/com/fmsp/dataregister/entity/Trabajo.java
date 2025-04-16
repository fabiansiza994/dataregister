package com.fmsp.dataregister.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Trabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private BigDecimal valorLabor;
    private BigDecimal valorMateriales;
    private BigDecimal valorTotal;
    private BigDecimal ganancias;

    @Column(length = 500)
    private String descripcionLabor;

    // Guardar imágenes en Base64
    @Column(length = 1000)
    private String foto1;

    @Column(length = 1000)
    private String foto2;

    @Column(length = 1000)
    private String foto3;

    @Column(length = 1000)
    private String foto4;

    @ManyToOne
    @JoinColumn(name = "CLIENTE_ID", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "FORMA_PAGO_ID", nullable = false)
    private FormaPago formaPago;

    @ManyToOne
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private Usuario usuario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getValorLabor() {
        return valorLabor;
    }

    public void setValorLabor(BigDecimal valorLabor) {
        this.valorLabor = valorLabor;
    }

    public BigDecimal getValorMateriales() {
        return valorMateriales;
    }

    public void setValorMateriales(BigDecimal valorMateriales) {
        this.valorMateriales = valorMateriales;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getGanancias() {
        return ganancias;
    }

    public void setGanancias(BigDecimal ganancias) {
        this.ganancias = ganancias;
    }

    public String getDescripcionLabor() {
        return descripcionLabor;
    }

    public void setDescripcionLabor(String descripcionLabor) {
        this.descripcionLabor = descripcionLabor;
    }

    public String getFoto1() {
        return foto1;
    }

    public void setFoto1(String foto1) {
        this.foto1 = foto1;
    }

    public String getFoto2() {
        return foto2;
    }

    public void setFoto2(String foto2) {
        this.foto2 = foto2;
    }

    public String getFoto3() {
        return foto3;
    }

    public void setFoto3(String foto3) {
        this.foto3 = foto3;
    }

    public String getFoto4() {
        return foto4;
    }

    public void setFoto4(String foto4) {
        this.foto4 = foto4;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(FormaPago formaPago) {
        this.formaPago = formaPago;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
