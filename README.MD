## keystore для applepay
чтобы сгенерировать jks файл нужно воспользоватся утилитой keytool от java
1. получите .cer и .p12 от ios разработчика
2. сгенерируйте .jks файл <br>
   ``
   keytool -import -alias my_alias -file C:/Users/askar/certs/apple_pay.cer -keystore C:/Users/askar/certs/keystore.jks
   keytool -importkeystore -srckeystore C:/Users/askar/certs/ApplePayCert.p12 -srcstoretype PKCS12 -destkeystore C:/Users/askar/certs/keystore.jks -deststoretype JKS
   ``
<br>2.1 Убедитесь что сертификат и ключ сгенерировались в кейстор под одним алиасом
3. загрузите на сервер .jks файл
4. перезагрузите приложение