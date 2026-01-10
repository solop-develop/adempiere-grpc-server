package org.solop.queue;

import org.adempiere.engine.CostEngineFactory;
import org.compiere.model.MInOutLine;
import org.compiere.model.MMatchInv;
import org.compiere.model.MMatchPO;
import org.compiere.model.MProduct;
import org.compiere.model.MTransaction;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.spin.queue.model.MADQueue;
import org.spin.queue.util.QueueManager;


/**
 * Material Cost Processor (PMC)
 * @author Gabriel Escalona
 */
public class MaterialCostProcessor extends QueueManager {
    public static String QueueType_MaterialCost = "PMC";
    /**	Logger						*/
    protected CLogger log = CLogger.getCLogger(getClass());
    @Override
    public void add(int queueId) {
        log.fine("Material Cost Queue Added: " + queueId);
    }

    @Override
    public void process(int queueId) {
        MADQueue queue = new MADQueue(getContext(), queueId, getTransactionName());
        //  Ignore without record
        if(queue.getAD_Table_ID() <= 0 || queue.getRecord_ID() <= 0) {
            return;
        }
        if (queue.getAD_Table_ID() == MTransaction.Table_ID) {
            MTransaction transaction = new MTransaction(getContext(), queue.getRecord_ID(), getTransactionName());
            CostEngineFactory.getCostEngine(Env.getAD_Client_ID(getContext())).createCostDetail(transaction , transaction.getDocumentLine());

        } else if (queue.getAD_Table_ID() == MMatchInv.Table_ID){
            MMatchInv matchInv = new MMatchInv(getContext(), queue.getRecord_ID(), getTransactionName());
            MInOutLine inOutLine = (MInOutLine) matchInv.getM_InOutLine();
            for (MTransaction trx : MTransaction.getByInOutLine(inOutLine)) {
                CostEngineFactory.getCostEngine(Env.getAD_Client_ID(getContext())).createCostDetail(trx, matchInv);
            }

        } else if (queue.getAD_Table_ID() == MMatchPO.Table_ID){
            MMatchPO matchPO = new MMatchPO(getContext(), queue.getRecord_ID(), getTransactionName());
            MInOutLine inOutLine = (MInOutLine) matchPO.getM_InOutLine();
            for (MTransaction trx : MTransaction.getByInOutLine(inOutLine)) {
                if (!inOutLine.getM_Product().getProductType().equals(MProduct.PRODUCTTYPE_Item) || trx == null){
                    continue;
                }
                CostEngineFactory.getCostEngine(Env.getAD_Client_ID(getContext())).createCostDetail(trx, matchPO);
            }
        }


        //	Validate if it document is allowed for print on fiscal print
        /*MDocType documentType = MDocType.get(getContext(), invoice.getC_DocType_ID());
        if(!documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)) {
            queue.setDescription(Msg.parseTranslation(getContext(), "@SP013.DocumentInvalidForED@")); //TODO: Validate
            queue.saveEx();
            return;
        }*/

    }
}
