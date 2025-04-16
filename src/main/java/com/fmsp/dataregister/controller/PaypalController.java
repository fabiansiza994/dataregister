package com.fmsp.dataregister.controller;

import com.fmsp.dataregister.config.paypal.PaypalService;
import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Pago;
import com.fmsp.dataregister.entity.Plan;
import com.fmsp.dataregister.entity.Usuario;
import com.fmsp.dataregister.entity.dto.UsuarioSesionDTO;
import com.fmsp.dataregister.repository.EmpresaRepository;
import com.fmsp.dataregister.repository.PagoRepository;
import com.fmsp.dataregister.repository.PlanRepository;
import com.fmsp.dataregister.repository.UsuarioRepository;
import com.fmsp.dataregister.util.LoadDataConfig;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class PaypalController {

    private final PaypalService paypalService;
    private final PlanRepository planRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final LoadDataConfig loadDataConfig;

    public PaypalController(PaypalService paypalService, PlanRepository planRepository, EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository, PagoRepository pagoRepository, LoadDataConfig loadDataConfig) {
        this.paypalService = paypalService;
        this.planRepository = planRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.loadDataConfig = loadDataConfig;
    }

    @GetMapping("/index")
    public String home() {
        return "payment/index";
    }

    @PostMapping("/payment/create")
    public RedirectView create() {
        try {
            String cancelUrl = loadDataConfig.getCancelUrl();
            String successUrl = loadDataConfig.getSuccessUrl();
            Payment payment = paypalService.createPayment(Double.valueOf(loadDataConfig.paypalValue),
                    loadDataConfig.paypalCurrency, "paypal",
                    "sale", "Payment description", cancelUrl, successUrl);
            for(Links link : payment.getLinks()) {
                if(link.getRel().equals("approval_url")) {
                    return new RedirectView(link.getHref());
                }
            }
        }catch (PayPalRESTException e) {
            System.out.println("error ocurred:: "+e.getMessage());
        }
        return new RedirectView("/payment/error");
    }

    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam("paymentId") String paymentId,
                                 @RequestParam("PayerID") String payerId, HttpSession session) {

        try {
            Payment payment = paypalService.executePayment(paymentId, payerId);

            if (payment.getState().equalsIgnoreCase("approved")) {
                // Obtener el usuario logueado
                UsuarioSesionDTO usuarioDto = (UsuarioSesionDTO) session.getAttribute("usuarioLogueado");

                Usuario usuario = usuarioRepository.findById(usuarioDto.getId()).orElseThrow();
                if (usuario != null && usuario.getGrupo() != null && usuario.getGrupo().getEmpresa() != null) {
                    Empresa empresa = usuario.getGrupo().getEmpresa();

                    // Buscar el plan actual de la empresa
                    Plan plan = planRepository.findByEmpresa(empresa);

                    if (plan != null) {
                        // Actualizar la fecha de vigencia sumando un mes
                        LocalDate nuevaFechaVigencia = plan.getFechaVigencia().plusMonths(1);
                        plan.setFechaVigencia(nuevaFechaVigencia);
                        planRepository.save(plan);
                    }

                    // Cambiar el estado de la empresa a ACTIVO
                    empresa.setEstado("ACTIVO");
                    empresaRepository.save(empresa);

                    // Obtener detalles del pago de PayPal
                    String transactionId = payment.getTransactions().get(0).getRelatedResources().get(0).getSale().getId();
                    BigDecimal monto = new BigDecimal(payment.getTransactions().get(0).getAmount().getTotal());
                    String moneda = payment.getTransactions().get(0).getAmount().getCurrency();
                    String estado = payment.getState();

                    // Guardar el pago en la base de datos
                    Pago nuevoPago = new Pago(
                            paymentId,
                            usuario,
                            empresa,
                            monto,
                            moneda,
                            estado,
                            LocalDateTime.now(),
                            transactionId
                    );
                    pagoRepository.save(nuevoPago);
                }

                return "payments/paymentSuccess";
            }
        } catch (PayPalRESTException e) {
            System.out.println("Error occurred: " + e.getMessage());
        }

        return "payments/paymentSuccess";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel() {
        return "payments/paymentCancel";
    }

    @GetMapping("/payment/error")
    public String paymentError() {
        return "payments/paymentError";
    }
}
