package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.*;
import com.fmsp.dataregister.repository.ClienteRepository;
import com.fmsp.dataregister.repository.FormaPagoRepository;
import com.fmsp.dataregister.repository.TrabajoRepository;
import com.fmsp.dataregister.service.impl.S3Service;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.Tika;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/trabajos")
public class TrabajoController {

    private final TrabajoRepository trabajoRepository;
    private final ClienteRepository clienteRepository;
    private final FormaPagoRepository formaPagoRepository;
    private final S3Service s3Service;

    private final Tika tika = new Tika();

    public TrabajoController(TrabajoRepository trabajoRepository, ClienteRepository clienteRepository, FormaPagoRepository formaPagoRepository, S3Service s3Service) {
        this.trabajoRepository = trabajoRepository;
        this.clienteRepository = clienteRepository;
        this.formaPagoRepository = formaPagoRepository;
        this.s3Service = s3Service;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("foto1", "foto2", "foto3", "foto4");
    }

    @GetMapping("/registro")
    public String mostrarFormulario(@RequestParam(required = false) Map<String, String> params, Model model, HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:auth/login";
        }

        Empresa empresaActual = usuario.getGrupo().getEmpresa();

        if ("INACTIVO".equals(empresaActual.getEstado())) {
            return "payments/index";
        }

        // Validar si hay al menos un cliente en la empresa y el grupo
        boolean hayClientesEnEmpresa = clienteRepository.existsByUsuario_Grupo_Empresa(empresaActual);
        boolean hayClientesEnGrupo = clienteRepository.existsByUsuario_Grupo(usuario.getGrupo());

        if (!hayClientesEnEmpresa || !hayClientesEnGrupo) {
            return "redirect:/clientes/nuevo?error=Debe crear al menos un cliente antes de registrar trabajos.";
        }

        List<Cliente> clientes;
        if (usuario.getRol().getNombre().equals("ADMIN")) {
            clientes = clienteRepository.findByEmpresa(empresaActual);  // ADMIN ve todos los clientes
        } else {
            clientes = clienteRepository.findByGrupo(usuario.getGrupo()); // Usuario normal solo ve los suyos
        }
        List<FormaPago> formasPago = formaPagoRepository.findByEmpresaAndEstado(empresaActual, 1);

        // Si params está vacío, inicializar un mapa vacío para evitar NullPointerException
        model.addAttribute("datosPrevios", params.isEmpty() ? Map.of() : params);

        model.addAttribute("trabajo", new Trabajo());
        model.addAttribute("clientes", clientes);
        model.addAttribute("formasPago", formasPago);

        return "trabajos/registro_trabajo";
    }


    @PostMapping("/guardar")
    @Transactional
    public String guardarTrabajo(@ModelAttribute Trabajo trabajo,
                                 @RequestParam(value = "foto1", required = false) MultipartFile foto1,
                                 @RequestParam(value = "foto2", required = false) MultipartFile foto2,
                                 @RequestParam(value = "foto3", required = false) MultipartFile foto3,
                                 @RequestParam(value = "foto4", required = false) MultipartFile foto4,
                                 HttpSession session) {
        try {
            // Comprimir y subir cada imagen
            if (foto1 != null && !foto1.isEmpty()) {
                byte[] comprimida = comprimirImagen(foto1);
                trabajo.setFoto1(s3Service.uploadFile("trabajos/foto1_", comprimida, "image/jpeg"));
            }
            if (foto2 != null && !foto2.isEmpty()) {
                byte[] comprimida = comprimirImagen(foto2);
                trabajo.setFoto2(s3Service.uploadFile("trabajos/foto2_", comprimida, "image/jpeg"));
            }
            if (foto3 != null && !foto3.isEmpty()) {
                byte[] comprimida = comprimirImagen(foto3);
                trabajo.setFoto3(s3Service.uploadFile("trabajos/foto3_", comprimida, "image/jpeg"));
            }
            if (foto4 != null && !foto4.isEmpty()) {
                byte[] comprimida = comprimirImagen(foto4);
                trabajo.setFoto4(s3Service.uploadFile("trabajos/foto4_", comprimida, "image/jpeg"));
            }

            // Usuario que registró el trabajo
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
            if (usuario != null) {
                trabajo.setUsuario(usuario);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/trabajos/registro?error";
        }

        trabajoRepository.save(trabajo);
        return "redirect:/trabajos/listar?success";
    }

    public byte[] comprimirImagen(MultipartFile archivo) throws IOException {
        if (archivo.isEmpty() || archivo.getSize() == 0) {
            throw new IOException("Archivo vacío: " + archivo.getOriginalFilename());
        }

        byte[] originalBytes = archivo.getBytes();
        String tipoReal = new Tika().detect(originalBytes);

        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(originalBytes));

            if (imagen == null) {
                System.out.println("⚠️ No se puede procesar la imagen. Subiendo sin comprimir. Tipo: " + tipoReal);
                return originalBytes;
            }

            int maxWidth = 800;
            int maxHeight = 800;
            int width = imagen.getWidth();
            int height = imagen.getHeight();

            double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);

            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(imagen.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(0.8f);

            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            jpgWriter.setOutput(ios);
            jpgWriter.write(null, new IIOImage(resized, null, null), jpgWriteParam);
            jpgWriter.dispose();
            ios.close();

            byte[] finalBytes = baos.toByteArray();

            System.out.println("✅ Imagen comprimida. Original: " + originalBytes.length + " bytes | Final: " + finalBytes.length + " bytes");
            return finalBytes;

        } catch (Exception ex) {
            System.err.println("❌ No se pudo comprimir. Se subirá imagen original. Tipo detectado: " + tipoReal);
            return originalBytes;
        }
    }

    @Transactional
    @GetMapping("/listar")
    public String listarTrabajos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:auth/login";  // Redirigir si no está autenticado
        }

        Empresa empresaActual = usuario.getGrupo().getEmpresa();

        if ("INACTIVO".equals(empresaActual.getEstado())) {
            return "payments/index";
        }

        List<Trabajo> trabajos;
        if (usuario.getRol().getNombre().equals("ADMIN")) {
            trabajos = trabajoRepository.findByUsuario_Grupo_Empresa(usuario.getGrupo().getEmpresa());
        } else {
            trabajos = trabajoRepository.findByUsuario_Grupo(usuario.getGrupo()); // Usuario normal solo ve los suyos
        }

        model.addAttribute("trabajos", trabajos);
        return "trabajos/listar_trabajos";
    }


    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Integer id, Model model) {
        Trabajo trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajo no encontrado con ID: " + id));
        model.addAttribute("trabajo", trabajo);
        return "trabajos/detalle_trabajo";
    }

    @GetMapping("/reportes")
    public String mostrarReportes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Model model, HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:auth/login";
        }

        // Si no se selecciona ninguna fecha, usar el mes actual por defecto
        if (fechaInicio == null || fechaFin == null) {
            LocalDate today = LocalDate.now();
            fechaInicio = today.withDayOfMonth(1);  // Primer día del mes
            fechaFin = today.withDayOfMonth(today.lengthOfMonth());  // Último día del mes
        }

        BigDecimal totalVentas;
        BigDecimal totalGanancias;

        if (usuario.getRol().getNombre().equals("ADMIN")) {
            totalVentas = trabajoRepository.sumarVentasPorEmpresaEntreFechas(usuario.getGrupo().getEmpresa(), fechaInicio, fechaFin);
            totalGanancias = trabajoRepository.sumarGananciasPorEmpresaEntreFechas(usuario.getGrupo().getEmpresa(), fechaInicio, fechaFin);
        } else {
            totalVentas = trabajoRepository.sumarVentasPorGrupoEntreFechas(usuario.getGrupo(), fechaInicio, fechaFin);
            totalGanancias = trabajoRepository.sumarGananciasPorGrupoEntreFechas(usuario.getGrupo(), fechaInicio, fechaFin);
        }

        model.addAttribute("totalVentas", totalVentas != null ? totalVentas : BigDecimal.ZERO);
        model.addAttribute("totalGanancias", totalGanancias != null ? totalGanancias : BigDecimal.ZERO);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        return "trabajos/reportes";
    }

    @Transactional
    @GetMapping("/exportar-excel")
    public void exportarTrabajosExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            HttpServletResponse response, HttpSession session) throws IOException {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            response.sendRedirect("/login");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String fechaFormateada = LocalDateTime.now().format(formatter);

        String nombreArchivo = "reporte_" + fechaFormateada + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="+nombreArchivo);

        // Si no se selecciona ninguna fecha, usar el mes actual como filtro por defecto
        if (fechaInicio == null || fechaFin == null) {
            LocalDate today = LocalDate.now();
            fechaInicio = today.withDayOfMonth(1);  // Primer día del mes
            fechaFin = today.withDayOfMonth(today.lengthOfMonth());  // Último día del mes
        }

        List<Trabajo> trabajos;
        Empresa empresaActual = usuario.getGrupo().getEmpresa();

        if (usuario.getRol().getNombre().equals("ADMIN")) {
            trabajos = trabajoRepository.findByUsuario_Grupo_EmpresaAndFechaBetween(empresaActual, fechaInicio, fechaFin);
        } else {
            trabajos = trabajoRepository.findByUsuarioAndFechaBetween(usuario, fechaInicio, fechaFin);
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Trabajos");

        Row headerRow = sheet.createRow(0);
        String[] columnas = {"ID", "Fecha", "Cliente", "Valor Labor", "Valor Materiales", "Ganancias", "Valor Total", "Realizado por"};

        for (int i = 0; i < columnas.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnas[i]);
        }

        int rowNum = 1;
        BigDecimal totalLabor = BigDecimal.ZERO;
        BigDecimal totalMateriales = BigDecimal.ZERO;
        BigDecimal totalGanancias = BigDecimal.ZERO;
        BigDecimal totalValorTotal = BigDecimal.ZERO;

        for (Trabajo trabajo : trabajos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(trabajo.getId());
            row.createCell(1).setCellValue(trabajo.getFecha().toString());
            row.createCell(2).setCellValue(trabajo.getCliente().getNombre());

            BigDecimal valorLabor = trabajo.getValorLabor() != null ? trabajo.getValorLabor() : BigDecimal.ZERO;
            BigDecimal valorMateriales = trabajo.getValorMateriales() != null ? trabajo.getValorMateriales() : BigDecimal.ZERO;
            BigDecimal ganancias = trabajo.getGanancias() != null ? trabajo.getGanancias() : BigDecimal.ZERO;
            BigDecimal valorTotal = trabajo.getValorTotal() != null ? trabajo.getValorTotal() : BigDecimal.ZERO;

            row.createCell(3).setCellValue(valorLabor.doubleValue());
            row.createCell(4).setCellValue(valorMateriales.doubleValue());
            row.createCell(5).setCellValue(ganancias.doubleValue());
            row.createCell(6).setCellValue(valorTotal.doubleValue());
            row.createCell(7).setCellValue(trabajo.getUsuario() != null ? trabajo.getUsuario().getUsuario() : "Desconocido");

            // Acumular los totales
            totalLabor = totalLabor.add(valorLabor);
            totalMateriales = totalMateriales.add(valorMateriales);
            totalGanancias = totalGanancias.add(ganancias);
            totalValorTotal = totalValorTotal.add(valorTotal);
        }

        // Agregar la fila de totales al final
        Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(2).setCellValue("Total:");
        totalRow.createCell(3).setCellValue(totalLabor.doubleValue());
        totalRow.createCell(4).setCellValue(totalMateriales.doubleValue());
        totalRow.createCell(5).setCellValue(totalGanancias.doubleValue());
        totalRow.createCell(6).setCellValue(totalValorTotal.doubleValue());

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @DeleteMapping("/eliminar/{id}")
    @Transactional
    public String eliminarTrabajo(@PathVariable Integer  id, HttpSession session, HttpServletResponse response) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "redirect:/auth/login";
        }

        Trabajo trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajo no encontrado con ID: " + id));

        if (!usuario.getRol().getNombre().equals("ADMIN") && !trabajo.getUsuario().getId().equals(usuario.getId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return "redirect:/trabajos/listar?error=No tienes permisos para eliminar este trabajo";
        }

        trabajoRepository.delete(trabajo);
        response.setStatus(HttpServletResponse.SC_OK);
        return "redirect:/trabajos/listar";
    }

}