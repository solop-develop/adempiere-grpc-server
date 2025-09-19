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
package com.solop.sp013.core.util;

/**
 * Add here all changes for core and static methods
 * Please rename this class and package
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public interface ElectronicInvoicingChanges {
	/**	Transaction Type Table	*/
	String SP013_TransactionType = "SP013_TransactionType";
	/**	Allocate Table	*/
	String C_AllocateInvoice = "C_AllocateInvoice";
	/**	Transaction Type ID	*/
	String SP013_TransactionType_ID = "SP013_TransactionType_ID";
	/**	Tax Type	*/
	String SP013_TaxType = "SP013_TaxType";
	/**	Fiscal Document Type	*/
	String SP013_FiscalDocumentType = "SP013_FiscalDocumentType";
	/**	Fiscal Document Type Invoice	*/
	String SP013_FiscalDocumentType_Invoice = "01";
	/**	Fiscal Document Type Debit Note	*/
	String SP013_FiscalDocumentType_Debit_Note = "02";
	/**	Fiscal Document Type Credit Note	*/
	String SP013_FiscalDocumentType_Credit_Note = "03";
	/**	Fiscal Document Type Withholding	*/
	String SP013_FiscalDocumentType_Withholding = "04";
	/**	Fiscal Document Type Delivery Note	*/
	String SP013_FiscalDocumentType_DeliveryNote = "05";
	/**	application Type	*/
	String ElectronicInvoicing_ApplicationType = "EIS";
	/**	Fiscal Printer Reference	*/
	String SP013_FiscalSender_ID = "SP013_FiscalSender_ID";
	/**	Invoice to Allocate	*/
	String ReferenceDocument_ID = "ReferenceDocument_ID";
	/**	Fiscal Print Date	*/
	String SP013_FiscalDate = "SP013_FiscalDate";
	/**	Authorization Type	*/
	String SP013_AuthorizationType = "SP013_AuthorizationType";
	/**	Authorization Type Void	*/
	String SP013_AuthorizationType_Void = "VO";
	/**	Authorization Type Send	*/
	String SP013_AuthorizationType_Send = "SE";
	/**	Authorize Invoice	*/
	String SP013_AuthorizeInvoice = "SP013_AuthorizeInvoice";

	/**	Printed on Fiscal Print	*/
	String SP013_IsElectronicDocument = "SP013_IsElectronicDocument";
	/**	Allows Reverse Document	*/
	String SP013_IsAllowsReverse = "SP013_IsAllowsReverse";
	/**	Send Ok	*/
	String SP013_IsSent = "SP013_IsSent";
	/**	Download URL	*/
	String SP013_DownloadURL = "SP013_DownloadURL";
	/**	Fiscal Comment	*/
	String SP013_FiscalComment = "SP013_FiscalComment";
	/**	Save Files After Send	*/
	String SP013_SaveFilesAfterSend = "SP013_SaveFilesAfterSend";
	/**	Print Invoice Receipt	*/
	String SP013_PrintInvoiceReceipt = "SP013_PrintInvoiceReceipt";
	/**	Send Invoice	*/
	String SP013_SendInvoice = "SP013_SendInvoice";
	/**	Find Locally First	*/
	String SP013_FindLocallyFirst = "SP013_FindLocallyFirst";
	/**	Is Printed after complete	*/
	String SP013_IsSendAfterComplete = "SP013_IsSendAfterComplete";
	/**	Printer Custom Local Port	*/
	String SP013_ReadResponseAfterSend = "SP013_ReadResponseAfterSend";
	/**	Each Unit Count Separately	*/
	String SP013_EachUnitCount = "SP013_EachUnitCount";
	/**	Show Sales Representative	*/
	String SP013_ShowSalesRep = "SP013_ShowSalesRep";
	/**	Show Warehouse	*/
	String SP013_ShowWarehouse = "SP013_ShowWarehouse";
	/**	Show Terminal (POS)	*/
	String SP013_ShowTerminal = "SP013_ShowTerminal";
	/**	Show Order Reference	*/
	String SP013_ShowSOReference = "SP013_ShowSOReference";
	/**	Show Document Note	*/
	String SP013_ShowDocumentNote = "SP013_ShowDocumentNote";
	/**	Show Item Quantity	*/
	String SP013_ShowItemQuantity = "SP013_ShowItemQuantity";
	/**	Show Item Quantity	*/
	String SP013_OrgFiscalCode = "SP013_OrgFiscalCode";
	/**	Show Item Quantity	*/
	String SP013_FiscalCurrency_ID = "SP013_FiscalCurrency_ID";
	/**	Tax Type	*/
	String SP013_TaxType_ID = "SP013_TaxType_ID";
	/**	Invoice Document Type	*/
	String SP013_InvoiceDocType_ID = "SP013_InvoiceDocType_ID";
	/**	Debit Memo Document Type	*/
	String SP013_DebitDocType_ID = "SP013_DebitDocType_ID";
	/**	Credit Memo Document Type	*/
	String SP013_CreditDocType_ID = "SP013_CreditDocType_ID";
	/**	Fiscal Document No	*/
	String SP013_FiscalDocumentNo = "SP013_FiscalDocumentNo";
	/**	Document Format */
	String SP013_DocumentFormat = "SP013_DocumentFormat";
	/** Uses Electronic Invoicing*/
	String SP013_IsElectronicInvoicing = "SP013_IsElectronicInvoicing";
}
