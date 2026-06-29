# SslContextBuilder와 개인키 (Private Key)

> 원본: https://netty.io/wiki/sslcontextbuilder-and-private-key.html

---

`SslContextBuilder`와 Netty의 `SslContext` 구현은 PKCS8 키만 지원합니다.

다른 형식의 키를 사용하려면 먼저 PKCS8로 변환해야 합니다. openssl을 사용하면 손쉽게 변환할 수 있습니다.

예를 들어 암호화되지 않은 PKCS1 키를 PKCS8로 변환하려면 다음 명령을 사용합니다.

```sh
openssl pkcs8 -topk8 -nocrypt -in pkcs1_key_file -out pkcs8_key.pem
```

자세한 내용은 [OpenSSL 문서](https://www.openssl.org/docs/manmaster/man1/pkcs8.html)를 참고하세요.
