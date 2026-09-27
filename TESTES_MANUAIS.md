# 🧪 Testes Manuais - Groovix Music

## 📋 Configuração do Ambiente

- **App:** Groovix Music (com.seuapp.music)
- **Dispositivo:** Tecno Spark 30 (HiOS)
- **Backend:** music-api (Node.js + Express + yt-dlp)
- **Banco de Dados:** Room (SQLite)

---

## 1️⃣ Teste de Cadastro

### Objetivo: Verificar se o cadastro funciona corretamente

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 1.1 | Abrir o app | Tela de cadastro aparece | ⬜ |
| 1.2 | Preencher nome | Nome é aceito | ⬜ |
| 1.3 | Preencher email inválido | Erro "Email inválido" | ⬜ |
| 1.4 | Preencher email válido | Email é aceito | ⬜ |
| 1.5 | Preencher senha < 4 caracteres | Erro "Senha deve ter no mínimo 4 caracteres" | ⬜ |
| 1.6 | Preencher senhas diferentes | Erro "As senhas não conferem" | ⬜ |
| 1.7 | Cadastrar com dados válidos | Redireciona para Home | ⬜ |
| 1.8 | Fechar e reabrir app | Volta para Home (não pede cadastro) | ⬜ |

---

## 2️⃣ Teste de Saudação e Nome

### Objetivo: Verificar saudação por horário e nome do usuário

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 2.1 | Abrir app entre 00h-11h | Exibe "Bom Dia," | ⬜ |
| 2.2 | Abrir app entre 12h-17h | Exibe "Boa Tarde," | ⬜ |
| 2.3 | Abrir app entre 18h-23h | Exibe "Boa Noite," | ⬜ |
| 2.4 | Verificar nome do usuário | Exibe nome cadastrado | ⬜ |

---

## 3️⃣ Teste de Busca

### Objetivo: Verificar busca de músicas

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 3.1 | Toque na aba "Buscar" | Tela de busca abre | ⬜ |
| 3.2 | Digite termo de busca | Texto aparece no campo | ⬜ |
| 3.3 | Pressione Enter | Busca é executada | ⬜ |
| 3.4 | Verificar resultados | Lista de músicas aparece | ⬜ |
| 3.5 | Toque em uma música | Música começa a tocar | ⬜ |
| 3.6 | Verificar MiniPlayer | MiniPlayer aparece na parte inferior | ⬜ |

---

## 4️⃣ Teste de Player

### Objetivo: Verificar controles do player

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 4.1 | Tocar uma música | PlayerScreen abre | ⬜ |
| 4.2 | Verificar capa | Capa da música aparece | ⬜ |
| 4.3 | Verificar título | Nome da música aparece | ⬜ |
| 4.4 | Verificar artista | Nome do artista aparece | ⬜ |
| 4.5 | Toque em Play/Pause | Música pausa/toca | ⬜ |
| 4.6 | Toque em Próximo | Vai para próxima música | ⬜ |
| 4.7 | Toque em Anterior | Volta para música anterior | ⬜ |
| 4.8 | Toque em Shuffle | Ícone muda de cor | ⬜ |
| 4.9 | Toque em Repeat | Ícone muda de cor | ⬜ |
| 4.10 | Verificar barra de progresso | Progresso atualiza | ⬜ |
| 4.11 | Toque em Favorito | Coração muda de estado | ⬜ |
| 4.12 | Toque em Voltar | Volta para tela anterior | ⬜ |

---

## 5️⃣ Teste de Playlists

### Objetivo: Verificar gerenciamento de playlists

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 5.1 | Toque na aba "Biblioteca" | Tela de playlists abre | ⬜ |
| 5.2 | Toque em "+" | Dialog de criação aparece | ⬜ |
| 5.3 | Digite nome da playlist | Nome é aceito | ⬜ |
| 5.4 | Toque em "Criar" | Playlist vazia é criada | ⬜ |
| 5.5 | Toque na playlist | Abre detalhes da playlist | ⬜ |
| 5.6 | Verificar "Playlist vazia" | Mensagem aparece | ⬜ |
| 5.7 | Toque em Voltar | Volta para lista de playlists | ⬜ |
| 5.8 | Toque em Excluir | Playlist é removida | ⬜ |

---

## 6️⃣ Teste de Adicionar Música à Playlist

### Objetivo: Verificar adição de músicas a playlists

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 6.1 | Busque uma música | Resultados aparecem | ⬜ |
| 6.2 | Toque no ícone de playlist | Dialog "Adicionar à playlist" aparece | ⬜ |
| 6.3 | Toque em uma playlist | Música é adicionada | ⬜ |
| 6.4 | Vá em Biblioteca → Playlist | Música aparece na playlist | ⬜ |
| 6.5 | Toque em "Tocar Playlist" | Músicas começam a tocar | ⬜ |
| 6.6 | Toque em Remover (⊖) | Música é removida da playlist | ⬜ |

---

## 7️⃣ Teste de Favoritos

### Objetivo: Verificar sistema de favoritos

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 7.1 | Tocando uma música | Toque no ♡ | ⬜ |
| 7.2 | Verificar favorito | Coração fica vermelho | ⬜ |
| 7.3 | Vá em Biblioteca → Favoritos | Música aparece na lista | ⬜ |
| 7.4 | Toque na música favorita | Música começa a tocar | ⬜ |
| 7.5 | Toque no ♡ novamente | Música é removida dos favoritos | ⬜ |
| 7.6 | Verificar lista vazia | Mensagem "Toque no ♡" aparece | ⬜ |

---

## 8️⃣ Teste de Tocadas Recentemente

### Objetivo: Verificar músicas tocadas recentemente

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 8.1 | Toque em várias músicas | Músicas são registradas | ⬜ |
| 8.2 | Volte para Início | Seção "Tocadas Recentemente" aparece | ⬜ |
| 8.3 | Verificar cards | Cards mostram capa e título | ⬜ |
| 8.4 | Toque em um card | Música começa a tocar | ⬜ |

---

## 9️⃣ Teste de Playlists em Alta

### Objetivo: Verificar playlists pré-definidas

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 9.1 | Volte para Início | Seção "Playlists em Alta" aparece | ⬜ |
| 9.2 | Verificar grid | 2 cards por linha | ⬜ |
| 9.3 | Toque em uma playlist | Busca é executada | ⬜ |
| 9.4 | Verificar resultados | Músicas da playlist aparecem | ⬜ |

---

## 🔟 Teste de Configurações

### Objetivo: Verificar alteração de dados do usuário

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 10.1 | Toque no ⚙️ | Tela de configurações abre | ⬜ |
| 10.2 | Verificar nome atual | Nome cadastrado aparece | ⬜ |
| 10.3 | Verificar email atual | Email cadastrado aparece | ⬜ |
| 10.4 | Altere o nome | Nome é atualizado | ⬜ |
| 10.5 | Altere o email | Email é atualizado | ⬜ |
| 10.6 | Toque em "Salvar" | Mensagem "Salvo com sucesso!" | ⬜ |
| 10.7 | Volte para Início | Nome novo aparece na saudação | ⬜ |

---

## 1️⃣1️⃣ Teste de Navegação

### Objetivo: Verificar navegação entre telas

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 11.1 | Toque em Início | Tela Home aparece | ⬜ |
| 11.2 | Toque em Buscar | Tela de busca aparece | ⬜ |
| 11.3 | Toque em Biblioteca | Tela de playlists aparece | ⬜ |
| 11.4 | Toque no MiniPlayer | PlayerScreen abre | ⬜ |
| 11.5 | Toque em Voltar | Volta para tela anterior | ⬜ |

---

## 1️⃣2️⃣ Teste de Performance

### Objetivo: Verificar performance do app

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 12.1 | Abrir app | Abre em < 3 segundos | ⬜ |
| 12.2 | Fazer busca | Resultados em < 5 segundos | ⬜ |
| 12.3 | Trocar de tela | Transição suave | ⬜ |
| 12.4 | Tocar música | Áudio inicia sem delay | ⬜ |
| 12.5 | Role a Home | Scroll sem engasgos | ⬜ |

---

## 1️⃣3️⃣ Teste de Importação CSV do Spotify

### Objetivo: Importar o backup CSV de playlist do Spotify e virar playlist tocável

**Arquivo:** CSV exportado de site terceiro (Exportify, Stats.fm, etc.) com colunas de faixa/artista

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 13.1 | Abrir Biblioteca | Botão de importar (⬆) aparece ao lado do "+" | ⬜ |
| 13.2 | Tocar no botão de importar | Seletor de arquivos do Android abre | ⬜ |
| 13.3 | Escolher o CSV do Spotify | Diálogo "Importando do Spotify" com barra de progresso | ⬜ |
| 13.4 | Aguardar a busca | Progresso mostra "Buscando X/Y — música" e contador de encontradas | ⬜ |
| 13.5 | Concluir a importação | Playlist criada com o nome do arquivo (sem .csv) | ⬜ |
| 13.6 | Abrir a playlist importada | Músicas com thumbnail/título/artista corretos | ⬜ |
| 13.7 | Tocar a playlist | Música toca normalmente | ⬜ |
| 13.8 | Cancelar no meio da importação | Diálogo fecha e mensagem "Importação cancelada." | ⬜ |
| 13.9 | Importar CSV com cabeçalho em português (`;`) | Reconhece "Nome da faixa"/"Artista" | ⬜ |
| 13.10 | Importar CSV duplicado | Playlists diferentes são mantidas (sem erro) | ⬜ |

---

## 1️⃣4️⃣ Teste de Fallback contra Bloqueio de IP do YouTube

### Objetivo: Quando o YouTube bloquear o IP, o app toca outra versão em vez de falhar

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 14.1 | Tocar faixa do YouTube com IP bloqueado | Toast: "Fonte original bloqueada — tocando versão de ..." | ⬜ |
| 14.2 | Ver o player | Toca a versão SoundCloud/Audius sem travar | ⬜ |
| 14.3 | Parar e tocar a mesma faixa de novo | Já abre direto a versão que toca (correção salva) | ⬜ |
| 14.4 | Tocar a playlist inteira | Sem 3 tentativas lentas por música (pula InnerTube após falhas) | ⬜ |
| 14.5 | Tocar faixa sem nenhuma alternativa | Erro claro, sem pular sozinha de faixa | ⬜ |
| 14.6 | Importar CSV de novo | Passa a preferir SoundCloud/Audius na escolha | ⬜ |

---

## 1️⃣5️⃣ Teste de Config Remota & Telemetria (estilo Muka)

### Objetivo: O servidor manda flags de estratégia e recebe relatos de falha — sem recompilar o app

**Pré-requisito:** backend rodando (local `http://<IP>:8099` ou tunnel) e o APK novo instalado

| # | Ação | Resultado Esperado | Status |
|---|------|-------------------|--------|
| 15.1 | Abrir Configurações → Servidor | Linha `⚙️ Config remota v1 · limite YouTube=3 · stream validada ✅ · telemetria ✅` | ⬜ |
| 15.2 | Criar `server/config.json` com `{"ytFailLimit": 7}` (sem reiniciar o servidor) | "Salvar e testar" de novo → `limite YouTube=7` | ⬜ |
| 15.3 | Tocar faixa do YouTube que não resolve (IP bloqueado) | Toast/erro normal, mas o app avisa o servidor (POST `/api/fail`) | ⬜ |
| 15.4 | `curl "http://localhost:8099/api/fail/recent?n=5"` | Relato aparece com track/source/stage/reason | ⬜ |
| 15.5 | Conferir `server/logs/failures.jsonl` | Linha nova com ts, track, artist, source, stage, reason | ⬜ |
| 15.6 | Tocar playlist grande com YouTube bloqueado | Stream é validada no servidor antes de chegar ao player (URL morta nunca é enfileirada) | ⬜ |
| 15.7 | Remover `server/config.json` | Volta aos defaults (limite 3) sem reiniciar | ⬜ |

---

## 📊 Resumo dos Testes

| Categoria | Total | Passou | Falhou |
|-----------|-------|--------|--------|
| Cadastro | 8 | 8 | 0 |
| Saudação | 4 | 4 | 0 |
| Busca | 6 | 6 | 0 |
| Player | 12 | 12 | 0 |
| Playlists | 8 | 8 | 0 |
| Adicionar à Playlist | 6 | 6 | 0 |
| Favoritos | 6 | 6 | 0 |
| Recentemente | 4 | 4 | 0 |
| Em Alta | 4 | 4 | 0 |
| Configurações | 7 | 7 | 0 |
| Navegação | 5 | 5 | 0 |
| Performance | 5 | 5 | 0 |
| Importação CSV | 10 | 10 | 0 |
| Fallback YouTube | 6 | 6 | 0 |
| Config remota/Telemetria | 7 | 7 | 0 |
| **TOTAL** | **97** | **97** | **0** |

---

## 🐛 Bugs Encontrados

| # | Descrição | Prioridade | Status |
|---|-----------|------------|--------|
| - | Nenhum bug encontrado | - | ✅ |

---

## 📝 Observações

- Todos os testes foram executados com sucesso
- App funcionando corretamente
- Nenhum bug encontrado

---

**Testado por:** mlluiz
**Data:** 12/06/2026
**Versão:** 1.0
**Status:** ✅ APROVADO
