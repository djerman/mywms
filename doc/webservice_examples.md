# Примери SOAP позива за постојеће веб сервисе

Овај документ садржи примере SOAP захтева за постојеће веб сервисе у систему *myWMS - Release 2*, као и преглед очекиваних одговора и понашања у случају грешке. Сви сервиси се излажу преко контекст путање `/webservice` на WildFly серверу и користе BASIC аутентикацију у JAAS домену `los-login`. У Postman-у (или другом SOAP клијенту) подесите:

- **URL**: `http://<wildfly-host>:<port>/webservice/<ИМЕ_СЕРВИСА>`
- **HTTP метод**: `POST`
- **Заглавља**: `Content-Type: text/xml;charset=UTF-8`, `SOAPAction: ""`
- **Authorization**: Basic Auth (корисничко име и лозинка из табеле `mywms_user`)

## QueryInventoryBean

Сервис за преглед залиха по клијенту, артиклу или лоту. Успешан одговор увек садржи један или више елемената `return` типа `QueryInventoryTO` (или један елемент за позив по лоту). У случају грешке баца SOAP Fault са кодом `InventoryException` и једним од следећих кључева:

- `NO_SUCH_CLIENT` – клијент не постоји.
- `NO_SUCH_ITEMDATA` – артикал не постоји.

За `getInventoryAmountList` када клијент не постоји метод враћа празан низ уместо Fault-а.

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

**Напомена о транзакцијама:** методи само читају стање залиха, па немају побочних ефеката. Уколико се баци `InventoryException`, операција се прекида без измена података.

## ManageInventoryBean

Сервис за креирање најава, управљање залихом и основно одржавање артикала. Сви методи очекују техничко корисничко име и лозинку и враћају `boolean` (осим `createStockUnitOnStorageLocation`) као сигнал да ли је операција успела:

- `true` – радња је завршена и подаци су уписани.
- `false` – фасада је наишла на доменску грешку и ништа није изменила (нпр. клијент или артикал не постоји, није могуће ажурирати референцу, није направљена најава).

`createItemData` баца `InventoryException` када недостају обавезни параметри или када артикал већ постоји. `createStockUnitOnStorageLocation` не враћа вредност, већ баца:

- `InventoryException` са кључевима као што су `NO_SUCH_ITEMDATA`, `CREATE_STOCKUNIT_ON_STORAGELOCATION_FAILED` или `CREATE_STOCKUNIT_ONSTOCK` у случају проблема са артиклом, лотом или креирањем јединице залихе.
- `FacadeException`/`EntityNotFoundException` ако инфраструктурни сервис не може да пронађе или креира потребне ресурсе.

Сви методи се извршавају у оквиру EJB трансакције. Ако дође до изузетка (checked или unchecked), цела операција се поништава и база остаје у непромењеном стању.

### Пример: креирање најаве (најава улаза)
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.inventory.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
       <ws:createAvis>
         <username>integration</username>
         <password>tajna</password>
         <clientRef>ACME</clientRef>
         <articleRef>ITEM-1001</articleRef>
         <batchRef>LOT-2024-0001</batchRef>
         <amount>120</amount>
         <expectedAt>2024-04-15T08:00:00</expectedAt>
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
         <username>integration</username>
         <password>tajna</password>
         <clientRef>ACME</clientRef>
         <storageLocationRef>A-01-01</storageLocationRef>
         <itemRef>ITEM-1001</itemRef>
         <quantity>50</quantity>
         <lotRef>LOT-2024-0002</lotRef>
         <unitLoadRef>UL-ACME-00042</unitLoadRef>
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
         <username>integration</username>
         <password>tajna</password>
         <clientRef>ACME</clientRef>
         <itemRef>ITEM-OBSOLETE</itemRef>
       </ws:deleteItemData>
   </soapenv:Body>
</soapenv:Envelope>
```

## OrderBean

Сервис за креирање налога за издавање робе. Метод `order` враћа `true` ако је налога креиран (`DeliveryOrder` није `null`), односно `false` ако фасада врати `null`. Уколико настану грешке у пословној логици, баца се `FacadeException`, чији узрок најчешће носи `InventoryException` са детаљном поруком (нпр. непостојећи клијент, стратегија, артикал или количина ≤ 0).

Пошто је EJB метод трансакциони, све креиране позиције, налози и пратеће ентитетске измене се аутоматски поништавају ако се баци изузетак.

### Пример: креирање налога са позицијама
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ws="http://ws.order.los.linogistix.de/">
   <soapenv:Header/>
   <soapenv:Body>
      <ws:order>
         <username>integration</username>
         <password>tajna</password>
         <clientRef>ACME</clientRef>
         <orderRef>ORD-2024-050</orderRef>
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
         <documentUrl>http://files.example.com/docs/ORD-2024-050.pdf</documentUrl>
         <labelUrl>http://files.example.com/labels/ORD-2024-050.zpl</labelUrl>
         <destination>SHIP-001</destination>
      </ws:order>
   </soapenv:Body>
</soapenv:Envelope>
```

## ManageItemDataWSBean

Сервис за одржавање каталога артикала и BOM структуре. Сви методи враћају `void`, а сигнализација успеха/неуспеха иде преко изузетка `ManageItemDataWSFault` који носи код (`ManageItemDataErrorCodes`) и описни текст.

Успешан позив резултује HTTP 200 без Fault елемента. Грешке покривају:

- `UNAUTHORIZED_CALLER` – позивач нема приступ клијенту.
- `ERROR_UNKNOWN_CLIENT` / `ERROR_UNKNOWN_ITEMDATA` / `DELETE_ERROR_UNKNOWN_ITEMNUMBER` – недостају клијент или артикал.
- `ERROR_ITEMNAME_NULL`, `UPDATE_ERROR_*` – покушај измене који није дозвољен (нпр. постоје залихе).
- `ERROR_UPDATE_BOM`, `ERROR_DELETE`, `DELETE_ERROR_STOCK_EXIST` – проблеми при брисању/ажурирању BOM-а или артикла.

Сви методи раде у једној трансакцији: ако се баци `ManageItemDataWSFault` или нека checked/unchecked грешка, транзакција се поништава и делимично уписани подаци се неће сачувати.

### Пример: ажурирање података о артиклу
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:item="http://itemdata.management.los.linogistix.de">
   <soapenv:Header/>
   <soapenv:Body>
      <item:updateItemDataRequest>
         <clientNumber>ACME</clientNumber>
         <name>Артикал 1001</name>
         <number>ITEM-1001</number>
         <description>Промена описа артикла</description>
         <lotMandatory>false</lotMandatory>
         <adviceMandatory>false</adviceMandatory>
         <serialNoRecordType>NO_RECORD</serialNoRecordType>
         <handlingUnit>комад</handlingUnit>
         <scale>0</scale>
         <eanCodes>8601234567890</eanCodes>
      </item:updateItemDataRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: креирање или ажурирање BOM уноса
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:item="http://itemdata.management.los.linogistix.de">
   <soapenv:Header/>
   <soapenv:Body>
      <item:updateBomRequest>
         <clientNumber>ACME</clientNumber>
         <parentNumber>ITEM-SET-01</parentNumber>
         <childNumber>ITEM-1001</childNumber>
         <amount>2</amount>
         <pickable>true</pickable>
      </item:updateBomRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: брисање артикла
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:item="http://itemdata.management.los.linogistix.de">
   <soapenv:Header/>
   <soapenv:Body>
      <item:deleteItemDataRequest>
         <clientNumber>ACME</clientNumber>
         <itemNumber>ITEM-OBSOLETE</itemNumber>
      </item:deleteItemDataRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

### Пример: преузимање каталога артикала
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:item="http://itemdata.management.los.linogistix.de">
   <soapenv:Header/>
   <soapenv:Body>
      <item:getItemNumbersRequest/>
   </soapenv:Body>
</soapenv:Envelope>
```

## Савети за тестирање

1. Подесите „Body“ у Postman-у на „raw“ и „XML“.
2. Укључите Basic Auth пре слања захтева.
3. За тест околину користите симулиране податке као у примерима, али замените их реалним вредностима из базе (клијент, артикал, локација, налози).
4. У случају грешке сервис враћа SOAP Fault или `false` (у зависности од метода); изузетак у EJB-у поништава целу операцију.
