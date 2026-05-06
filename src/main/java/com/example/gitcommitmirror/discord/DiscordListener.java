package com.example.gitcommitmirror.discord;

import com.example.gitcommitmirror.scheduler.ScheduledCommitService;
import com.example.gitcommitmirror.service.GitAutomationService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class DiscordListener extends ListenerAdapter {

    // Mapa de dias: aceita PT-BR (DOM/SEG/...) e inglês (SUN/MON/...) → cron EN
    private static final Map<String, String> DIA_SEMANA_MAP = new java.util.HashMap<>(Map.of(
            "DOM", "SUN", "SEG", "MON", "TER", "TUE", "QUA", "WED",
            "QUI", "THU", "SEX", "FRI", "SAB", "SAT"));
    static {
        // Inglês já é o próprio valor de cron — mapeia para si mesmo
        DIA_SEMANA_MAP.put("SUN", "SUN");
        DIA_SEMANA_MAP.put("MON", "MON");
        DIA_SEMANA_MAP.put("TUE", "TUE");
        DIA_SEMANA_MAP.put("WED", "WED");
        DIA_SEMANA_MAP.put("THU", "THU");
        DIA_SEMANA_MAP.put("FRI", "FRI");
        DIA_SEMANA_MAP.put("SAT", "SAT");
    }

    private final GitAutomationService gitAutomationService;

    // Null-safe: o bean pode não existir se o agendador estiver desabilitado
    private final ScheduledCommitService scheduledCommitService;

    @Value("${discord.bot.channel-id:0}")
    private String configuredChannelId;

    @Autowired
    public DiscordListener(GitAutomationService gitAutomationService,
            Optional<ScheduledCommitService> scheduledCommitService) {
        this.gitAutomationService = gitAutomationService;
        this.scheduledCommitService = scheduledCommitService.orElse(null);
    }

    // Mensagem de boas-vindas no canal configurado
    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("🤖 Bot iniciado com sucesso! Aguardando slash commands...");
        if (!configuredChannelId.equals("0")) {
            TextChannel channel = event.getJDA().getTextChannelById(configuredChannelId);
            if (channel != null) {
                channel.sendMessage(
                        "🤖 **Bot Iniciado com Sucesso!**\n\n" +
                                "**Comandos disponíveis:**\n" +
                                "`/commit mensagem:<texto>` — Cria um commit no GitHub.\n" +
                                "`/agendamento` — Configura o agendador automático de commits.\n\n" +
                                "> ℹ️ As respostas do bot são **efêmeras** — só você verá.")
                        .queue();
            } else {
                System.out.println("Aviso: Canal ID " + configuredChannelId + " não encontrado.");
            }
        }
    }

    // SLASH COMMANDS
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "commit" -> handleCommit(event);
            case "agendamento" -> handleAgendamento(event);
        }
    }

    private void handleCommit(SlashCommandInteractionEvent event) {
        String commitMessage = event.getOption("mensagem").getAsString().trim();
        if (commitMessage.isEmpty()) {
            event.reply("❌ A mensagem do commit não pode estar vazia.")
                    .setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();
        try {
            gitAutomationService.createCommit(commitMessage);
            event.getHook()
                    .editOriginal("✅ Commit criado e enviado com sucesso!\n> `" + commitMessage + "`")
                    .queue();
        } catch (Exception e) {
            event.getHook()
                    .editOriginal("❌ Falha ao criar o commit: " + e.getMessage())
                    .queue();
        }
    }

    private void handleAgendamento(SlashCommandInteractionEvent event) {
        if (scheduledCommitService == null) {
            event.reply("⚠️ O agendador está desabilitado.\n" +
                    "Defina `commit.mirror.scheduler.enabled=true` no `application.properties` e reinicie o bot.")
                    .setEphemeral(true).queue();
            return;
        }

        // Monta o SelectMenu com as três modalidades de agendamento
        StringSelectMenu menu = StringSelectMenu.create("menu_agendamento")
                .setPlaceholder("Escolha o tipo de agendamento...")
                .addOption("📅 Diário", "diario", "Commit todo dia em um horário fixo")
                .addOption("🗓️ Semanal", "semanal", "Commit uma vez por semana")
                .addOption("🔁 Sequencial (Intervalo)", "sequencial", "Commit a cada N horas e/ou minutos")
                .build();

        event.reply("**🔧 Configurar Agendamento Automático**\nEscolha o tipo de agendamento desejado:")
                .addActionRow(menu)
                .setEphemeral(true)
                .queue();
    }

    // STRING SELECT MENU
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("menu_agendamento"))
            return;

        String escolha = event.getValues().get(0);

        Modal modal = switch (escolha) {
            case "diario" -> Modal.create("modal_diario", "⏰ Configurar Agendamento Diário")
                    .addComponents(
                            ActionRow.of(
                                    TextInput.create("horario", "Horário (HH:mm)", TextInputStyle.SHORT)
                                            .setPlaceholder("Ex: 20:00")
                                            .setMinLength(4)
                                            .setMaxLength(5)
                                            .setRequired(true)
                                            .build()))
                    .build();

            case "semanal" -> Modal.create("modal_semanal", "📅 Configurar Agendamento Semanal")
                    .addComponents(
                            ActionRow.of(
                                    TextInput.create("dia_semana", "Dia da Semana", TextInputStyle.SHORT)
                                            .setPlaceholder("Ex: DOM, SEG, TER...")
                                            .setMinLength(3)
                                            .setMaxLength(4)
                                            .setRequired(true)
                                            .build()),
                            ActionRow.of(
                                    TextInput.create("horario", "Horário (HH:mm)", TextInputStyle.SHORT)
                                            .setPlaceholder("Ex: 08:30")
                                            .setMinLength(4)
                                            .setMaxLength(5)
                                            .setRequired(true)
                                            .build()))
                    .build();

            case "sequencial" -> Modal.create("modal_sequencial", "🔁 Configurar Intervalo")
                    .addComponents(
                            ActionRow.of(
                                    TextInput.create("horas", "Horas (0–23)", TextInputStyle.SHORT)
                                            .setPlaceholder("Ex: 2  (deixe 0 se não quiser horas)")
                                            .setMinLength(1)
                                            .setMaxLength(2)
                                            .setRequired(true)
                                            .build()),
                            ActionRow.of(
                                    TextInput.create("minutos", "Minutos (0–59)", TextInputStyle.SHORT)
                                            .setPlaceholder("Ex: 30  (deixe 0 se não quiser minutos)")
                                            .setMinLength(1)
                                            .setMaxLength(2)
                                            .setRequired(true)
                                            .build()))
                    .build();

            default -> null;
        };

        if (modal != null) {
            event.replyModal(modal).queue();
        }
    }

    // MODAL INTERACTION
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        switch (event.getModalId()) {
            case "modal_diario" -> handleModalDiario(event);
            case "modal_semanal" -> handleModalSemanal(event);
            case "modal_sequencial" -> handleModalSequencial(event);
        }
    }

    private void handleModalDiario(ModalInteractionEvent event) {
        String horarioRaw = event.getValue("horario").getAsString().trim();
        try {
            int[] hm = parseHorario(horarioRaw);
            String descricao = scheduledCommitService.updateDaily(hm[0], hm[1]);
            event.reply("✅ Agendamento atualizado: **" + descricao + "**")
                    .setEphemeral(true).queue();
        } catch (IllegalArgumentException e) {
            event.reply("❌ Horário inválido: " + e.getMessage() +
                    "\nFormato esperado: `HH:mm` (ex: `20:00`)")
                    .setEphemeral(true).queue();
        }
    }

    private void handleModalSemanal(ModalInteractionEvent event) {
        String diaRaw = event.getValue("dia_semana").getAsString().trim().toUpperCase();
        String horarioRaw = event.getValue("horario").getAsString().trim();

        // Valida o dia da semana
        String diaCron = DIA_SEMANA_MAP.get(diaRaw);
        if (diaCron == null) {
            event.reply("❌ Dia da semana inválido: `" + diaRaw + "`\n" +
                    "Use: `DOM`, `SEG`, `TER`, `QUA`, `QUI`, `SEX`, `SAB` (em português).\n" +
                    "Ou: `SUN`, `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT` (em inglês).")
                    .setEphemeral(true).queue();
            return;
        }

        try {
            int[] hm = parseHorario(horarioRaw);
            String descricao = scheduledCommitService.updateWeekly(diaCron, hm[0], hm[1]);
            event.reply("✅ Agendamento atualizado: **" + descricao + "**")
                    .setEphemeral(true).queue();
        } catch (IllegalArgumentException e) {
            event.reply("❌ Horário inválido: " + e.getMessage() +
                    "\nFormato esperado: `HH:mm` (ex: `08:30`)")
                    .setEphemeral(true).queue();
        }
    }

    private void handleModalSequencial(ModalInteractionEvent event) {
        String horasRaw = event.getValue("horas").getAsString().trim();
        String minutosRaw = event.getValue("minutos").getAsString().trim();

        int horas, minutos;
        try {
            horas = Integer.parseInt(horasRaw);
            minutos = Integer.parseInt(minutosRaw);
        } catch (NumberFormatException e) {
            event.reply("❌ Valores inválidos. Certifique-se de digitar apenas **números inteiros**.\n" +
                    "Exemplos: Horas `2`, Minutos `30`")
                    .setEphemeral(true).queue();
            return;
        }

        // Validações de range
        if (horas < 0 || minutos < 0) {
            event.reply("❌ Os valores não podem ser negativos.")
                    .setEphemeral(true).queue();
            return;
        }
        if (horas > 23 || minutos > 59) {
            event.reply("❌ Valores fora do intervalo.\n" +
                    "Horas: `0–23` | Minutos: `0–59`")
                    .setEphemeral(true).queue();
            return;
        }

        int totalMinutos = horas * 60 + minutos;

        // Limite máximo: 23h59min = 1439 minutos
        if (totalMinutos > 1439) {
            event.reply("❌ O intervalo total não pode ultrapassar **23h 59min**.")
                    .setEphemeral(true).queue();
            return;
        }
        // Mínimo: pelo menos 1 minuto
        if (totalMinutos < 1) {
            event.reply("❌ O intervalo mínimo é de **1 minuto**. Informe ao menos 1 hora ou 1 minuto.")
                    .setEphemeral(true).queue();
            return;
        }

        String descricao = scheduledCommitService.updateInterval(totalMinutos);
        event.reply("✅ Agendamento atualizado: **" + descricao + "**")
                .setEphemeral(true).queue();
    }

    // Utilitários
    private int[] parseHorario(String horario) {
        String[] partes = horario.split(":");
        if (partes.length != 2) {
            throw new IllegalArgumentException("formato deve ser `HH:mm` (ex: `20:00`)");
        }
        try {
            int hour = Integer.parseInt(partes[0].trim());
            int minute = Integer.parseInt(partes[1].trim());
            if (hour < 0 || hour > 23) {
                throw new IllegalArgumentException("hora deve estar entre 0 e 23, recebido: " + hour);
            }
            if (minute < 0 || minute > 59) {
                throw new IllegalArgumentException("minuto deve estar entre 0 e 59, recebido: " + minute);
            }
            return new int[] { hour, minute };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("hora e minuto devem ser números inteiros");
        }
    }
}