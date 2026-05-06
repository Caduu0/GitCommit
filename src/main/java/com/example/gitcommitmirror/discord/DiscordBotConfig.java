package com.example.gitcommitmirror.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordBotConfig {

    @Value("${discord.bot.token}")
    private String token;

    @Bean
    public JDA jda(DiscordListener discordListener) throws InterruptedException {
        if (token == null || token.isEmpty() || token.equals("${DISCORD_BOT_TOKEN}")) {
            System.out.println("Discord token not configured. Skipping JDA initialization.");
            return null;
        }

        try {
            JDA jda = JDABuilder.createLight(token, GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(discordListener)
                    .build();

            jda.awaitReady();

            // Registra os slash commands
            jda.updateCommands()
                    .addCommands(
                            Commands.slash("commit", "Cria um commit no repositório do GitHub")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "mensagem",
                                                    "A mensagem do commit a ser registrada no GitHub", true)
                                    ),
                            Commands.slash("agendamento", "Configura o agendador automático de commits")
                    )
                    .queue(cmds -> System.out.println("✅ Slash commands registrados: " + cmds.size() + " comando(s)."));

            return jda;

        } catch (Exception e) {
            System.err.println("Failed to start Discord Bot: " + e.getMessage());
            return null;
        }
    }
}
