# myWMS Playbook: GR Unit Load Label Printing

Овај документ је практичан playbook за подешавање штампања `Unit load` етикета при пријему робе (Goods Receipt).

## 1. Шта тачно иде у `workstation` вредност

Кратак одговор: у `workstation` стављаш име рачунара на ком је покренут `rich.client`.

- Није име сервера.
- Није име штампача.
- Није IP штампача.

`rich.client` узима workstation име из environment променљивих овим редом:

1. `WORKSTATION_NAME`
2. `COMPUTERNAME` (Windows)
3. `HOSTNAME`
4. `HOST`

Практично:

1. Ако на Windows рачунару имаш `COMPUTERNAME=RCV-01`, у `System Properties` стављаш `workstation=RCV-01`.
2. Ако желиш да форсираш друго име, подеси `WORKSTATION_NAME` на том рачунару, нпр. `WORKSTATION_NAME=PRIJEM-1`, па у myWMS користи `workstation=PRIJEM-1`.
3. Обавезно додај и fallback ред са `workstation=DEFAULT`.

## 2. Кључеви и вредности које се подешавају у myWMS

У `BOBrowser -> System -> System Properties` подеси:

1. `propertyKey=GOODS_RECEIPT_PRINTER_NAME`
2. `workstation=<ime_klijentskog_racunara_ili_DEFAULT>`
3. `value=<nacin_stampanja>`

Примери `value`:

1. `prn:Zebra_GK420d` (сервер штампа на named printer queue)
2. `cmd:/usr/local/bin/print-label :file:` (сервер зове спољну команду)
3. `none` (искључена штампа за ту workstation вредност)

Пример за више `rich.client` рачунара:

1. `GOODS_RECEIPT_PRINTER_NAME | workstation=RCV-01 | value=prn:Zebra_GK420d`
2. `GOODS_RECEIPT_PRINTER_NAME | workstation=RCV-02 | value=prn:Zebra_ZD421`
3. `GOODS_RECEIPT_PRINTER_NAME | workstation=DEFAULT | value=none`

## 3. Сценарио А: Ubuntu сервер + локална мрежа

### 3.1 Ако је штампач мрежни

На Ubuntu серверу:

```bash
sudo apt update
sudo apt install -y cups cups-client
sudo systemctl enable --now cups
```

Додај штампач као CUPS queue:

```bash
# IPP пример
sudo lpadmin -p Zebra_GK420d -E -v ipp://192.168.1.50/ipp/print -m everywhere

# raw socket 9100 пример
sudo lpadmin -p Zebra_GK420d_raw -E -v socket://192.168.1.50:9100 -m raw
```

Провери тачно име queue-а:

```bash
lpstat -p
lpstat -a
```

Ту вредност користиш у myWMS као `prn:<ime>`, нпр. `prn:Zebra_GK420d`.

Тест са сервера:

```bash
echo "myWMS test" | lp -d Zebra_GK420d
```

### 3.2 Ако је штампач прикључен на Windows рачунар (где је и rich.client)

Ово ради само ако Ubuntu сервер може мрежно да види тај Windows рачунар.

Кораци:

1. На Windows укључи sharing за тај штампач.
2. На Ubuntu додај га као мрежни queue (SMB/IPP, зависно шта је омогућено).
3. У myWMS постави `value=prn:<queue_name_na_ubuntu_serveru>`.

Важно: за GR `Unit load` штампа је серверска. То што је `rich.client` на Windows PC не значи да ће GR штампа ићи локално са тог PC ако сервер нема приступ штампачу.

## 4. Сценарио Б: VPS/cloud сервер + локални штампач код корисника

Класичан `prn:` често не може директно, јер VPS не види локални LAN/USB штампач.

Решење 1: VPN тунел између VPS и локалне мреже.

1. Подигни site-to-site или client VPN.
2. На VPS додај CUPS queue ка локалном штампачу.
3. У myWMS стави `prn:<queue_na_vps>`.

Решење 2: print gateway + `cmd:` (често најпрактичније).

1. На VPS у myWMS подеси, нпр:
   `cmd:/usr/bin/curl -sS -X POST --data-binary @:file: https://print-gateway.example.local/gr-label`
2. `myWMS` направи temp фајл и замени `:file:` путањом.
3. Gateway сервис локално шаље документ на физички штампач.

## 5. Како да провериш да је `workstation` добро подешен

На клијентском рачунару:

1. Windows CMD: `echo %WORKSTATION_NAME%` и `echo %COMPUTERNAME%`
2. Linux shell: `echo $WORKSTATION_NAME` и `echo $HOSTNAME`

У myWMS `System Properties` мора да постоји ред где `workstation` тачно одговара вредности коју `rich.client` шаље. Ако не постоји, користи се `DEFAULT`.

## 6. Брза production чеклиста

1. На серверу штампач queue ради (`lpstat -p` га види).
2. `GOODS_RECEIPT_PRINTER_NAME` постоји за сваку битну workstation вредност.
3. Постоји `DEFAULT` fallback.
4. `value` је `prn:<tačno_ime_queue>` или исправан `cmd:...`.
5. Тестирано из Goods Receipt процеса у `rich.client`.

## 7. Ограничење које треба знати

За GR `Unit load` етикете у постојећем коду штампа иде преко сервера (не директно локално преко `rich.client`).  
Ако сервер нема пут до штампача, потребан је VPN или `cmd:` gateway приступ.

