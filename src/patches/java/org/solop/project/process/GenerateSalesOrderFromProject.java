/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package org.solop.project.process;

import org.adempiere.core.domains.models.I_C_Project;
import org.adempiere.core.domains.models.I_C_ProjectLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProject;
import org.compiere.model.MProjectLine;
import org.compiere.model.MUOMConversion;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 	Generated Process for (Generate Sales Order (From Project))
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *  @version Release 3.9.4
 */
public class GenerateSalesOrderFromProject extends GenerateSalesOrderFromProjectAbstract {

	private final List<String> generated = new ArrayList<>();
	protected void prepare() {
		super.prepare();
		if(!isSelection()) {
			if(getRecord_ID() == 0 || !I_C_Project.Table_Name.equals(getTableName())) {
				throw new AdempiereException("@C_Project_ID@ @NotFound@");
			}
		}
	}

	@Override
	protected String doIt() throws Exception {
		getProjectIds().forEach(projectId -> {
			try {
				Trx.run(transactionName -> {
					generateOrderFromProject(projectId, transactionName);
				});
			} catch (Exception e) {
				addLog(e.getLocalizedMessage());
			}
		});
		return "@Created@ " + generated.toString();
	}

	private void generateOrderFromProject(int projectId, String transactionName) {
		MProject project = getProject (getCtx(), projectId, transactionName);
		if (project.getC_PaymentTerm_ID() <= 0) {
			throw new AdempiereException(project.getName() + " @C_PaymentTerm_ID@ @NotFound@");
		}
		List<Integer> projectLineIds = getProjectLineIds(projectId, transactionName);
		if(projectLineIds == null || projectLineIds.isEmpty()) {
			throw new AdempiereException(project.getName() + " @C_ProjectLine_ID@ @NotFound@");
		}
		MOrder order = new MOrder (project, true, MDocType.COLUMNNAME_DocSubTypeSO);
		//	Add Document Type Target
		if(getDocTypeTargetId() > 0) {
			order.setC_DocTypeTarget_ID(getDocTypeTargetId());
		}
		order.setC_Project_ID(projectId);
		order.setDescription(order.getDescription() + " - " + project.getName());
		order.setDateOrdered(getDateOrdered());
		order.saveEx(transactionName);
		//	Add Lines
		AtomicInteger count = new AtomicInteger(0);
		projectLineIds.forEach(projectLineId -> {
			MProjectLine projectLine = new MProjectLine(getCtx(), projectLineId, transactionName);
			MOrderLine orderLine = new MOrderLine(order);
			orderLine.setLine(projectLine.getLine());
			orderLine.setDescription(projectLine.getDescription());
			//
			orderLine.setM_Product_ID(projectLine.getM_Product_ID(), true);
			MProduct product = new MProduct(getCtx(),projectLine.getM_Product_ID(),transactionName);
			BigDecimal toOrder = projectLine.getPlannedQty().subtract(projectLine.getInvoicedQty());
			setQuantityToOrder(orderLine, product, projectLine.get_ValueAsInt("C_UOM_ID"), projectLine.getPlannedQty(), toOrder);
			orderLine.setPrice();
			if (projectLine.getPlannedPrice() != null && projectLine.getPlannedPrice().compareTo(Env.ZERO) != 0){
				orderLine.setPrice(projectLine.getPlannedPrice());
				orderLine.setPriceList(projectLine.getPlannedPrice());
			}
			orderLine.setDiscount();
			orderLine.setCost(projectLine.getCost());
			orderLine.setTax();
			orderLine.setC_Project_ID(projectId);
			orderLine.setC_ProjectPhase_ID(projectLine.getC_ProjectPhase_ID());
			if(projectLine.getDatePromised() != null) {
				orderLine.setDatePromised(projectLine.getDatePromised());
			}
			orderLine.set_ValueOfColumn(I_C_ProjectLine.COLUMNNAME_StartDate, projectLine.getStartDate());
			orderLine.set_ValueOfColumn(I_C_ProjectLine.COLUMNNAME_EndDate, projectLine.getEndDate());
			orderLine.set_ValueOfColumn(I_C_ProjectLine.COLUMNNAME_C_ProjectLine_ID, projectLine.getC_ProjectLine_ID());
			orderLine.saveEx(transactionName);
			count.getAndUpdate(no -> no + 1);
		});
		generated.add(project.getValue() + " - " + project.getName() + ": " + order.getDocumentNo() + "(" + count.get() + ")");
	}

	/**
	 * Set line Quantity
	 */
	private void setQuantityToOrder(MOrderLine orderLine, MProduct product, int uomToId, BigDecimal quantityEntered, BigDecimal quantityOrdered) {
		int uomId = product.getC_UOM_ID();
		if(uomToId > 0
				&& quantityEntered != null
				&& quantityEntered != Env.ZERO) {
			uomId = uomToId;
			if(uomId != product.getC_UOM_ID()) {
				BigDecimal convertedQuantity = MUOMConversion.convertProductFrom (getCtx(), product.getM_Product_ID(), uomToId, quantityEntered);
				if (convertedQuantity == null) {
					quantityEntered = quantityOrdered;
				} else {
					quantityOrdered = convertedQuantity;
				}
				orderLine.setQty(quantityEntered);
				orderLine.setQtyOrdered(quantityOrdered);
			} else {
				orderLine.setQty(quantityOrdered);
			}
		} else {
			orderLine.setQty(quantityOrdered);
		}
		orderLine.setC_UOM_ID(uomId);
	}

	private List<Integer> getProjectLineIds(int projectId, String transactionName) {
		return new Query(getCtx(), I_C_ProjectLine.Table_Name, "C_Project_ID = ? AND IsSummary = 'N'", transactionName)
				.setParameters(projectId).setOrderBy(I_C_ProjectLine.COLUMNNAME_Line)
				.getIDsAsList();
	}

	private List<Integer> getProjectIds() {
		if(isSelection()) {
			return Optional.ofNullable(getSelectionKeys()).orElse(List.of());
		}
		return List.of(getRecord_ID());
	}

	private MProject getProject (Properties ctx, int projectId, String transactionName) {
		MProject fromProject = new MProject (ctx, projectId, transactionName);
		if (fromProject.getC_Project_ID() == 0)
			throw new AdempiereException(fromProject.getName() + " @C_Project_ID@ @NotFound@" + projectId);
		if (fromProject.getM_PriceList_Version_ID() == 0)
			throw new AdempiereException(fromProject.getName() + " @M_PriceList_ID@ @NotFound @@To@ @C_Project_ID@");
		if (fromProject.getM_Warehouse_ID() == 0)
			throw new AdempiereException(fromProject.getName() + " @M_Warehouse_ID@ @NotFound@ @To@ @C_Project_ID@");
		if (fromProject.getC_BPartner_ID() == 0)
			throw new AdempiereException(fromProject.getName() + " @C_BPartner_ID@ @NotFound@");
		if (fromProject.getC_BPartner_Location_ID() == 0)
			throw new AdempiereException(fromProject.getName() + " @C_BPartner_Location_ID@ @NotFound@");
		return fromProject;
	}
}