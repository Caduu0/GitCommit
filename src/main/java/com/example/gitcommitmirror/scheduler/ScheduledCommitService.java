package com.example.gitcommitmirror.scheduler;

import com.example.gitcommitmirror.service.GitAutomationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// Serviço de agendamento dinâmico de commits.
// Ativo apenas quando commit.mirror.scheduler.enabled=true.
@ConditionalOnProperty(name = "commit.mirror.scheduler.enabled", havingValue = "true", matchIfMissing = false)
@Service
public class ScheduledCommitService {

    private final GitAutomationService gitAutomationService;
    private final TaskScheduler taskScheduler;

    @Value("${commit.mirror.scheduler.cron}")
    private String initialCron;

    // Referência à tarefa em execução — null se nenhuma estiver ativa.
    private ScheduledFuture<?> currentTask;

    @Autowired
    public ScheduledCommitService(GitAutomationService gitAutomationService,
            TaskScheduler taskScheduler) {
        this.gitAutomationService = gitAutomationService;
        this.taskScheduler = taskScheduler;
    }

    // Dispara o agendamento inicial com a cron do application.properties.
    @PostConstruct
    public void startInitialSchedule() {
        System.out.println("⏰ Agendador iniciado com cron padrão: " + initialCron);
        scheduleCron(initialCron);
    }

    // API pública — chamada pelo DiscordListener após validação do Modal

    // Agenda commits diários no horário informado (ex: 20h30 → "0 30 20 * * *").
    public synchronized String updateDaily(int hour, int minute) {
        String cron = String.format("0 %d %d * * *", minute, hour);
        scheduleCron(cron);
        return String.format("Diário às %02d:%02d", hour, minute);
    }

    // Agenda commits semanais em um dia e horário específicos.
    public synchronized String updateWeekly(String dayOfWeek, int hour, int minute) {
        // Spring aceita nomes em inglês abreviados: SUN, MON, TUE, WED, THU, FRI, SAT
        String cron = String.format("0 %d %d * * %s", minute, hour, dayOfWeek.toUpperCase());
        scheduleCron(cron);
        return String.format("Semanal — %s às %02d:%02d", dayOfWeek.toUpperCase(), hour, minute);
    }

    // Agenda commits a cada N minutos usando PeriodicTrigger.
    public synchronized String updateInterval(int totalMinutes) {
        cancelCurrent();
        PeriodicTrigger trigger = new PeriodicTrigger(totalMinutes, TimeUnit.MINUTES);
        trigger.setFixedRate(true);
        currentTask = taskScheduler.schedule(this::executeCommit, trigger);
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        return String.format("Intervalo de %dh %02dmin", hours, mins);
    }

    // Internals
    private void scheduleCron(String cron) {
        cancelCurrent();
        currentTask = taskScheduler.schedule(this::executeCommit, new CronTrigger(cron));
    }

    private void cancelCurrent() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false); // false = aguarda a execução atual terminar
            System.out.println("🔄 Agendamento anterior cancelado.");
        }
    }

    private void executeCommit() {
        System.out.println("⏰ Executando commit agendado em " + LocalDateTime.now());
        try {
            gitAutomationService.createCommit("Automated scheduled mirror sync");
            System.out.println("✅ Commit agendado executado com sucesso.");
        } catch (Exception e) {
            System.err.println("❌ Falha no commit agendado: " + e.getMessage());
        }
    }
}