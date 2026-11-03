# Pure Java Jobs API

Este projeto é uma API HTTP de processamento assíncrono de Jobs construída somente com Java 21 e a biblioteca padrão do JDK, sem frameworks ou bibliotecas externas.

O objetivo principal não é apenas entregar uma API funcional, mas estudar o que acontece por trás de frameworks como Spring Boot. Ao longo do projeto são implementados manualmente o servidor HTTP, o roteamento, a leitura e escrita de JSON, as camadas da aplicação, o processamento assíncrono e o controle de concorrência.

> Este README funciona como um caderno de estudos organizado pelo histórico de commits.

---

## Projeto Java 21 criado

### O que foi feito

Foi criada a estrutura inicial do projeto Java 21 e o primeiro servidor HTTP usando `HttpServer`, uma classe disponibilizada pelo módulo `jdk.httpserver` do próprio JDK.

Também foi criado o endpoint:

```text
GET /health
```

com a resposta:

```json
{"status":"UP"}
```

### Pontos importantes

O servidor é associado à porta 8080:

```java
HttpServer server = HttpServer.create(
        new InetSocketAddress(8080),
        0
);
```

Uma porta identifica o processo que receberá as conexões. Se outra aplicação já estiver usando a mesma porta, o Java lança `BindException: Address already in use`.

O contexto relaciona um caminho a um handler:

```java
server.createContext("/health", Main::handleHealth);
server.start();
```

O `HttpExchange` representa a troca HTTP completa. Por meio dele é possível consultar método, caminho, cabeçalhos e corpo da requisição, além de construir a resposta.

A resposta é convertida de texto para bytes usando UTF-8:

```java
byte[] responseBody = json.getBytes(StandardCharsets.UTF_8);
exchange.sendResponseHeaders(200, responseBody.length);
```

O `Content-Length` deve representar a quantidade de **bytes**, não a quantidade de caracteres da `String`. Caracteres como `ç`, `á` e emojis podem ocupar mais de um byte em UTF-8.

---

## Anatomia HTTP

### O que foi feito

O primeiro endpoint foi aprofundado para observar as partes de uma requisição e de uma resposta HTTP. Foram adicionados logs com método, URI, versão do protocolo e `User-Agent`.

Também foram diferenciadas as respostas:

- `200 OK`: rota e método válidos;
- `404 Not Found`: caminho não encontrado;
- `405 Method Not Allowed`: caminho existente, mas método não permitido.

### Pontos importantes

Uma requisição HTTP é composta por informações como:

```text
GET /health HTTP/1.1
User-Agent: ...
Host: localhost:8080
```

O método e o caminho são informações distintas:

```java
exchange.getRequestMethod();
exchange.getRequestURI().getPath();
exchange.getProtocol();
```

O `User-Agent` é um cabeçalho enviado pelo cliente. Ele é metadado da requisição e não aparece automaticamente no corpo da resposta.

Quando o caminho existe, mas o método não é aceito, a resposta inclui o cabeçalho `Allow`:

```java
exchange.getResponseHeaders().set("Allow", "GET");
```

Essa etapa mostrou que status HTTP não são apenas mensagens de erro: eles descrevem semanticamente o resultado da requisição.

---

## Roteamento Manual

### O que foi feito

A lógica de seleção de rotas foi retirada do `Main` e movida para uma classe `Router`. Também foi criada a classe `HttpResponse` para centralizar a escrita de respostas HTTP.

O fluxo passou a ser:

```text
HttpServer → Router → HttpHandler → HttpResponse
```

### Pontos importantes

As rotas foram armazenadas em um mapa aninhado:

```java
Map<String, Map<String, HttpHandler>> routes;
```

Sua estrutura pode ser entendida assim:

```text
/health
└── GET → handleHealth

/hello
└── GET → handleHello
```

O primeiro mapa seleciona o caminho. O segundo seleciona o método HTTP daquele caminho.

O `Router` implementa `HttpHandler`:

```java
public final class Router implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // seleciona e chama o handler correto
    }
}
```

Isso permite registrá-lo como o handler principal do servidor. Depois de encontrar a rota, ele delega a execução:

```java
handler.handle(exchange);
```

Essa é uma versão manual do trabalho normalmente realizado por anotações como `@GetMapping` em frameworks.

---

## Modelo Job

### O que foi feito

Foi criado o primeiro modelo de domínio da aplicação usando `record`, junto aos enums `JobType` e `JobStatus`.

Um Job passou a possuir:

```java
public record Job(
        UUID id,
        JobType type,
        String content,
        JobStatus status
) {
}
```

### Pontos importantes

Um `record` é indicado para objetos cujo foco é carregar dados. O compilador gera automaticamente construtor, acessores, `equals`, `hashCode` e `toString`.

O construtor compacto foi usado para preservar as regras do modelo:

```java
public Job {
    Objects.requireNonNull(id, "id must not be null");

    if (content.isBlank()) {
        throw new IllegalArgumentException(
                "content must not be blank"
        );
    }
}
```

O método de fábrica controla a criação de novos Jobs:

```java
public static Job pending(JobType type, String content) {
    return new Job(
            UUID.randomUUID(),
            type,
            content,
            JobStatus.PENDING
    );
}
```

O cliente informa tipo e conteúdo, mas não escolhe o UUID nem o estado inicial. Essas decisões pertencem ao domínio da aplicação.

Dois Jobs com o mesmo conteúdo não são iguais se tiverem UUIDs diferentes, pois todos os componentes do record participam de `equals` por padrão.

---

## JSON manual, DTOs e validação

### O que foi feito

Foi implementada manualmente a leitura de JSON sem Jackson ou Gson. Também foram criados DTOs, validações de requisição, respostas de texto e JSON, além da exceção `InvalidRequestException`.

O parser suporta objetos JSON simples com campos textuais, espaços, escapes e caracteres Unicode.

### Pontos importantes

O corpo HTTP chega como bytes:

```java
byte[] requestBytes = requestBody.readAllBytes();
String requestJson = new String(
        requestBytes,
        StandardCharsets.UTF_8
);
```

O `InputStream` existe porque o corpo pode representar qualquer sequência de bytes, não apenas texto. A conversão para `String` só é correta depois que escolhemos explicitamente o charset.

O parser avança pelo texto mantendo uma posição:

```java
private final String text;
private int position;
```

Em vez de separar o JSON por vírgulas, ele reconhece sua estrutura caractere por caractere. Um `split(",")` falharia com conteúdo válido como:

```json
{"content":"Olá, mundo"}
```

Também foram tratados escapes:

```java
return switch (escaped) {
    case 'n' -> '\n';
    case 't' -> '\t';
    case 'u' -> readUnicodeEscape();
    default -> throw error("invalid escape sequence");
};
```

Os DTOs separam o contrato HTTP do modelo interno. Por exemplo, o cliente pode enviar somente `type` e `content`, enquanto o modelo também possui `id` e `status`.

Essa implementação mostrou parte da complexidade escondida por bibliotecas de serialização: leitura de bytes, charset, sintaxe, escapes, campos obrigatórios e transformação para objetos Java.

---

## Armazenamento em Memória

### O que foi feito

Foi criado o `InMemoryJobRepository`, inicialmente baseado em `HashMap`, para armazenar Jobs por UUID.

Foram implementadas as operações:

```text
save
findById
findAll
deleteById
```

### Pontos importantes

O mapa usa o UUID como chave:

```java
private final Map<UUID, Job> jobsById = new HashMap<>();
```

`Optional<Job>` representa explicitamente que uma busca pode ou não encontrar um resultado:

```java
return Optional.ofNullable(jobsById.get(id));
```

Ausência de um Job não é necessariamente uma falha interna. Ela pode ser transformada corretamente em `404 Not Found` pela camada HTTP.

`findAll()` devolve uma snapshot não modificável:

```java
return List.copyOf(jobsById.values());
```

Essa lista não acompanha mudanças futuras do mapa e não permite `add` ou `remove`. Uma tentativa de alteração lança `UnsupportedOperationException`.

O repositório passou a concentrar operações de armazenamento, sem conhecer regras HTTP ou regras de negócio.

---

## Camada de Serviço

### O que foi feito

Foi criada a classe `JobService` entre controller e repositório. A criação do Job passou a ser uma regra coordenada pelo serviço.

O fluxo ficou:

```text
Controller → Service → Repository
```

### Pontos importantes

Cada camada ganhou uma responsabilidade:

- o controller traduz HTTP para objetos Java;
- o service coordena regras e casos de uso;
- o repository armazena e recupera dados.

O repositório é recebido pelo construtor:

```java
public JobService(InMemoryJobRepository repository) {
    this.repository = Objects.requireNonNull(repository);
}
```

Isso é uma forma de injeção de dependência manual. O `Main` cria os objetos e conecta as dependências, trabalho que um container como o Spring faria automaticamente.

É importante que controller e service recebam a mesma instância do repositório. Duas instâncias distintas teriam mapas independentes e enxergariam dados diferentes.

---

## Endpoints de Job

### O que foi feito

Foram implementados os principais endpoints da API:

```text
POST   /jobs
GET    /jobs
GET    /jobs/{id}
DELETE /jobs/{id}
```

Também foram adicionados `JobController`, DTOs de resposta, encoders JSON e suporte a rotas dinâmicas.

### Pontos importantes

Os códigos HTTP foram escolhidos de acordo com o resultado:

- `201 Created` para criação;
- `200 OK` para consultas;
- `204 No Content` para exclusão bem-sucedida;
- `400 Bad Request` para entrada inválida;
- `404 Not Found` para Job inexistente;
- `405 Method Not Allowed` para método incompatível;
- `500 Internal Server Error` para falhas inesperadas.

Rotas como `/jobs/{id}` não podem ser encontradas apenas por igualdade de strings. O router compara os segmentos:

```text
/jobs/{id}
/jobs/abc-123
```

O segmento `{id}` aceita um valor dinâmico, mas o controller ainda é responsável por convertê-lo:

```java
UUID.fromString(idText);
```

O router também passou a funcionar como ponto central de tratamento de exceções:

```java
try {
    route(exchange);
} catch (InvalidRequestException exception) {
    HttpResponse.sendJson(exchange, 400, ...);
} catch (RuntimeException exception) {
    HttpResponse.sendJson(exchange, 500, ...);
}
```

Esse mecanismo é semelhante, em escala menor, a um tratamento global de exceções de frameworks.

---

## Aprofundamento em Threads

### O que foi feito

O processamento do Job passou a acontecer em outra Platform Thread. O Job é salvo como `PENDING`, processado como `PROCESSING` e finalizado como `COMPLETED`.

### Pontos importantes

Uma tarefa foi representada por `Runnable`:

```java
Runnable task = () -> process(id);
```

Depois, uma thread foi criada e iniciada:

```java
Thread worker = new Thread(task, "job-worker-" + id);
worker.start();
```

`start()` pede à JVM que agende uma nova thread, que posteriormente executará `run()`. Chamar `run()` diretamente não cria concorrência: o método é executado pela thread atual.

O experimento deixou visível a diferença:

```text
start() → main continua e worker processa separadamente
run()   → main executa todo o processamento
```

Também foi estudada a interrupção:

```java
catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    updateStatus(id, JobStatus.FAILED);
    return;
}
```

Restaurar a flag de interrupção evita esconder o pedido de cancelamento recebido pela thread.

---

## Executor e ExecutorService

### O que foi feito

A criação manual de uma thread por Job foi substituída por um `ExecutorService` com pool fixo de duas threads.

Também foi implementado o encerramento controlado do executor usando um shutdown hook.

### Pontos importantes

O executor separa a tarefa do mecanismo que irá executá-la:

```java
ExecutorService executor = Executors.newFixedThreadPool(2);
executor.execute(task);
```

Com cinco tarefas e duas threads, duas tarefas começam imediatamente e as demais aguardam em uma fila. Quando uma worker termina, ela reutiliza a mesma Platform Thread para executar a próxima tarefa.

Diferenças centrais:

- `Executor` oferece o contrato básico `execute(Runnable)`;
- `ExecutorService` também controla ciclo de vida, encerramento e submissão de tarefas;
- `Executors` é uma classe fábrica que cria implementações comuns.

O shutdown hook é executado durante o encerramento da JVM:

```java
Runtime.getRuntime().addShutdownHook(
        new Thread(() -> shutdownExecutor(executor))
);
```

O fluxo de encerramento foi:

```text
shutdown()
→ awaitTermination(...)
→ shutdownNow(), se necessário
```

`shutdown()` para de aceitar novas tarefas, mas permite que as já submetidas terminem.

---

## Race Condition & Synchronized

### O que foi feito

Foi criado um experimento no qual oito tarefas incrementavam o mesmo contador milhões de vezes. Primeiro foram observados resultados menores que o esperado; depois, o incremento foi protegido com `synchronized`.

### Pontos importantes

A operação abaixo não é atômica:

```java
counter++;
```

Conceitualmente, ela envolve:

```text
ler o valor
somar 1
escrever o novo valor
```

Duas threads podem ler o mesmo valor e sobrescrever uma à outra. Isso é uma race condition e produz lost updates.

A correção estudada foi:

```java
private static synchronized void increment() {
    counter++;
}
```

O monitor associado à classe permite que apenas uma thread execute o método estático sincronizado por vez. Além de exclusão mútua, `synchronized` estabelece garantias de visibilidade de memória.

O bloco crítico foi mantido pequeno: sincronizar o método que executa um milhão de incrementos faria uma thread monopolizar o lock por muito tempo e reduziria drasticamente a concorrência.

---

## Contador Atomico com AtomicInteger

### O que foi feito

O contador protegido por `synchronized` foi substituído por `AtomicInteger`. O mesmo experimento passou a atingir o valor esperado sem um bloco sincronizado explícito.

### Pontos importantes

O contador passou a ser declarado assim:

```java
private static final AtomicInteger counter =
        new AtomicInteger(0);
```

O incremento atômico é realizado por:

```java
counter.incrementAndGet();
```

E a leitura por:

```java
int obtained = counter.get();
```

Classes atômicas oferecem operações indivisíveis para atualizações simples. Internamente, elas podem usar instruções como compare-and-set, evitando um lock explícito em muitos cenários.

`AtomicInteger` é adequado para um contador independente, mas não substitui automaticamente locks quando várias variáveis precisam mudar juntas preservando uma regra comum.

---

## Experimento com ReentrantLock

### O que foi feito

Foram comparadas soluções de exclusão mútua usando `ReentrantLock`. O arquivo experimental foi removido ao final para não permanecer no código de produção.

### Pontos importantes

O uso básico exige liberar o lock em `finally`:

```java
lock.lock();

try {
    // região crítica
} finally {
    lock.unlock();
}
```

Sem o `finally`, uma exception poderia impedir o `unlock()` e bloquear indefinidamente as outras threads.

Em comparação com `synchronized`, `ReentrantLock` oferece controles adicionais:

- `tryLock()` tenta adquirir o lock sem esperar indefinidamente;
- `tryLock(timeout, unit)` limita o tempo de espera;
- `lockInterruptibly()` permite interromper a espera;
- políticas de fairness podem reduzir favorecimento repetido de certas threads;
- várias `Condition` podem ser associadas ao mesmo lock.

`synchronized` continua sendo uma boa escolha para exclusão mútua simples. `ReentrantLock` é útil quando esses controles extras são realmente necessários.

---

## Thread-Safe com ConcurrenceHashMap

### O que foi feito

O repositório deixou de usar `HashMap` e passou a usar `ConcurrentHashMap`. Também foi criada uma operação atômica de atualização por ID com `computeIfPresent`.

### Pontos importantes

O armazenamento passou a ser declarado como interface concorrente:

```java
private final ConcurrentMap<UUID, Job> jobsById =
        new ConcurrentHashMap<>();
```

`HashMap` não oferece segurança para leituras e escritas concorrentes. `ConcurrentHashMap` permite concorrência com coordenação interna mais granular do que proteger todo o mapa com um único lock externo.

Somente trocar o tipo do mapa não tornaria atômica uma sequência composta como:

```text
buscar Job
criar versão atualizada
salvar Job
```

Outra thread poderia modificar o valor entre essas etapas. Por isso a atualização passou a usar:

```java
jobsById.computeIfPresent(
        id,
        (ignoredId, currentJob) ->
                updateFunction.apply(currentJob)
);
```

Para aquela chave, leitura e substituição passam a formar uma única operação atômica do mapa.

O `UnaryOperator<Job>` representa uma função que recebe um `Job` e devolve outro `Job`:

```java
job -> job.withStatus(newStatus)
```

Isso preserva a imutabilidade do record: o Job existente não é alterado; uma nova versão é criada e substituída atomicamente.

---

## Process Jobs com Virtual Threads

### O que foi feito

O pool fixo de Platform Threads foi substituído por um executor que cria uma Virtual Thread por tarefa:

```java
Executors.newVirtualThreadPerTaskExecutor();
```

### Pontos importantes

Virtual Threads são threads administradas principalmente pela JVM. Elas são montadas sobre um conjunto menor de Platform Threads, chamadas carrier threads.

Quando uma Virtual Thread bloqueia em uma operação de I/O compatível ou em `Thread.sleep`, a JVM normalmente pode desmontá-la da carrier. A Platform Thread fica livre para executar outra Virtual Thread.

Isso favorece aplicações com muitas tarefas simultâneas que passam boa parte do tempo esperando:

```text
requisições HTTP
consultas a banco
leitura de arquivos
esperas e sleeps
```

O ganho não vem de executar cálculos mais rapidamente. Em tarefas CPU-bound, a quantidade de núcleos continua sendo o limite. Criar milhares de Virtual Threads não cria milhares de CPUs.

O modelo “uma thread por tarefa” volta a ser escalável para muitas operações bloqueantes:

```java
ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor();
```

O executor ainda precisa ser encerrado corretamente. Virtual Threads simplificam a escalabilidade, mas não eliminam ciclo de vida, interrupção, thread safety ou race conditions.

---

## Teste com CompletableFuture

### O que foi feito

Foi criado um experimento dedicado a `CompletableFuture`, executado sobre Virtual Threads. Foram estudados encadeamento, composição, combinação e tratamento de falhas.

O experimento foi posteriormente removido do código de produção.

### Pontos importantes

Uma operação assíncrona que produz um valor pode começar com:

```java
CompletableFuture<String> future =
        CompletableFuture.supplyAsync(task, executor);
```

O executor foi passado explicitamente para evitar depender silenciosamente do common pool.

Principais operações estudadas:

- `thenApply`: transforma um resultado;
- `thenCompose`: encadeia outra operação assíncrona dependente e evita futuros aninhados;
- `thenCombine`: combina duas operações independentes;
- `whenComplete`: observa sucesso ou falha sem recuperar o resultado;
- `exceptionally`: recupera uma falha com um valor alternativo;
- `handle`: trata sucesso e falha e produz um novo resultado;
- `join`: aguarda o resultado e encapsula falhas em `CompletionException`.

Exemplo de composição dependente:

```java
CompletableFuture<Integer> result = CompletableFuture
        .supplyAsync(this::findDocumentId, executor)
        .thenCompose(id -> loadDocumentAsync(id, executor))
        .thenApply(this::countWords);
```

Exemplo de trabalhos independentes:

```java
titleFuture.thenCombine(
        contentFuture,
        (title, content) -> title + content
);
```

Também foi tomada uma decisão arquitetural importante: a API não adotou `CompletableFuture` apenas para usar um recurso moderno. O processamento existente era linear e já estava bem representado por `Executor.execute()`. Abstrações devem resolver um problema real, não apenas aumentar a complexidade.

---

## Tipos de Processors e Resultado de Job

### O que foi feito

O Job passou a armazenar o resultado do processamento. Foram criados dois tipos de operação:

```text
WORD_COUNT
CHARACTER_COUNT
```

Os algoritmos foram extraídos do `JobService` para classes que implementam `JobProcessor`. Um mapa passou a selecionar o processador correspondente ao `JobType`.

### Pontos importantes

O resultado inicialmente foi representado por `Integer`, permitindo `null` enquanto o Job ainda não estivesse concluído.

O método:

```java
public Job complete(int result) {
    return new Job(
            id,
            type,
            content,
            JobStatus.COMPLETED,
            result
    );
}
```

atualiza status e resultado juntos. Isso evita expor temporariamente um estado incoerente como:

```text
status = COMPLETED
result = null
```

Os processadores seguem um contrato comum:

```java
public interface JobProcessor {
    int process(String content);
}
```

As implementações conhecem somente o algoritmo. Elas não conhecem HTTP, JSON, UUID, repositório ou threads.

O registro de estratégias foi montado no `Main`:

```java
Map<JobType, JobProcessor> processors = Map.of(
        JobType.WORD_COUNT,
        new WordCountJobProcessor(),
        JobType.CHARACTER_COUNT,
        new CharacterCountJobProcessor()
);
```

Essa estrutura evita uma sequência crescente de `if` ou `switch` dentro do serviço e aplica polimorfismo: objetos diferentes respondem ao mesmo método `process`.

A contagem de caracteres usa pontos de código Unicode:

```java
content.codePointCount(0, content.length());
```

Isso lida melhor com caracteres que ocupam duas unidades UTF-16, como muitos emojis.

---

## Resultado de model processor com tipos sealed e genericos

### O que foi feito

O resultado inteiro sem significado específico foi substituído por uma hierarquia selada de resultados:

```text
JobResult
├── WordCountResult
└── CharacterCountResult
```

Os processadores também se tornaram genéricos, declarando exatamente qual resultado produzem. O encoder JSON passou a usar pattern matching no `switch`.

### Pontos importantes

A hierarquia fechada é declarada assim:

```java
public sealed interface JobResult
        permits WordCountResult, CharacterCountResult {
}
```

Os resultados são records imutáveis e validam seus próprios valores:

```java
public record WordCountResult(int value)
        implements JobResult {

    public WordCountResult {
        if (value < 0) {
            throw new IllegalArgumentException(...);
        }
    }
}
```

Embora dois resultados possam conter o número `5`, seus tipos preservam significados diferentes:

```java
new WordCountResult(5);
new CharacterCountResult(5);
```

O contrato genérico ficou:

```java
public interface JobProcessor<R extends JobResult> {
    R process(String content);
}
```

Cada implementação especifica seu resultado:

```java
WordCountJobProcessor
        implements JobProcessor<WordCountResult>
```

Isso impede que o processador de palavras retorne acidentalmente um `CharacterCountResult`.

Como o registro contém processadores de resultados diferentes, foi usado um wildcard:

```java
Map<JobType, JobProcessor<? extends JobResult>> processors;
```

`? extends JobResult` significa “um processador de algum subtipo específico de `JobResult`”. É possível tratar o valor produzido como `JobResult`, sem perder a especialização de cada implementação.

O encoder usa pattern matching:

```java
return switch (result) {
    case WordCountResult wordCount ->
            wordCount.value();

    case CharacterCountResult characterCount ->
            characterCount.value();
};
```

Como `JobResult` é selada, o compilador conhece todas as possibilidades e o `switch` não precisa de `default`. Se um novo resultado for permitido futuramente, o compilador indicará os switches que precisam ser atualizados.

Essa etapa reuniu recursos modernos do Java 21 com uma finalidade concreta: preservar significado, aumentar a segurança de tipos e detectar casos ausentes durante a compilação.
