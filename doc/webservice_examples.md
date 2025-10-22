# Примери SOAP позива за постојеће веб сервисе

Овај документ садржи примере SOAP захтева за постојеће веб сервисе у систему *myWMS - Release 2*. Сви сервиси су доступни преко контекст путање `/webservice` на WildFly серверу и користе BASIC аутентикацију у JAAS домену `los-login`. У Postman-у (или другом SOAP клијенту) подесите:

- URL: `http://<wildfly-host>:<port>/webservice/<ИМЕ_СЕРВИСА>`
- HTTP метод: `POST`
- Заглавља: `Content-Type: text/xml;charset=UTF-8`, `SOAPAction: ""`
- Authorization: Basic Auth (корисничко име и лозинка из табеле `mywms_user`)

## QueryInventoryBean

Сервис за преглед залиха по клијенту, артиклу или лоту.

### Пример: добијање количина по артиклу
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.inventory.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:getInventoryByArticle>
         <clientRef>ACME</clientRef>
         <articleRef>ITEM-1001</articleRef>
         <consolidateLot>true</consolidateLot>
      </ws:getInventoryByArticle>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: добијање количина по лоту
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.inventory.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:getInventoryByLot>
         <clientRef>ACME</clientRef>
         <articleRef>ITEM-1001</articleRef>
         <lotRef>LOT-2024-0001</lotRef>
      </ws:getInventoryByLot>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: целокупан списак залиха
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.inventory.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:getInventoryAmountList>
         <clientRef>ACME</clientRef>
         <consolidateLot>false</consolidateLot>
         <withAmountOnly>true</withAmountOnly>
      </ws:getInventoryAmountList>
   </soapenv:Body>
</soapenv:Envelope>
```

## ManageInventoryBean

Сервис за креирање најава, управљање залихом и ажурирање артикала.

### Пример: креирање најаве (најава улаза)
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.inventory.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:createAvis>
         <clientRef>ACME</clientRef>
         <orderNumber>NAJAVA-2024-001</orderNumber>
         <supplierRef>SUP-77</supplierRef>
         <expectedAt>2024-04-15T08:00:00</expectedAt>
         <positions>
            <itemRef>ITEM-1001</itemRef>
            <expectedQuantity>120</expectedQuantity>
            <lotRef>LOT-2024-0001</lotRef>
         </positions>
      </ws:createAvis>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: ручно креирање јединице залихе
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.inventory.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:createStockUnitOnStorageLocation>
         <clientRef>ACME</clientRef>
         <storageLocationRef>A-01-01</storageLocationRef>
         <itemRef>ITEM-1001</itemRef>
         <quantity>50</quantity>
         <lotRef>LOT-2024-0002</lotRef>
      </ws:createStockUnitOnStorageLocation>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: брисање артикла
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.inventory.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:deleteItemData>
         <clientRef>ACME</clientRef>
         <itemRef>ITEM-OBSOLETE</itemRef>
      </ws:deleteItemData>
   </soapenv:Body>
</soapenv:Envelope>
```

## OrderServiceBean

Сервис за креирање налога за издавање робе.

### Пример: креирање налога са позицијама
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.order.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:createOrder>
         <clientRef>ACME</clientRef>
         <orderNumber>ORD-2024-050</orderNumber>
         <destinationRef>SHIP-001</destinationRef>
         <positions>
            <orderPosNumber>1</orderPosNumber>
            <itemRef>ITEM-1001</itemRef>
            <quantity>30</quantity>
            <lotRef/>
         </positions>
         <positions>
            <orderPosNumber>2</orderPosNumber>
            <itemRef>ITEM-2002</itemRef>
            <quantity>10</quantity>
            <lotRef>LOT-2024-0003</lotRef>
         </positions>
      </ws:createOrder>
   </soapenv:Body>
</soapenv:Envelope>
```

## ManageItemDataWSBean

Сервис за одржавање каталога артикала и BOM структуре.

### Пример: ажурирање података о артиклу
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.masterdata.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:updateItemData>
         <clientRef>ACME</clientRef>
         <itemRef>ITEM-1001</itemRef>
         <description>Промена описа артикла</description>
         <weight>1.25</weight>
         <dimension>
            <length>0.40</length>
            <width>0.30</width>
            <height>0.20</height>
         </dimension>
      </ws:updateItemData>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: креирање BOM компоненте
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.masterdata.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:createBomEntry>
         <clientRef>ACME</clientRef>
         <parentItemRef>ITEM-SET-01</parentItemRef>
         <componentItemRef>ITEM-1001</componentItemRef>
         <quantity>2</quantity>
      </ws:createBomEntry>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: листа свих артикала
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.masterdata.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:getItemDataList>
         <clientRef>ACME</clientRef>
         <pageSize>100</pageSize>
         <pageNumber>0</pageNumber>
      </ws:getItemDataList>
   </soapenv:Body>
</soapenv:Envelope>
```

## Савети за тестирање

1. Подесите „Body“ у Postman-у на „raw“ и „XML“.
2. Укључите Basic Auth пре слања захтева.
3. За тест околину можете користити симулиране податке као у примерима, али замените их реалним вредностима из базе (клијент, артикал, локација, налози).
4. У случају грешке сервис враћа SOAP Fault са детаљима (нпр. `InventoryException`, `OrderException`, `MasterDataException`).
