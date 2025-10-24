# Водич кроз штампање и Jasper извештаје

Овај документ описује како су у систему *myWMS Release 2* организовани Jasper извештаји, како се подижу и компајлирају JRXML шаблони, на који начин се покрећу из пословних процеса и како се подешава штампа на различитим штампачима.

## 1. Инфраструктура извештаја

* Свака форма за штампу представљена је ентитетом `Report` са називом, верзијом и стањем (активан/неактиван). Јединственост је обезбеђена комбинацијом назив + верзија + клијент.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/Report.java†L36-L79】
* JRXML извор, компајлирани `.jasper` и пратеће слике чувају се као `Document` записи повезани са извештајем. Читање/упис се реализује у `ReportBusiness` преко метода `readSourceDocument`, `saveSourceDocument`, `readJasperDocument` и `saveJasperDocument`.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L423-L470】
* Ако се тражени шаблон не пронађе у бази, `ReportBusiness` аутоматски покушава да учита JRXML из classpath-а (`/reports/<назив>.jrxml`) и да га компајлира, уз додавање подразумеване слике за лого.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L300-L420】
* Попуњавање извештаја користи `JRBeanCollectionDataSource`, локализацију из системске особине `REPORT_LOCALE` и уноси све прилоге (нпр. лого) у параметре пре покретања Jasper-а.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L90-L152】

## 2. Управљање извештајима у NetBeans RCP клијенту

Мени **Jasper Reports** омогућава администраторима (улога `ADMIN`) да одржавају шаблоне директно из RCP клијента.

1. **Учитавање новог JRXML-а** – акција *Учитај JRXML фајл* отвара дијалог, учитава изабрани `.jrxml`, шаље га на сервер (`writeSourceDocument`) и одмах покреће компајлирање (`compileReport`).【F:rich.client/los.clientsuite/LOS Common/src/de/linogistix/common/bobrowser/action/BOJasperReportLoadSourceAction.java†L83-L133】
2. **Чување постојећег JRXML-а** – акција *Сачувај JRXML фајл* преузима тренутни извор са сервера и снима га у изабрани директоријум за локално уређивање.【F:rich.client/los.clientsuite/LOS Common/src/de/linogistix/common/bobrowser/action/BOJasperReportSaveSourceAction.java†L83-L130】
3. **Ручно компајлирање** – акција *Компајлира* поново покреће компајлер над већ постављеним извором; корисно је после едитовања JRXML-а без новог отпремања.【F:rich.client/los.clientsuite/LOS Common/src/de/linogistix/common/bobrowser/action/BOJasperReportCompileAction.java†L67-L106】
4. **Приказ статуса** – у таблици се виде колоне „Source attached“ и „Compiled“ које `LOSJasperReportQueryBean` попуњава на основу постојања изворног/компајлираног документа.【F:dev/los-ejb/src/main/java/de/linogistix/los/query/LOSJasperReportQueryBean.java†L92-L108】
5. **Активирање верзије** – при снимању извештаја у стању `ACTIVE` остале верзије истог шаблона се аутоматски деактивирају, тако да је за једног клијента увек активна једна варијанта.【F:dev/los-ejb/src/main/java/de/linogistix/los/crud/LOSJasperReportCRUDBean.java†L43-L58】

## 3. Подразумевани шаблони

Основни JRXML шаблони испоручују се у изворима и аутоматски се копирају у базу ако није постављена сопствена верзија:

| Назив | Путања у репоу | Опис |
|-------|----------------|------|
| `PickingPacketlist.jrxml` | `dev/wms2-ejb/src/main/resources/reports/PickingPacketlist.jrxml` | Списак артикала по налогу за пиковaње, параметри `pickingOrder`, `printDate`, `address`, итд.【F:dev/wms2-ejb/src/main/resources/reports/PickingPacketlist.jrxml†L1-L10】 |
| `Contentlist.jrxml`, `ShippingPacketlist.jrxml`, `DeliveryPacketlist.jrxml`, `Deliverynote.jrxml` | `dev/wms2-ejb/src/main/resources/reports/` | Форме за пакете, отпрему и отпремницу које користи `DeliveryReportGenerator`. |
| `StockUnitLabel.jrxml` | `dev/wms2-ejb/src/main/resources/reports/StockUnitLabel.jrxml` | Етикета за јединицу робе (unit load) са QR/бар кодом.【F:dev/wms2-ejb/src/main/resources/reports/StockUnitLabel.jrxml†L1-L5】 |
| `GenericBarcodeLabels.jrxml` | `dev/los-ejb/src/main/resources/de/linogistix/los/res/GenericBarcodeLabels.jrxml` | Универзалне баркод етикете за више различитих налепница.【F:dev/los-ejb/src/main/resources/de/linogistix/los/res/GenericBarcodeLabels.jrxml†L1-L8】 |
| `StorageLocationLabels.jrxml` | `dev/los-ejb/src/main/resources/de/linogistix/los/location/res/StorageLocationLabels.jrxml` | Етикете за локације у магацину (двоколонски изглед).【F:dev/los-ejb/src/main/resources/de/linogistix/los/location/res/StorageLocationLabels.jrxml†L1-L8】 |

## 4. Где се извештаји користе

* **Спискови и отпремница** – `DeliveryReportGenerator` припрема PDF-ове за садржај пакета, листе за пиковaње/отпрему и отпремнице. Улазни параметри укључују наруџбину, адресу, листу пакета и временски печат.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/delivery/DeliveryReportGenerator.java†L61-L199】
* **Етикете јединица робе** – `StockUnitReportGenerator` пролази кроз све `StockUnit` записе на изабраном unit load-у, додаје слику артикула ако постоји и предаје податке Jasper-у.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/inventory/StockUnitReportGenerator.java†L50-L107】
* **Етикете локација** – `StorageLocationLabelReportBean` и `BarcodeLabelReportBean` генеришу PDF са етикетама за складишне локације и генеричке баркодове, користећи JRXML шаблоне из пакета `de.linogistix.los`.【F:dev/los-ejb/src/main/java/de/linogistix/los/location/report/StorageLocationLabelReportBean.java†L23-L103】【F:dev/los-ejb/src/main/java/de/linogistix/los/report/BarcodeLabelReportBean.java†L24-L38】

## 5. Рад са верзијама и клијентима

* Метод `createPdfDocument` увек бира најпре верзију извештаја активну за клијента, а ако није постављена користи системску (ID=0) активну или подразумевану верзију. По потреби ће креирати нови запис за системског клијента са називом „Default“.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L188-L262】
* Доступне верзије извештаја могу се прочитати преко `readReportVersions`, што омогућава клијентској апликацији да понуди избор верзије (нпр. различити изгледи налепница по клијенту).【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L264-L298】

## 6. Прилагођавање шаблона

1. **Извоз** – користите *Сачувај JRXML фајл* да преузмете тренутни шаблон (видети §2).
2. **Уређивање** – шаблоне можете мењати у Jaspersoft Studio-у; препоручено је да параметре и називе поља оставите непромењене (нпр. `printDate`, `pickingOrder`, `unitLoad`) јер се попуњавају из Java кода.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/delivery/DeliveryReportGenerator.java†L136-L193】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/inventory/StockUnitReportGenerator.java†L85-L103】
3. **Учитавање и компајлирање** – поставите измењени JRXML преко *Учитај JRXML фајл*; акција ће одмах компајлирати `.jasper` варијанту (или користите *Компајлира* за ручно покретање компајлера).
4. **Активирање верзије** – у форми извештаја подесите статус на „Активан“. Остале верзије ће бити аутоматски деактивиране.【F:dev/los-ejb/src/main/java/de/linogistix/los/crud/LOSJasperReportCRUDBean.java†L48-L56】
5. **Додатни прилози** – `ReportBusiness` аутоматски укључује све документе са именом `attachment` везане за извештај (нпр. лого клијента) у мапу параметара, па их JRXML може користити као параметре истог имена.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L121-L128】

### 6.1 Превођење поља и локализација

1. **Идентификујте кључеве** – већина подразумеваних шаблона (нпр. `PickingPacketlist.jrxml`) користи `resourceBundle` и изразе `"$R{...}"` за наслове колона и етикете.【F:dev/wms2-ejb/src/main/resources/reports/PickingPacketlist.jrxml†L52-L91】
2. **Измените bundle** – текстови за packet листе и отпремнице налазе се у датотеци `dev/wms2-ejb/src/main/resources/translation/Bundle.properties` (енглески) и њеним варијантама као што је `Bundle_de.properties` (немачки). За свако поље измените одговарајућу вредност или додajte нови фајл `Bundle_sr.properties` са истим кључевима ако желите посебну локализацију.【F:dev/wms2-ejb/src/main/resources/translation/Bundle.properties†L199-L210】
3. **Подесите локал** – системска особина `REPORT_LOCALE` одређује који bundle ће Jasper одабрати; нпр. вредност `sr_RS` активираће преводе из `Bundle_sr.properties`. Особину можете поставити у NetBeans клијенту (Системска својства) или преко `Wms2SetupService` при иницијализацији.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/module/Wms2SetupService.java†L176-L186】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L130-L148】
4. **Статичка поља** – ако шаблон садржи фиксни текст (без `$R{}`), промените га директно у Jaspersoft Studio-у или замените `Static Text` елемент `Text Field` елементом са изразом `"$R{...}"` како би пратио bundle.
5. **Тестирајте превод** – након измена bundle-а поново компајлирајте извештај и покрените пробно штампање; ако је кључ одсутан, Jasper ће исписати сам кључ (нпр. `Packetlist.name`), што је користан сигнал да допуните превод.

## 7. Подешавање штампе

### 7.1 Избор штампача

* Системски сервис `LOSPrintServiceBean` подржава:
  * `default` – подразумевани системски штампач (`PrintServiceLookup.lookupDefaultPrintService`).
  * `none` – онемогућава штампање (корисно за тест окружења).
  * `prn:<име>` – директно адресирање штампача по имену у Java Print Service-у.
  * `cmd:<наредба>` – извршавање спољне команде, где се `:file:` замењује путањом до привремене `.prn` датотеке.【F:dev/los-ejb/src/main/java/de/linogistix/los/common/businessservice/LOSPrintServiceBean.java†L42-L129】【F:dev/los-ejb/src/main/java/de/linogistix/los/common/businessservice/LOSPrintService.java†L19-L27】
* Уколико штампач не постоји, сервис избацује грешку `PRINTER_UNDEFINED` и у лог уписује сва доступна имена, што олакшава дијагностику.【F:dev/los-ejb/src/main/java/de/linogistix/los/common/businessservice/LOSPrintServiceBean.java†L140-L168】

### 7.2 Особине за аутоматску штампу

* При пријему робе постоје две битне особине:
  * `GOODS_RECEIPT_PRINT_LABEL` – да ли се етикета unit load-а штампа аутоматски при креирању позиције.【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/model/LOSInventoryPropertyKey.java†L22-L32】【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/facade/LOSGoodsReceiptFacadeBean.java†L433-L488】
  * `GOODS_RECEIPT_PRINTER_NAME` – подразумевани штампач за етикете при пријему; креира се као системска особина у `InventoryBasicDataServiceBean` и може се задати по клијенту или радној станици.【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/businessservice/InventoryBasicDataServiceBean.java†L55-L72】【F:dev/los.mobile-web/src/main/java/de/linogistix/mobile/processes/gr_direct/GRDirectBean.java†L1210-L1229】
* Мобилни процеси (конкретно опција „Пријем и складиштење“ у мобилном менију, позната као GR Direct) читају штампач преко `propertyService.getStringDefault(...)` и прослеђују га EJB фасади за штампу етикете.【F:dev/los.mobile-web/src/main/java/de/linogistix/mobile/processes/gr_direct/GRDirectBean.java†L1214-L1229】

### 7.3 Додељивање различитих штампача

* За етикете (нпр. `StockUnitLabel`) можете поставити штампач типа `cmd:` који позива посебан драјвер за термални штампач, док за А4 извештаје оставите `default` или конкретно име. На овај начин се у зависности од процеса бира одговарајући уређај без измене JRXML-а.
* Уколико желите ручни избор штампача у мобилним процесима, `PickingMobileBean` већ подржава скенирање кода штампача и прослеђивање вредности сервису за штампу (`printLabel`).【F:dev/los.mobile-web/src/main/java/de/linogistix/mobile/processes/picking/PickingMobileBean.java†L285-L307】

## 8. Савети за даље проширење

### 8.1 Повезивање са пословним процесом

* **Нов извештај у пословном току** – креирајте запис у табели „Jasper Reports“, поставите JRXML и компајлирајте га, а затим у постојећем фасадном EJB-у додајте метод који прикупља податке и позива `reportBusiness.createPdfDocument(...)`. Управо тако ради `DeliveryReportGenerator.generatePacketList(...)`, који прима налог (пиковање, отпрема или наруџбина), припрема DTO листу и враћа `Document` са готовим PDF-ом.【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/facade/LOSOrderFacadeBean.java†L482-L538】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L90-L152】
* **Аутоматско покретање** – нови метод можете позвати из тачке у процесу где желите документ (нпр. након завршетка отпреме или при пријему) баш као што `LOSOrderFacadeBean` позива генераторе при креирању отпремнице или packet листе. На тај начин извештај настаје без учешћа корисника, а PDF остаје доступан кроз исту фасаду ако је потребно касније преузимање.【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/facade/LOSOrderFacadeBean.java†L482-L538】
* **Више језика** – зато што `ReportBusiness` аутоматски убацује `REPORT_LOCALE` и ресурсни bundle, JRXML може да користи `$R{...}` за преводе без додатног кода.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L130-L135】
* **Додатни прилози** – ако је потребно убацити лого клијента или QR шаблон, додајте документ као „attachment“ на извештај; `ReportBusiness` ће га ставити у мапу параметара под његовим једноставним именом.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L121-L128】

### 8.2 Параметарски извештај у RCP клијенту

1. **EJB метод са параметрима** – дефинишите јавни метод у фасади (нпр. `InventoryReportFacade.generateArticlePlacement(String articleRef)`) који прима улазне параметре, чита податке (нпр. преко `StockUnitEntityService.readByItemData(...)`) и враћа `Document` из `ReportBusiness.createPdfDocument(...)`. Ово је исти образац као у методима `generatePacketList(...)` који већ враћају PDF за избор налога.【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/facade/LOSOrderFacadeBean.java†L482-L538】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L90-L152】
2. **RCP акција која прикупља унос** – у NetBeans модулу направите нову акцију или проширите постојећу по узору на `BOMasterNodeReportAction`: акција добија EJB преко `J2EEServiceLocator`, позива методу и кориснику нуди да сачува/отвори PDF.【F:rich.client/los.clientsuite/LOS Report/src/de/linogistix/reports/action/BOMasterNodeReportAction.java†L37-L127】 Пре позивања сервиса прикажите дијалог за унос параметара (`DialogDisplayer` или wizard), слично као што `ImportLocationAction` отвара чаробњак, валидацију и на крају преузима генерисане етикете са сервера.【F:rich.client/los.clientsuite/LOS Data Importer/src/de/linogistix/losdataimporter/storagelocation/ImportLocationAction.java†L66-L118】
3. **Интеграција у мени** – региструјте акцију у одговарајућем `layer.xml` или контекстном менију (нпр. Inventory Browser). Корисник тада може да покрене команду, унесе тражени параметар (шифра артикла, датум) и добије PDF без додатног повезивања на посебан процес. Ово одговара сценарију који си поменуо: корисник изабере команду „Штампај распоред артикла“, унесе шифру, а акција преузме и отвори извештај у истој RCP сесији.

На тај начин постојећи систем подржава и извештаје који се генеришу унутар аутоматизованих токова и ад-хок извештаје које корисник покреће ручно са произвољним параметрима.

Овим корацима добијате комплетну контролу над изгледом докумената и тиме како се штампају у оквиру система, било да штампате налепнице на посебном уређају или велике листе на класичном штампачу.

## 9. Подешавање штампача у пракси

### 9.1 Терминал за налепнице (термални штампач)

1. У NetBeans RCP клијенту отворите мени **System → Системске особине** и пронађите кључ `GOODS_RECEIPT_PRINTER_NAME`. Ако не постоји, креирајте га на нивоу клијента или конкретне радне станице (workstation) јер се ова особина иницијално генерише за системски клијент приликом иницијализације инвентар модула.【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/businessservice/InventoryBasicDataServiceBean.java†L55-L74】
2. У вредност унесите `prn:<име_штампача>` (нпр. `prn:Zebra GK420d`). Префикс `prn:` каже `LOSPrintServiceBean`-у да потражи именовани уређај у Java Print Service-у, а ако штампач није пронађен биће исписана листа постојећих имена у логу.【F:dev/los-ejb/src/main/java/de/linogistix/los/common/businessservice/LOSPrintServiceBean.java†L68-L168】
3. Ако користите посебног произвођача са сопственим алатом (нпр. Zebra Utilities), уместо `prn:` можете поставити `cmd:/usr/local/bin/zpl-send :file:`. У том случају сервис ће генерисати привремену `.prn` датотеку и позвати екстерну команду са путањом уметнутом на место `:file:`.【F:dev/los-ejb/src/main/java/de/linogistix/los/common/businessservice/LOSPrintServiceBean.java†L78-L99】
4. Особину можете задати и по радној станици (колона *Workstation*) – `LOSSystemPropertyServiceBean` ће најпре покушати да пронађе вредност за конкретан терминал, а ако је нема преузеће системски подешену вредност и по потреби је аутоматски креирати.【F:dev/los-ejb/src/main/java/de/linogistix/los/util/entityservice/LOSSystemPropertyServiceBean.java†L170-L245】
5. Мобилни процес „Пријем и складиштење“ (GR Direct) чита ову особину при иницијализацији и додељује штампач свакој етикети која се штампа након креирања јединице робе, па није потребна додатна логика у самом процесу.【F:dev/los.mobile-web/src/main/java/de/linogistix/mobile/processes/gr_direct/GRDirectBean.java†L185-L209】

### 9.2 Класичан А4 штампач

* За документе као што су отпремнице или packet листе оставите вредност `default`. `LOSPrintServiceBean` ће у том случају користити подразумевани системски штампач преко `PrintServiceLookup.lookupDefaultPrintService()`. Ако промените подразумевани штампач на оперативном систему, апликација не захтева додатну конфигурацију.【F:dev/los-ejb/src/main/java/de/linogistix/los/common/businessservice/LOSPrintServiceBean.java†L100-L125】
* Уколико желите специфичан А4 уређај, унесите `prn:<име>` исто као код термалног штампача. Добра пракса је да за излазне документе (packet листе, отпремнице) оставите `default`, а за етикете користите `prn:` или `cmd:` конфигурацију.
* За тест окружења можете привремено поставити вредност `none`. Сервис ће у том случају прескочити штампање без грешке, што је погодно за аутоматизоване тестове.【F:dev/los-ejb/src/main/java/de/linogистix/los/common/businessservice/LOSPrintServiceBean.java†L68-L75】

### 9.3 Раздвајање штампача по процесима

* Пошто се системске особине могу дефинисати на нивоу клијента, радне станице или целог система, лако је раздвојити штампаче по терминалима: један терминал може имати `GOODS_RECEIPT_PRINTER_NAME = prn:Zebra`, док други има `default` и тако иде на А4 уређај.【F:dev/los-ejb/src/main/java/de/linogistix/los/util/entityservice/LOSSystemPropertyServiceBean.java†L170-L245】
* Мобилни процеси читају додатне прекидаче из система својстава – нпр. `GRD_COLLECT_UNITLOAD_TYPE`, `GRD_COLLECT_LOT_ALWAYS` и `GOODS_RECEIPT_PRINT_LABEL` – па је могуће по процесу управљати да ли се етикета штампа аутоматски и који се унос тражи од оператера.【F:dev/los.mobile-web/src/main/java/de/linogistix/mobile/processes/gr_direct/GRDirectBean.java†L185-L209】【F:dev/los-ejb/src/main/java/de/linogistix/los/inventory/facade/LOSGoodsReceiptFacadeBean.java†L433-L505】
* Ако мобилни процеси треба да терају оператера да унесе дестинацију (нпр. за отпрему на друго место пре штампања), активирајте `SHIPPING_SCAN_DESTINATION` преко system својстава. Вредност `true` приморава мобилни процес отпреме да у workflow убаци корак за скенирање дестинације пре завршетка, што је корисно кад желите да отпремнице изађу тек када је дестинација потврђена.【F:dev/los.mobile-ejb/src/main/java/de/linogistix/mobileserver/util/MobileProperties.java†L33-L38】【F:dev/los.mobile-web/src/main/java/de/linogistix/mobile/processes/shipping/ShippingBean.java†L46-L94】

## 10. Проширење портфолија извештаја

Систем већ испоручује више JRXML шаблона (нпр. `PickingPacketlist`, `Contentlist`, `StockUnitLabel`, `StorageLocationLabels`) који покривају основне сценарије пиковaња, отпреме и етикетирања.【F:dev/wms2-ejb/src/main/resources/reports/PickingPacketlist.jrxml†L1-L10】【F:dev/wms2-ejb/src/main/resources/reports/Contentlist.jrxml†L1-L8】【F:dev/wms2-ejb/src/main/resources/reports/StockUnitLabel.jrxml†L1-L5】【F:dev/los-ejb/src/main/resources/de/linogistix/los/location/res/StorageLocationLabels.jrxml†L1-L8】
Поред њих, у пракси су најчешће тражени:

* преглед расположивости артикула по локацијама (са количином, лотом, статусом),
* листе за инвентаризацију (задати артикал/зону и одштампати све јединице које треба пребројати),
* консолидациони извештаји за фиксне локације (пре допуне).

### 10.1 Пример: извештај о распореду артикла

**1. JRXML шаблон** – креирајте шаблон у Jaspersoft Studio-у који очекује поља као што су `storageLocation`, `unitLoad`, `lotNumber`, `amount` и `lock`. Подешавање подршке за локализацију преузима се из `ReportBusiness` (параметри `REPORT_LOCALE` и ресурсни bundle), тако да можете користити `$R{...}` за наслове.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L121-L152】

**2. Сервисни слој** – имплементирајте класу по узору на `StockUnitReportGenerator` која ће скупити податке и позвати Jasper. Подаци се могу прочитати преко `StockUnitEntityService.readByItemData(...)`, који враћа све залихе за задати артикал сортиране по `strategyDate` и `id`.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/inventory/StockUnitReportGenerator.java†L61-L104】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/inventory/StockUnitEntityService.java†L53-L111】

**3. Веза са артиком** – ако корисник уноси шифру, користите `QueryItemDataServiceRemote` из мобилног модула или `ItemDataBusiness` да прочитате `ItemData` пре слања у генератор. Сама колекција DTO објеката може да изгледа исто као у `StockUnitReportDto`, јер већ садржи количину, лот и ознаке јединице робе.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/inventory/StockUnitReportGenerator.java†L71-L99】

**4. Објављивање као веб сервис/факада** – додajte метод у постојећи фасадни EJB (нпр. `InventoryReportFacade`) који прима шифру артикла и враћа `Document` добијен позивом `reportBusiness.createPdfDocument(...)`. Овај метод већ припрема локализацију и прилоге (нпр. лого клијента) на исти начин као постојећи генератори.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L90-L152】

**5. Регистрација у Jasper репозиторијуму** – у NetBeans клијенту отворите мени **Jasper Reports**, креирајте нови запис са називом извештаја и отпремите JRXML. Акције *Учитај*, *Сачувај* и *Компајлирај* користе исте сервисе који смо описали у §2, тако да ће нова верзија одмах бити доступна.【F:rich.client/los.clientsuite/LOS Common/src/de/linogistix/common/bobrowser/action/BOJasperReportLoadSourceAction.java†L83-L133】【F:rich.client/los.clientsuite/LOS Common/src/de/linogistix/common/bobrowser/action/BOJasperReportCompileAction.java†L67-L106】

**6. Акција у клијенту** – додате нови `BOMasterNodeReportAction` у модулу који приказује артикле (Inventory Browser). Акција узима селектоване чворове, позива EJB метод и кориснику нуди да сачува или отвори PDF, што је исто понашање које постојећа класа већ имплементира.【F:rich.client/los.clientsuite/LOS Report/src/de/linogistix/reports/action/BOMasterNodeReportAction.java†L37-L127】

**7. Корисничка употреба** – у Inventory Browser-у корисник означи артикал, изабере нову ставку контекстног менија (нпр. *Штампај распоред артикла*) и добије PDF са свим локацијама. Ако је потребно подесити улазни параметар, може се приказати `JOptionPane` пре позива EJB-а или користити постојећи дијалог за избор артикула.

### 10.2 Још идеја за извештаје

* **Инвентарни лист по зони** – користи `StockUnitEntityService.readList(...)` са ограничењем на зону (`StorageLocation.getZone()`), што омогућава брзу припрему за цикличну инвентаризацију.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/inventory/StockUnitEntityService.java†L55-L124】
* **Распоред фиксних локација за допуну** – ослонити се на `ReplenishBusiness` који већ зна да ли се допуна врши са pick локација (`KEY_REPLENISH_FROM_PICKING`). Извештај може да обележи локације које немају довољно залиха пре него што се покрене допуна.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/replenish/ReplenishBusiness.java†L780-L844】
* **Извештај о отпреми** – допунити постојећи `ShippingPacketlist` додатним подацима (нпр. датум транспорта, превозник) који су већ доступни у `DeliveryReportGenerator` када се креира PDF за отпрему.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/delivery/DeliveryReportGenerator.java†L146-L399】

## 11. Системске особине и њихов утицај

Следеће табеле су резиме свих кључних системских својстава која се користе у коду. За свако је наведено где се дефинише, подразумевана вредност, контекст (систем, клијент, радно место) и функционални утицај.

### 11.1 Општа и клијентска својства (LOSCommonPropertyKey)

| Кључ | Подразумевано | Пример вредности | Контекст | Ефекат |
|------|---------------|------------------|----------|--------|
| `MAIL_SENDER`, `MAIL_SERVER`, `MAIL_AUTHORIZE`, `MAIL_HOST_USER`, `MAIL_HOST_PASSWD` | празно / `false` | `MAIL_SENDER = warehouse@firma.rs`, `MAIL_SERVER = smtp.firma.rs`, `MAIL_AUTHORIZE = true`, `MAIL_HOST_USER = wms`, `MAIL_HOST_PASSWD = ***` | систем/клијент | Конфигуришу SMTP пошиљаоца; ако су празни, `LOSMailServiceBean` прекида слање и уписује грешку.【F:dev/los-ejb/src/main/java/de/linогистix/los/model/LOSCommonPropertyKey.java†L16-L39】【F:dev/los-ejb/src/main/java/de/linогistix/los/common/businessservice/LOSMailServiceBean.java†L34-L108】 |
| `NBCLIENT_SHOW_DETAIL_PROPERTIES`, `NBCLIENT_RESTORE_TABS`, `NBCLIENT_SELECTION_ON_START`, `NBCLIENT_SELECTION_UNLIMITED` | `true/false` (видети табелу) | нпр. `NBCLIENT_RESTORE_TABS = true`, `NBCLIENT_SELECTION_UNLIMITED = false` | клијент или workstation | Подешавају понашање NetBeans RCP клијента (аутоматски приказ панела, враћање табова, иницијална селекција, ограничење селекције).【F:dev/los-ejb/src/main/java/de/linогistix/los/model/LOSCommonPropertyKey.java†L41-L56】【F:dev/los-ejb/src/main/java/de/linогistix/los/common/businessservice/CommonBasicDataServiceBean.java†L52-L69】 |
| `NBCLIENT_VERSION_MATCHER` | `.*` | `^2\.3\..*` | workstation (са fallback-ом на систем) | Регуларни израз којим сервер проверава верзију десктоп клијента при login-у; неслагање узрокује грешку „Client version outdated“.【F:dev/los-ejb/src/main/java/de/linогистix/los/model/LOSCommonPropertyKey.java†L57-L60】【F:dev/los-ejb/src/main/java/de/linогистix/los/user/LoginServiceBean.java†L44-L63】 |

### 11.2 Својства процеса пријема (LOSInventoryPropertyKey и WMS2)

| Кључ | Подразумевано | Пример вредности | Контекст | Ефекат |
|------|---------------|------------------|----------|--------|
| `GOODS_RECEIPT_PRINT_LABEL` | `false` | `true` за аутоматску штампу етикете | систем | Када је укључено, етикета јединице робе се штампа аутоматски након креирања позиције пријема.【F:dev/los-ejb/src/main/java/de/linогistix/los/inventory/model/LOSInventoryPropertyKey.java†L22-L33】【F:dev/los-ejb/src/main/java/de/linогистix/los/inventory/facade/LOSGoodsReceiptFacadeBean.java†L433-L505】 |
| `GOODS_RECEIPT_PRINTER_NAME` | `null` | `prn:Zebra GK420d` или `cmd:/usr/local/bin/zpl-send :file:` | клијент/радна станица | Одређује штампач за етикете при пријему (види §9.1). Ако је празно, користи се системска вредност или се особина аутоматски креира при првом читању.【F:dev/los-ejb/src/main/java/de/linогистix/los/inventory/model/LOSInventoryPropertyKey.java†L22-L32】【F:dev/los-ejb/src/main/java/de/linогистix/los/util/entityservice/LOSSystemPropertyServiceBean.java†L170-L209】 |
| `GOODS_IN_DEFAULT_LOCK` | `0` | `100` за карантин, `0` за без закључавања | систем | Закључавање нове залихе (нпр. карантин); вредност је ниво браве који се примењује одмах након пријема.【F:dev/los-ejb/src/main/java/de/linогистix/los/inventory/model/LOSInventoryPropertyKey.java†L27-L31】【F:dev/los-ejb/src/main/java/de/linогистix/los/inventory/businessservice/InventoryBasicDataServiceBean.java†L55-L68】 |
| `GOODS_RECEIPT_LOCATION_DEFAULT` | празно/прва goods-in локација | `GR-DOCK-01` | клијент | Подразумевана пријемна локација када оператер не унесе ништа; у иницијалној поставци аутоматски се попуњава ако постоји тачно једна локација за пријем.【F:dev/los-ejb/src/main/java/de/linогистix/los/inventory/model/LOSInventoryPropertyKey.java†L12-L20】【F:dev/los-ejb/src/main/java/de/linогистix/los/inventory/businessservice/InventoryBasicDataServiceBean.java†L62-L73】 |
| `GOODS_RECEIPT_LIMIT_AMOUNT_TO_NOTIFIED` | `false` | `true` да се количина ограничи на најаву | систем | Када је `true`, у мобилном пријему није дозвољено унети више од најављене количине; логика се примењује и при бирању најава и при уносу количине.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/module/Wms2SetupService.java†L176-L186】【F:dev/los.mobile-web/src/main/java/de/linогистix/mobile/processes/gr_direct/GRDirectBean.java†L185-L209】【F:dev/los.mobile-web/src/main/java/de/linогистix/mobile/processes/gr_direct/GRDirectBean.java†L523-L533】 |
| `GRD_COLLECT_UNITLOAD_TYPE`, `GRD_COLLECT_UNITLOAD_NO`, `GRD_COLLECT_LOT_ALWAYS` | `true/true/false` | нпр. `GRD_COLLECT_UNITLOAD_TYPE = false` (тип није обавезан), `GRD_COLLECT_LOT_ALWAYS = true` | радно место | Управљају корацима у GR Direct процесу: да ли се тражи тип јединице, број јединице и да ли се лот увек уноси. Може се подесити по терминалу да би се оптимизовао ток за различите линије пријема.【F:dev/los.mobile-web/src/main/java/de/linогистix/mobile/processes/gr_direct/GRDirectBean.java†L185-L205】【F:dev/los.mobile-web/src/main/java/de/linогистix/mobile/processes/gr_direct/GRDirectBean.java†L480-L506】 |

### 11.3 Мобилни процеси (MobileProperties)

| Кључ | Подразумевано | Пример вредности | Контекст | Ефекат |
|------|---------------|------------------|----------|--------|
| `PICKING_IDENTIFY_PACKET` | `false` | `true` када желите да оператер прво скенира празан носач | радно место | У пиковaњу захтева од оператера да унапред идентификује јединицу на коју одлаже робу (pick-to); без овог корака процес одмах нуди прву позицију за одвајање.【F:dev/los.mobile-ejb/src/main/java/de/linогистix/mobileserver/util/MobileProperties.java†L30-L38】【F:dev/los.mobile-web/src/main/java/de/linогистix/mobile/processes/picking/PickingMobileData.java†L61-L105】 |
| `SHIPPING_SCAN_DESTINATION` | `false` | `true` да би се унео/скенирао одлазни док | радно место | У мобилној отпреми додаје корак за скенирање/унос дестинације пре завршетка отпреме.【F:dev/los.mobile-ejb/src/main/java/de/linогистix/mobileserver/util/MobileProperties.java†L33-L38】【F:dev/los.mobile-web/src/main/java/de/linогистix/mobile/processes/shipping/ShippingBean.java†L46-L91】 |

### 11.4 WMS2 функционална својства (Wms2Properties)

| Кључ | Подразумевано | Пример вредности | Контекст | Ефекат |
|------|---------------|------------------|----------|--------|
| `PASSWORD_REGULAR_EXPRESSION` | `null` (без провере) | `^(?=.*[A-Z])(?=.*\d).{8,}$` | систем | Када се постави, `PatternPasswordValidator` примењује регуларни израз приликом промене лозинке корисника.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/util/Wms2Properties.java†L33-L48】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/user/PatternPasswordValidator.java†L53-L74】 |
| `STRATEGY_ORDER_DEFAULT`, `STRATEGY_ORDER_EXTINGUISH` | генерисани називи | `STRATEGY_ORDER_DEFAULT = PICK-BY-ZONE`, `STRATEGY_ORDER_EXTINGUISH = RETURN-TO-STOCK` | клијент/систем | Одређују подразумеване стратегије за робу и „extinguish“ налоге; ако стратегија не постоји, сервис је аутоматски креира са описом и чува назив у својству.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/util/Wms2Properties.java†L34-L35】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/strategy/OrderStrategyEntityService.java†L90-L141】 |
| `STRATEGY_STORAGE_DEFAULT`, `STRATEGY_STORAGE_REPLENISHMENT` | генерисани називи | `STRATEGY_STORAGE_DEFAULT = FIFO-ZONE-A`, `STRATEGY_STORAGE_REPLENISHMENT = BULK-TO-PICK` | систем | Дефинишу подразумеване стратегије складиштења и допуне; приликом креирања се поставља `sorts` (нпр. зона, Y, X) и омогућава да LocationFinder ради у задатом редоследу.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/util/Wms2Properties.java†L36-L37】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/strategy/StorageStrategyEntityService.java†L90-L132】 |
| `LOCATIONCLUSTER_DEFAULT`, `LOCATIONTYPE_DEFAULT`, `UNITLOADTYPE_DEFAULT`, `UNITLOADTYPE_PICKING`, `AREA_DEFAULT` | генерисани називи | нпр. `LOCATIONCLUSTER_DEFAULT = MAIN-WH`, `UNITLOADTYPE_PICKING = TOTE-60x40` | систем | Подразумеване структуре топологије (кластери, типови локација и јединица) које се креирају ако не постоје и користе као „fallback“ у разним процесима (нпр. резервисање, допуна).【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/util/Wms2Properties.java†L38-L43】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/location/LocationClusterEntityService.java†L68-L118】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/location/LocationTypeEntityService.java†L80-L132】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/inventory/UnitLoadTypeEntityService.java†L96-L170】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/location/AreaEntityService.java†L96-L134】 |
| `REPORT_LOCALE` | локал система | `sr_RS` или `en_GB` | систем | Локал који се прослеђује Jasper извештајима (`JRParameter.REPORT_LOCALE`), тако да је цела апликација у истој локализацији.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/module/Wms2SetupService.java†L176-L186】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/report/ReportBusiness.java†L130-L148】 |
| `SHIPPING_LOCATION` | празно | `SHIPPING-DOCK-02` | систем/клијент | Ако је попуњено, отпремљене јединице се аутоматски трансферишу на ову локацију након завршетка наруџбине; у супротном остају на тренутној дестинацији из налога.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/util/Wms2Properties.java†L44-L45】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/shipping/ShippingBusiness.java†L474-L486】 |
| `SHIPPING_RENAME_UNITLOAD` | `true` | `false` ако желите да задржите оригиналну ознаку | систем/клијент | Одређује да ли се ознака јединице при отпреми надовезује ID-јем (нпр. `UL123-456`) ради избегавања дупликата.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/util/Wms2Properties.java†L44-L46】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/shipping/ShippingBusiness.java†L386-L406】 |
| `REPLENISH_FROM_PICKING_LOCATION` | `true` | `false` ако желите да допуне долазе само из резервног складишта | систем | Дозвољава да се допуна фиксних локација ради са пиковних локација уколико нема довољно резерви на резервним складиштима.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/module/Wms2SetupService.java†L176-L186】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/replenish/ReplenishBusiness.java†L780-L844】 |
| `STRATEGY_ZONE_FLOW` | `A,B,C;B,C,A;C,B,A` | `A1,A2;B1,B2` | систем/клијент | Дефинише листе зона кроз које `LocationFinderBean` пролази при избору нове локације; прва зона је стартна тачка, а остале се проверавају редом како би се осигурао „flow“ кроз магацин.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/module/Wms2SetupService.java†L185-L186】【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/strategy/LocationFinderBean.java†L332-L370】 |

### 11.5 Пројектне особине

| Кључ | Подразумевано | Пример вредности | Контекст | Ефекат |
|------|---------------|------------------|----------|--------|
| `CREATE_DEMO_TOPOLOGY` | `false` | `true` током обуке нових оператера | систем | Омогућава приказ дугмета за креирање демо-топологије у RCP клијенту; када је постављено на `true`, администратор може из менија да генерише пример локација и података за тестирање.【F:dev/project-ejb/src/main/java/de/linogistix/los/reference/model/ProjectPropertyKey.java†L5-L8】【F:dev/project-ejb/src/main/java/de/linogistix/los/reference/facade/RefTopologyFacadeBean.java†L46-L68】 |

Комбинујући ове табеле са упутствима за конфигурацију штампача и израду нових извештаја, можете да стандардујете изглед докумената и прилагодите процесе конкретним магацинским токовима без измене базе података.

## 12. Честа питања

### Могу ли да одштампам један списак пакета за више завршених захтева за отпрему?

Подразумевана имплементација генерише списак пакета за **један** пиковни, отпремни или испоручни налог по PDF документу. `DeliveryReportGenerator.generatePacketList(...)` увек добија један `ShippingOrder` или `DeliveryOrder`, припрема ставке и позива Jasper извештај за тај појединачни налог.【F:dev/wms2-ejb/src/main/java/de/wms2/mywms/delivery/DeliveryReportGenerator.java†L202-L260】 Клијентски дијалог у NetBeans RCP-у затим пролази кроз изабране налоге и за сваки посебно позива фасаду `orderFacade.generatePacketList(orderTO.getId())`, тако да резултат буде по један PDF по налогу (уз могуће вишеструко редно штампање).【F:rich.client/los.clientsuite/LOS Inventory Browser/src/de/linogistix/inventory/browser/dialog/CustomerOrderPrintDialog.java†L437-L513】

Ако је потребан збирни документ за више завршених захтева за отпрему, неопходно је прилагодити серверски код – нпр. увести фасаду која прихвата листу `ShippingOrder` идентификатора, сакупи све пакете у једну колекцију и проследи је Jasper извештају као јединствен извор података. Алтернативно, могуће је после генерисања појединачних PDF-ова спојити документе у клијенту или у спољној интеграционој компоненти. У стандардној дистрибуцији не постоји опција да се више захтева за отпрему аутоматски групише у један списак пакета.

