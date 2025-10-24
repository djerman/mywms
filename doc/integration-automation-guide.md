# Интеграција ERP система и аутоматизација обавештавања

Овај документ описује на који начин постојећи myWMS сервисни слој може да сарађује са ERP системом, како се конфигурише слање е-поште и на који начин се могу ухватити догађаји (нпр. завршетак отпреме) ради даљих интеграција. Сви примери су на српском језику и користе постојеће SOAP веб-сервисе.

## 1. Слање е-поште из myWMS

### 1.1 Конфигурација SMTP параметара

Сервис `LOSMailServiceBean` чита системска својства `MAIL_SENDER`, `MAIL_SERVER`, `MAIL_AUTHORIZE`, `MAIL_HOST_USER` и `MAIL_HOST_PASSWD`. Ако било који обавезан параметар недостаје, слање се прекида и у лог се уписује грешка.【F:dev/los-ejb/src/main/java/de/linogistix/los/common/businessservice/LOSMailServiceBean.java†L34-L149】 Кључеви су дефинисани у `LOSCommonPropertyKey` и распоређени су у групу „SERVER“.【F:dev/los-ejb/src/main/java/de/linogistix/los/model/LOSCommonPropertyKey.java†L12-L39】

**Пример подешавања (у системским својствима):**

| Кључ | Вредност | Опис |
|------|----------|------|
| `MAIL_SENDER` | `wms@firma.rs` | Адреса са које се шаље е-пошта. |
| `MAIL_SERVER` | `smtp.firma.rs` | SMTP хост. |
| `MAIL_AUTHORIZE` | `true` | Да ли је потребна аутентикација. |
| `MAIL_HOST_USER` | `wms` | Корисник за SMTP (обавезан ако је `MAIL_AUTHORIZE=true`). |
| `MAIL_HOST_PASSWD` | `***` | Лозинка за SMTP. |

### 1.2 Ручно слање мејла из сопственог кода

У било којој EJB/ CDI компоненти може да се инјектује `LOSMailService` и позове метода `sendSMTPMail(recipients, subject, body)`. Ако је потребно слати аутоматске извештаје (нпр. после испоруке), довољно је да се овај сервис позове у оквиру посматрача догађаја који је описан у поглављу 3.

```java
@Inject
private LOSMailService mailService;

public void посаљиИзвештај(String[] примаоци, String тема, String текст) {
    mailService.sendSMTPMail(примаоци, тема, текст);
}
```

## 2. Интеграција са ERP системом

### 2.1 Креирање налога за испоруку из ERP-а

ERP шаље SOAP захтев на `/webservice/OrderBean` и попуњава листу `OrderPositionTO` структуралним подацима (артикал, количина, серија). Сервис `OrderBean.order(...)` прослеђује позив ка `LOSOrderFacade.order(...)`, који креира `DeliveryOrder`, ставке и — ако је `startPicking=true` — одмах покреће генерисање налога за пиковaње.【F:dev/los.ws-ejb/src/main/java/de/linogistix/los/inventory/ws/OrderBean.java†L43-L75】【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/facade/LOSOrderFacadeBean.java†L181-L247】

`OrderPositionTO` је намењен управо за размену између ERP-а и WMS-а и садржи поља `clientRef`, `batchRef`, `articleRef` и `amount`.【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/facade/OrderPositionTO.java†L23-L104】

### 2.2 Шта ако нема довољно количина

Током генерисања пиковaња, `PickingOrderLineGenerator.generatePicks(...)` проверава преосталу количину сваке ставке. Ако је параметар `completeOnly=true` (подразумевано понашање SOAP сервиса) и недостаје било који део количине, баца се `BusinessException` и цела операција се прекида — ERP добија SOAP грешку да у складишту нема довољно робе.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/picking/PickingOrderLineGenerator.java†L125-L138】

Да би ERP прихватио делимичне испоруке, позив `LOSOrderFacade.order(...)` мора да се направи са `completeOnly=false`. У том режиму, пикови се праве за доступну количину, линија наруџбине прелази у стање `PENDING`, а недостатак се види у ERP-у приликом накнадне провере (види следећу тачку). Исти параметар је доступан и у NetBeans чаробњаку (*Само комплетно*) за ручне операције, па администратори могу да генеришу делимичне налоге без измена у бази када год је то потребно.【F:rich.client/los.clientsuite/LOS Inventory Browser/src/de/linogistix/inventory/browser/dialog/CustomerOrderStartDataPanel.java†L82-L140】【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/facade/LOSPickingFacadeBean.java†L259-L349】

> Напомена: постојећи SOAP веб-сервис `OrderBean.order(...)` увек прослеђује `startPicking=true` и `completeOnly=true` фасади (`delegate.order(..., true, true, null)`), тако да га није могуће користити за делимичне количине без измене кода сервиса.【F:dev/los.ws-ejb/src/main/java/de/linogistix/los/inventory/ws/OrderBean.java†L61-L75】 У пракси је потребно додати нови SOAP/REST метод (нпр. `orderWithShortage`) или прилагођени параметар који ће позвати `LOSOrderFacade.order(..., false, ...)` и тако дозволити да WMS креира пикове за оно што је доступно.

### 2.3 Провера статуса и стварно испоручених количина

myWMS чува напредак у објектима `DeliveryOrder` и `DeliveryOrderLine`, а стања су дефинисана у класи `OrderState` (`PROCESSABLE`, `STARTED`, `PICKED`, `SHIPPING`, `SHIPPED`, `FINISHED`...).【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/delivery/DeliveryOrder.java†L85-L170】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/strategy/OrderState.java†L32-L120】

Поред класичног читања стања, практичније је да прилагођени сервис који прихвата налог одмах врати ERP-у листу стварно креираних пиковa или збирне количине (нпр. читањем `DeliveryOrderLine` након позива фасаде и враћањем JSON-а/XML-а са пољима `amount` и `pickedAmount`). На тај начин ERP не мора да „погађа“ стање складишта већ у истом одговору зна шта ће бити припремљено. Овај приступ је погодан и за сценарије у којима се један налог дели на више делимичних испорука.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/delivery/DeliveryOrderLine.java†L45-L134】

Ипак, ако се тражи независна верификација, постоје два устаљена начина да ERP провери шта је стварно испоручено:

1. **SOAP провера стања залиха.** После отпреме, ERP може да позове `QueryInventoryBean.getInventoryByArticle(...)` и упореди количину „пре“ и „после“. Сервис враћа листу `QueryInventoryTO` објеката са количинама по артиклу/лоту за датог клијента.【F:dev/los.ws-ejb/src/main/java/de/linogistix/los/inventory/ws/QueryInventoryBean.java†L62-L94】
2. **Додавање извештајног сервиса.** Уколико је потребно добити директан статус налога (нпр. листу стварно одвојених количина), у пракси се креира сопствени EJB/REST сервис који чита `DeliveryOrder` и његове линије, или се послужи готовим Jasper извештајем и врати PDF ERP систему. Захваљујући CDI догађајима (види поглавље 3), сервис може аутоматски да шаље обавештење чим налог пређе у `SHIPPED`/`FINISHED` стање.

### 2.4 Пример тока интеграције

1. ERP позива прилагођени сервис (SOAP или REST) који прослеђује `completeOnly=false` ка `LOSOrderFacade.order(...)` како би се дозволиле делимичне количине.
2. Ако је количина делимична, `PickingOrderLineGenerator` оставља налог у стању `PENDING` и генерише пикове за расположиву количину.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/picking/PickingOrderLineGenerator.java†L141-L170】
3. По завршетку пиковaња и отпреме, `ShippingBusiness` поставља стање на `SHIPPED/FINISHED` и активира CDI догађај.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/shipping/ShippingBusiness.java†L69-L168】
4. Посматрач догађаја (нпр. у `DeliveryEventObserver`) може да генерише отпремне налоге, а додатно проширење може да позове ERP REST/SOAP сервис, пошаље JSON или активира Jasper извештај за фактурисање.【F:dev/project-ejb/src/main/java/de/wms2/mywms/project/DeliveryEventObserver.java†L44-L118】
5. ERP преузима PDF или JSON и тачно зна шта је стварно испоручено.

## 3. Аутоматске акције по догађајима

`ShippingBusiness` убризгава CDI догађаје за сваку промену стања отпреме, линија и пакетa (`ShippingOrderStateChangeEvent`, `PacketStateChangeEvent`, `StockUnitStateChangeEvent`).【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/shipping/ShippingBusiness.java†L69-L139】 `DeliveryEventObserver` показује како се ти догађаји користе за креирање отпремних налога чим пиковaње пређе у стање `FINISHED`.【F:dev/project-ejb/src/main/java/de/wms2/mywms/project/DeliveryEventObserver.java†L44-L118】

**Пример проширеног посматрача који шаље JSON ERP-у након отпреме:**

```java
@RequestScoped
public class ShippingWebhookObserver {
    @Inject
    private JsonErpClient erpClient; // сопствени REST клијент

    public void onShippingFinished(@Observes ShippingOrderStateChangeEvent event) {
        if (event.getNewState() < OrderState.SHIPPED) {
            return;
        }
        ShippingOrder наруџбина = event.getShippingOrder();
        erpClient.sendShipmentUpdate(наруџбина);
    }
}
```

Комбинација CDI догађаја и `LOSMailServiceBean` омогућава да се по завршетку отпреме аутоматски пошаље и е-пошта и JSON обавештење.

## 4. Класификација залиха по клијентима

Алгоритам за проналажење залиха прво тражи робу клијента који је наручио, а затим (ако је артикал системског клијента) шири претрагу на системског клијента и на крају на све клијенте. То омогућава сценарио у ком се роба води на „System“ клијента, а наруџбине долазе од појединачних клијената без копирања залиха.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/picking/PickingStockFinder.java†L94-L138】

Уколико ERP жели да користи заједничку робу, довољно је да артикле креира као системске, док се стварни клијент и даље уписује у `DeliveryOrder` ради евиденције.

## 5. Примери проширења

| Ситуација | Предложено решење |
|-----------|-------------------|
| ERP мора да добије PDF листу стварно отпремљених ставки | Написати CDI посматрача који на `SHIPPED` креира Jasper извештај и пошаље га `LOSMailServiceBean`-ом или REST клијенту. |
| Потребно је дозволити делимичне испоруке | Позвати `LOSOrderFacade.order(...)` са `completeOnly=false` (нови сервисни метод ако се користи SOAP), тако да WMS остави налог у `PENDING` за преостале количине. |
| ERP треба да зна да је налог започет | Проверити `DeliveryOrder` стање преко новог REST сервиса или преко Jasper извештаја који враћа табелу са пољима `state`, `pickedAmount`, `shippedAmount`. |
| Неопходно је аутоматско обавештавање партнерима | Креирати CDI посматрача на `ShippingOrderStateChangeEvent` који позива спољни веб-хук или `sendSMTPMail`. |

Овим смерницама ERP може да креира налоге, прати стварно испоручене количине и добија нотификације без ручне интервенције администратора.

