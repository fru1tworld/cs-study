CREATE TABLE ACCOUNT
(
    numkey         string PRIMARY KEY,
    number         INT,
    firstName      STRING,
    lastName       STRING,
    numberAddress  STRING,
    streetAddress  STRING,
    cityAddress    STRING,
    countryAddress STRING,
    creationDate   BIGINT,
    updateDate     BIGINT
) WITH (KAFKA_TOPIC = 'account', VALUE_FORMAT = 'avro', PARTITIONS = 6, REPLICAS = 1);

CREATE
STREAM TRANSACTION_SUCCESS (
  numkey string KEY,
  transaction STRUCT<guid STRING, account STRING, amount DECIMAL(9, 2), type STRING, currency STRING, country STRING>,
  funds STRUCT<account STRING, balance DECIMAL(9, 2)>,
  success boolean,
  errorType STRING
  --errorType STRUCT<type STRING>
) WITH (KAFKA_TOPIC='transaction-success', VALUE_FORMAT='avro');

CREATE
STREAM TRANSACTION_STATEMENT AS
SELECT *
FROM TRANSACTION_SUCCESS
         LEFT JOIN ACCOUNT ON TRANSACTION_SUCCESS.numkey = ACCOUNT.numkey EMIT CHANGES;

