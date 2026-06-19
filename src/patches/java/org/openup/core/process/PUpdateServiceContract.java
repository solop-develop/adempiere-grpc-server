/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.openup.core.process;


import com.eevolution.model.MServiceType;
import org.adempiere.core.domains.models.I_AD_Memo;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_Substitute;
import org.adempiere.core.domains.models.I_S_ContractLine;
import org.adempiere.core.domains.models.X_S_ContractLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MMemo;
import org.compiere.model.MProduct;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.context.service.infrastructure.domain.entities.MSContract;
import org.eevolution.context.service.infrastructure.domain.entities.MSContractLine;
import org.openup.core.model.MSContractReason;

import java.util.List;

/**
 * Generated Process for (Actualización de Contratos)
 *
 * @author ADempiere (generated)
 * @version Release 3.9.3
 */
public class PUpdateServiceContract extends PUpdateServiceContractAbstract {
    @Override
    protected void prepare() {
        super.prepare();
    }

    @Override
    protected String doIt() throws Exception {
        List<Integer> recordIds = getSelectionKeys();

        recordIds.stream().forEach(key -> {
            MSContract contract = new MSContract(getCtx(), key, get_TrxName());//instancio contrato actual

            if (getAction().equalsIgnoreCase("CLN")) {
                closeContractAndCreate(contract);
            } else closeContract(contract, 0);

        });

        return "OK";
    }

    private void closeContract(MSContract contract, int reasonID) {

        String message = "";

        if (reasonID == 0)
            reasonID = getContractReasonId();

        contract.set_ValueOfColumn("S_ContractReason_ID", reasonID);
        contract.saveEx();

        //cierro contrato actual, si esta en estado completo
        if (contract.getDocStatus().equals(MSContract.DOCSTATUS_Completed)) {
            if (!contract.processIt(DocumentEngine.ACTION_Close)) {
                message = contract.getProcessMsg();
                throw new AdempiereException(message);
            }

            contract.saveEx();
        }
    }

    private void closeContractAndCreate(MSContract contract) {

        String message = "";

        //cierro contrato actual, si esta en estado completo
        if (contract.getDocStatus().equals(MSContract.DOCSTATUS_Completed)) {
            if (!contract.processIt(DocumentEngine.ACTION_Close)) {
                message = contract.getProcessMsg();
                throw new AdempiereException(message);
            }

            contract.saveEx();
        }

        //creo nuevo contrato
        //antes se verifica si el producto tiene sustituto, en caso contrario no se crea nuevo contrato
        String sql = "select s.substitute_id\n" +
                "from S_ContractLine scl\n" +
                "join m_product p on scl.m_product_id = p.m_product_id\n" +
                "join m_product_category c on p.m_product_category_id = c.m_product_category_id\n" +
                "join m_substitute s on p.m_product_id = s.m_product_id\n" +
                "where c.isdefault = 'Y'\n" +
                "and scl.s_contract_id = " + contract.get_ID();
        int substituteID = DB.getSQLValueEx(get_TrxName(), sql);

        if (substituteID > 0) {//hay producto sustituto

            MSContract newContract = new MSContract(getCtx(), 0, get_TrxName());
            newContract.setAD_Org_ID(contract.getAD_Org_ID());
            newContract.setC_DocType_ID(contract.getC_DocType_ID());
            newContract.setC_BPartner_ID(contract.getC_BPartner_ID());
            newContract.setC_BPartner_Location_ID(contract.getC_BPartner_Location_ID());
            newContract.setBill_BPartner_ID(contract.getBill_BPartner_ID());
            newContract.setBill_Location_ID(contract.getBill_Location_ID());
            newContract.set_ValueOfColumn("UY_Family_ID", contract.get_Value("UY_Family_ID"));
            newContract.setDateDoc(getDateDoc());
            newContract.setDateAcct(getDateAcct());
            newContract.setDateStart(getDateStart());
            newContract.setDateFinishSchedule(getDateFinishSchedule());
            newContract.setDuration(contract.getDuration());
            newContract.setDurationUnit(contract.getDurationUnit());
            newContract.setInvoiceRule(contract.getInvoiceRule());
            newContract.setM_PriceList_ID(contract.getM_PriceList_ID());
            newContract.setC_Currency_ID(contract.getC_Currency_ID());
            newContract.setSalesRep_ID(contract.getSalesRep_ID());
            newContract.setPaymentRule(contract.getPaymentRule());
            newContract.setC_PaymentTerm_ID(contract.getC_PaymentTerm_ID());
            newContract.setM_Warehouse_ID(contract.getM_Warehouse_ID());
            newContract.setIsCreditApproved(contract.isCreditApproved());
            newContract.setPP_Calendar_ID(contract.getPP_Calendar_ID());
            newContract.setPosted(true);
            newContract.setIsInvoiced(false);
            newContract.setIsSOTrx(contract.isSOTrx());
            newContract.setTotalLines(Env.ZERO);
            newContract.setGrandTotal(Env.ZERO);
            newContract.setDocStatus(MSContract.STATUS_Drafted);
            newContract.setDocAction(MSContract.ACTION_Complete);
            newContract.saveEx();

            List<X_S_ContractLine> mSContractLines = new Query(getCtx(), I_S_ContractLine.Table_Name, I_S_ContractLine.COLUMNNAME_S_Contract_ID + "=?", get_TrxName())
                    .setParameters(contract.get_ID())
                    .list();

            MProduct newProd = null;

            //obtengo y recorro lineas del contrato
            for (X_S_ContractLine mSContractLine : mSContractLines) {

                MSContractLine line = new MSContractLine(getCtx(), 0, get_TrxName());
                MProduct prod = (MProduct) mSContractLine.getM_Product();

                //si en el proceso se ha elegido un tipo de servicio entonces se utiliza ese, de lo contrario su sustituto
                if (getServiceTypeId() > 0) {

                    //obtengo producto para el tipo de servicio
                    newProd = new Query(getCtx(), I_M_Product.Table_Name, "S_ServiceType_ID" + "=?", get_TrxName())
                            .setParameters(getServiceTypeId())
                            .first();

                    if (newProd != null && newProd.get_ID() > 0) {
                        line.setM_Product_ID(newProd.get_ID());
                    } else
                        throw new AdempiereException("ERROR: No se obtuvo producto para el tipo de servicio seleccionado");

                } else {

                    //obtengo producto sustituto
                    PO substitute = new Query(getCtx(), I_M_Substitute.Table_Name, I_M_Product.COLUMNNAME_M_Product_ID + "=?", get_TrxName())
                            .setParameters(prod.get_ID())
                            .first();

                    if (substitute != null && substitute.get_ID() > 0 && substitute.get_ValueAsInt("M_Product_ID") > 0) {
                        if(substitute.get_ValueAsInt("Substitute_ID") > 0) {
                            newProd = MProduct.get(substitute.getCtx(), substitute.get_ValueAsInt("Substitute_ID"));
                            line.setM_Product_ID(newProd.get_ID());
                        }
                    } else
                        throw new AdempiereException("ERROR: No se obtuvo sustituto para el producto '" + prod.getName() + "'");
                }

                //obtengo la actividad desde MEMO en el producto nuevo
                MMemo memo = new Query(getCtx(), I_AD_Memo.Table_Name, I_M_Product.COLUMNNAME_M_Product_ID + "=?", get_TrxName())
                        .setParameters(newProd.get_ID())
                        .first();

                if (memo != null && memo.get_ID() > 0 && memo.getC_Activity_ID() > 0) {
                    line.setC_Activity_ID(memo.getC_Activity_ID());
                    newContract.setC_Activity_ID(memo.getC_Activity_ID());
                    newContract.saveEx();
                } else
                    throw new AdempiereException("ERROR: No se obtuvo Actividad para el producto '" + newProd.getName() + "'");

                if (newProd.get_ValueAsInt("S_ServiceType_ID") > 0) {
                    MServiceType serviceType = new MServiceType(getCtx(), newProd.get_ValueAsInt("S_ServiceType_ID"), get_TrxName());
                    line.setS_ServiceType_ID(newProd.get_ValueAsInt("S_ServiceType_ID"));

                    if (serviceType.get_ValueAsBoolean("IsRecurrent")) {
                        line.setIsRecurrent(true);
                        line.setFrequencyType(serviceType.get_ValueAsString("FrequencyType"));
                        line.setFrequency(serviceType.get_ValueAsInt("Frequency"));
                    }

                } else
                    throw new AdempiereException("ERROR: El producto '" + newProd.getName() + "' no tiene Tipo de Servicio definido");

                line.setS_Contract_ID(newContract.get_ID());
                line.setC_BPartner_ID(mSContractLine.getC_BPartner_ID());
                line.setC_BPartner_Location_ID(mSContractLine.getC_BPartner_Location_ID());
                line.setC_Currency_ID(mSContractLine.getC_Currency_ID());
                line.setQtyEntered(mSContractLine.getQtyEntered());
                line.setQtyOrdered(mSContractLine.getQtyOrdered());
                line.setQtyDelivered(Env.ZERO);
                line.setC_UOM_ID(mSContractLine.getC_UOM_ID());
                line.setC_Tax_ID(mSContractLine.getC_Tax_ID());
                line.setDiscount(mSContractLine.getDiscount());
                line.setPriceActual(Env.ZERO);
                line.setPriceEntered(Env.ZERO);
                line.setPriceList(Env.ZERO);
                line.setLineNetAmt(Env.ZERO);
                line.setFreightAmt(Env.ZERO);
                line.saveEx();

            }

            //si se indica completo el contrato nuevo
            if (getDocAction() != null) {
                if (!newContract.processIt(getDocAction())) {
                    message = newContract.getProcessMsg();
                    throw new AdempiereException(message);
                }
                newContract.saveEx();
            }


        } else {//no hay producto sustituto

            MSContractReason reasonFinal = MSContractReason.getFinalReason(getCtx(), getAD_Client_ID(), get_TrxName());

            if (reasonFinal == null)
                throw new AdempiereException("ERROR: No se obtuvo motivo de cancelación final de contrato.");

            contract.set_ValueOfColumn("S_ContractReason_ID", reasonFinal.get_ID());
            contract.saveEx();

        }
    }
}