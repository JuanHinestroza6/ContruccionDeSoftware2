# ContruccionDeSoftware2

Desarrollado por :
Juan  Francisco Hinestroza Andrade
Miguel Angel Gonzales Mazo

Domain Model – Aplicación de Gestión de Información de un Banco

---

## Base de Datos

| Motor | Propósito | Host | Puerto | Base de datos |
|-------|-----------|------|--------|---------------|
| **MySQL** | Datos relacionales: clientes, cuentas bancarias, préstamos, transferencias, productos bancarios | `localhost` | `3306` | `bankdb` |
| **MongoDB** | Bitácora de operaciones (auditoría y trazabilidad) | `localhost` | `27017` | `bank_audit` |

### Crear la base de datos MySQL

```sql
CREATE DATABASE IF NOT EXISTS bankdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> MongoDB crea la base `bank_audit` automáticamente al recibir el primer documento.

## Ejecución

```bash
# Desde la carpeta /bank
./mvnw spring-boot:run        # Linux / macOS
mvnw.cmd spring-boot:run      # Windows
```

La aplicación arranca en **http://localhost:8081**.

### Requisitos previos

- Java 17+
- MySQL 8.x corriendo en `localhost:3306`
- MongoDB 6.x / 7.x corriendo en `localhost:27017`

### Variables de entorno opcionales

| Variable | Default | Descripción |
|----------|---------|-------------|
| `DB_USER` | `root` | Usuario de MySQL |
| `DB_PASS` | `root` | Contraseña de MySQL |

## Estado actual de infraestructura

- [x] Modelo de dominio puro (sin anotaciones de framework)
- [x] Puertos de entrada (use cases) y salida (repository ports)
- [x] Servicios de aplicación
- [x] Excepciones de negocio compartidas (`shared.exception`)
- [x] Configuración de Spring Boot con MySQL y MongoDB
- [ ] Adaptadores JPA para persistencia SQL (en construcción)
- [ ] Adaptador MongoDB para bitácora de auditoría (en construcción)
- [ ] Controladores REST
- [ ] Seguridad (JWT / Spring Security)

---

1. Introducción
Este Domain Model implementa las entidades descritas en el enunciado “Actividad – Funcionamiento de la Aplicación de Gestión de Información de un Banco”, separando la información estructurada en un modelo relacional (SQL) y la bitácora de operaciones en un modelo documental NoSQL, tal como se solicita en el documento.
​

El objetivo es reflejar fielmente los campos, restricciones y reglas de negocio definidos para:

Usuarios y clientes (persona natural y empresa).

Cuentas bancarias, préstamos/créditos y transferencias.

Catálogo de productos bancarios.

Bitácora de operaciones con datos de detalle variables.
​

2. Paquete sqlbank.domain.model (Modelo Relacional – SQL)
Este paquete contiene las entidades que se almacenan en la Base de Datos Relacional, siguiendo las tablas y campos definidos en el enunciado.
​

2.1. Mapeo de Tablas → Clases
Entidad enunciado	Tabla / descripción	Clase Java
Información de los Usuarios del Sistema	Campos: ID_Usuario, ID_Relacionado, Nombre_Completo, ID_Identificacion, Correo_Electronico, Telefono, Fecha_Nacimiento, Direccion, Rol_Sistema, Estado_Usuario.
​	User (abstracta)
Cliente Persona Natural	Persona física titular de productos; reutiliza estructura de usuario con restricciones adicionales (mayor de edad, rol fijo “Cliente Persona Natural”).
​	IndividualClient extends User
Cliente Empresa	Entidad jurídica con razón social, NIT, representante legal y rol fijo “Cliente Empresa”.
​	BusinessClient extends User
Cuenta Bancaria	Numero_Cuenta, Tipo_Cuenta, ID_Titular, Saldo_Actual, Moneda, Estado_Cuenta, Fecha_Apertura.
​	BankAccount
Préstamo / Crédito	ID_Prestamo, Tipo_Prestamo, ID_Cliente_Solicitante, Monto_Solicitado, Monto_Aprobado, Tasa_Interes, Plazo_Meses, Estado_Prestamo, Fecha_Aprobacion, Fecha_Desembolso, Cuenta_Destino_Desembolso.
​	Loan
Transferencia	ID_Transferencia, Cuenta_Origen, Cuenta_Destino, Monto, Fecha_Creacion, Fecha_Aprobacion, Estado_Transferencia, ID_Usuario_Creador, ID_Usuario_Aprobador.
​	Transfer
Producto Bancario General (Catálogo)	Codigo_Producto, Nombre_Producto, Categoria (Cuentas, Préstamos, Servicios), Requiere_Aprobacion.
​	BankingProduct
2.2. Detalle por clase
User (abstracta)
Representa la información centralizada de cualquier usuario del sistema, sin importar el rol (clientes, empleados del banco, usuarios de empresa).
​

userID (PK), relatedID, fullName, identificationID (único), email, phone, birthDate, address, systemRole, userStatus.

Sirve como Aggregate Root abstracto; no se instancia directamente.

IndividualClient

Extiende User y modela al Cliente Persona Natural descrito en el documento.
​

Campo adicional: identificationNumber (único para clientes individuales).

Rol conceptual: “Cliente Persona Natural”.

BusinessClient

Extiende User y modela al Cliente Empresa.
​

Campos adicionales: companyName, taxID (NIT, único), legalRepresentative (referencia a una Persona Natural), role = "Business Client".

Representa la entidad jurídica y su relación con el representante legal.

BankAccount

Mapea la entidad Cuenta Bancaria.
​

Campos:

accountNumber (PK, Numero_Cuenta).

accountType (Tipo_Cuenta).

holderID (ID_Titular → User.identificationID).

currentBalance (Saldo_Actual).

currency (Moneda).

accountStatus (Estado_Cuenta).

openingDate (Fecha_Apertura).

Loan

Mapea el Préstamo / Crédito.
​

Campos:

loanID (PK, ID_Prestamo).

loanType (Tipo_Prestamo).

applicantClientID (ID_Cliente_Solicitante → User.identificationID).

requestedAmount, approvedAmount, interestRate, termMonths.

loanStatus (Estado_Prestamo).

approvalDate, disbursementDate.

disbursementTargetAccount (Cuenta_Destino_Desembolso → BankAccount.accountNumber).

Transfer

Mapea la entidad Transferencia.
​

Campos:

transferID (PK, ID_Transferencia).

sourceAccount, destinationAccount (FK a BankAccount.accountNumber).

amount (Monto).

creationDate (Fecha_Creacion), approvalDate (Fecha_Aprobacion).

transferStatus (Estado_Transferencia).

createdByUserID, approvedByUserID (FK a User.userID).

BankingProduct

Mapea el Producto Bancario General (Catálogo).
​

Campos: productCode, productName, category, requiresApproval (coinciden con Codigo_Producto, Nombre_Producto, Categoria, Requiere_Aprobacion).
​

3. Paquete nosql.domain.model (Bitácora – NoSQL)
El documento indica que la Bitácora de Operaciones debe almacenarse en una Base de Datos No Relacional, utilizando un modelo de documento con un diccionario Datos_Detalle variable según el tipo de operación.
​

3.1. Documento principal: AuditLogEntry
Representa los campos mínimos del registro de bitácora:

auditLogID (ID_Bitacora).

operationType (Tipo_Operacion).

operationDateTime (Fecha_Hora_Operacion).

userID (ID_Usuario).

userRole (Rol_Usuario).

affectedProductID (ID_Producto_Afectado).

detail (Datos_Detalle) como objeto embebido con estructura variable.
​

3.2. Objetos de detalle embebidos
Siguiendo los ejemplos de contenido de Datos_Detalle del enunciado, se definen tres Value Objects embebidos:
​

Escenario en Bitácora	Campos de Datos_Detalle sugeridos por el enunciado 
​	Clase Java
Transferencia Ejecutada	Monto involucrado, Saldo_Antes_Origen, Saldo_Despues_Origen, Saldo_Antes_Destino, Saldo_Despues_Destino.
​	TransferDetail
Aprobación de Préstamo	Monto Aprobado, Tasa de Interés, Estado Anterior (“En estudio”), Nuevo Estado (“Aprobado”), ID del Analista Aprobador.
​	LoanDetail
Vencimiento de Transferencia	Motivo de vencimiento (“Falta de aprobación a tiempo”), Fecha y Hora de Vencimiento, ID del Usuario Creador.
​	ExpirationDetail
4. Ejemplos de Documentos JSON de Bitácora
Estos ejemplos ilustran cómo se verían los documentos NoSQL basados en AuditLogEntry y las clases de detalle, alineados con los casos del enunciado.
​

4.1. Transferencia Ejecutada
json
{
  "auditLogID": "LOG-0001",
  "operationType": "Transfer_Executed",
  "operationDateTime": "2026-03-10T14:35:00",
  "userID": 120,
  "userRole": "Supervisor Empresa",
  "affectedProductID": 987,
  "detail": {
    "amount": 1500000.00,
    "balanceBeforeSource": 5000000.00,
    "balanceAfterSource": 3500000.00,
    "balanceBeforeDestination": 1000000.00,
    "balanceAfterDestination": 2500000.00
  }
}
4.2. Aprobación de Préstamo
json
{
  "auditLogID": "LOG-0002",
  "operationType": "Loan_Approval",
  "operationDateTime": "2026-03-11T09:20:00",
  "userID": 45,
  "userRole": "Analista Interno",
  "affectedProductID": 1234,
  "detail": {
    "approvedAmount": 8000000.00,
    "interestRate": 0.145,
    "previousStatus": "En estudio",
    "newStatus": "Aprobado",
    "approverAnalystID": 45
  }
}
4.3. Vencimiento de Transferencia
json
{
  "auditLogID": "LOG-0003",
  "operationType": "Transfer_Expiration",
  "operationDateTime": "2026-03-11T15:40:00",
  "userID": 200,
  "userRole": "Sistema",
  "affectedProductID": 5678,
  "detail": {
    "reason": "Vencida por falta de aprobación en el tiempo establecido",
    "expirationDateTime": "2026-03-11T15:35:00",
    "creatorUserID": 160
  }
}
Estos ejemplos reflejan exactamente los tres tipos de contenido de Datos_Detalle propuestos en el enunciado.
​

5. Decisiones de Diseño
Separación SQL / NoSQL:
Las entidades de negocio con estructura fija (usuarios, clientes, cuentas, préstamos, transferencias, productos) se modelan en sqlbank.domain.model para persistir en una Base de Datos Relacional, tal como indica la sección de “Productos y Servicios Bancarios manejados por la aplicación”.
​
La Bitácora se modela en nosql.domain.model, siguiendo la recomendación explícita de usar una Base de Datos No Relacional con documentos y un campo Datos_Detalle variable.
​

Herencia en usuarios:
User centraliza los campos comunes de todos los usuarios del sistema, en línea con la tabla “Información de los Usuarios del Sistema”, mientras que IndividualClient y BusinessClient especializan según si se trata de persona natural o empresa.
​

Alineación con reglas de negocio:
En los Javadocs de las clases se documentan reglas importantes: unicidad de identificaciones, restricciones de estados, rol del Analista Interno en préstamos, flujos de aprobación de transferencias empresariales y vencimiento automático a los 60 minutos, tal como se describe en las secciones de reglas de negocio y flujos de aprobación.
