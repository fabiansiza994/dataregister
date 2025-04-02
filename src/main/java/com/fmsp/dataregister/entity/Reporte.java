package com.fmsp.dataregister.entity;

import jakarta.persistence.*;

@Entity
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreReporte; // Ej. "Factura trabajo"

    private String piePagina;     // Ej. "Gracias por confiar..."

    private String logoUrl;       // Imagen de logo (puede ser URL de S3)

    private String marcaAguaUrl;  // Imagen opcional de marca de agua

    private String bineta;

    @ManyToOne
    private Empresa empresa;

    public String getBineta() {
        return bineta;
    }

    public void setBineta(String bineta) {
        this.bineta = bineta;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreReporte() {
        return nombreReporte;
    }

    public void setNombreReporte(String nombreReporte) {
        this.nombreReporte = nombreReporte;
    }

    public String getPiePagina() {
        return piePagina;
    }

    public void setPiePagina(String piePagina) {
        this.piePagina = piePagina;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getMarcaAguaUrl() {
        return marcaAguaUrl;
    }

    public void setMarcaAguaUrl(String marcaAguaUrl) {
        this.marcaAguaUrl = marcaAguaUrl;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
}

