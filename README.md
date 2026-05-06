# Git Commit Mirror

Git Commit Mirror é uma aplicação construída com **Spring Boot**, **JGit** e **JDA** cujo objetivo é ouvir comandos de um bot do Discord e convertê-los em commits num repositório pessoal do GitHub, mantendo o gráfico de atividade sempre verde.

---

## 🚀 Funcionalidades Principais

| Funcionalidade | Descrição |
|---|---|
| **`/commit`** | Cria um commit no GitHub com a mensagem informada |
| **`/agendamento`** | Abre um menu interativo para configurar o agendador em tempo real |
| **Agendamento Diário** | Commit todo dia em um horário fixo (ex: 20:00) |
| **Agendamento Semanal** | Commit uma vez por semana em dia e horário escolhidos |
| **Agendamento Sequencial** | Commit a cada intervalo fixo (ex: a cada 2h30min) |
| **Respostas Efêmeras** | Feedback visível apenas para quem executou o comando |
| **Git Automatizado (JGit)** | Clone → pull → commit → push sem Git client instalado |
| **Pronto para Docker** | Multi-stage build com `Dockerfile` e `docker-compose.yml` |

---

## 🛠 Pré-requisitos

- **Java 17+** (apenas para rodar localmente sem Docker)
- **Docker e Docker Compose** (recomendado para implantação)
- **Conta no Discord** (para criar e gerenciar o bot)
- **Conta no GitHub** (para criar o repositório alvo e gerar o PAT)

---

## 🔑 Configuração: Obtendo as Credenciais

### 1. Token do Bot no Discord Developer Portal

1. Acesse o [Discord Developer Portal](https://discord.com/developers/applications).
2. Clique em **New Application** e dê um nome (ex: `CommitMirrorBot`).
3. No menu lateral, clique em **Bot** → **Reset Token**. Copie o token.
4. Role até **Privileged Gateway Intents**. Para Slash Commands **não é necessário** ativar `Message Content Intent`.
5. Vá em **OAuth2 → URL Generator**.
6. Selecione os escopos `bot` **e** `applications.commands` (obrigatório para slash commands).
7. Nas **Bot Permissions**, selecione `Send Messages`.
8. Copie a URL, cole no navegador e adicione o bot ao servidor.

> **⚠️ Atenção:** O escopo `applications.commands` é **obrigatório**. Sem ele, `/commit` e `/agendamento` não aparecerão no chat.

### 2. Personal Access Token (PAT) no GitHub

1. GitHub → [Settings](https://github.com/settings/profile) → **Developer settings** → **Personal access tokens → Tokens (classic)**.
2. Clique em **Generate new token (classic)**, marque o escopo **repo** e gere o token (começa com `ghp_`).

---

## ⚙️ Configurando a Aplicação

### Via `application.properties` (Desenvolvimento local / IDE)

```properties
# Discord
discord.bot.token=seu_token_do_discord_aqui
discord.bot.channel-id=0          # 0 = todos os canais

# GitHub / Git
github.repo.url=https://github.com/SeuUsuario/SeuRepo.git
github.pat=seu_pat_aqui
git.author.name=Seu Nome
git.author.email=seu@email.com
git.local.dir=./mirror-repo

# Spring Boot
server.port=8080

# Agendador
commit.mirror.scheduler.enabled=true          # true | false
commit.mirror.scheduler.cron=0 0 20 * * *    # Agendamento inicial (cron de 6 campos)
```

O agendamento definido em `commit.mirror.scheduler.cron` é apenas o **valor inicial** na startup. Uma vez que o bot estiver rodando, qualquer usuário com acesso ao canal pode reconfigurar o horário via `/agendamento` — sem reiniciar a aplicação.

### Formato da expressão Cron (6 campos — Spring)

```
segundos  minutos  horas  dia-do-mês  mês  dia-da-semana
   0         0      20       *         *        *        → Todo dia às 20:00
   0         30     8        *         *       MON-FRI   → Seg–Sex às 08:30
```

---

## 🐳 Buildando e Rodando com Docker Compose

```bash
docker-compose up -d --build         # Build + start em background
docker-compose logs -f git-commit-mirror  # Acompanhar logs
```

---

## 🎮 Como Usar

### `/commit`

```
/commit mensagem:Resolvendo issues de performance
```

1. Digite `/commit` no Discord — autocomplete exibe a opção `mensagem`.
2. Preencha e pressione **Enter**.
3. O bot responde com mensagem efêmera confirmando sucesso ✅ ou falha ❌.

---

### `/agendamento`

```
/agendamento
```

1. O bot exibe um **menu dropdown** com três opções:

#### 📅 Diário
- Selecione **Diário** → aparece um Modal com o campo **"Horário (HH:mm)"**.
- Validações: formato `HH:mm`, hora entre `00` e `23`, minuto entre `00` e `59`.
- Exemplo: `20:00` → commit todo dia às 20h.

#### 🗓️ Semanal
- Selecione **Semanal** → aparece um Modal com dois campos: **"Dia da Semana"** e **"Horário (HH:mm)"**.
- Dias aceitos: `SEGUNDA`, `TERCA`, `QUARTA`, `QUINTA`, `SEXTA`, `SABADO`, `DOMINGO`.
- Exemplo: `SEGUNDA` + `08:30` → commit toda segunda às 8h30.

#### 🔁 Sequencial (Intervalo)
- Selecione **Sequencial** → aparece um Modal com dois campos: **"Horas"** e **"Minutos"**.
- Preencha ambos ou apenas um (ex: `0` horas e `30` minutos).
- Limites: total entre **1 minuto** e **23h 59min** (1439 minutos).
- Exemplo: `2` horas e `30` minutos → commit a cada 2h30min.

> 💡 Todas as respostas são **efêmeras** — só o usuário que executou o comando as vê.

---

## 🏗 Arquitetura

```
src/main/java/com/example/gitcommitmirror/
├── GitCommitMirrorApplication.java
├── discord/
│   ├── DiscordBotConfig.java       — Cria o bean JDA, registra /commit e /agendamento
│   └── DiscordListener.java        — Trata slash commands, select menu e modals
├── scheduler/
│   ├── SchedulerConfig.java        — Expõe ThreadPoolTaskScheduler como bean
│   └── ScheduledCommitService.java — Agendamento dinâmico com ScheduledFuture
└── service/
    └── GitAutomationService.java   — JGit: clone → pull → commit → push
```

---

## 🔍 Notas Técnicas

### Fluxo do `/agendamento`

```
/agendamento
    └─→ StringSelectMenu (Diário | Semanal | Sequencial)
             └─→ Modal (campos específicos por tipo)
                      └─→ ModalInteractionEvent
                               ├─→ Validação (formato, range, limites)
                               └─→ ScheduledCommitService.updateDaily/Weekly/Interval()
                                        └─→ cancela ScheduledFuture anterior
                                        └─→ agenda novo via TaskScheduler
```

### Por que `ThreadPoolTaskScheduler` + `ScheduledFuture`?

O `@Scheduled` padrão do Spring é estático — a expressão cron é resolvida uma única vez na inicialização. Para reprogramação em runtime (sem restart), usamos o `TaskScheduler` diretamente:

1. `scheduleCron(cron)` / `scheduleAtFixedRate(task, period)` retornam um `ScheduledFuture<?>`.
2. Antes de criar um novo agendamento, chamamos `currentTask.cancel(false)` — garante que não haja múltiplas tasks sobrepostas.
3. `removeOnCancelPolicy = true` no `ThreadPoolTaskScheduler` limpa a task da fila interna imediatamente após o cancelamento.

### Por que Slash Commands em vez de prefixo (`!commit`)?

| Aspecto | Prefixo (`!commit`) | Slash Command (`/commit`) |
|---|---|---|
| **Intent necessária** | `MESSAGE_CONTENT` (privilegiada) | Nenhuma extra |
| **Autocomplete** | ❌ | ✅ |
| **Validação de tipos** | Manual | Nativa (OptionType) |
| **Modals / SelectMenu** | ❌ | ✅ |
| **API oficial** | Legada | Atual e recomendada |
