# Demo API — Catálogo de Produtos

Este é um projeto backend desenvolvido em Java 17 com Spring Boot 4. A ideia foi construir uma API REST de catálogo de produtos que fosse simples de entender, mas que ao mesmo tempo cobrisse os tópicos mais cobrados no dia a dia de um desenvolvedor backend Java: ORM, migrações de banco, segurança, mensageria assíncrona, profiles de ambiente e testes unitários.

O projeto usa PostgreSQL como banco principal, RabbitMQ para notificações assíncronas ao criar um produto, Spring Security para proteger os endpoints de escrita, e Flyway para controlar as migrações de banco de forma versionada.

---

## Tecnologias utilizadas

O projeto foi construído com Spring Boot 4 gerenciando as versões de todas as dependências. Para acesso ao banco de dados, foi utilizado Spring Data JPA com Hibernate. A segurança dos endpoints é feita com Spring Security usando autenticação HTTP Basic e BCrypt para hash de senha. A comunicação com o RabbitMQ acontece via Spring AMQP. As validações de entrada são feitas com Bean Validation. Para os testes, o stack é JUnit 5 com Mockito e AssertJ rodando contra um banco H2 em memória, portanto não é necessário nenhuma infraestrutura para rodar os testes.

---

## Como o projeto está organizado

```
src/main/java/com/mblabs/demo/
├── DemoApplication.java
├── config/
│   ├── RabbitMQConfig.java
│   └── SecurityConfig.java
├── controller/
│   └── ProductController.java
├── domain/
│   └── Product.java
├── dto/
│   ├── CreateProductRequest.java
│   └── ProductResponse.java
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
│   └── ProductRepository.java
└── service/
    ├── ProductService.java
    └── ProductNotificationService.java
```

A separação segue a arquitetura em camadas clássica: o controller recebe a requisição HTTP e a repassa para o service, que contém as regras de negócio e chama o repository para persistência. A entidade de domínio fica isolada em seu próprio pacote, e os DTOs garantem que o contrato da API não dependa da estrutura interna da entidade.

---

## Pré-requisitos

Para rodar o projeto localmente você vai precisar de:

- JDK 17 ou superior
- PostgreSQL rodando localmente com um banco chamado `demodb`
- RabbitMQ é opcional — a aplicação trata a ausência do broker com um `try/catch` e apenas loga um aviso

---

## Configuração do banco de dados

Crie o banco antes de subir a aplicação:

```sql
CREATE DATABASE demodb;
```

As credenciais padrão configuradas no profile de desenvolvimento são usuário `postgres` e senha `root`. Você pode alterar isso em `src/main/resources/application-dev.properties`.

---

## Rodando a aplicação

O projeto usa Maven Wrapper, então não é necessário ter o Maven instalado globalmente. Certifique-se de que a variável `JAVA_HOME` aponta para o JDK 17.

```bash
./mvnw spring-boot:run
```

No Windows com PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação sobe na porta `8080` com o profile `dev` ativo por padrão.

---

## Rodando os testes

```bash
./mvnw test
```

Os testes usam H2 em memória e não precisam de banco ou RabbitMQ rodando. O profile `test` é ativado automaticamente durante a execução dos testes e isola completamente o ambiente de infraestrutura.

---

## Endpoints disponíveis

Todos os endpoints de leitura são públicos. Os endpoints de escrita (criar e desativar produto) exigem autenticação Basic com as credenciais `admin` / `admin123`.

### Criar um produto

```
POST /api/products
Authorization: Basic admin admin123
Content-Type: application/json

{
  "name": "Notebook",
  "description": "Dell XPS 15",
  "price": 5000.00,
  "category": "Eletronicos"
}
```

Retorna `201 Created` com o produto criado no corpo da resposta.

### Listar todos os produtos ativos

```
GET /api/products
```

### Buscar produto por ID

```
GET /api/products/{id}
```

Retorna `404 Not Found` com uma mensagem JSON caso o produto não exista.

### Filtrar por categoria

```
GET /api/products/category/{category}
```

### Desativar produto

```
DELETE /api/products/{id}
Authorization: Basic admin admin123
```

Retorna `204 No Content`. O registro não é apagado do banco — o campo `active` é setado como `false` (soft delete).

---

## Profiles de ambiente

O projeto tem três profiles configurados:

**dev** é o profile padrão para desenvolvimento local. Usa PostgreSQL local, `ddl-auto=update` para que o Hibernate gerencie o schema automaticamente, e Flyway desabilitado para não conflitar com o gerenciamento do Hibernate.

**prod** é o profile para produção. Todo valor sensível é lido de variáveis de ambiente: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD`. Flyway está habilitado e `ddl-auto=validate` garante que o schema do banco está de acordo com as entidades.

**test** é ativado automaticamente nos testes. Usa H2 em memória com modo de compatibilidade PostgreSQL, `ddl-auto=create-drop` e exclui a auto-configuração do RabbitMQ para que os testes rodem sem infraestrutura.

---

## Mensageria com RabbitMQ

Toda vez que um produto é criado, a aplicação tenta enviar uma mensagem para a fila `products.queue` via a exchange `products.exchange` com a routing key `products.created`. Esse envio acontece de forma assíncrona em uma thread separada usando `@Async`, então o cliente que fez o POST recebe a resposta imediatamente sem esperar o RabbitMQ.

Caso o RabbitMQ não esteja disponível, a exceção é capturada e um aviso é registrado no log. A criação do produto não é afetada.

---

## Migração de banco com Flyway

O arquivo `src/main/resources/db/migration/V1__create_products_table.sql` contém o script de criação da tabela `products`. Em produção, o Flyway executa esse script na primeira inicialização e registra na tabela `flyway_schema_history` que a versão 1 já foi aplicada. Nas inicializações seguintes, ele não re-executa. Para evoluir o schema no futuro, basta adicionar um `V2__...sql` e o Flyway se encarrega de aplicar apenas o que ainda não foi executado.

---

## Infraestrutura local com Docker

Se preferir rodar PostgreSQL e RabbitMQ via Docker em vez de instalar localmente:

```bash
docker-compose up -d
```

O `docker-compose.yml` na raiz do projeto sobe os dois serviços com as configurações que o profile `dev` espera.

---

## Estrutura de testes

Os testes estão divididos em dois níveis. `ProductServiceTest` é um teste unitário puro que usa Mockito para simular o repository e o serviço de notificação — não sobe o contexto Spring, não acessa banco, roda em milissegundos. Ele cobre criação de produto, listagem, filtro por categoria e o cenário de erro quando um produto não é encontrado.

`DemoApplicationTests` é um teste de integração que sobe o contexto Spring completo com o profile `test`. Ele garante que toda a configuração da aplicação está correta e que o contexto carrega sem erros.
