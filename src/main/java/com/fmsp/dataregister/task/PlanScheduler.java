package com.fmsp.dataregister.task;

import com.fmsp.dataregister.entity.Empresa;
import com.fmsp.dataregister.entity.Plan;
import com.fmsp.dataregister.repository.EmpresaRepository;
import com.fmsp.dataregister.repository.PlanRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PlanScheduler {

    private final PlanRepository planRepository;
    private final EmpresaRepository empresaRepository;

    public PlanScheduler(PlanRepository planRepository, EmpresaRepository empresaRepository) {
        this.planRepository = planRepository;
        this.empresaRepository = empresaRepository;
    }

    @Scheduled(cron = "0 0 0 * * *") // Se ejecuta a las 00:00 cada día
    @Transactional
    public void actualizarEstadoEmpresas() {
        LocalDate hoy = LocalDate.now();
        LocalDate haceCincoDias = hoy.minusDays(5);

        // Obtener todos los planes cuya fecha de vigencia ha vencido
        List<Plan> planes = planRepository.findByFechaVigenciaBefore(hoy);

        for (Plan plan : planes) {
            Empresa empresa = plan.getEmpresa();
            if (empresa != null) {
                // Si la fecha de vigencia es mayor a hace 5 días, poner "INACTIVO"
                if (plan.getFechaVigencia().isBefore(haceCincoDias)) {
                    empresa.setEstado("INACTIVO");
                } else {
                    // Si la fecha de vigencia pasó pero no han pasado 5 días, poner "PENDIENTE"
                    empresa.setEstado("PENDIENTE");
                }
                empresaRepository.save(empresa);
            }
        }
    }
}
