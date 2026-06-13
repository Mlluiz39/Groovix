# Groovix — Product Requirements Document (PRD)

**Versão:** 1.0
**Data:** 2026-06-12
**Status:** MVP funcional — documentação do estado atual

---

## 1. Visão Geral

Groovix é um aplicativo Android nativo de streaming de música com estética cyberpunk luxo. O app oferece busca, reprodução, playlists, favoritos e histórico — com identidade visual premium baseada em glassmorphism e tons neon (cyan e purple) sobre fundo deep navy.

**Nome do projeto:** Groovix Music
**Package:** `com.seuapp.music`
**Nome interno Gradle:** `FrontMusic`

---

## 2. Objetivo do Produto

Oferecer experiência de streaming de música imersiva e visualmente diferenciada para o mercado brasileiro, com foco em:

- Navegação fluida e estética premium
- Reprodução contínua em background (foreground service)
- Gerenciamento local de playlists e favoritos
- Busca integrada com backend de catálogo musical

---

## 3. Público-Alvo

Usuários brasileiros consumidores de música digital, com preferência por:

- Interface visual rica e moderna (cyberpunk/luxo)
- Gêneros populares nacionais (funk, sertanejo, MPB, samba, pagode, eletrônica)
- Experiência mobile-first

---

## 4. Funcionalidades Atuais (MVP)

### 4.1 Registro / Onboarding

- Tela de cadastro (`RegisterScreen`) com campos: nome, email, senha, confirmação de senha
- Validação local: nome obrigatório, email com `@`, senha ≥ 4 caracteres, senhas conferem
- Dados salvos localmente via Room (tabela `user`, chave primária fixa `id=1`)
- Após cadastro, redireciona para Home — sem autenticação real (local apenas)
- Estado `isLoggedIn` controla fluxo de navegação: não logado → Register, logado → Home

### 4.2 Home Screen

- Saudação contextual por horário: "Bom Dia", "Boa Tarde", "Boa Noite"
- Nome do usuário (fallback: "Neon Rider")
- Card de destaque "MÚSICAS DO BRASIL 2026" com gradiente roxo-ciano
- Seção "Tocadas Recentemente" (LazyRow horizontal, últimos 10 tracks do Room)
- Grid "Playlists em Alta" com 14 playlists estáticas hardcoded (2 colunas)
  - Top Brasil 2026/2025, Funk Hits, Samba e Pagode, Top Spotify, Melhores de, Rock Clássico, Eletrônica, Hip Hop, Sertanejo, MPB, Indie
- Cada playlist dispara busca pelo nome da playlist no backend
- Acesso a Settings (ícone engrenagem)

### 4.3 Busca (Search)

- Campo de busca pill-shaped com ícone de lupa e placeholder "Artistas, músicas ou gêneros..."
- Chips de buscas recentes hardcoded: "Neon Genesis", "Synthwave 2084", "Cyber Grind"
- Resultados exibidos como lista de `TrackItem`
- Integração com `MusicApi.search(q)` via Retrofit
- Indicador de loading (CircularProgressIndicator cyan)
- Teclado: ação IME Search, foco gerenciado

### 4.4 Player

- Tela dedicada (`PlayerScreen`) exibindo track atual
- Capa do álbum com animação de escala pulsante (0.95x ↔ 1.05x, 1500ms)
- Halos animados (cyan e roxo translúcidos) ao redor da capa
- Background com gradiente radial (NeonPurple + ElectricCyanDim + DeepNavy)
- Barra de progresso customizada (gradiente roxo-ciano, cantos arredondados)
- Tempo atual / duração formatado (`m:ss`)
- Controles: shuffle, anterior, play/pause, próximo, repeat
- Botão play/pause com gradiente (ElectricCyan → NeonPurple), ícone 44dp
- Toggle favorito (coração preenchido/vazio)
- Navegação: botão voltar

### 4.5 Biblioteca (Playlists)

- Tela com tabs: "Playlists" | "Favoritos"
- Playlists: lista de cards com nome + contagem de músicas, botão delete
- Playlist vazia: empty state com ícone e mensagem
- Criação de playlist via AlertDialog (nome, cria vazia)
- Detalhe da playlist (`PlaylistDetailScreen`):
  - Header com nome, contagem, botão "Tocar Playlist"
  - Lista de tracks com thumbnail, título, canal
  - Remover track da playlist (ícone RemoveCircleOutline)
  - Playlist vazia: empty state
- Favoritos: lista de tracks favoritadas com opções:
  - Tocar (inicia queue com todos favoritos)
  - Adicionar a playlist (diálogo de seleção de playlist)
  - Remover dos favoritos

### 4.6 Configurações (Settings)

- Edição de nome e email do usuário
- Botão "Salvar" com feedback visual ("Salvo com sucesso!")
- Persistência local via Room

### 4.7 Reprodução de Áudio

- **Player:** ExoPlayer (Media3) gerenciado pelo `MusicViewModel`
- **Serviço:** `MusicService` (MediaSessionService) — foreground service com notificação persistente
- **Notificação:** canal "Reprodução de música", prioridade LOW, visível na tela bloqueada
- **Resolução de URL:** `MusicApi.getAudio(url)` — backend retorna `streamUrl` real
- **Queue:** lista de `Track` com índice atual
- **Shuffle:** ordem aleatória gerada no toggle, mantém posição
- **Repeat:** toggle booleano (não implementa loop real — apenas estado)
- **Progresso:** polling a cada 500ms, exposto como `StateFlow<Float>` (0f a 1f)
- **Transição automática:** listener `onMediaItemTransition` avança para próxima track

### 4.8 Mini Player

- Barra persistente acima da NavigationBar quando há track atual
- Exibe: thumbnail (44dp), título, canal, botão favorito, botão play/pause
- Progresso visual: barra horizontal no topo com gradiente cyan-roxo (35% largura)
- Clique expande para PlayerScreen

### 4.9 Navegação

- **Bottom Navigation Bar** (3 itens, visível apenas logado):
  - Início (Home)
  - Buscar (Search)
  - Biblioteca (LibraryMusic)
- **Rotas** (Navigation Compose):
  - `register` → RegisterScreen
  - `home` → HomeScreen
  - `search` → SearchScreen
  - `player` → PlayerScreen
  - `playlists` → PlaylistScreen
  - `settings` → SettingsScreen
- Ícones ativos: ElectricCyan; inativos: MutedSteel
- Fundo da nav bar: NavyGlass com 85% alpha

---

## 5. Arquitetura Técnica

### 5.1 Stack Tecnológica

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Kotlin | 2.1.20 |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Navegação | Navigation Compose | 2.7.7 |
| Player | Media3 ExoPlayer + MediaSession | 1.3.1 |
| Rede | Retrofit + Gson + OkHttp | 2.11.0 / 4.12.0 |
| Imagens | Coil Compose | 2.6.0 |
| Banco local | Room | 2.6.1 |
| ViewModel | AndroidX Lifecycle ViewModel | 2.8.0 |
| Build | Gradle KTS + KSP | AGP 8.7.3 |

### 5.2 Estrutura de Pacotes

```
com.seuapp.music/
├── MainActivity.kt            # Activity principal, bind do MusicService
├── data/
│   ├── api/
│   │   ├── MusicApi.kt        # Interface Retrofit (search, getAudio)
│   │   └── RetrofitClient.kt  # Singleton Retrofit (base URL hardcoded)
│   ├── local/
│   │   ├── Entities.kt        # PlaylistEntity, FavoriteEntity, RecentTrackEntity
│   │   ├── MusicDao.kt        # DAO Room (playlists, favorites, recent, user)
│   │   ├── MusicDatabase.kt   # Room DB (4 entidades, v3)
│   │   └── UserEntity.kt      # Entidade de usuário local
│   └── model/
│       └── Models.kt          # Track, SearchResponse, AudioResponse, Playlist
├── navigation/
│   └── AppNavigation.kt       # Sealed class Screen + NavHost + Scaffold
├── player/
│   └── MusicService.kt        # MediaSessionService foreground
├── ui/
│   ├── components/
│   │   ├── MiniPlayer.kt      # Barra de reprodução compacta
│   │   └── TrackItem.kt       # Item de track reutilizável
│   ├── screens/
│   │   ├── HomeScreen.kt      # Tela inicial
│   │   ├── PlayerScreen.kt    # Player full-screen
│   │   ├── PlaylistScreen.kt  # Biblioteca (playlists + favoritos)
│   │   ├── RegisterScreen.kt  # Cadastro
│   │   ├── SearchScreen.kt    # Busca
│   │   └── SettingsScreen.kt  # Configurações
│   ├── theme/
│   │   ├── Color.kt           # Paleta completa (DeepNavy, ElectricCyan, etc.)
│   │   ├── Theme.kt           # MaterialTheme darkColorScheme
│   │   └── Type.kt            # Typography customizada
│   └── viewmodel/
│       └── MusicViewModel.kt  # ViewModel central (AndroidViewModel)
```

### 5.3 ViewModel Centralizado

`MusicViewModel` é um `AndroidViewModel` que concentra toda a lógica de estado:

- **Estado exposto** (14 `StateFlow`): query, tracks, isLoading, currentTrack, isPlaying, playlists, favorites, recentTracks, userName, userEmail, isLoggedIn, shuffle, repeat, progress
- **Player:** uma instância de `ExoPlayer` (criada no init)
- **Gerenciamento de queue:** lista interna + índice + shuffle order
- **Resolução de URL:** cache local (`resolvedUrls: MutableMap<String, String>`)
- **Persistência:** todas as operações de Room via `viewModelScope.launch(Dispatchers.IO)`

### 5.4 Fluxo de Dados

```
[Backend API] ←→ [RetrofitClient/MusicApi]
                          ↕
                   [MusicViewModel]
                    ↕              ↕
              [StateFlow]    [Room Database]
                    ↕
            [Compose UI Screens]
                    ↕
         [ExoPlayer] → [MusicService (Foreground)]
```

---

## 6. Design System — "Cyan-Neon Luxury Audio"

### 6.1 Personalidade Visual

Cyberpunk refinado, glassmorphism, luminescência. Superfícies translúcidas flutuando em espaço profundo, acentos neon brilhantes. Sensação de exclusividade e sofisticação tecnológica.

### 6.2 Paleta de Cores

| Token | Hex | Uso |
|-------|-----|-----|
| `DeepNavy` | `#0A1322` | Fundo principal |
| `ElectricCyan` | `#00E5FF` | Ações primárias, progresso, destaque |
| `NeonPurple` | `#571BC1` | Acentos atmosféricos, gradientes |
| `SoftCoral` | `#FFC1BE` | Favoritos, indicadores de estado ativo |
| `OffWhite` | `#DAE2F8` | Texto principal |
| `SteelText` | `#BAC9CC` | Texto secundário |
| `MutedSteel` | `#849396` | Ícones inativos, texto terciário |
| `NavyGlass` | `#131C2B` | Superfícies vidro (cards, nav) |
| `SurfaceHigh` | `#222A3A` | Superfícies elevadas |
| `SurfaceHighest` | `#2C3545` | Máxima elevação |
| `ErrorRed` | `#FFB4AB` | Erros, ações destrutivas |

### 6.3 Tipografia

| Papel | Fonte (design) | Implementação |
|-------|---------------|---------------|
| Headlines | Sora (600-700) | MaterialTheme `headlineLarge`/`headlineMedium` |
| Body | Hanken Grotesk (400) | MaterialTheme `bodyLarge`/`bodyMedium` |
| Labels | Geist (600) | MaterialTheme `labelSmall` |

Nota: fontes customizadas não estão incluídas no build atual — usa fallback do sistema.

### 6.4 Elevação & Profundidade

3 níveis de profundidade via transparência e blur:

1. **Base:** `DeepNavy` (`#0A1322`)
2. **Cards:** `NavyGlass` com alpha 0.6-0.75, cantos 12-16dp
3. **Controles flutuantes:** MiniPlayer e botão play/pause com glow interno (ElectricCyan 10%)

### 6.5 Formas

- Cards e containers: cantos 12-24dp
- Botões e chips: totalmente arredondados (pill/cápsula)
- Elementos aninhados: raio proporcionalmente reduzido (10-12dp)

---

## 7. Modelo de Dados

### 7.1 API (Backend)

**Base URL:** `http://192.168.15.30:3000/` (hardcoded em `RetrofitClient`)

```
GET /api/search?q={query}
  → { results: [{ id, title, channel, duration?, thumbnail?, url }] }

GET /api/audio?url={url}
  → { title?, channel?, thumbnail?, streamUrl? }
```

### 7.2 Entidades Locais (Room)

```
user (id=1 fixo)
  id: Int PK, name: String, email: String, password: String

playlists
  id: String PK (UUID), name: String, tracksJson: String (JSON array de Track)

favorites
  trackId: String PK, id, title, channel, duration?, thumbnail?, url

recent_tracks
  trackId: String PK, id, title, channel, duration?, thumbnail?, url, playedAt: Long
```

### 7.3 Modelos de Domínio

```kotlin
@Parcelize
data class Track(
    id: String, title: String, channel: String,
    duration: Long?, thumbnail: String?, url: String
) : Parcelable

data class Playlist(id: String, name: String, tracks: List<Track>)
```

---

## 8. Estados e Edge Cases

| Estado | Tratamento |
|--------|-----------|
| **Loading** | `CircularProgressIndicator` cyan na SearchScreen |
| **Empty (playlists)** | Ícone + mensagem "Nenhuma playlist ainda" |
| **Empty (favoritos)** | Ícone + mensagem "Toque no ♡ em qualquer música!" |
| **Empty (player)** | Texto "Nenhuma música tocando" centralizado |
| **Erro de rede** | `try/catch` com `printStackTrace()` — sem tratamento visual |
| **Track sem thumbnail** | Coil AsyncImage lida com null (placeholder vazio) |
| **Duração desconhecida** | `player.duration` fallback para `1L` (evita divisão por zero) |
| **Primeiro acesso** | `startDestination` aguarda `isLoading`/`isLoggedIn` — mostra fundo DeepNavy |

---

## 9. Permissões

| Permissão | Uso |
|-----------|-----|
| `INTERNET` | API + streaming de áudio |
| `ACCESS_NETWORK_STATE` | Monitoramento de conectividade |
| `FOREGROUND_SERVICE` | MusicService em foreground |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Tipo do foreground service (Android 14+) |
| `WAKE_LOCK` | Manter CPU ativa durante playback |
| `POST_NOTIFICATIONS` | Canal de notificação do player (Android 13+) |

---

## 10. Configuração de Build

| Parâmetro | Valor |
|-----------|-------|
| `minSdk` | 26 (Android 8.0) |
| `targetSdk` / `compileSdk` | 34 (Android 14) |
| `jvmTarget` | 17 |
| `versionCode` | 1 |
| `versionName` | 1.0 |
| `applicationId` | `com.seuapp.music` |

---

## 11. Funcionalidades Não Implementadas (Fora do MVP Atual)

- Autenticação real (oauth, tokens, sessão remota)
- Sincronização remota de playlists/favoritos
- Download offline
- Letras (lyrics) sincronizadas
- Social (seguir, playlists colaborativas)
- Recomendações personalizadas
- Equalizador
- Integração com serviços de terceiros (Spotify, YouTube Music)
- Casting (Chromecast)
- Widgets
- Android Auto
- Analytics / crash reporting

---

## 12. Débitos Técnicos Conhecidos

| Item | Severidade | Descrição |
|------|-----------|-----------|
| URL hardcoded | Alta | `RetrofitClient.BASE_URL` fixa (`192.168.15.30`). Deve ser configurável ou usar build variants |
| Senha em plain text | Alta | `UserEntity.password` salva sem hash no Room |
| Sem error handling visual | Média | Falhas de rede só logam — usuário não vê feedback |
| Playlists estáticas | Média | 14 playlists hardcoded no HomeScreen, não vêm do backend |
| ViewModel gigante | Média | `MusicViewModel` ~365 linhas, muitas responsabilidades |
| Repeat não funcional | Baixa | Toggle existe mas ExoPlayer não configura loop |
| Chips de busca hardcoded | Baixa | "Neon Genesis", "Synthwave 2084", "Cyber Grind" fixos |
| Fontes customizadas ausentes | Baixa | Design spec menciona Sora/Hanken Grotesk/Geist mas não estão no build |
| `fallbackToDestructiveMigration()` | Média | Migração de schema do Room destrói dados em vez de migrar |
| Sem testes | Alta | Zero testes unitários ou de UI |
| Sem ProGuard/R8 | Baixa | `isMinifyEnabled = false` no release |
| Base URL usa HTTP cleartext | Média | `usesCleartextTraffic=true` — aceitável dev local, inaceitável produção |
