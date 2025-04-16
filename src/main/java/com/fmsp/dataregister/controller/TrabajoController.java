package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.entity.*;
import com.fmsp.dataregister.entity.dto.TrabajoDTO;
import com.fmsp.dataregister.entity.dto.UsuarioSesionDTO;
import com.fmsp.dataregister.repository.*;
import com.fmsp.dataregister.service.impl.PdfService;
import com.fmsp.dataregister.service.impl.S3Service;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final UsuarioRepository usuarioRepository;
    private final ReporteRepository reporteRepository;
    private final S3Service s3Service;

    public TrabajoController(TrabajoRepository trabajoRepository, ClienteRepository clienteRepository,
                             FormaPagoRepository formaPagoRepository, UsuarioRepository usuarioRepository,
                             ReporteRepository reporteRepository, S3Service s3Service) {
        this.trabajoRepository = trabajoRepository;
        this.clienteRepository = clienteRepository;
        this.formaPagoRepository = formaPagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.reporteRepository = reporteRepository;
        this.s3Service = s3Service;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("foto1", "foto2", "foto3", "foto4");
    }

    @GetMapping("/registro")
    public String mostrarFormulario(@RequestParam(required = false) Map<String, String> params, Model model, HttpSession session) {

        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null) {
            return "redirect:auth/login";
        }

        Usuario usuario = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();
        Empresa empresaActual = usuario.getGrupo().getEmpresa();

        if ("INACTIVO".equals(empresaActual.getEstado())) {
            return "payments/index";
        }

        boolean hayClientesEnEmpresa = clienteRepository.existsByUsuario_Grupo_Empresa(empresaActual);
        boolean hayClientesEnGrupo = clienteRepository.existsByUsuario_Grupo(usuario.getGrupo());

        List<Cliente> clientes;
        if (usuario.getRol().getNombre().equals("ADMIN")) {
            if (!hayClientesEnEmpresa) {
                return "redirect:/clientes/nuevo?error=Debe crear al menos un cliente antes de registrar trabajos.";
            }
            clientes = clienteRepository.findByEmpresa(empresaActual);  // ADMIN ve todos los clientes
        } else {
            if (!hayClientesEnEmpresa || !hayClientesEnGrupo) {
                return "redirect:/clientes/nuevo?error=Debe crear al menos un cliente antes de registrar trabajos.";
            }
            clientes = clienteRepository.findByGrupo(usuario.getGrupo()); // Usuario normal solo ve los suyos
        }
        List<FormaPago> formasPago = formaPagoRepository.findByEmpresaAndEstado(empresaActual, 1);

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
                                 HttpSession session, RedirectAttributes redirectAttributes) {
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
            UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
            if (usuarioDto != null) {
                Usuario usuario = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();
                trabajo.setUsuario(usuario);
            }

        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al guardar el trabajo.");
            return "redirect:/trabajos/registro?error";
        }

        trabajoRepository.save(trabajo);
        redirectAttributes.addFlashAttribute("success", "¡Trabajo guardado con éxito!");
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
        UsuarioSesionDTO usuarioLogueado = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return "redirect:auth/login";  // Redirigir si no está autenticado
        }
        Usuario usuario = usuarioRepository.findById(usuarioLogueado.getId()).orElseThrow();
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

        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null) {
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

        Usuario usuario = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();

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

        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null) {
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
        Usuario usuario = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();
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

    @PostMapping("/eliminar/{id}")
    @Transactional
    public String eliminarTrabajo(@PathVariable Integer id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null) {
            return "redirect:/auth/login";
        }

        Trabajo trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajo no encontrado con ID: " + id));

        // Validar permisos
        Usuario usuario = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();
        if (!trabajo.getUsuario().getId().equals(usuario.getId())) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para eliminar este trabajo");
            return "redirect:/trabajos/listar";
        }

        try {
            if (trabajo.getFoto1() != null) s3Service.deleteFile(trabajo.getFoto1());
            if (trabajo.getFoto2() != null) s3Service.deleteFile(trabajo.getFoto2());
            if (trabajo.getFoto3() != null) s3Service.deleteFile(trabajo.getFoto3());
            if (trabajo.getFoto4() != null) s3Service.deleteFile(trabajo.getFoto4());
        } catch (Exception ex) {
            System.err.println("⚠️ Error al eliminar imágenes del S3: " + ex.getMessage());
        }

        trabajoRepository.delete(trabajo);
        redirectAttributes.addFlashAttribute("success", "Trabajo eliminado correctamente");
        return "redirect:/trabajos/listar";
    }


    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(@PathVariable Integer id, Model model, HttpSession session) {
        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null) return "redirect:/auth/login";

        Trabajo trabajo = trabajoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajo no encontrado con ID: " + id));

        Usuario usuario = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();
        if (!usuario.getRol().getNombre().equals("ADMIN") &&
                !trabajo.getUsuario().getId().equals(usuario.getId())) {
            return "redirect:/trabajos/listar?error=No tienes permisos para editar este trabajo";
        }

        List<Cliente> clientes = usuario.getRol().getNombre().equals("ADMIN") ?
                clienteRepository.findByEmpresa(usuario.getGrupo().getEmpresa()) :
                clienteRepository.findByGrupo(usuario.getGrupo());

        List<FormaPago> formasPago = formaPagoRepository.findByEmpresaAndEstado(usuario.getGrupo().getEmpresa(), 1);

        model.addAttribute("trabajo", trabajo);
        model.addAttribute("clientes", clientes);
        model.addAttribute("formasPago", formasPago);

        return "trabajos/editar_trabajo";
    }

    @PostMapping("/actualizar")
    @Transactional
    public String actualizarTrabajo(@ModelAttribute Trabajo trabajo,
                                    @RequestParam Map<String, String> params,
                                    @RequestParam(value = "foto1", required = false) MultipartFile nuevaFoto1,
                                    @RequestParam(value = "foto2", required = false) MultipartFile nuevaFoto2,
                                    @RequestParam(value = "foto3", required = false) MultipartFile nuevaFoto3,
                                    @RequestParam(value = "foto4", required = false) MultipartFile nuevaFoto4,
                                    HttpSession session) {

        Trabajo original = trabajoRepository.findById(trabajo.getId())
                .orElseThrow(() -> new IllegalArgumentException("Trabajo no encontrado"));

        original.setValorLabor(trabajo.getValorLabor());
        original.setValorMateriales(trabajo.getValorMateriales());
        original.setGanancias(trabajo.getGanancias());
        original.setValorTotal(trabajo.getValorTotal());
        original.setDescripcionLabor(trabajo.getDescripcionLabor());
        original.setCliente(trabajo.getCliente());
        original.setFormaPago(trabajo.getFormaPago());

        try {
            if ("on".equals(params.get("eliminarFoto1")) && original.getFoto1() != null) {
                s3Service.deleteFile(original.getFoto1());
                original.setFoto1(null);
            }
            if ("on".equals(params.get("eliminarFoto2")) && original.getFoto2() != null) {
                s3Service.deleteFile(original.getFoto2());
                original.setFoto2(null);
            }
            if ("on".equals(params.get("eliminarFoto3")) && original.getFoto3() != null) {
                s3Service.deleteFile(original.getFoto3());
                original.setFoto3(null);
            }
            if ("on".equals(params.get("eliminarFoto4")) && original.getFoto4() != null) {
                s3Service.deleteFile(original.getFoto4());
                original.setFoto4(null);
            }

            if (nuevaFoto1 != null && !nuevaFoto1.isEmpty()) {
                original.setFoto1(s3Service.uploadFile("trabajos/foto1_", comprimirImagen(nuevaFoto1), "image/jpeg"));
            }
            if (nuevaFoto2 != null && !nuevaFoto2.isEmpty()) {
                original.setFoto2(s3Service.uploadFile("trabajos/foto2_", comprimirImagen(nuevaFoto2), "image/jpeg"));
            }
            if (nuevaFoto3 != null && !nuevaFoto3.isEmpty()) {
                original.setFoto3(s3Service.uploadFile("trabajos/foto3_", comprimirImagen(nuevaFoto3), "image/jpeg"));
            }
            if (nuevaFoto4 != null && !nuevaFoto4.isEmpty()) {
                original.setFoto4(s3Service.uploadFile("trabajos/foto4_", comprimirImagen(nuevaFoto4), "image/jpeg"));
            }

            trabajoRepository.save(original);
            return "redirect:/trabajos/listar?success";

        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/trabajos/listar?error=actualizacion";
        }
    }

    @GetMapping("/factura/{id}")
    public void generarFactura(@PathVariable Integer id, HttpServletResponse response) {
        try {
            Trabajo trabajo = trabajoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Trabajo no encontrado con ID: " + id));

            // Obtener reporte asociado a la empresa (puedes ajustar esta lógica según tu diseño)
            Reporte reporte = reporteRepository.findTopByEmpresa(trabajo.getUsuario().getGrupo().getEmpresa());
            if (reporte == null) {
                throw new IllegalStateException("No se encontró un reporte configurado para esta empresa.");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfService.generarFacturaTrabajo(trabajo, reporte, baos);

            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            String nombreArchivo = "detalle_trabajo_" + id + "_" + fechaActual + ".pdf";

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename= "+ nombreArchivo);
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/buscar")
    public String buscarPorCliente(@RequestParam("cliente") String cliente,
                                   Model model,
                                   HttpSession session) {

        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null) {
            return "redirect:auth/login";
        }

        List<Trabajo> trabajos = trabajoRepository.findByCliente_NombreContainingIgnoreCase(cliente);
        model.addAttribute("trabajos", trabajos);
        model.addAttribute("clienteBuscado", cliente);
        return "trabajos/listar_trabajos";
    }

    @GetMapping("/listarAjax")
    @ResponseBody
    public Page<TrabajoDTO> listarAjax(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(required = false) String cliente,
                                       HttpSession session) {
        UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");
        if (usuarioDto == null) return Page.empty();

        Pageable pageable = PageRequest.of(page, 10, Sort.by("fecha").descending());
        Usuario user = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();

        Page<Trabajo> trabajos = cliente != null && !cliente.isBlank()
                ? (user.getRol().getNombre().equals("ADMIN")
                ? trabajoRepository.findByCliente_NombreContainingIgnoreCaseAndUsuario_Grupo_Empresa(cliente, user.getGrupo().getEmpresa(), pageable)
                : trabajoRepository.findByCliente_NombreContainingIgnoreCaseAndUsuario_Grupo(cliente, user.getGrupo(), pageable))
                : (user.getRol().getNombre().equals("ADMIN")
                ? trabajoRepository.findByUsuario_Grupo_Empresa(user.getGrupo().getEmpresa(), pageable)
                : trabajoRepository.findByUsuario_Grupo(user.getGrupo(), pageable));

        return trabajos.map(t -> new TrabajoDTO(
                Math.toIntExact(t.getId()),
                t.getFecha().toString(),
                t.getCliente().getNombre(),
                t.getValorTotal(),
                t.getUsuario().getUsuario()
        ));
    }


}