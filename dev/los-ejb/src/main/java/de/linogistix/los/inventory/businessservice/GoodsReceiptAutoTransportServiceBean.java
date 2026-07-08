/*
 * Copyright (c) 2006 - 2013 LinogistiX GmbH
 *
 *  www.linogistix.com
 *
 *  Project myWMS-LOS
 */
package de.linogistix.los.inventory.businessservice;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.mywms.facade.FacadeException;

import de.linogistix.los.inventory.exception.InventoryException;
import de.linogistix.los.inventory.exception.InventoryExceptionKey;
import de.linogistix.los.inventory.model.LOSInventoryPropertyKey;
import de.linogistix.los.util.entityservice.LOSSystemPropertyService;
import de.wms2.mywms.inventory.StockUnit;
import de.wms2.mywms.inventory.StockUnitEntityService;
import de.wms2.mywms.inventory.UnitLoad;
import de.wms2.mywms.location.StorageLocation;
import de.wms2.mywms.location.StorageLocationEntityService;
import de.wms2.mywms.transport.TransportBusiness;
import de.wms2.mywms.transport.TransportOrderType;

@Stateless
public class GoodsReceiptAutoTransportServiceBean {

	private static final Logger log = Logger.getLogger(GoodsReceiptAutoTransportServiceBean.class);

	@EJB
	private LOSSystemPropertyService propertyService;
	@Inject
	private StorageLocationEntityService locationService;
	@Inject
	private StockUnitEntityService stockUnitService;
	@Inject
	private TransportBusiness transportBusiness;

	public void createTransportOrderIfConfigured(UnitLoad unitLoad) throws FacadeException {
		String destinationName = propertyService.getStringDefault(
				LOSInventoryPropertyKey.TRANSPORT_ORDER_DESTINATION, "");
		if (StringUtils.isBlank(destinationName)) {
			return;
		}

		if (unitLoad == null) {
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT,
					"Cannot create automatic transport order. Unit load is missing.");
		}
		if (unitLoad.getClient() == null) {
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT,
					"Cannot create automatic transport order. Unit load has no client: " + unitLoad);
		}
		if (unitLoad.getStorageLocation() == null) {
			throw new InventoryException(InventoryExceptionKey.CUSTOM_TEXT,
					"Cannot create automatic transport order. Unit load has no source location: " + unitLoad);
		}

		List<StockUnit> stockUnits = stockUnitService.readByUnitLoad(unitLoad);
		if (stockUnits.isEmpty()) {
			log.warn("Skip automatic transport order. Unit load has no stock units. unitLoad=" + unitLoad);
			return;
		}

		StorageLocation destinationLocation = locationService.readByName(destinationName);
		if (destinationLocation == null) {
			log.error("Cannot create automatic transport order. Configured destination location does not exist. "
					+ "propertyKey=" + LOSInventoryPropertyKey.TRANSPORT_ORDER_DESTINATION
					+ ", destinationName=" + destinationName
					+ ", unitLoad=" + unitLoad);
			throw new InventoryException(InventoryExceptionKey.NO_SUCH_STORAGELOCATION, destinationName);
		}

		transportBusiness.createOrder(unitLoad, destinationLocation, TransportOrderType.TRANSFER, null);
	}
}
