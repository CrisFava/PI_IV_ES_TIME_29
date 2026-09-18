# Converge

> Mobilidade universitária compartilhada, mais econômica, organizada, segura e sustentável.

O **Converge** é uma proposta de plataforma para conectar estudantes universitários que realizam trajetos e horários compatíveis. O projeto busca substituir a dependência de grupos informais — como grupos de WhatsApp — por uma experiência centralizada para encontrar, organizar e acompanhar caronas.

Além de aproximar motoristas e passageiros, a solução prevê divisão transparente dos custos, verificação de vínculo acadêmico, avaliações e recursos de segurança. Também propõe indicadores de mobilidade e sustentabilidade, como quilômetros compartilhados, veículos potencialmente evitados e estimativa de CO₂ evitado.

O projeto foi elaborado no componente curricular **Ideação e Validação em Engenharia de Software** da Escola Politécnica da PUC-Campinas.

## Objetivos

- Conectar estudantes com rotas e horários compatíveis.
- Tornar a organização das caronas mais previsível e segura.
- Estimar e dividir os custos de viagem de forma transparente.
- Fortalecer a confiança por meio da verificação universitária, histórico, avaliações, denúncias e bloqueios.
- Reduzir o uso de veículos individuais e apoiar a mobilidade sustentável no ambiente universitário.

## Funcionalidades previstas para o MVP

- Cadastro de usuário e verificação de vínculo com a universidade.
- Cadastro de trajetos para oferecer ou procurar carona.
- Matching de caronas por origem, destino, horário, dias da semana e desvio aceitável.
- Visualização dos perfis e combinações compatíveis.
- Chat entre participantes e detalhes da viagem.
- Cálculo e rateio dos custos, com informações para pagamento via PIX.
- Histórico de viagens e avaliações mútuas.
- Denúncia e bloqueio de usuários.
- Indicadores de mobilidade e impacto ambiental.

## Tecnologias

O aplicativo atualmente presente no repositório é desenvolvido em **Dart** e **Flutter**. Conforme o relatório de ideação, a arquitetura planejada também contempla:

- Backend: Java 21, Spring Boot 3 e MongoDB;
- Serviços: Google Maps API, Firebase Storage e Firebase Realtime Database;
- Qualidade e infraestrutura: JUnit, Flutter Test e Docker.

## Integrantes

| Integrante                      | RA                         |
| ------------------------------- | -------------------------- |
| Allan Giovanni Matias Paes      | 25008211                   |
| Cristian Eduardo Fava           | 25000636                   |
| Daniela Mikie Kikuchi Gonçalves | Não informado no relatório |
| Gustavo Alves de Siqueira Costa | 25001650                   |
| Sara Fernandes Monteiro         | 25024107                   |

## Estrutura do projeto

```text
.
├── README.md
├── app/
│   └── converge/                 # Aplicativo Flutter
│       ├── lib/
│       │   └── main.dart          # Ponto de entrada da aplicação
│       ├── android/               # Configuração nativa Android
│       ├── ios/                   # Configuração nativa iOS
│       ├── analysis_options.yaml  # Regras de análise estática
│       └── pubspec.yaml           # Dependências e metadados Flutter
└── docs/
    ├── Relatório_PI_IV.pdf        # Relatório de ideação do Converge
    ├── MapaMental.pdf             # Mapa mental da solução
    ├── Business Model Canvas.png   # Canvas do modelo de negócio
    └── Telas/                      # Wireframes e telas do aplicativo
```

## Como executar o aplicativo

Pré-requisito: ter o [Flutter](https://docs.flutter.dev/get-started/install) instalado e configurado.

```bash
cd app/converge
flutter pub get
flutter run
```
