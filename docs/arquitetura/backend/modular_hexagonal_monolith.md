# Arquitetura Backend: Monólito Modular com Spring Boot e MongoDB

## 1. Visão Geral

Este documento descreve a arquitetura oficial do backend do projeto. O sistema adota o padrão de **Monólito Modular** (_Modular Monolith_), orientado a conceitos de **Arquitetura Hexagonal (Ports & Adapters)** com **Spring Boot** e **MongoDB** como banco de dados orientado a documentos.

A meta desta arquitetura é garantir:

- **Alta Coesão e Baixo Acoplamento:** Cada módulo de negócio é autônomo e isolado.
- **Simplicidade de Operação:** Único artefato de deploy e infraestrutura simples.
- **Flexibilidade de Provedores Externos:** Integrações com nuvem (AWS S3, Google Maps) desacopladas por interfaces, alternáveis diretamente via `application.yaml`.
- **Pragmatismo com MongoDB:** Uso nativo de documentos e repositórios do Spring Data, eliminando mapeamentos desnecessários e complexidades relacionais.

---

## 2. Estrutura Geral de Pastas e Pacotes

A aplicação é dividida em dois grandes blocos: **`modules/`** (funcionalidades de negócio) e **`shared/`** (recursos transversais e infraestrutura compartilhada).

```text
src/main/java/br/com/projeto/
│
├── ProjetoApplication.java                     # Inicializador do Spring Boot
│
├── modules/                                    # Domínios de Negócio (Bounded Contexts)
│   │
│   └── {nomeModulo}/                           # Ex: usuario, pedido, etc.
│       ├── application/                        # Casos de Uso (Orquestração de regras)
│       │   └── {moduloUseCase}                 # Classes UseCase (ex: CriarUsuarioUseCase)
│       │
│       ├── domain/                             # Núcleo de Domínio
│       │   ├── enums/                          # Enums de negócio (ex: StatusUsuario)
│       │   ├── model/                          # Entidades Documento (@Document)
│       │   └── repository/                     # Interfaces MongoRepository
│       │
│       ├── infrastructure/                     # Adaptadores e integrações do módulo
│       │   ├── listener/                       # Ouvintes de eventos internos do Spring
│       │   └── client/                         # Feign/WebClient específicos do módulo
│       │
│       └── presentation/                       # Porta de entrada HTTP
│           ├── controller/                     # Controladores REST (@RestController)
│           └── dtos/                           # DTOs de Request e Response da API
│
└── shared/                                     # Recursos Transversais e Infraestrutura
    ├── config/                                 # Configurações globais (MongoConfig, Swagger, CORS)
    ├── exception/                              # Tratamento global de erros e exceções base
    ├── security/                               # Segurança, filtros JWT e autenticação
    └── infrastructure/                         # Portas e Adaptadores de Nuvem / APIs Externas
        ├── storage/                            # Provedor de Upload (AWS S3 / Local)
        └── maps/                               # Provedor de Geolocalização (Google Maps / Mock)
```

---

## 3. Responsabilidades das Camadas nos Módulos (`modules/{nomeModulo}`)

<img src="../../assets/mermaid-arch.png">

### 3.1. `domain/` (Abordagem Pragmática com MongoDB)

O MongoDB não possui o rigor de tabelas relacionais ou impedâncias de ORMs clássicos. Por isso, adota-se a **abordagem pragmática**:

- **Documentos (`@Document`):** As entidades de domínio contêm as anotações do Spring Data Mongo (`@Document`, `@Id`, `@Indexed`).
- **Repositórios:** Interfaces que estendem diretamente `MongoRepository<T, ID>`, permitindo queries por convenção e consultas `@Query`.
- **Enums:** Estados e tipos imutáveis do domínio.

**Exemplo (`domain/model/Usuario.java`):**

```java
package br.com.projeto.modules.usuario.domain.model;

import br.com.projeto.modules.usuario.domain.enums.PerfilUsuario;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String nome;

    @Indexed(unique = true)
    private String email;

    private PerfilUsuario perfil;

    private String fotoUrl;

    private Instant criadoEm;

    public Usuario(String nome, String email, PerfilUsuario perfil) {
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.criadoEm = Instant.now();
    }

    // Getters e métodos de negócio
    public void atualizarFoto(String url) {
        this.fotoUrl = url;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public PerfilUsuario getPerfil() { return perfil; }
    public String getFotoUrl() { return fotoUrl; }
}
```

**Exemplo (`domain/repository/UsuarioRepository.java`):**

```java
package br.com.projeto.modules.usuario.domain.repository;

import br.com.projeto.modules.usuario.domain.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

### 3.2. `application/` (Casos de Uso)

- Orquestra as regras de negócio.
- Não lida diretamente com HTTP (não conhece `HttpServletRequest`, `HttpStatus`, etc.).
- Invoca o `Repository` do módulo e interfaces de infraestrutura necessárias (como upload de arquivos ou geolocalização).

**Exemplo (`application/usecase/CriarUsuarioUseCase.java`):**

```java
package br.com.projeto.modules.usuario.application.usecase;

import br.com.projeto.modules.usuario.domain.enums.PerfilUsuario;
import br.com.projeto.modules.usuario.domain.model.Usuario;
import br.com.projeto.modules.usuario.domain.repository.UsuarioRepository;
import br.com.projeto.shared.exception.NegocioException;
import org.springframework.stereotype.Service;

@Service
public class CreateUserUseCase {

    private final UserRepository userRepository;

    public CreateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User executar(String nome, String email, PerfilUsuario perfil) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("E-mail já cadastrado na plataforma.");
        }

        User novoUsuario = new User(nome, email, perfil);
        return userRepository.save(novoUsuario);
    }
}
```

---

### 3.3. `presentation/` (Controllers e DTOs)

- Ponto de contato com o cliente (Frontend / Mobile).
- **`dtos/`**: Records Java que representam o corpo de requisições de entrada (_Requests_) com anotações de validação (`jakarta.validation`), e respostas de saída (_Responses_).
- **`controller/`**: Endpoints REST anotados com `@RestController` que validam a entrada, delegam a execução para o Use Case correspondente e mapeiam a resposta para o DTO de saída.

**Exemplo DTOs (`presentation/dtos/`):**

```java
package br.com.projeto.modules.usuario.presentation.dtos;

import br.com.projeto.modules.usuario.domain.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @NotNull(message = "O perfil é obrigatório")
    PerfilUsuario perfil
) {}
```

```java
package br.com.projeto.modules.usuario.presentation.dtos;

import br.com.projeto.modules.usuario.domain.enums.PerfilUsuario;
import br.com.projeto.modules.usuario.domain.model.Usuario;

public record UserResponseDTO(
    String id,
    String nome,
    String email,
    PerfilUsuario perfil,
    String fotoUrl
) {
    public static UserResponseDTO from(User usuario) {
        return new UserResponseDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil(),
            usuario.getFotoUrl()
        );
    }
}
```

**Exemplo Controller (`presentation/controller/UsuarioController.java`):**

```java
package br.com.projeto.modules.usuario.presentation.controller;

import br.com.projeto.modules.usuario.application.usecase.CriarUsuarioUseCase;
import br.com.projeto.modules.usuario.domain.model.Usuario;
import br.com.projeto.modules.usuario.presentation.dtos.CriarUsuarioRequest;
import br.com.projeto.modules.usuario.presentation.dtos.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> criar(@RequestBody @Valid CriarUsuarioRequest request) {
        User usuario = createUserUseCase.executar(request.nome(), request.email(), request.perfil());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
    }
}
```

---

### 3.4. `infrastructure/` (Dentro do Módulo)

Destinada a componentes específicos do módulo que lidam com detalhes de sistema ou eventos:

- Ouvintes de eventos do Spring (`@EventListener` ou `@TransactionalEventListener`).
- Clients HTTP Feign específicos do módulo para comunicação com outros serviços.

---

## 4. Camada Compartilhada (`shared/`) e Suporte Multiplas Implementações via `application.yaml`

Para viabilizar a troca de provedores de nuvem ou APIs externas alterando apenas o arquivo `application.yaml`, utiliza-se o padrão **Ports and Adapters** com anotações `@ConditionalOnProperty` do Spring.

<img src='../../assets/mermaid-hexa.png'>

---

### 4.1. Provedor de Armazenamento de Arquivos: AWS S3 vs Local (exemplo)

#### 1. A Porta / Interface (`shared/infrastructure/storage/contract/StorageService.java`):

```java
package br.com.projeto.shared.infrastructure.storage.contract;

import java.io.InputStream;

public interface StorageService {
    String upload(String nomeArquivo, String contentType, InputStream dados, long tamanho);
    void remover(String arquivoUrl);
}
```

#### 2. Implementação AWS S3 (`shared/infrastructure/storage/impl/AwsS3StorageService.java`):

Ativada quando `app.storage.provider=aws`:

```java
package br.com.projeto.shared.infrastructure.storage.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "aws")
public class AwsS3StorageService implements StorageService {

    // Injeção do S3Client do AWS SDK v2
    @Override
    public String upload(String nomeArquivo, String contentType, InputStream dados, long tamanho) {
        // Envia para o bucket S3 e retorna a URL pública gerada
        return "https://meu-bucket.s3.amazonaws.com/" + nomeArquivo;
    }

    @Override
    public void remover(String arquivoUrl) {
        // Remove o objeto no S3
    }
}
```

#### 3. Implementação Local / Desenvolvimento (`shared/infrastructure/storage/impl/LocalStorageService.java`):

Ativada quando `app.storage.provider=local` ou se a propriedade não estiver configurada:

```java
package br.com.projeto.shared.infrastructure.storage.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Override
    public String upload(String nomeArquivo, String contentType, InputStream dados, long tamanho) {
        // Salva na pasta local /uploads e retorna URL de endpoint estático
        return "http://localhost:8080/uploads/" + nomeArquivo;
    }

    @Override
    public void remover(String arquivoUrl) {
        // Remove o arquivo do disco local
    }
}
```

---

### 4.2. Provedor de Geolocalização: Google Maps API vs Mock (exemplo)

#### 1. A Porta / Interface (`shared/infrastructure/maps/contract/GeoLocationService.java`):

```java
package br.com.projeto.shared.infrastructure.maps.contract;

public interface GeoLocationService {
    GeoCoordenadas obterCoordenadasPorEndereco(String endereco);
    Double calcularDistanciaKm(GeoCoordenadas origem, GeoCoordenadas destino);

    record GeoCoordenadas(double latitude, double longitude) {}
}
```

#### 2. Implementação com Google Maps (`shared/infrastructure/maps/impl/GoogleMapsService.java`):

Ativada quando `app.maps.provider=google`:

```java
package br.com.projeto.shared.infrastructure.maps.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.maps.provider", havingValue = "google")
public class GoogleMapsService implements GeoLocationService {

    @Override
    public GeoCoordenadas obterCoordenadasPorEndereco(String endereco) {
        // Chamada via Google Maps Geocoding API usando RestClient ou Google Maps SDK
        return new GeoCoordenadas(-23.550520, -46.633308);
    }

    @Override
    public Double calcularDistanciaKm(GeoCoordenadas origem, GeoCoordenadas destino) {
        // Chamada via Google Maps Distance Matrix API
        return 5.2;
    }
}
```

#### 3. Implementação Mock para Testes/Dev (`shared/infrastructure/maps/impl/MockMapsService.java`):

Ativada quando `app.maps.provider=mock` ou ausente:

```java
package br.com.projeto.shared.infrastructure.maps;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.maps.provider", havingValue = "mock", matchIfMissing = true)
public class MockMapsService implements GeoLocationService {

    @Override
    public GeoCoordenadas obterCoordenadasPorEndereco(String endereco) {
        // Retorna coordenadas fixas para testes sem gastar cota de API
        return new GeoCoordenadas(0.0, 0.0);
    }

    @Override
    public Double calcularDistanciaKm(GeoCoordenadas origem, GeoCoordenadas destino) {
        return 10.0;
    }
}
```

---

### 4.3. Exemplo de Configuração no `application.yaml`

Para alternar entre nuvem e ambiente local de desenvolvimento, basta alterar os valores no arquivo de configuração:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/projeto_db

# Configurações de Provedores Alternáveis
app:
  storage:
    provider: local # Opções: 'local' ou 'aws'
    aws:
      bucket-name: meu-bucket-prod
      region: sa-east-1
      access-key: ${AWS_ACCESS_KEY_ID:}
      secret-key: ${AWS_SECRET_ACCESS_KEY:}

  maps:
    provider: mock # Opções: 'mock' ou 'google'
    google:
      api-key: ${GOOGLE_MAPS_API_KEY:}
```

---

## 5. Tratamento de Exceções Global (`shared/exception`)

O backend padroniza as respostas de erro usando o padrão oficial **RFC 7807 (Problem Details)** do Spring Boot:

### 5.1. Exceção Base de Negócio

```java
package br.com.projeto.shared.exception;

// supondo que tenhamos um enum com vários códigos e etc.
@Data
public class NegocioException extends RuntimeException {
    private final ErrorCode code;


    public NegocioException(String mensagem, ErrorCode code) {
        super(mensagem);
        this.code = code;
    }


    public ErrorCode getCode(){
        return this.code;
    }
}
```

### 5.2. Manipulador Global com `@RestControllerAdvice`

```java
package br.com.projeto.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleException(BusinessException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Violação de Regra de Negócio");
        problemDetail.setType(URI.create("http://urlProjeto.com:8080/problems/business"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetails.setProperty("code", ex.getCode());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erro de validação nos campos informados.");
        problemDetail.setTitle("Dados Inválidos");

        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> erros.put(f.getField(), f.getDefaultMessage()));
        problemDetail.setProperty("errors", erros);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("code", ErrorCode.MethodArgumentNotValid.value());
        return problemDetail;
    }
}
```

---

## 6. Comunicação entre Módulos

Como o sistema é um monólito com MongoDB:

1. **Isolamento de Coleções:** Cada módulo é dono de suas coleções (`collection = "usuarios"`, `collection = "pedidos"`). O módulo de `pedidos` **nunca** executa queries diretas na coleção `usuarios`.
2. **Referência por Identificador Simples:** Para relacionar dados entre módulos, armazena-se apenas o ID (`String usuarioId`). Não são utilizadas referências cruzadas complexas (`@DBRef`).
3. **Comunicação Síncrona:** Se um caso de uso precisa de dados de outro módulo, ele injeta e invoca o caso de uso de leitura daquele módulo (ex: `ConsultarUsuarioPorIdUseCase`), recebendo um DTO imutável.
4. **Comunicação Assíncrona por Eventos:** Para processos que geram efeitos secundários (ex: envio de e-mail ou auditoria), o módulo emissor publica um evento via `ApplicationEventPublisher`, e o listener no pacote `infrastructure/listener` do módulo destinatário consome o evento.

---

## 7. Checklist para Criação de um Novo Módulo

Ao criar uma nova funcionalidade no sistema:

1. Criar a pasta do módulo em `src/main/java/br/com/projeto/modules/{novoModulo}/`.
2. Criar os quatro subpacotes:
   - `application/`
   - `domain/model/`, `domain/repository/` e `domain/enums/`
   - `infrastructure/` (se houver listeners ou adaptadores específicos)
   - `presentation/controller/` e `presentation/dtos/`
3. Mapear o documento com `@Document(collection = "{novoModulo}")`.
4. Criar a interface de repositório estendendo `MongoRepository`.
5. Se necessitar de serviços de nuvem ou externos (upload, mapas), injetar as interfaces presentes em `br.com.projeto.shared.infrastructure.*`.
