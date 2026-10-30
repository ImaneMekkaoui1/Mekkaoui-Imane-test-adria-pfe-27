# PLATEFORME BANCAIRE MICROSERVICES - RAPPORT D'ARCHITECTURE ET DE CONCEPTION

> **Épreuve Technique de Sélection — Architecture micro-services, IA et développement full-stack**  
> **Réalisation : Parties A à F**  
> **Auteur : Candidat Adria PFE 2026**

---

## 1. Schéma d'Architecture Cible

L'architecture est découpée en microservices métiers et techniques selon les principes de Spring Cloud, EDA (Event-Driven Architecture) avec Apache Kafka, communication synchrones résilientes (OpenFeign + Resilience4J), connecteurs unifiés (REST, GraphQL, MCP), et agent conversationnel basé sur le Model Context Protocol (MCP) et Spring AI.

```mermaid
flowchart TB
    subgraph Clients["Canaux Utilisateurs & Clients"]
        WEB["🌐 Interface Web / Angular"]
        MOB["📱 Client Mobile"]
        TG["💬 Bot Telegram"]
        DISC["🎮 Bot Discord"]
        REST_CLI["💻 Client HTTP / REST (Postman/Curl)"]
    end

    subgraph Technical["Microservices Techniques"]
        GW["🚪 Spring Cloud Gateway\n(Port: 8080)"]
        EUR["🔍 Eureka Discovery Server\n(Port: 8761)"]
        CONF["⚙️ Spring Cloud Config Server\n(Port: 8888)\nbank.transaction.max-amount=1000"]
    end

    subgraph AI_Layer["Couche IA & Assistant Agentique"]
        CHAT["🤖 Chatbot Agentic Service\n(Port: 8084)\nSpring AI + MCP Client"]
    end

    subgraph Business["Microservices Métier"]
        direction TB
        subgraph ACC_SRV["🏦 Account-Service (Port: 8081)"]
            ACC_REST["REST Controller (/api/accounts)"]
            ACC_GQL["GraphQL (/graphql)"]
            ACC_MCP["MCP Server (/mcp)"]
            ACC_SERVICE["AccountService"]
            ACC_REPO["AccountRepository (JPA)"]
            ACC_DB[("H2 Database\naccountdb")]
            
            ACC_REST --> ACC_SERVICE
            ACC_GQL --> ACC_SERVICE
            ACC_MCP --> ACC_SERVICE
            ACC_SERVICE --> ACC_REPO
            ACC_REPO --> ACC_DB
        end

        subgraph TX_SRV["💳 Transaction-Service (Port: 8082)"]
            TX_REST["REST Controller (/api/transactions)"]
            TX_GQL["GraphQL (/graphql)"]
            TX_MCP["MCP Server (/mcp)"]
            TX_SERVICE["TransactionService"]
            TX_FEIGN["OpenFeign + Resilience4J\nCircuitBreaker & Fallback"]
            TX_KAFKA_PROD["Kafka Event Producer"]
            TX_REPO["TransactionRepository (JPA)"]
            TX_DB[("H2 Database\ntransactiondb")]

            TX_REST --> TX_SERVICE
            TX_GQL --> TX_SERVICE
            TX_MCP --> TX_SERVICE
            TX_SERVICE --> TX_REPO
            TX_REPO --> TX_DB
            TX_SERVICE --> TX_FEIGN
            TX_SERVICE --> TX_KAFKA_PROD
        end
    end

    subgraph EventStreaming["Événements Asynchrones (Kafka)"]
        TOPIC[["📨 Topic: bank.transactions.created\n(Key: accountId)"]]
        AUDIT_SRV["🛡️ Audit-Service (Port: 8083)\nKafka Consumer (audit-group)\nJournalisation & Audit Trail"]
        AUDIT_DB[("H2 Database\nauditdb")]
    end

    %% Interactions
    Clients --> GW
    GW --> ACC_SRV
    GW --> TX_SRV
    GW --> CHAT
    GW --> AUDIT_SRV

    %% Discovery & Config
    ACC_SRV -.-> EUR
    TX_SRV -.-> EUR
    GW -.-> EUR
    CHAT -.-> EUR
    AUDIT_SRV -.-> EUR

    ACC_SRV -.-> CONF
    TX_SRV -.-> CONF

    %% Inter-service Feign
    TX_FEIGN -- "GET /api/accounts/{id}\nPUT /api/accounts/{id}/balance" --> ACC_SRV

    %% MCP Agent interaction
    CHAT -- "tools/call (MCP JSON-RPC)" --> ACC_MCP
    CHAT -- "tools/call (MCP JSON-RPC)" --> TX_MCP

    %% Kafka
    TX_KAFKA_PROD -- "TransactionCreatedEvent" --> TOPIC
    TOPIC --> AUDIT_SRV
    AUDIT_SRV --> AUDIT_DB
```

---

## 2. Diagramme de Classes Métier

Le diagramme ci-dessous illustre le modèle objet et les contrats de données selon les spécifications :

```mermaid
classDiagram
    direction TB

    class AccountStatus {
        <<enumeration>>
        ACTIVE
        BLOCKED
    }

    class BankAccount {
        -UUID id
        -String customerReference
        -AccountStatus status
        -String currency
        -double balance
        +prePersist()
    }

    class AccountDTO {
        <<record>>
        +UUID id
        +String customerReference
        +AccountStatus status
        +String currency
        +double balance
    }

    class CreateAccountRequest {
        <<record>>
        +String customerReference
        +String currency
        +double initialBalance
    }

    class UpdateBalanceRequest {
        <<record>>
        +double amount
        +String operationType
    }

    class BankTransaction {
        -UUID id
        -UUID accountId
        -BigDecimal amount
        -String currency
        -String merchantCategory
        -String country
        -Instant createdAt
        +prePersist()
    }

    class TransactionDTO {
        <<record>>
        +UUID id
        +UUID accountId
        +BigDecimal amount
        +String currency
        +String merchantCategory
        +String country
        +Instant createdAt
    }

    class CreateTransactionRequest {
        <<record>>
        +UUID accountId
        +BigDecimal amount
        +String currency
        +String merchantCategory
        +String country
    }

    class TransferRequest {
        <<record>>
        +UUID sourceAccountId
        +UUID targetAccountId
        +BigDecimal amount
        +String currency
    }

    class TransferResult {
        <<record>>
        +UUID sourceTransactionId
        +UUID targetTransactionId
        +String status
        +String message
    }

    class TransactionCreatedEvent {
        <<record>>
        +UUID eventId
        +String eventType
        +UUID transactionId
        +UUID accountId
        +BigDecimal amount
        +String currency
        +Instant occurredAt
    }

    class AuditEvent {
        -UUID id
        -UUID eventId
        -String eventType
        -UUID transactionId
        -UUID accountId
        -BigDecimal amount
        -String currency
        -Instant occurredAt
        -Instant receivedAt
    }

    BankAccount "1" --> "1" AccountStatus
    BankAccount ..> AccountDTO : converts to
    CreateAccountRequest ..> BankAccount : creates
    UpdateBalanceRequest ..> BankAccount : modifies

    BankTransaction ..> TransactionDTO : converts to
    BankTransaction ..> TransactionCreatedEvent : generates
    CreateTransactionRequest ..> BankTransaction : creates
    TransferRequest ..> TransferResult : returns

    TransactionCreatedEvent ..> AuditEvent : stored as
```

---

## 3. Séparation Stricte des Couches (Règle d'Architecture)

Pour chaque microservice métier (`account-service` et `transaction-service`), une séparation rigoureuse en 6 couches a été appliquée :

1. **`domain/`** : Entités JPA, DTOs (Records Java 21), requêtes validées avec Bean Validation (`@Valid`), enums et exceptions métier.
2. **`repository/`** : Interfaces Spring Data JPA étendant `JpaRepository`.
3. **`service/`** : Couche d'orchestration et logique métier (`AccountService`, `TransactionService`, `AccountServiceClient` OpenFeign, `TransactionEventProducer`).
4. **`web/rest/`** : Contrôleurs REST (`@RestController`) exposant les endpoints HTTP CRUD standards et gestionnaire d'exceptions global (`@RestControllerAdvice`).
5. **`web/graphql/`** : Contrôleurs Spring GraphQL (`@Controller`, `@QueryMapping`, `@MutationMapping`) s'appuyant sur les schémas `schema.graphqls`.
6. **`mcp/`** : Implémentation du protocole **Model Context Protocol (Anthropic MCP standard)** exposant les définitions de tools (`tools/list`) et leur exécution (`tools/call`) via JSON-RPC 2.0 et endpoints REST.

> **Règle fondamentale respectée** : Les contrôleurs REST, GraphQL et MCP appellent **strictement la même couche service**, garantissant l'intégrité et la centralisation des règles métier bancaires.

---

## 4. Règles Métier et Tolérance aux Pannes

### 4.1. Règles Métier dans `transaction-service`
1. **Montant strictement positif** : Tout montant $\le 0$ lève immédiatement une `TransactionException`.
2. **Plafond configuré** : Injection dynamique du paramètre `bank.transaction.max-amount=1000` depuis le `config-service`. Toute transaction ou virement dépassant ce plafond est rejeté.
3. **Existence et statut actif du compte** : Appel préalable de l'API de `account-service` via OpenFeign. Si le statut est `BLOCKED`, la transaction est refusée.
4. **Concordance des devises** : La devise de la transaction (ex: `EUR`) doit concorder exactement avec la devise du compte.
5. **Vérification et mise à jour du solde** : Le solde disponible est vérifié avant persistance. Après l'enregistrement réussi de la transaction en base, le solde du compte est débité via OpenFeign.
6. **Virement inter-comptes** : Débit du compte émetteur, crédit du compte bénéficiaire, enregistrement de deux transactions miroir et publication des événements Kafka correspondants.

### 4.2. Tolérance aux Pannes avec Resilience4J
- Intégration de **Spring Cloud OpenFeign** et **Resilience4J CircuitBreaker**.
- Décoration de l'interface client `AccountServiceClient` avec un `fallback` dédié (`AccountServiceClientFallback`).
- `@CircuitBreaker(name = "accountService", fallbackMethod = "createTransactionFallback")` sur les méthodes sensibles du service.
- En cas de panne, ralentissement ou indisponibilité de `account-service`, le disjoncteur bascule en mode ouvert et invoque une réponse dégradée sécurisée sans bloquer le thread ni propager d'erreur fatale.

---

## 5. Agent Conversationnel et Protocole MCP (Partie E)

Le microservice `chatbot-service` (Port 8084) réalise l'assistant bancaire agentique :
- Connecté en tant que client MCP aux serveurs MCP de `account-service` et `transaction-service`.
- Découvre dynamiquement les outils bancaires (`get_account_by_id`, `create_bank_account`, `update_account_balance`, `create_transaction`, `transfer_funds`, `list_all_accounts`).
- Analyse en langage naturel la requête utilisateur (NL Intent Resolution).
- Exécute le tool approprié et retourne une réponse claire et sécurisée.
- Exposé via un endpoint REST `POST /api/chat` et adaptable directement pour Telegram et Discord.

---

## 6. Communication Asynchrone avec Apache Kafka (Partie F)

1. **Producteur (dans `transaction-service`)** :
   - Publication asynchrone après persistance dans le topic `bank.transactions.created`.
   - Utilisation explicite de `accountId` comme clé de partitionnement Kafka (`key = accountId.toString()`).
   - Format d'événement conforme à la spécification :
     ```json
     {
       "eventId": "UUID",
       "eventType": "TransactionCreated",
       "transactionId": "UUID",
       "accountId": "UUID",
       "amount": 250.00,
       "currency": "EUR",
       "occurredAt": "2026-01-15T10:30:00Z"
     }
     ```
2. **Consommateur (dans `audit-service`)** :
   - `@KafkaListener` sur le topic avec groupe `audit-group`.
   - Journalisation structurée SLF4J de l'événement.
   - Enregistrement dans la table `audit_events` pour audit trail, **sans aucune modification des données bancaires**.

---

## 7. Guide d'Exécution et Tests

### Compilation et validation du projet complet :
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH=$JAVA_HOME/bin:$PATH
mvn clean test
```

### Lancement avec Docker Compose :
```bash
docker-compose up --build
```

