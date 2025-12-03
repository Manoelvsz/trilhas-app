# Análise do Código - Trilhas App

## Visão Geral

O **Trilhas App** (MapsApp) é uma aplicação Android desenvolvida em Java que funciona como um **rastreador de atividades físicas** (corrida, caminhada, ciclismo, etc.). O aplicativo utiliza GPS e sensores do dispositivo para registrar trilhas/percursos, calcular métricas de desempenho e armazenar o histórico de atividades do usuário.

## Tecnologias Utilizadas

- **Linguagem**: Java
- **Build System**: Gradle (Kotlin DSL)
- **SDK Android**: Compilação para SDK 36, mínimo SDK 24
- **APIs Principais**:
  - Google Maps Android API (visualização de mapas e trilhas)
  - Google Play Services Location (rastreamento GPS)
  - SQLite (armazenamento local de dados)
  - Gson (serialização/deserialização JSON)
  - Sensores Android (acelerômetro/giroscópio para orientação)

## Arquitetura da Aplicação

### Estrutura de Atividades (Activities)

A aplicação segue uma arquitetura baseada em Activities, com 6 telas principais:

#### 1. MainActivity
**Arquivo**: `MainActivity.java`

**Função**: Tela inicial e hub de navegação da aplicação.

**Características**:
- 4 botões principais de navegação:
  - **Configurações**: Abre ConfigActivity
  - **Registrar Trilha**: Abre TrilhaActivity (iniciar rastreamento)
  - **Visualizar Trilhas**: Abre TrilhasSalvasActivity (histórico)
  - **Créditos**: Abre CreditsActivity

**Código Principal**:
```java
- setContentView(R.layout.activity_main)
- Listeners de click para navegação entre telas
```

#### 2. TrilhaActivity
**Arquivo**: `TrilhaActivity.java` (515 linhas)

**Função**: Núcleo da aplicação - rastreia e registra trilhas em tempo real.

**Funcionalidades Principais**:

1. **Rastreamento GPS**:
   - Usa `FusedLocationProviderClient` para obter localização precisa
   - Atualização a cada 2 segundos (mínimo 1 segundo)
   - Prioridade para alta precisão (PRIORITY_HIGH_ACCURACY)

2. **Cálculo de Métricas**:
   - **Distância Total**: Soma das distâncias entre pontos GPS consecutivos
   - **Velocidade Atual**: Calculada pelo GPS (em km/h)
   - **Velocidade Máxima**: Maior velocidade registrada durante a trilha
   - **Velocidade Média**: Distância total / tempo total
   - **Calorias Queimadas**: Fórmula baseada em velocidade e peso do usuário
     - `calorieBurnPerMinute = currentSpeedKmh * userWeight * 0.0175f`
   - **Tempo Decorrido**: Cronômetro em formato HH:MM:SS

3. **Visualização no Mapa**:
   - Desenha linha azul (Polyline) mostrando o percurso em tempo real
   - Duas opções de navegação:
     - **North Up**: Mapa fixo com norte para cima
     - **Course Up**: Mapa rotaciona conforme direção do movimento (usa sensores de rotação)
   - Suporte a mapas Normal e Satélite
   - Estilo escuro customizado (map_style_dark)

4. **Sensores**:
   - Usa `TYPE_ROTATION_VECTOR` para detectar orientação do dispositivo
   - Implementa filtro low-pass para suavizar rotações da câmera
   - Atualização da câmera a ~60 FPS quando em modo Course Up

5. **Gerenciamento de Estado**:
   - `isTracking`: Indica se está rastreando
   - `hasUnsavedTrack`: Indica se há trilha não salva
   - Estados do botão: "Iniciar" → "Parar" → "Salvar"

6. **Validações**:
   - Requer permissões de localização (ACCESS_FINE_LOCATION)
   - Valida configurações do usuário (peso, altura, gênero) antes de iniciar
   - Filtra velocidades abaixo de 0.5 km/h como zero (evita ruído GPS)

7. **Salvamento**:
   - Dialog para nomear a trilha
   - Salva no banco de dados SQLite
   - Serializa pontos GPS em JSON usando Gson

**Ciclo de Vida Importante**:
- `onResume()`: Recarrega configurações e reinicia sensores se necessário
- `onPause()`: Para atualizações de localização e sensores para economizar bateria
  - Se estiver rastreando, trata como parada e solicita salvamento

#### 3. TrilhasSalvasActivity
**Arquivo**: `TrilhasSalvasActivity.java`

**Função**: Gerenciamento de trilhas salvas (histórico).

**Funcionalidades**:

1. **Listagem**:
   - Lista todas as trilhas salvas ordenadas por data (mais recente primeiro)
   - Exibe nome e data de cada trilha
   - Usa `SimpleCursorAdapter` com ListView

2. **Operações por Trilha** (clique longo):
   - **Editar**: Renomear a trilha
   - **Excluir**: Remove trilha e seus detalhes do banco
   - **Compartilhar**: Exporta dados da trilha como JSON
     - Inclui: nome, data, distância, tempo, velocidades, calorias, path (pontos GPS)

3. **Menu de Opções**:
   - **Apagar Todas**: Remove todas as trilhas (com confirmação)
   - **Apagar por Data**: Remove trilhas anteriores a uma data selecionada

4. **Navegação**:
   - Clique simples: Abre DetalhesTrilhaActivity

**Formato de Compartilhamento JSON**:
```json
{
  "nome": "Corrida Matinal",
  "data": "2025-12-03 08:30:00",
  "distancia": 5.2,
  "tempo": "00:32:15",
  "velocidade_max": 18.5,
  "velocidade_media": 9.7,
  "calorias": 350,
  "path": "[{\"lat\":-23.55,\"lng\":-46.63},...]"
}
```

#### 4. DetalhesTrilhaActivity
**Arquivo**: `DetalhesTrilhaActivity.java`

**Função**: Visualização detalhada de uma trilha específica.

**Características**:
- **Visualização no Mapa**:
  - Desenha a trilha completa usando Polyline
  - Ajusta câmera automaticamente para mostrar toda a trilha (LatLngBounds)
  - Suporta mapas Normal/Satélite
  - Usa mesmo estilo escuro da TrilhaActivity

- **Métricas Exibidas**:
  - Velocidade Média
  - Tempo Total
  - Distância Total
  - Velocidade Máxima
  - Calorias (ou "N/A" se não calculado)
  - Data da trilha

- **Deserialização**:
  - Carrega pontos GPS do JSON armazenado usando Gson
  - `TypeToken<List<LatLng>>` para conversão correta

#### 5. ConfigActivity
**Arquivo**: `ConfigActivity.java`

**Função**: Configurações do usuário e preferências da aplicação.

**Configurações Disponíveis**:

1. **Dados do Usuário** (para cálculo de calorias):
   - Peso (float, em kg)
   - Altura (float, em metros)
   - Gênero (RadioGroup)
   - Data de Nascimento (texto)

2. **Preferências de Mapa**:
   - **Tipo de Mapa**:
     - Vetorial/Normal (`MAP_TYPE_NORMAL`)
     - Satélite (`MAP_TYPE_SATELLITE`)
   - **Modo de Navegação**:
     - North Up (norte fixo para cima)
     - Course Up (rotaciona conforme movimento)

**Armazenamento**:
- Usa `SharedPreferences` ("MyPreferences")
- Salva automaticamente em `onPause()`
- Campos numéricos com tratamento de exceção (NumberFormatException)

**Chaves SharedPreferences**:
```
- peso_salvo (float)
- altura_salvo (float)
- genero_selecionado (int - RadioButton ID)
- nascimento_salvo (String)
- mapa_tipo_valor (int - GoogleMap constant)
- mapa_id_selecionado (int - RadioButton ID)
- navegacao_modo_valor (int - 0 ou 1)
- navegacao_id_selecionada (int - RadioButton ID)
```

#### 6. CreditsActivity
**Arquivo**: `CreditsActivity.java`

**Função**: Tela de créditos (simples, apenas exibe layout).

### Banco de Dados (SQLite)

**Arquivo**: `TrilhaDBHelper.java`

**Nome do Banco**: `trilhas.db`
**Versão**: 2

#### Esquema de Tabelas:

##### Tabela: `trilhas`
```sql
CREATE TABLE trilhas (
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  data TEXT NOT NULL  -- Formato: "yyyy-MM-dd HH:mm:ss"
)
```

**Função**: Armazena informações básicas de cada trilha registrada.

##### Tabela: `detalhes_trilha`
```sql
CREATE TABLE detalhes_trilha (
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  trilha_id INTEGER NOT NULL,
  distancia_total REAL NOT NULL,           -- em km
  velocidade_maxima REAL NOT NULL,         -- em km/h
  velocidade_media REAL NOT NULL,          -- em km/h
  tempo_total TEXT NOT NULL,               -- formato HH:MM:SS
  calorias_totais REAL NOT NULL,           -- em kcal (-1 se não calculado)
  path_pontos TEXT NOT NULL,               -- JSON array de LatLng
  FOREIGN KEY(trilha_id) REFERENCES trilhas(_id)
)
```

**Função**: Armazena métricas detalhadas e o percurso GPS de cada trilha.

**Estratégia de Upgrade**:
- `onUpgrade()`: DROP e recria tabelas (dados são perdidos)
- Versão 2 adicionou `velocidade_media` à tabela detalhes

**Relacionamento**: Um-para-um entre trilhas e detalhes (Foreign Key).

## Fluxo de Usuário

### Fluxo Principal - Registrar Nova Trilha

1. **Início**: Usuário abre o app (MainActivity)
2. **Configuração** (primeira vez):
   - Clica em "Configurações"
   - Preenche peso, altura, gênero
   - Escolhe tipo de mapa e modo de navegação
3. **Registrar Trilha**:
   - Clica em "Registrar Trilha" na MainActivity
   - TrilhaActivity abre e pede permissão de localização
   - Mapa exibe localização atual
4. **Validação**:
   - Se dados do usuário não configurados → Dialog direcionando para Configurações
5. **Rastreamento**:
   - Clica em "Iniciar"
   - App começa a rastrear GPS
   - Métricas atualizam em tempo real
   - Linha azul desenha o percurso no mapa
6. **Finalização**:
   - Clica em "Parar"
   - Dialog solicita nome para a trilha
   - Opções: Salvar / Cancelar / Descartar
7. **Salvamento**:
   - Se Salvar: Dados gravados no SQLite
   - Se Descartar: Trilha é perdida
   - Se Cancelar: Pode clicar "Salvar" depois

### Fluxo Secundário - Visualizar Histórico

1. Na MainActivity, clica em "Visualizar Trilhas"
2. TrilhasSalvasActivity lista todas as trilhas
3. **Clique Simples**: Abre detalhes da trilha
4. **Clique Longo**: Menu de opções
   - Editar nome
   - Excluir trilha
   - Compartilhar (via JSON)
5. **Menu**: Opções de limpeza em massa

## Permissões e Recursos Android

### AndroidManifest.xml

**Permissões**:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

**Metadados**:
- API Key do Google Maps injetada via variável `${MAPS_API_KEY}`
- Definida em `app/readme.md` (⚠️ ATENÇÃO: Chave exposta no repositório)

**Activities Exportadas**:
- Apenas MainActivity (LAUNCHER)
- Demais Activities são internas (exported="false")

## Características Técnicas Avançadas

### 1. Otimização de Performance

- **Atualização de Câmera**: Handler separado para Course Up (~60 FPS)
- **Filtro Low-Pass**: Suaviza rotações do sensor (BEARING_SMOOTHING_FACTOR = 0.1)
- **Economia de Bateria**:
  - Para sensores em onPause()
  - Remove callbacks de Handlers
  - Para atualizações de GPS quando app está em background

### 2. Precisão de Dados

- **GPS**: 
  - Intervalo de atualização: 2s (pode ser 1s)
  - Aguarda localização precisa (waitForAccurateLocation)
- **Filtros**:
  - Velocidades < 0.5 km/h consideradas zero
  - Smoothing em rotações do sensor

### 3. Gestão de Estado

- Três estados principais do rastreamento:
  1. Pronto para iniciar (isTracking=false, hasUnsavedTrack=false)
  2. Rastreando (isTracking=true)
  3. Parado com dados (isTracking=false, hasUnsavedTrack=true)

### 4. Compatibilidade

- **SDK Mínimo**: Android 7.0 (API 24)
- **SDK Target/Compilação**: Android 13+ (API 36)
- **Java**: Version 11
- **ViewBinding**: Habilitado para segurança de tipos

## Pontos de Atenção

### Segurança
⚠️ **CRÍTICO**: Chave da API do Google Maps está exposta no arquivo `app/readme.md`
- Deveria estar em arquivo não versionado (local.properties)
- Potencial uso indevido da quota da API

### Privacidade
- Trilhas contêm coordenadas GPS precisas do usuário
- Ao compartilhar, todos os pontos GPS são exportados
- Não há criptografia nos dados locais (SQLite padrão)

### Usabilidade
- Se app é pausado durante rastreamento, a trilha é automaticamente parada
  - Pode causar perda de dados se usuário não salvar
- Upgrade do banco deleta todos os dados existentes
  - Não há migração de dados entre versões

### Cálculo de Calorias
- Fórmula simplificada pode não ser precisa para todos os tipos de atividade
- Não considera idade do usuário apesar de solicitar data de nascimento
- Não diferencia tipos de atividade (corrida vs. caminhada)

## Possíveis Melhorias

1. **Segurança**:
   - Mover API key para local.properties
   - Implementar criptografia no banco de dados
   - Obfuscação de código (ProGuard/R8)

2. **Funcionalidades**:
   - Importação de trilhas compartilhadas
   - Gráficos de progresso ao longo do tempo
   - Suporte a diferentes tipos de atividade
   - Pausar/retomar rastreamento
   - Metas e desafios
   - Integração com serviços de fitness

3. **Performance**:
   - Usar Room ao invés de SQLite direto
   - Implementar Repository pattern
   - ViewModel para sobreviver a mudanças de configuração

4. **UX**:
   - Notificação persistente durante rastreamento
   - Widget para início rápido
   - Temas claro/escuro
   - Unidades configuráveis (mi vs km)
   - Compartilhamento em formatos populares (GPX, KML)

5. **Robustez**:
   - Migração de banco de dados ao invés de DROP
   - Tratamento de erros de GPS
   - Modo offline robusto
   - Testes unitários e de integração

## Conclusão

O **Trilhas App** é uma aplicação funcional e bem estruturada para rastreamento de atividades físicas. Utiliza corretamente as APIs do Android para GPS, mapas e sensores, implementando funcionalidades essenciais como:

✅ Rastreamento GPS em tempo real
✅ Cálculo de métricas de performance
✅ Armazenamento persistente local
✅ Visualização de histórico
✅ Compartilhamento de dados
✅ Modos de navegação avançados

A aplicação demonstra bom uso de recursos Android modernos (ViewBinding, FusedLocationProvider) e implementa otimizações importantes (filtros de sensor, gestão de bateria). No entanto, há oportunidades de melhoria em segurança (API key exposta), privacidade (criptografia) e arquitetura (padrões modernos como MVVM, Room).

O código é majoritariamente bem organizado e comentado onde necessário, tornando-o um bom ponto de partida para expansão futura.
