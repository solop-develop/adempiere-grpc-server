package com.solop.sp012.model.validator;

import com.solop.sp012.util.ShoppingMetadata;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrgInfo;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.spin.queue.util.QueueLoader;

/**
 * Write here your change comment
 * Please rename this class and package
 * @author Yamel Senih ysenih@erpya.com
 *
 */
public class ShoppingManagement implements ModelValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(ShoppingManagement.class);
	/** Client */
	private int clientId = -1;
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		//	Add Persistence for IsDefault values
		engine.addDocValidate(I_C_Invoice.Table_Name, this);
		engine.addModelChange(I_C_Invoice.Table_Name, this);
	}
	
	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}
	
	@Override
	public String modelChange(PO entity, int type) throws Exception {
		if (!MOrgInfo.get(entity.getCtx(), entity.getAD_Org_ID(), entity.get_TrxName()).get_ValueAsBoolean(ShoppingMetadata.SP012_IsShopping)) {
			return null;
		}
		if(type == TYPE_BEFORE_NEW
				|| type == TYPE_BEFORE_CHANGE) {
			log.fine(" TYPE_BEFORE_NEW || TYPE_BEFORE_CHANGE");
			if (entity.get_TableName().equals(MInvoice.Table_Name)) {
				MInvoice invoice = (MInvoice) entity;
				if(invoice.is_ValueChanged(I_C_Invoice.COLUMNNAME_C_DocTypeTarget_ID)
						&& invoice.isSOTrx()) {
					MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
					invoice.set_ValueOfColumn(ShoppingMetadata.SP012_IsShoppingExport, documentType.get_ValueAsBoolean(ShoppingMetadata.SP012_IsShoppingExport));
				}
			}
		}
		return null;
	}

	@Override
	public String docValidate(PO entity, int timing) {
		if (!MOrgInfo.get(entity.getCtx(), entity.getAD_Org_ID(), entity.get_TrxName()).get_ValueAsBoolean(ShoppingMetadata.SP012_IsShopping)) {
			return null;
		}
		log.fine("TIMING_BEFORE_COMPLETE");
		if(timing == TIMING_BEFORE_COMPLETE) {
			if(entity.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice invoice = (MInvoice) entity;
				MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
				if(documentType.get_ValueAsBoolean(ShoppingMetadata.SP012_IsShoppingExport) && !invoice.isReversal() && invoice.isSOTrx()) {
					QueueLoader.getInstance().getQueueManager(ShoppingMetadata.QueueType_Shopping_S12)
							.withContext(invoice.getCtx())
							.withTransactionName(invoice.get_TrxName())
							.withEntity(invoice)
							.addToQueue();
				}
			}
		}
		return null;
	}
}
