/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service.accounting;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Org;
import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_AD_Reference;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.I_A_Asset_Addition;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_ConversionType;
import org.adempiere.core.domains.models.I_C_Conversion_Rate;
import org.adempiere.core.domains.models.I_C_Currency;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.I_M_MatchInv;
import org.adempiere.core.domains.models.I_M_MatchPO;
import org.adempiere.core.domains.models.X_Fact_Acct;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.asset.model.MAssetAddition;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MConversionRate;
import org.compiere.model.MConversionType;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrg;
import org.compiere.model.MPayment;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.solop.sp032.model.MSP032Conversion;
import org.solop.sp032.util.CurrencyConvertDocumentsUtil;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.general_ledger.AccountingDocument;
import org.spin.backend.grpc.general_ledger.ConversionRate;
import org.spin.backend.grpc.general_ledger.ConversionType;
import org.spin.backend.grpc.general_ledger.CreateConversionRateRequest;
import org.spin.backend.grpc.general_ledger.ExistsAccountingDocumentRequest;
import org.spin.backend.grpc.general_ledger.ExistsAccountingDocumentResponse;
import org.spin.backend.grpc.general_ledger.ListAccountingDocumentsRequest;
import org.spin.backend.grpc.general_ledger.ListAccountingDocumentsResponse;
import org.spin.backend.grpc.general_ledger.ListAccountingSchemasRequest;
import org.spin.backend.grpc.general_ledger.ListConversionTypesRequest;
import org.spin.backend.grpc.general_ledger.ListConversionTypesResponse;
import org.spin.backend.grpc.general_ledger.ListPostingTypesRequest;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of General Ledger
 */
public class GeneralLedgerServiceLogic {

	private static String TABLE_NAME = MAccount.Table_Name;

	private final static List<String> POSTED_TABLES_WITHOUT_DOCUMENT = Arrays.asList(
		I_M_MatchInv.Table_Name,
		I_M_MatchPO.Table_Name
	);


	public static MOrg validateAndGetOrganization(int organizationId) {
		if (organizationId < 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		}
		if (organizationId == 0) {
			throw new AdempiereException("@Org0NotAllowed@");
		}
		MOrg organization = new Query(
			Env.getCtx(),
			I_AD_Org.Table_Name,
			" AD_Org_ID = ? ",
			null
		)
			.setParameters(organizationId)
			.setClient_ID()
			.first()
		;
		if (organization == null || organization.getAD_Org_ID() <= 0) {
			throw new AdempiereException("@AD_Org_ID@ @NotFound@");
		}
		if (!organization.isActive()) {
			throw new AdempiereException("@AD_Org_ID@ @NotActive@");
		}
		return organization;
	}


	public static MCurrency validateAndGetCurrency(int currencyId) {
		if (currencyId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Currency_ID@");
		}
		MCurrency currency = new Query(
			Env.getCtx(),
			I_C_Currency.Table_Name,
			" C_Currency_ID = ? ",
			null
		)
			.setParameters(currencyId)
			.first()
		;
		if (currency == null || currency.getC_Currency_ID() <= 0) {
			throw new AdempiereException("@C_Currency_ID@ @NotFound@");
		}
		if (!currency.isActive()) {
			throw new AdempiereException("@C_Currency_ID@ @NotActive@");
		}
		return currency;
	}


	public static MBPartner validateAndGetBusinessPartner(int businessPartnerId) {
		if (businessPartnerId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BPartner_ID@");
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			throw new AdempiereException("@C_BPartner_ID@ @NotFound@");
		}
		if (!businessPartner.isActive()) {
			throw new AdempiereException("@C_BPartner_ID@ @NotActive@");
		}
		return businessPartner;
	}


	public static ListLookupItemsResponse.Builder listAccountingSchemas(ListAccountingSchemasRequest request) {
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		List<MAcctSchema> accountingShemasList = Arrays.asList(
			MAcctSchema.getClientAcctSchema(Env.getCtx(), clientId)
		);

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(accountingShemasList.size())
		;

		accountingShemasList.stream()
			.forEach(accountingShema -> {
				LookupItem.Builder lookupBuilder = LookupUtil.convertObjectFromResult(
					accountingShema.getC_AcctSchema_ID(),
					accountingShema.getUUID(),
					null,
					accountingShema.getName(),
					accountingShema.isActive()
				);
				builderList.addRecords(lookupBuilder);
			})
		;

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listPostingTypes(ListPostingTypesRequest request) {
		// Posting Type = 125
		int referenceId = X_Fact_Acct.POSTINGTYPE_AD_Reference_ID;

		final String whereClause = I_AD_Reference.COLUMNNAME_AD_Reference_ID + " = ? ";
		Query query = new Query(
			Env.getCtx(),
			I_AD_Ref_List.Table_Name,
			whereClause,
			null
		)
			.setParameters(referenceId)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int recordCount = query.count();

		ListLookupItemsResponse.Builder builder = ListLookupItemsResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		//	Get List
		query.setLimit(limit, offset)
			.<MRefList>list()
			.forEach(refList -> {
				LookupItem.Builder lookup = LookupUtil.convertLookupItemFromReferenceList(refList);
				builder.addRecords(lookup);
			})
		;

		return builder;
	}


	public static ListAccountingDocumentsResponse.Builder listAccountingDocuments(ListAccountingDocumentsRequest request) {
		final String whereClause = " IsView='N' "
			+ " AND EXISTS(SELECT 1 FROM AD_Column c"
			+ " WHERE AD_Table.AD_Table_ID = c.AD_Table_ID "
			+ " AND c.ColumnName = 'Posted')"
		;
		Query query = new Query(
			Env.getCtx(),
			I_AD_Table.Table_Name,
			whereClause,
			null
		);

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int recordCount = query.count();

		ListAccountingDocumentsResponse.Builder builderList = ListAccountingDocumentsResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		//	Get List
		List<Integer> tableAccountigIds = query
			.setLimit(limit, offset)
			.getIDsAsList()
		;

		// query.setLimit(limit, offset)
		// 	.<MRefList>list()
		tableAccountigIds.forEach(tableId -> {
			AccountingDocument.Builder accountingDocument = GeneralLedgerConvertUtil.convertAccountingDocument(tableId);
			builderList.addRecords(accountingDocument);
		});

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listOrganizations(ListAccountingSchemasRequest request) {
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		MClient client = MClient.get(Env.getCtx(), clientId);

		List<MOrg> organizationList = Arrays.asList(
			MOrg.getOfClient(client)
		)
			.stream()
			.sorted(
				Comparator.comparing(MOrg::getValue)
			)
			.collect(Collectors.toList())
		;

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(organizationList.size())
		;

		organizationList.stream()
			.forEach(organization -> {
				LookupItem.Builder lookupBuilder = LookupUtil.convertObjectFromResult(
					organization.getAD_Org_ID(),
					organization.getUUID(),
					organization.getValue(),
					organization.getName(),
					organization.isActive()
				);
				builderList.addRecords(lookupBuilder);
			})
		;

		return builderList;
	}



	public static ExistsAccountingDocumentResponse.Builder existsAccountingDocument(ExistsAccountingDocumentRequest request) {
		ExistsAccountingDocumentResponse.Builder builder = ExistsAccountingDocumentResponse.newBuilder();
		MRole role = MRole.getDefault();
		if (role == null || !role.isShowAcct()) {
			return builder;
		}

		// Validate accounting schema
		int acctSchemaId = request.getAccountingSchemaId();
		if (acctSchemaId <= 0) {
			// throw new AdempiereException("@FillMandatory@ @C_AcctSchema_ID@");
			return builder;
		}

		// Validate table
		if (Util.isEmpty(request.getTableName(), true)) {
			// throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
			return builder;
		}
		final MTable documentTable = MTable.get(Env.getCtx(), request.getTableName());
		if (documentTable == null || documentTable.getAD_Table_ID() == 0) {
			// throw new AdempiereException("@AD_Table_ID@ @Invalid@");
			return builder;
		}

		if (documentTable.isView()) {
			return builder;
		}
		if (!documentTable.isDocument()) {
			// TODO: Remove this condition when complete support to document table
			if (!POSTED_TABLES_WITHOUT_DOCUMENT.contains(documentTable.getTableName())) {
				// With `Posted` column
				if (documentTable.getColumn(I_C_Invoice.COLUMNNAME_Posted) == null) {
					return builder;
				}
			}
		}

		// Validate record
		final int recordId = request.getRecordId();
		if (!RecordUtil.isValidId(recordId, TABLE_NAME)) {
			// throw new AdempiereException("@FillMandatory@ @Record_ID@");
			return builder;
		}
		PO record = RecordUtil.getEntity(Env.getCtx(), documentTable.getTableName(), recordId, null);
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}

		// Validate `Posted` column
		if (record.get_ColumnIndex(I_C_Invoice.COLUMNNAME_Posted) < 0) {
			// without `Posted` button
			return builder;
		}

		// Validate `Processed` column
		if (record.get_ColumnIndex(I_C_Invoice.COLUMNNAME_Processed) < 0) {
			return builder;
		}
		if (!record.get_ValueAsBoolean(I_C_Invoice.COLUMNNAME_Processed)) {
			return builder;
		}

		builder.setIsShowAccounting(true);
		return builder;
	}



	public static ListConversionTypesResponse.Builder listConversionTypes(ListConversionTypesRequest request) {
		MTable table = MTable.get(
			Env.getCtx(),
			I_C_ConversionType.Table_Name
		);

		String whereClause = "";
		List<Object> filtersList = new ArrayList<Object>();
		if (table.get_ColumnIndex(I_C_BPartner.COLUMNNAME_C_BPartner_ID) >= 0) {
			whereClause = "(C_BPartner_ID IS NULL OR C_BPartner_ID = ?)";
			if (request.getBusinessPartnerId() > 0) {
				validateAndGetBusinessPartner(
					request.getBusinessPartnerId()
				);
			}
			filtersList.add(
				request.getBusinessPartnerId()
			);

			if (request.getOrderId() > 0) {
				MOrder order = new MOrder(Env.getCtx(), request.getOrderId(), null);
				if (order != null && order.getC_Order_ID() > 0) {
					whereClause += " AND C_Order_ID = ?";
					filtersList.add(
						request.getOrderId()
					);
				}
			} else if (request.getInvoiceId() > 0) {
				MInvoice invoice = new MInvoice(Env.getCtx(), request.getInvoiceId(), null);
				if (invoice != null && invoice.getC_Invoice_ID() > 0) {
					whereClause += " AND C_Invoice_ID = ?";
					filtersList.add(
						request.getInvoiceId()
					);
				}
			} else if (request.getPaymentId() > 0) {
				MPayment payment = new MPayment(Env.getCtx(), request.getPaymentId(), null);
				if (payment != null && payment.getC_Payment_ID() > 0) {
					whereClause += " AND C_Order_ID = ?";
					filtersList.add(
						request.getPaymentId()
					);
				}
			} else if (request.getAssetAdditionId() > 0) {
				MAssetAddition assetAddition = new MAssetAddition(Env.getCtx(), request.getAssetAdditionId(), null);
				if (assetAddition != null && assetAddition.getA_Asset_Addition_ID() > 0) {
					whereClause += " AND A_Asset_Addition_ID = ?";
					filtersList.add(
						request.getAssetAdditionId()
					);
				}
			} else if (request.getExpedientId() > 0) {
				MTable expedientTable = MTable.get(Env.getCtx(), "SP009_Expedient");
				if (expedientTable != null && expedientTable.getAD_Table_ID() > 0) {
					PO expedient = expedientTable.getPO(request.getExpedientId(), null);
					if (expedient != null && expedient.get_ID() > 0) {
						whereClause += " AND SP009_Expedient_ID = ?";
						filtersList.add(
							request.getExpedientId()
						);
					}
				}
			}
		}

		Query query = new Query(
			Env.getCtx(),
			table,
			whereClause,
			null
		)
			.setParameters(filtersList)
			.setApplyAccessFilter(true)
			.setOnlyActiveRecords(true)
		;

		ListConversionTypesResponse.Builder builderList = ListConversionTypesResponse.newBuilder()
			.setRecordCount(
				query.count()
			)
		;

		query
			.getIDsAsList()
			.forEach(conversionTypeId -> {
				PO conversionType = table.getPO(conversionTypeId, null);
				ConversionType.Builder currencyBuilder = GeneralLedgerConvertUtil.convertConversionType(
					(MConversionType) conversionType
				);
				builderList.addRecords(
					currencyBuilder
				);
			})
		;

		return builderList;
	}


	public static ConversionRate.Builder createConversionRate(CreateConversionRateRequest request) {
		MOrg organization = new MOrg(Env.getCtx(), request.getOrganizationId(), null);

		int currencyFromId = request.getCurrencyFromId();
		if (currencyFromId <= 0) {
			final int clientId = Env.getAD_Client_ID(Env.getCtx());
			final int accountingSchemaId = DB.getSQLValue(null, "SELECT MIN(C_AcctSchema_ID) FROM C_AcctSchema WHERE AD_CLient_ID = ?", clientId);
			MAcctSchema accountingSchema = MAcctSchema.get(Env.getCtx(), accountingSchemaId);
			currencyFromId = accountingSchema.getC_Currency_ID();
		}
		MCurrency currencyFrom = validateAndGetCurrency(
			currencyFromId
		);
		MCurrency currencyTo = validateAndGetCurrency(
			request.getCurrencyToId()
		);

		MBPartner businessPartner = validateAndGetBusinessPartner(
			request.getBusinessPartnerId()
		);

		BigDecimal negotiatedRate = NumberManager.getBigDecimalFromString(
			request.getNegotiatedRate()
		);

		AtomicReference<MConversionRate> conversionRateReference = new AtomicReference<MConversionRate>();
		Trx.run(transactionName -> {
			String whereClause = "C_BPartner_ID = ?";
			List<Object> filtersList = new ArrayList<Object>();
			filtersList.add(businessPartner.getC_BPartner_ID());

			String documentNo = "";
			Timestamp dateFrom = null;
			if (request.getOrderId() > 0) {
				MOrder order = new MOrder(Env.getCtx(), request.getOrderId(), null);
				if (order != null && order.getC_Order_ID() > 0) {
					whereClause += " AND C_Order_ID = ?";
					filtersList.add(
						request.getOrderId()
					);
					documentNo = order.getDocumentNo();
					dateFrom = order.getDateAcct();
				}
			} else if (request.getInvoiceId() > 0) {
				MInvoice invoice = new MInvoice(Env.getCtx(), request.getInvoiceId(), null);
				if (invoice != null && invoice.getC_Invoice_ID() > 0) {
					whereClause += " AND C_Invoice_ID = ?";
					filtersList.add(
						request.getInvoiceId()
					);
					documentNo = invoice.getDocumentNo();
					dateFrom = invoice.getDateAcct();
				}
			} else if (request.getPaymentId() > 0) {
				MPayment payment = new MPayment(Env.getCtx(), request.getPaymentId(), null);
				if (payment != null && payment.getC_Payment_ID() > 0) {
					whereClause += " AND C_Order_ID = ?";
					filtersList.add(
						request.getPaymentId()
					);
					documentNo = payment.getDocumentNo();
					dateFrom = payment.getDateAcct();
				}
			} else if (request.getAssetAdditionId() > 0) {
				MAssetAddition assetAddition = new MAssetAddition(Env.getCtx(), request.getAssetAdditionId(), null);
				if (assetAddition != null && assetAddition.getA_Asset_Addition_ID() > 0) {
					whereClause += " AND A_Asset_Addition_ID = ?";
					filtersList.add(
						request.getAssetAdditionId()
					);
					documentNo = assetAddition.getDocumentNo();
					dateFrom = assetAddition.getDateAcct();
				}
			} else if (request.getExpedientId() > 0) {
				MTable table = MTable.get(Env.getCtx(), "SP009_Expedient");
				if (table != null && table.getAD_Table_ID() > 0) {
					PO expedient = table.getPO(request.getExpedientId(), transactionName);
					if (expedient != null && expedient.get_ID() > 0) {
						whereClause += " AND SP009_Expedient_ID = ?";
						filtersList.add(
							request.getExpedientId()
						);
						documentNo = expedient.get_ValueAsString(I_C_Order.COLUMNNAME_DocumentNo);
						dateFrom = TimeManager.getTimestampFromObject(
							expedient.get_Value(MSP032Conversion.COLUMNNAME_DateDoc)
						);
					}
				}
			}

			MConversionType conversionType = new Query(
				Env.getCtx(),
				I_C_ConversionType.Table_Name,
				whereClause,
				transactionName
			)
				.setParameters(filtersList)
				.setClient_ID()
				// .setApplyAccessFilter(true)
				.first()
			;
			if (conversionType == null || conversionType.getC_ConversionType_ID() <= 0) {
				conversionType = new MConversionType(Env.getCtx(), 0, transactionName);
				// fill with parent conversion type
				if (request.getConversionTypeId() > 0) {
					MConversionType conversionTypeBase = new MConversionType(Env.getCtx(), request.getConversionTypeId(), transactionName);
					PO.copyValues(conversionTypeBase, conversionType);
					conversionType.set_CustomColumn(
						CurrencyConvertDocumentsUtil.COLUMNNAME_SP032_ParentCType_ID,
						request.getConversionTypeId()
					);
				}
				conversionType.setAD_Org_ID(0);
				conversionType.setIsDefault(false);
				String name = businessPartner.getDisplayValue();
				String value = businessPartner.getValue();
				if (!Util.isEmpty(documentNo, true)) {
					name += " - " + documentNo;
					value += " - " + documentNo;
				}
				conversionType.setValue(value);
				conversionType.setName(name);

				conversionType.set_CustomColumn(I_C_BPartner.COLUMNNAME_C_BPartner_ID, businessPartner.getC_BPartner_ID());
				if (request.getOrderId() > 0) {
					conversionType.set_CustomColumn(I_C_Order.COLUMNNAME_C_Order_ID, request.getOrderId());
				} else if (request.getInvoiceId() > 0) {
					conversionType.set_CustomColumn(I_C_Invoice.COLUMNNAME_C_Order_ID, request.getInvoiceId());
				} else if (request.getPaymentId() > 0) {
					conversionType.set_CustomColumn(I_C_Payment.COLUMNNAME_C_Payment_ID, request.getPaymentId());
				} else if (request.getAssetAdditionId() > 0) {
					conversionType.set_CustomColumn(I_A_Asset_Addition.COLUMNNAME_A_Asset_Addition_ID, request.getAssetAdditionId());
				} else if (request.getExpedientId() > 0) {
					conversionType.set_CustomColumn(CurrencyConvertDocumentsUtil.ColumnName_SP009_Expedient_ID, request.getExpedientId());
				}
				conversionType.saveEx();
			}

			if (dateFrom == null) {
				Timestamp date = ValueManager.getDateFromTimestampDate(
					request.getDate()
				);
				dateFrom = TimeUtil.getDay(date); // Remove time mark
			}
			final Timestamp dateTo = TimeUtil.addYears(dateFrom, CurrencyConvertDocumentsUtil.TIME_Interval);

			final int clientId = Env.getAD_Client_ID(Env.getCtx());
			final int organizationId = organization.getAD_Org_ID();

			MConversionRate conversionRate = new Query(
				Env.getCtx(),
				I_C_Conversion_Rate.Table_Name,
				"C_Currency_ID = ? AND C_Currency_ID_To = ? AND C_ConversionType_ID = ? AND ? >= ValidFrom AND ? <= ValidTo AND AD_Client_ID IN (0, ?) AND AD_Org_ID IN (0, ?) ",
				transactionName
			)
				.setParameters(currencyFrom.getC_Currency_ID(), currencyTo.getC_Currency_ID(), conversionType.getC_ConversionType_ID(), dateFrom, dateTo, clientId, organizationId)
				.first()
			;
			if (conversionRate == null || conversionRate.getC_ConversionType_ID() <= 0) {
				conversionRate = new MConversionRate(Env.getCtx(), 0, transactionName);
				conversionRate.setAD_Org_ID(0);
				conversionRate.setC_ConversionType_ID(
					conversionType.getC_ConversionType_ID()
				);
				conversionRate.setC_Currency_ID(
					currencyFrom.getC_Currency_ID()
				);
				conversionRate.setC_Currency_ID_To(
					currencyTo.getC_Currency_ID()
				);
				conversionRate.setValidFrom(dateFrom);
				conversionRate.setValidTo(dateTo);
			}
			conversionRate.setMultiplyRate(negotiatedRate);
			conversionRate.saveEx();

			//
			conversionRateReference.set(conversionRate);

			// Invert conversion rate
			MConversionRate invertConversionRate = new Query(
				Env.getCtx(),
				I_C_Conversion_Rate.Table_Name,
				"C_Currency_ID = ? AND C_Currency_ID_To = ? AND C_ConversionType_ID = ? AND ? >= ValidFrom AND ? <= ValidTo AND AD_Client_ID IN (0, ?) AND AD_Org_ID IN (0, ?) ",
				transactionName
			)
				.setParameters(currencyTo.getC_Currency_ID(), currencyFrom.getC_Currency_ID(), conversionType.getC_ConversionType_ID(), dateFrom, dateTo, clientId, organizationId)
				.first()
			;
			if (invertConversionRate == null || invertConversionRate.getC_ConversionType_ID() <= 0) {
				invertConversionRate = new MConversionRate(Env.getCtx(), 0, transactionName);
				invertConversionRate.setAD_Org_ID(0);
				invertConversionRate.setC_ConversionType_ID(
					conversionType.getC_ConversionType_ID()
				);
				invertConversionRate.setC_Currency_ID(
					currencyTo.getC_Currency_ID()
				);
				invertConversionRate.setC_Currency_ID_To(
					currencyFrom.getC_Currency_ID()
				);
				invertConversionRate.setValidFrom(dateFrom);
				invertConversionRate.setValidTo(dateTo);
			}
			invertConversionRate.setDivideRate(negotiatedRate);
			invertConversionRate.saveEx();
		});

		ConversionRate.Builder builder = GeneralLedgerConvertUtil.convertConversionRate(
			conversionRateReference.get()
		);
		return builder;
	}

}
