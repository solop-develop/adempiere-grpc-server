/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package com.solop.sp009.util;

import com.solop.sp009.model.MSP009Expedient;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_DD_TransportUnit;
import org.adempiere.core.domains.models.I_M_Package;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MPackage;
import org.compiere.model.MShipper;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.distribution.model.MDDTransportAssignment;
import org.eevolution.distribution.model.MDDTransportUnit;
import org.eevolution.distribution.model.MDDTransportUnitType;
import org.eevolution.distribution.model.MDDVehicle;
import org.eevolution.distribution.model.MDDVehicleAssignment;
import org.eevolution.distribution.model.MDDVehicleType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Add here all changes for core and statci methods
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ImportExportUtil {
	/**Document Base Type Table Name*/
	public static final String DocBaseType_TableName = "C_DocBaseType";
	/**Column Name Document Base Type Identifier*/
	public static final String COLUMNNAME_C_DocBaseType_ID = "C_DocBaseType_ID";
	/**Document Base Type Table Name*/
	public static final String DocBaseType_TableName_Trl = "C_DocBaseType_Trl";
	/**Column Description for Document Base Table*/
	public static final String DocBaseType_ColumnDescription = "Description";
	/**Column Name for Document Base Table*/
	public static final String DocBaseType_ColumnName = "Name";
	/**Column UUID for Document Base Table*/
	public static final String DocBaseType_ColumnUUID = "UUID";
	/**Column Accounting Class Name for Document Base Table*/
	public static final String DocBaseType_ColumnAccountingClassName = "AccountingClassName";
	/**Column Document Base Type for Document Base Table*/
	public static final String DocBaseType_ColumnDocBaseType = "DocBaseType";
	/**Column Entity Type for Document Base Table*/
	public static final String DocBaseType_ColumnEntityType = "EntityType";
	/**Column Name Document Type Translation Print Name Column*/
	public static final String COLUMNNAME_C_DocType_Trl_PrintName = "PrintName";
	/**Import / Export Invoice Referenced*/
	public static final String COLUMNNAME_SP009_RefInvoice_ID = "SP009_RefInvoice_ID";
	/**Import / Export Is Expedient Tax*/
	public static final String COLUMNNAME_SP009_IsExpedientTax = "SP009_IsExpedientTax";
	/**Import / Export Is Affects Expedient Tax*/
	public static final String COLUMNNAME_SP009_IsAffectsExpedientTax = "SP009_IsAffectsExpedient";
	/** Column name MinimumVolume */
    public static final String COLUMNNAME_MinimumVolume = "MinimumVolume";
    /** Column name MaximumVolume */
    public static final String COLUMNNAME_MaximumVolume = "MaximumVolume";
    /** Column name MinimumWeight */
    public static final String COLUMNNAME_MinimumWeight = "MinimumWeight";
    /** Column name MaximumWeight */
    public static final String COLUMNNAME_MaximumWeight = "MaximumWeight";
    /** Column name TransportWidth */
    public static final String COLUMNNAME_TransportWidth = "TransportWidth";
    /** Column name TransportHeight */
    public static final String COLUMNNAME_TransportHeight = "TransportHeight";
    /** Column name TransportLength */
    public static final String COLUMNNAME_TransportLength = "TransportLength";
    /** Column name UnladenWeight */
    public static final String COLUMNNAME_UnladenWeight = "UnladenWeight";
    /** Column name AxlesNumber */
    public static final String COLUMNNAME_AxlesNumber = "AxlesNumber";
    /**	Transport Unit Type	*/
    public static final String COLUMNNAME_DD_TransportUnitType_ID = "DD_TransportUnitType_ID";
    /** Transport Unit	*/
    public static final String COLUMNNAME_DD_TransportUnit_ID = "DD_TransportUnit_ID";
    /** Default Vehicle type	*/
    public static final String COLUMNNAME_DefaultVehicleType_ID = "DefaultVehicleType_ID";
    
    /** Transport Unit allocation for Order	*/
    public static final String TABLENAME_DD_OrderTransportAssignment = "DD_OrderTransportAssignment";
    /** Allocate Packages to Transport Unit */
    public static final String COLUMNNAME_IsAllocatePackages = "IsAllocatePackages";
	/** Error Message for validate complete package on Order */
	public static final String MESSAGE_ContainerValidationError = "ContainerValidationError";
	/** Message for Order Line for complete package validation */
	public static final String MESSAGE_ContainerLineValidationError = "ContainerLineValidationError";
    
	public static final String COLUMNNAME_IsUsedByAdjustExpedient = "SP009_IsUsedByAdjustExpedient";

	public static final String COLUMNNAME_SP009_IsExpedientProject = "SP009_IsExpedientProject";
	/**
	 * Set values for transport unit from transport unit type
	 * @param transportUnit
	 */
	public static void setTransportUnitValueFromType(MDDTransportUnit transportUnit) {
		MDDTransportUnitType transportUnitType = (MDDTransportUnitType) transportUnit.getDD_TransportUnitType();
		transportUnit.setVolume_UOM_ID(transportUnitType.getVolume_UOM_ID());
		transportUnit.setDimension_UOM_ID(transportUnitType.getDimension_UOM_ID());
		transportUnit.setWeight_UOM_ID(transportUnitType.getWeight_UOM_ID());
		//	
		transportUnit.setMinimumVolume((BigDecimal) transportUnitType.get_Value(COLUMNNAME_MinimumVolume));
		transportUnit.setMaximumVolume((BigDecimal) transportUnitType.get_Value(COLUMNNAME_MaximumVolume));
		transportUnit.setMinimumWeight((BigDecimal) transportUnitType.get_Value(COLUMNNAME_MinimumWeight));
		transportUnit.setMaximumWeight((BigDecimal) transportUnitType.get_Value(COLUMNNAME_MaximumWeight));
		transportUnit.setTransportWidth((BigDecimal) transportUnitType.get_Value(COLUMNNAME_TransportWidth));
		transportUnit.setTransportHeight((BigDecimal) transportUnitType.get_Value(COLUMNNAME_TransportHeight));
		transportUnit.setTransportLength((BigDecimal) transportUnitType.get_Value(COLUMNNAME_TransportLength));
		transportUnit.setUnladenWeight((BigDecimal) transportUnitType.get_Value(COLUMNNAME_UnladenWeight));
		transportUnit.setAxlesNumber((BigDecimal) transportUnitType.get_Value(COLUMNNAME_AxlesNumber));
	}
	
	/**
	 * get Transport Unit Allocation from Order
	 * @param order
	 * @return
	 */
	public static List<PO> getTransportUnitAllocationFromOrder(MOrder order) {
		return new Query(order.getCtx(), TABLENAME_DD_OrderTransportAssignment, I_C_Order.COLUMNNAME_C_Order_ID + " = ?", order.get_TrxName())
				.setParameters(order.getC_Order_ID())
				.list();
	}
	
	/**
	 * Create Transport unit from order based on allocation
	 * @param order
	 */
	public static void createAndAllocateTransportUnitFromOrder(MOrder order) {
		if(!order.get_ValueAsBoolean(COLUMNNAME_IsAllocatePackages)) {
			return;
		}
		if(order.getM_Shipper_ID() <= 0) {
			throw new AdempiereException("@M_Shipper_ID@ @IsMandatory@");
		}
		AtomicReference<MDDVehicle> vehicle = new AtomicReference<MDDVehicle>();
		getTransportUnitAllocationFromOrder(order)
		.stream()
		.filter(orderTransportUnit -> orderTransportUnit.get_ValueAsInt(COLUMNNAME_DD_TransportUnit_ID) <= 0)
		.forEach(orderTransportUnit -> {
			if(vehicle.get() == null) {
				vehicle.set(createVehicleFromOrder(order));
			}
			//	Add
			createAndAllocateTransportUnit(vehicle.get(), orderTransportUnit, order);
		});
	}
	
	/**
	 * Create and 
	 * @param order
	 */
	private static void createAndAllocateTransportUnit(MDDVehicle vehicle, PO orderAllocation, MOrder order) {
		MDDTransportUnitType transportUnitType = new MDDTransportUnitType(order.getCtx(), orderAllocation.get_ValueAsInt(COLUMNNAME_DD_TransportUnitType_ID), order.get_TrxName());
		MDDTransportUnit transportUnit = new MDDTransportUnit(order.getCtx(), 0, order.get_TrxName());
		PO.copyValues(transportUnitType, transportUnit, true);
		transportUnit.setDD_TransportUnitType_ID(transportUnitType.getDD_TransportUnitType_ID());
		transportUnit.setValue(order.getDocumentNo());
		transportUnit.setName(order.getDocumentNo());
		transportUnit.setTransportStatus(MDDTransportUnit.TRANSPORTSTATUS_Available);
		transportUnit.saveEx();
		MDDTransportAssignment transportUnitAssignment = new MDDTransportAssignment(order.getCtx(), 0, order.get_TrxName());
		transportUnitAssignment.setDD_Vehicle_ID(vehicle.getDD_Vehicle_ID());
		transportUnitAssignment.setDD_TransportUnit_ID(transportUnit.getDD_TransportUnit_ID());
		transportUnitAssignment.saveEx();
		//	Set
		orderAllocation.set_ValueOfColumn(COLUMNNAME_DD_TransportUnit_ID, transportUnit.getDD_TransportUnit_ID());
		orderAllocation.saveEx();
	}
	
	
	/**
	 * create vehicle based on vehicle type created by default on shipper
	 * @param order
	 * @return
	 */
	private static MDDVehicle createVehicleFromOrder(MOrder order) {
		MShipper shipper = (MShipper) order.getM_Shipper();
		if(shipper.get_ValueAsInt(COLUMNNAME_DefaultVehicleType_ID) <= 0) {
			throw new AdempiereException("@DefaultVehicleType_ID@ @NotFound@");
		}
		MDDVehicleType defaultVehicleType = new MDDVehicleType(order.getCtx(), shipper.get_ValueAsInt(COLUMNNAME_DefaultVehicleType_ID), order.get_TrxName());
		//	Create Vehicle
		MDDVehicle vehicle = new MDDVehicle(order.getCtx(), 0, order.get_TrxName());
		//	Set values from type
		PO.copyValues(defaultVehicleType, vehicle, true);
		//	Set current values
		vehicle.setAD_Org_ID(order.getAD_Org_ID());
		vehicle.setValue(order.getDocumentNo());
		vehicle.setName(order.getDocumentNo());
		vehicle.setDD_VehicleType_ID(defaultVehicleType.getDD_VehicleType_ID());
		vehicle.setVehicleStatus(MDDVehicle.VEHICLESTATUS_Available);
		vehicle.saveEx();
		//	Add to shipper
		MDDVehicleAssignment assigment = new MDDVehicleAssignment(order.getCtx(), 0, order.get_TrxName());
		assigment.setDD_Vehicle_ID(vehicle.getDD_Vehicle_ID());
		assigment.setM_Shipper_ID(shipper.getM_Shipper_ID());
		assigment.setAD_Org_ID(order.getAD_Org_ID());
		assigment.setSeqNo(10);
		assigment.saveEx();
		return vehicle;
	}
	
	/**
	 * Allows create containers to packages
	 * @param order
	 */
	public static List<MDDTransportUnit> allocateContainersFromOrder(MOrder order) {
		//	Allocate this manually
		if(!order.get_ValueAsBoolean(COLUMNNAME_IsAllocatePackages)) {
			return new ArrayList<MDDTransportUnit>();
		}
		//	Factory
		ContainerFactory factory = ContainerFactory.newInstance()
				.withContext(order.getCtx())
				.withTransactionName(order.get_TrxName());
		//	
		getValidPackagesFromOrder(order).forEach(packageToAdd -> factory.addPackage(packageToAdd));
		//	
		getTransportUnitFromOrder(order).forEach(container -> factory.addContainer(container));
		List<MDDTransportUnit> containers = factory.allocateContainers();
		StringBuffer errors = new StringBuffer();
		containers.forEach(container -> {
			BigDecimal currentContainerWeightCapacity = Optional.ofNullable(container.getMaximumWeight()).orElse(Env.ZERO).subtract(Optional.ofNullable(container.getWeight()).orElse(Env.ZERO));
			BigDecimal currentContainerVolumeCapacity = Optional.ofNullable(container.getMaximumVolume()).orElse(Env.ZERO).subtract(Optional.ofNullable(container.getVolume()).orElse(Env.ZERO));
			if(currentContainerWeightCapacity.compareTo(Env.ZERO) < 0 
					|| currentContainerVolumeCapacity.compareTo(Env.ZERO) < 0) {
				if(errors.length() > 0) {
					errors.append(Env.NL);
				}
				errors.append(Msg.getMsg(order.getCtx(), MESSAGE_ContainerLineValidationError, 
						new Object[]{
								container.getName(),
								Optional.ofNullable(container.getWeight()).orElse(Env.ZERO),
								Optional.ofNullable(container.getVolume()).orElse(Env.ZERO),
								Optional.ofNullable(container.getMaximumWeight()).orElse(Env.ZERO), 
								Optional.ofNullable(container.getMaximumVolume()).orElse(Env.ZERO),
								}));
			}
		});
		if(errors.length() > 0) {
			throw new AdempiereException("@" + MESSAGE_ContainerValidationError + "@ " + errors.toString());
		}
		return containers;
	}
	
	/**
	 * Unlink package from container
	 * @param order
	 */
	public static void unLinkPackagesFromContainer(MOrder order) {
		if(!order.get_ValueAsBoolean(COLUMNNAME_IsAllocatePackages)) {
			return;
		}
		//	unlink
		getPackagesFromOrder(order).forEach(packageToSet -> {
			packageToSet.set_ValueOfColumn(COLUMNNAME_DD_TransportUnit_ID, null);
			packageToSet.saveEx();
		});
	}
	
	/**
	 * Get Packages from Order
	 * @param order
	 * @return
	 */
	public static List<MPackage> getValidPackagesFromOrder(MOrder order) {
		return new Query(order.getCtx(), I_M_Package.Table_Name, "M_Package.DocStatus = 'CO' "
				+ "AND EXISTS(SELECT 1 FROM M_PackageLine pl "
				+ "INNER JOIN C_OrderLine ol ON(ol.C_OrderLine_ID = pl.C_OrderLine_ID) "
				+ "WHERE pl.M_Package_ID = M_Package.M_Package_ID AND ol.C_Order_ID = ?)", order.get_TrxName()).setParameters(order.getC_Order_ID())
				.list();
	}
	
	/**
	 * Get Packages from Order
	 * @param order
	 * @return
	 */
	public static List<MPackage> getPackagesFromOrder(MOrder order) {
		return new Query(order.getCtx(), I_M_Package.Table_Name, "EXISTS(SELECT 1 FROM M_PackageLine pl "
				+ "INNER JOIN C_OrderLine ol ON(ol.C_OrderLine_ID = pl.C_OrderLine_ID) "
				+ "WHERE pl.M_Package_ID = M_Package.M_Package_ID AND ol.C_Order_ID = ?)", order.get_TrxName()).setParameters(order.getC_Order_ID())
				.list();
	}
	
	/**
	 * get Transport unit from Order
	 * @param order
	 * @return
	 */
	public static List<MDDTransportUnit> getTransportUnitFromOrder(MOrder order) {
		if(MTable.getTable_ID(TABLENAME_DD_OrderTransportAssignment) <= 0) {
			return new ArrayList<MDDTransportUnit>();
		}
		return new Query(order.getCtx(), I_DD_TransportUnit.Table_Name, 
				"EXISTS(SELECT 1 FROM DD_OrderTransportAssignment ta WHERE ta.DD_TransportUnit_ID = DD_TransportUnit.DD_TransportUnit_ID AND ta.C_Order_ID = ?)", 
				order.get_TrxName())
				.setParameters(order.getC_Order_ID())
				.list();
	}
	
	/**
	 *	Update Tax & Header
	 *	@return true if header updated
	 */
	public static void updateContainerFromOrder(MOrder order) {
		//	Recalculate Values
		//	Update header values
		getTransportUnitFromOrder(order).forEach(container -> {
			AtomicReference<BigDecimal> weight = new AtomicReference<BigDecimal>(Env.ZERO);
			AtomicReference<BigDecimal> volume = new AtomicReference<BigDecimal>(Env.ZERO);
			getPackagesFromContainerUnit(container).forEach(packageToProcess -> {
				weight.updateAndGet(value -> value.add(packageToProcess.getWeight()));
				volume.updateAndGet(value -> value.add(packageToProcess.getVolume()));
			});
			container.setWeight(weight.get());
			container.setVolume(volume.get());
			container.saveEx();
		});
	}	//	update containers
	
	/**
	 *	Update Tax & Header
	 *	@return true if header updated
	 */
	public static void updateContainerFromPackage(MPackage currentPackage) {
		//	Recalculate Tax for this Tax
		if (currentPackage.get_ValueAsInt(I_DD_TransportUnit.COLUMNNAME_DD_TransportUnit_ID) <= 0) {
			return;
		}
		//	Update header values
		AtomicReference<BigDecimal> weight = new AtomicReference<BigDecimal>(Env.ZERO);
		AtomicReference<BigDecimal> volume = new AtomicReference<BigDecimal>(Env.ZERO);
		//	
		MDDTransportUnit container = new MDDTransportUnit(currentPackage.getCtx(), currentPackage.get_ValueAsInt(I_DD_TransportUnit.COLUMNNAME_DD_TransportUnit_ID), currentPackage.get_TrxName());
		getPackagesFromContainerUnit(container).forEach(packageToProcess -> {
			weight.updateAndGet(value -> value.add(packageToProcess.getWeight()));
			volume.updateAndGet(value -> value.add(packageToProcess.getVolume()));
		});
		container.setWeight(weight.get());
		container.setVolume(volume.get());
		container.saveEx();

	}	//	updateHeaderTax
	
	/**
	 * Get Packages from container
	 * @param container
	 * @return
	 */
	public static List<MPackage> getPackagesFromContainerUnit(MDDTransportUnit container) {
		return new Query(container.getCtx(), I_M_Package.Table_Name, I_DD_TransportUnit.COLUMNNAME_DD_TransportUnit_ID + " = ? AND DocStatus IN('CO', 'CL')", container.get_TrxName())
				.setParameters(container.getDD_TransportUnit_ID())
				.setOnlyActiveRecords(true)
				.list();
	}

	/**
	 * Create Expedient Tax Adjustment
	 * @param ctx
	 * @param trxName
	 * @param docTypeId
	 * @param chargeId
	 * @param dateDoc
	 * @param priceListId
	 * @param expedientId
	 * @param refInvoiceId
	 * @param amount
	 */
	public static MInvoice createExpedientTaxAdjustment(Properties ctx, String trxName, int docTypeId , int chargeId, Timestamp dateDoc, int priceListId, int expedientId, int refInvoiceId ,BigDecimal amount) {
		AtomicReference<MInvoice> invoiceReturn = new AtomicReference<MInvoice>(null);
		MBPartner maybeBPartner = Optional.ofNullable(new Query(ctx, MBPartner.Table_Name, COLUMNNAME_IsUsedByAdjustExpedient.concat("='Y'"), trxName)
													.setOnlyActiveRecords(true)
													.<MBPartner>first())
									 .orElseGet(() ->{
										throw new AdempiereException("@NotFound@ @C_BPartner_ID@ -> @SP009_IsUsedByAdjustExpedient@");
									 });
		Optional.ofNullable(maybeBPartner)
				.ifPresent(bpartner ->{
					//Create Adjust Document
					MInvoice adjustmentDocument = new MInvoice(ctx, 0, trxName);
					adjustmentDocument.setBPartner(bpartner);
					adjustmentDocument.setDateInvoiced(dateDoc);
					adjustmentDocument.setDateAcct(dateDoc);
					adjustmentDocument.setC_DocTypeTarget_ID(docTypeId);
					adjustmentDocument.setC_DocType_ID(docTypeId);
					adjustmentDocument.setM_PriceList_ID(priceListId);
					adjustmentDocument.setIsSOTrx(MDocType.get(ctx, docTypeId).isSOTrx());
					adjustmentDocument.set_ValueOfColumn(MSP009Expedient.COLUMNNAME_SP009_Expedient_ID, expedientId);
					adjustmentDocument.set_ValueOfColumn(COLUMNNAME_SP009_IsAffectsExpedientTax, true);
					if (refInvoiceId > 0)
						adjustmentDocument.set_ValueOfColumn(COLUMNNAME_SP009_RefInvoice_ID, refInvoiceId);
					
					adjustmentDocument.saveEx();
					MInvoiceLine adjustmentDocumentLine = new MInvoiceLine(adjustmentDocument);
					adjustmentDocumentLine.setC_Charge_ID(chargeId);
					adjustmentDocumentLine.setQty(Env.ONE);
					adjustmentDocumentLine.setPrice(amount);
					adjustmentDocumentLine.saveEx();
					
					adjustmentDocument.processIt(MInvoice.DOCACTION_Complete);
					
					MDocType maybeAllocationDocType = Optional.ofNullable(new Query(ctx, MDocType.Table_Name, COLUMNNAME_IsUsedByAdjustExpedient.concat("='Y' AND ").concat(MDocType.COLUMNNAME_DocBaseType).concat("=?"), trxName)
																	.setParameters(MDocType.DOCBASETYPE_PaymentAllocation)
																	.setOnlyActiveRecords(true)
																	.<MDocType>first())
															  .orElse(MDocType.get(ctx, MDocType.getDocType(MDocType.DOCBASETYPE_PaymentAllocation)));
					
					Optional.ofNullable(maybeAllocationDocType)
							.ifPresent(allocationDocType -> {
								//Create Allocation
								MAllocationHdr allocation = new MAllocationHdr(ctx, true, adjustmentDocument.getDateAcct(), adjustmentDocument.getC_Currency_ID(), adjustmentDocument.getDescription(), adjustmentDocument.get_TrxName());
								
								allocation.setC_DocType_ID(allocationDocType.get_ID());	
								allocation.saveEx();
								MAllocationLine allocationLine = new MAllocationLine(allocation, adjustmentDocument.getGrandTotal(true).negate(), Env.ZERO, Env.ZERO, Env.ZERO);
								allocationLine.setDocInfo(adjustmentDocument.getC_BPartner_ID(), adjustmentDocument.getC_Order_ID(), adjustmentDocument.get_ID());
								allocationLine.saveEx();
								
								MAllocationLine allocationLineCharge = new MAllocationLine(allocation, adjustmentDocument.getGrandTotal(true).negate(), Env.ZERO, Env.ZERO, Env.ZERO);
								allocationLineCharge.setC_BPartner_ID(adjustmentDocument.getC_BPartner_ID());
								allocationLineCharge.setC_Charge_ID(chargeId);
								allocationLineCharge.saveEx();
								
								allocation.processIt(MAllocationHdr.DOCACTION_Complete);
								allocation.saveEx();
							});
					
					invoiceReturn.set(adjustmentDocument);
					
				});
		return invoiceReturn.get();
	}
}
