/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_C_BankAccount;
import org.adempiere.core.domains.models.I_C_BankAccountDoc;
import org.adempiere.core.domains.models.I_C_PaySelection;
import org.adempiere.core.domains.models.I_C_PaySelectionCheck;
import org.adempiere.core.domains.models.X_C_PaySelectionLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCurrency;
import org.compiere.model.MPaySelection;
import org.compiere.model.MRefList;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.BusinessPartner;
import org.spin.backend.grpc.common.Currency;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.payment_print_export.BankAccount;
import org.spin.backend.grpc.payment_print_export.BankAccountType;
import org.spin.backend.grpc.payment_print_export.ConfirmPrintRequest;
import org.spin.backend.grpc.payment_print_export.CreateEFTPaymentRequest;
import org.spin.backend.grpc.payment_print_export.ExportPaymentsRequest;
import org.spin.backend.grpc.payment_print_export.GetDocumentNoRequest;
import org.spin.backend.grpc.payment_print_export.GetDocumentNoResponse;
import org.spin.backend.grpc.payment_print_export.GetPaymentSelectionRequest;
import org.spin.backend.grpc.payment_print_export.ListPaymentRulesRequest;
import org.spin.backend.grpc.payment_print_export.ListPaymentSelectionRequest;
import org.spin.backend.grpc.payment_print_export.PaymentPrintExportGrpc.PaymentPrintExportImplBase;
import org.spin.backend.grpc.payment_print_export.PaymentRule;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.backend.grpc.payment_print_export.PaymentSelection;
import org.spin.backend.grpc.payment_print_export.PrintPaymentsRequest;
import org.spin.backend.grpc.payment_print_export.PrintRemittanceRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class PaymentPrintExportServiceImplementation extends PaymentPrintExportImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(PaymentPrintExportServiceImplementation.class);
	
	@Override
	public void getPaymentSelection(GetPaymentSelectionRequest request, StreamObserver<PaymentSelection> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			PaymentSelection.Builder builder = getPaymentSelection(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private PaymentSelection.Builder getPaymentSelection(GetPaymentSelectionRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());
		// validate key values
		if(request.getId() == 0 && Util.isEmpty(request.getUuid())) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}

		int paymentSelectionId = request.getId();
		if (paymentSelectionId <= 0) {
			if (!Util.isEmpty(request.getUuid(), true)) {
				paymentSelectionId = RecordUtil.getIdFromUuid(I_C_PaySelection.Table_Name, request.getUuid(), null);
			}
			if (paymentSelectionId <= 0) {
				throw new AdempiereException("@FillMandatory@ @C_PaySelection_ID@");
			}
		}

		MPaySelection paymentSelection = new Query(
			context,
			I_C_PaySelection.Table_Name,
			" C_PaySelection_ID = ? ",
			null
		)
			.setParameters(paymentSelectionId)
			.first();
		if (paymentSelection == null || paymentSelection.getC_PaySelection_ID() <= 0) {
			throw new AdempiereException("@C_PaySelection_ID@ @NotFound@");
		}

		return convertPaymentSelection(paymentSelection);
	}

	private PaymentSelection.Builder convertPaymentSelection(MPaySelection paymentSelection) {
		PaymentSelection.Builder builder = PaymentSelection.newBuilder();
		if (paymentSelection == null || paymentSelection.getC_PaySelection_ID() <= 0) {
			return builder;
		}
		builder.setId(paymentSelection.getC_PaySelection_ID())
			.setUuid(ValueUtil.validateNull(paymentSelection.getUUID()))
			.setDocumentNo(ValueUtil.validateNull(paymentSelection.getDocumentNo()))
			.setBankAccount(
				convertBankAccount(paymentSelection.getC_BankAccount_ID())
			)
		;
		Integer paymentCount  = new Query(Env.getCtx(), I_C_PaySelectionCheck.Table_Name, I_C_PaySelectionCheck.COLUMNNAME_C_PaySelection_ID + "=?" , null)
				.setClient_ID()
				.setParameters(paymentSelection.getC_PaySelection_ID())
				.count();
		builder.setNumberPayments(paymentCount);

		final String whereClause = "EXISTS ("
			+ "	SELECT 1 FROM C_PaySelectionLine "
			+ "	WHERE "
			+ "	AD_Ref_List.Value = C_PaySelectionLine.PaymentRule"
			+ "	AND C_PaySelectionLine.C_PaySelection_ID = ?"
			+ "	AND AD_Ref_List.AD_Reference_ID = ?"
			+ ")"
		;
		List<MRefList> paymemntRulesList = new Query(
			Env.getCtx(),
			I_AD_Ref_List.Table_Name,
			whereClause,
			null
		).setParameters(paymentSelection.getC_PaySelection_ID(), X_C_PaySelectionLine.PAYMENTRULE_AD_Reference_ID)
		.list(MRefList.class);

		paymemntRulesList.stream().forEach(paymentRule -> {
			PaymentRule.Builder builderPaymentRule = convertPaymentRule(paymentRule);
			builder.addPaymentRules(builderPaymentRule);
		});

		return builder;
	}

	private BusinessPartner.Builder convertBusinessPartner(int businessPartnerId) {
		if (businessPartnerId > 0) {
			MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
			return ConvertUtil.convertBusinessPartner(businessPartner);
		}
		return BusinessPartner.newBuilder();
	}
	
//	private PaymentRule.Builder convertPaymentRule(String value) {
//		if (Util.isEmpty(value, true)) {
//			return PaymentRule.newBuilder();
//		}
//		MRefList reference = MRefList.get(Env.getCtx(), X_C_PaySelectionLine.PAYMENTRULE_AD_Reference_ID, value, null);
//
//		return convertPaymentRule(reference);
//	}

	private PaymentRule.Builder convertPaymentRule(MRefList paymentRule) {
		PaymentRule.Builder builder = PaymentRule.newBuilder();
		if (paymentRule == null || paymentRule.getAD_Ref_List_ID() <= 0 || paymentRule.getAD_Reference_ID() != X_C_PaySelectionLine.PAYMENTRULE_AD_Reference_ID) {
			return builder;
		}
		builder.setId(paymentRule.getAD_Reference_ID())
			.setUuid(ValueUtil.validateNull(paymentRule.getUUID()))
			.setValue(ValueUtil.validateNull(paymentRule.getValue()))
			.setName(ValueUtil.validateNull(paymentRule.getName()))
			.setDescription(ValueUtil.validateNull(paymentRule.getDescription()))
		;
		return builder;
	}

	private BankAccountType.Builder convertBankAccountType(String value) {
		if (Util.isEmpty(value, true)) {
			return BankAccountType.newBuilder();
		}
		MRefList reference = MRefList.get(Env.getCtx(), MBankAccount.BANKACCOUNTTYPE_AD_Reference_ID, value, null);
		
		return convertBankAccountType(reference);
	}
	private BankAccountType.Builder convertBankAccountType(MRefList bankAccountType) {
		BankAccountType.Builder builder = BankAccountType.newBuilder();
		if (bankAccountType == null || bankAccountType.getAD_Ref_List_ID() <= 0 || bankAccountType.getAD_Reference_ID() != MBankAccount.BANKACCOUNTTYPE_AD_Reference_ID) {
			return builder;
		}
		builder.setId(bankAccountType.getAD_Reference_ID())
			.setUuid(ValueUtil.validateNull(bankAccountType.getUUID()))
			.setValue(ValueUtil.validateNull(bankAccountType.getValue()))
			.setName(ValueUtil.validateNull(bankAccountType.getName()))
			.setDescription(ValueUtil.validateNull(bankAccountType.getDescription()))
		;
		return builder;
	}

	private Currency.Builder convertCurrency(int currencyId) {
		if (currencyId > 0) {
			MCurrency currency = MCurrency.get(Env.getCtx(), currencyId);
			return ConvertUtil.convertCurrency(currency);
		}
		return Currency.newBuilder();
	}

	private BankAccount.Builder convertBankAccount(int bankAccountId) {
		if (bankAccountId > 0) {
			MBankAccount bankAccount = new MBankAccount(Env.getCtx(), bankAccountId, null);
			return convertBankAccount(bankAccount);
		}
		return BankAccount.newBuilder();
	}

	private BankAccount.Builder convertBankAccount(MBankAccount bankAccount) {
		BankAccount.Builder builder = BankAccount.newBuilder();
		if (bankAccount == null || bankAccount.getC_BankAccount_ID() <= 0) {
			return builder;
		}
		builder.setId(bankAccount.getC_BankAccount_ID())
			.setUuid(ValueUtil.validateNull(bankAccount.getUUID()))
			.setAccountNo(ValueUtil.validateNull(bankAccount.getAccountNo()))
			.setAccountName(ValueUtil.validateNull(bankAccount.getName()))
			.setBban(ValueUtil.validateNull(bankAccount.getBBAN()))
			.setIban(ValueUtil.validateNull(bankAccount.getIBAN()))
			.setDescription(ValueUtil.validateNull(bankAccount.getDescription()))
			.setIsDefault(bankAccount.isDefault())
			.setCurrency(
				convertCurrency(bankAccount.getC_Currency_ID())
			)
			.setBankAccountType(
				convertBankAccountType(bankAccount.getBankAccountType())
			)
			.setCreditLimit(
				ValueUtil.getDecimalFromBigDecimal(bankAccount.getCreditLimit())
			)
			.setCurrentBalance(
				ValueUtil.getDecimalFromBigDecimal(bankAccount.getCurrentBalance())
			)
			.setBusinessPartner(
				convertBusinessPartner(bankAccount.getC_BPartner_ID())
			)
		;

		return builder;
	}

	@Override
	public void listPaymentSelection(ListPaymentSelectionRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLookupItemsResponse.Builder builder = listPaymentSelection(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	private ListLookupItemsResponse.Builder listPaymentSelection(ListPaymentSelectionRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		//	Add DocStatus for validation
		final String validationCode = " C_PaySelection.C_BankAccount_ID IS NOT NULL "
			+ "AND C_PaySelection.DocStatus = 'CO' "
			+ "AND EXISTS("
			+ "		SELECT 1 FROM C_PaySelectionCheck psc "
			+ "		LEFT JOIN C_Payment p ON(p.C_Payment_ID = psc.C_Payment_ID) "
			+ "		WHERE psc.C_PaySelection_ID = C_PaySelection.C_PaySelection_ID "
			+ "		AND (psc.C_Payment_ID IS NULL OR p.DocStatus NOT IN('CO', 'CL'))"
			+ ")";
		Query query = new Query(
			context,
			I_C_PaySelection.Table_Name,
			validationCode,
			null
		);

		int count = query.count();
		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(count);
		
		List<MPaySelection> paymentSelectionChekList = query.list();
		paymentSelectionChekList.stream().forEach(paymentSelection -> {
			//	Display column
			String displayedValue = new StringBuffer()
				.append(Util.isEmpty(paymentSelection.getDocumentNo(), true) ? "-1" : paymentSelection.getDocumentNo())
				.append("_")
				.append(paymentSelection.getTotalAmt() == null ? "-1" : paymentSelection.getTotalAmt())
				.toString();
	
			LookupItem.Builder builderItem = LookupUtil.convertObjectFromResult(
				paymentSelection.getC_PaySelection_ID(), paymentSelection.getUUID(), null, displayedValue
			);

			builderItem.setTableName(I_C_PaySelection.Table_Name);
			builderItem.setId(paymentSelection.getC_PaySelection_ID());
			builderItem.setUuid(ValueUtil.validateNull(paymentSelection.getUUID()));

			builderList.addRecords(builderItem.build());
		});

		return builderList;
	}

	@Override
	public void listPaymentRules(ListPaymentRulesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLookupItemsResponse.Builder builder = listPaymentRules(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListLookupItemsResponse.Builder listPaymentRules(ListPaymentRulesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		int paymentSelectionId = request.getPaymentSelectionId();
		if (paymentSelectionId <= 0) {
			if (!Util.isEmpty(request.getPaymentSelectionUuid(), true)) {
				paymentSelectionId = RecordUtil.getIdFromUuid(I_C_PaySelection.Table_Name, request.getPaymentSelectionUuid(), null);
			}
			if (paymentSelectionId <= 0) {
				throw new AdempiereException("@FillMandatory@ @C_PaySelection_ID@");
			}
		}
		MPaySelection paymentSelection = new Query(
			context,
			I_C_PaySelection.Table_Name,
			" C_PaySelection_ID = ? ",
			null
		)
			.setParameters(paymentSelectionId)
			.first();
		if (paymentSelection == null || paymentSelection.getC_PaySelection_ID() <= 0) {
			throw new AdempiereException("@C_PaySelection_ID@ @NotFound@");
		}

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();
		final String whereClause = "EXISTS ("
			+ "	SELECT 1 FROM C_PaySelectionLine "
			+ "	WHERE "
			+ "	AD_Ref_List.Value = C_PaySelectionLine.PaymentRule"
			+ "	AND C_PaySelectionLine.C_PaySelection_ID = ?"
			+ "	AND AD_Ref_List.AD_Reference_ID = ?"
			+ ")"
		;
		List<MRefList> paymemntRulesList = new Query(
			Env.getCtx(),
			I_AD_Ref_List.Table_Name,
			whereClause,
			null
		).setParameters(paymentSelection.getC_PaySelection_ID(), X_C_PaySelectionLine.PAYMENTRULE_AD_Reference_ID)
		.list(MRefList.class);

		paymemntRulesList.stream().forEach(paymentRule -> {			
			LookupItem.Builder builderItem = LookupUtil.convertObjectFromResult(
				paymentSelection.getC_PaySelection_ID(), paymentSelection.getUUID(), null, paymentRule.getName()
			);

			builderItem.setTableName(I_C_PaySelection.Table_Name);
			builderItem.setId(paymentSelection.getC_PaySelection_ID());
			builderItem.setUuid(ValueUtil.validateNull(paymentSelection.getUUID()));

			builderList.addRecords(builderItem.build());
		});
		
		return builderList;
	}


	@Override
	public void getDocumentNo(GetDocumentNoRequest request, StreamObserver<GetDocumentNoResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			GetDocumentNoResponse.Builder builder = getDocumentNo(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private GetDocumentNoResponse.Builder getDocumentNo(GetDocumentNoRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		// Bank Account
		int bankAccountId = request.getBankAccountId();
		if (bankAccountId <= 0) {
			if (!Util.isEmpty(request.getBankAccountUuid(), true)) {
				bankAccountId = RecordUtil.getIdFromUuid(I_C_BankAccount.Table_Name, request.getBankAccountUuid(), null);
			}
			if (bankAccountId <= 0) {
				throw new AdempiereException("@FillMandatory@ @C_BankAccount_ID@");
			}
		}
		MBankAccount bankAccount = new Query(
			context,
			I_C_BankAccount.Table_Name,
			" C_BankAccount_ID = ? ",
			null
		)
			.setParameters(bankAccountId)
			.first();
		if (bankAccount == null || bankAccount.getC_BankAccount_ID() <= 0) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}

		// Payment Rule
		int paymentRuleId = request.getPaymentRuleId();
		if (paymentRuleId <= 0) {
			if (!Util.isEmpty(request.getPaymentRuleUuid(), true)) {
				paymentRuleId = RecordUtil.getIdFromUuid(I_AD_Ref_List.Table_Name, request.getPaymentRuleUuid(), null);
			}
			if (paymentRuleId <= 0) {
				throw new AdempiereException("@FillMandatory@ @PaymentRule@");
			}
		}
		MRefList paymentRule = new Query(
			context,
			I_AD_Ref_List.Table_Name,
			" AD_Ref_List_ID = ? ",
			null
		)
			.setParameters(paymentRuleId)
			.first();
		if (paymentRule == null || paymentRule.getAD_Ref_List_ID() <= 0) {
			throw new AdempiereException("@PaymentRule@ @NotFound@");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ")
			.append(I_C_BankAccountDoc.COLUMNNAME_CurrentNext)
			.append(" FROM ")
			.append(I_C_BankAccountDoc.Table_Name)
			.append(" WHERE ")
			.append(I_C_BankAccountDoc.COLUMNNAME_C_BankAccount_ID).append("=? AND ")
			.append(I_C_BankAccountDoc.COLUMNNAME_PaymentRule).append("=? AND ")
			.append(I_C_BankAccountDoc.COLUMNNAME_IsActive).append("=?");

		List<Object> parameters =  new ArrayList<>();
		parameters.add(bankAccount.getC_BankAccount_ID());
		parameters.add(paymentRule.getValue());
		parameters.add(true);

		int documentNo = DB.getSQLValueEx(null , sql.toString(), parameters.toArray());
		return GetDocumentNoResponse.newBuilder()
			.setDocumentNo(String.valueOf(documentNo));
	}

	@Override
	public void createEFTPayment(CreateEFTPaymentRequest request, StreamObserver<PaymentSelection> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			PaymentSelection.Builder builder = PaymentSelection.newBuilder();
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	@Override
	public void exportPayments(ExportPaymentsRequest request, StreamObserver<ProcessLog> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ProcessLog.Builder builder = ProcessLog.newBuilder();
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	@Override
	public void printPayments(PrintPaymentsRequest request, StreamObserver<ProcessLog> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ProcessLog.Builder builder = ProcessLog.newBuilder();
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	@Override
	public void confirmPrint(ConfirmPrintRequest request, StreamObserver<PaymentSelection> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			PaymentSelection.Builder builder = PaymentSelection.newBuilder();
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	@Override
	public void printRemittance(PrintRemittanceRequest request, StreamObserver<ProcessLog> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ProcessLog.Builder builder = ProcessLog.newBuilder();
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

}
