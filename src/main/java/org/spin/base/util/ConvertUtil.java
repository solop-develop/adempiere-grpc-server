/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   		 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 		 *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           		 *
 * See the GNU General Public License for more details.                       		 *
 * You should have received a copy of the GNU General Public License along    		 *
 * with this program; if not, write to the Free Software Foundation, Inc.,    		 *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     		 *
 * For the text or an alternative of this public license, you may reach us    		 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.base.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Optional;

import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MChatEntry;
import org.compiere.model.MClient;
import org.compiere.model.MConversionRate;
import org.compiere.model.MInOut;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.model.MPOSKey;
import org.compiere.model.MPOSKeyLayout;
import org.compiere.model.MPayment;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MRefList;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.DocumentAction;
import org.spin.backend.grpc.common.DocumentStatus;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ProcessInfoLog;
import org.spin.backend.grpc.pos.CustomerBankAccount;
import org.spin.backend.grpc.pos.Key;
import org.spin.backend.grpc.pos.KeyLayout;
import org.spin.backend.grpc.pos.Shipment;
import org.spin.backend.grpc.user_interface.ChatEntry;
import org.spin.backend.grpc.user_interface.ModeratorStatus;
import org.spin.base.interim.ContextTemporaryWorkaround;
import org.spin.grpc.service.FileManagement;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.pos.util.ColumnsAdded;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for convert any document
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ConvertUtil {

	/**
	 * Convert ProcessInfoLog to gRPC
	 * @param log
	 * @return
	 */
	public static ProcessInfoLog.Builder convertProcessInfoLog(org.compiere.process.ProcessInfoLog log) {
		ProcessInfoLog.Builder processLog = ProcessInfoLog.newBuilder();
		if (log == null) {
			return processLog;
		}
		processLog.setRecordId(
				log.getP_ID()
			)
			.setLog(
				TextManager.getValidString(
					Msg.parseTranslation(Env.getCtx(), log.getP_Msg())
				)
			)
		;
		return processLog;
	}

	/**
	 * Convert PO class from Chat Entry process to builder
	 * @param chatEntry
	 * @return
	 */
	public static ChatEntry.Builder convertChatEntry(MChatEntry chatEntry) {
		ChatEntry.Builder builder = ChatEntry.newBuilder();
		if (chatEntry == null) {
			return builder;
		}
		builder.setId(
				chatEntry.getCM_ChatEntry_ID()
			)
			.setChatId(
				chatEntry.getCM_Chat_ID()
			)
			.setSubject(
				TextManager.getValidString(
					chatEntry.getSubject()
				)
			)
			.setCharacterData(
				TextManager.getValidString(
					chatEntry.getCharacterData()
				)
			)
		;

		if (chatEntry.getAD_User_ID() > 0) {
			MUser user = MUser.get(chatEntry.getCtx(), chatEntry.getAD_User_ID());
			builder.setUserId(
					chatEntry.getAD_User_ID()
				)
				.setUserName(
					TextManager.getValidString(
						user.getName()
					)
				)
			;
		}

		builder.setLogDate(
			ValueManager.getProtoTimestampFromTimestamp(
				chatEntry.getCreated()
			)
		);
		//	Confidential Type
		if(!Util.isEmpty(chatEntry.getConfidentialType())) {
			if(chatEntry.getConfidentialType().equals(MChatEntry.CONFIDENTIALTYPE_PublicInformation)) {
				builder.setConfidentialType(org.spin.backend.grpc.user_interface.ConfidentialType.PUBLIC);
			} else if(chatEntry.getConfidentialType().equals(MChatEntry.CONFIDENTIALTYPE_PartnerConfidential)) {
				builder.setConfidentialType(org.spin.backend.grpc.user_interface.ConfidentialType.PARTER);
			} else if(chatEntry.getConfidentialType().equals(MChatEntry.CONFIDENTIALTYPE_Internal)) {
				builder.setConfidentialType(org.spin.backend.grpc.user_interface.ConfidentialType.INTERNAL);
			}
		}
		//	Moderator Status
		if(!Util.isEmpty(chatEntry.getModeratorStatus())) {
			if(chatEntry.getModeratorStatus().equals(MChatEntry.MODERATORSTATUS_NotDisplayed)) {
				builder.setModeratorStatus(ModeratorStatus.NOT_DISPLAYED);
			} else if(chatEntry.getModeratorStatus().equals(MChatEntry.MODERATORSTATUS_Published)) {
				builder.setModeratorStatus(ModeratorStatus.PUBLISHED);
			} else if(chatEntry.getModeratorStatus().equals(MChatEntry.MODERATORSTATUS_Suspicious)) {
				builder.setModeratorStatus(ModeratorStatus.SUSPICIUS);
			} else if(chatEntry.getModeratorStatus().equals(MChatEntry.MODERATORSTATUS_ToBeReviewed)) {
				builder.setModeratorStatus(ModeratorStatus.TO_BE_REVIEWED);
			}
		}
		//	Chat entry type
		if(!Util.isEmpty(chatEntry.getChatEntryType())) {
			if(chatEntry.getChatEntryType().equals(MChatEntry.CHATENTRYTYPE_NoteFlat)) {
				builder.setChatEntryType(org.spin.backend.grpc.user_interface.ChatEntryType.NOTE_FLAT);
			} else if(chatEntry.getChatEntryType().equals(MChatEntry.CHATENTRYTYPE_ForumThreaded)) {
				builder.setChatEntryType(org.spin.backend.grpc.user_interface.ChatEntryType.NOTE_FLAT);
			} else if(chatEntry.getChatEntryType().equals(MChatEntry.CHATENTRYTYPE_Wiki)) {
				builder.setChatEntryType(org.spin.backend.grpc.user_interface.ChatEntryType.NOTE_FLAT);
			}
		}
  		return builder;
	}

	/**
	 * Convert PO to Value Object
	 * @param entity
	 * @return
	 */
	public static Entity.Builder convertEntity(PO entity) {
		Entity.Builder entityBuilder = Entity.newBuilder();
		if(entity == null) {
			return entityBuilder;
		}

		final int identifier = entity.get_ID();
		entityBuilder.setId(identifier);

		final String uuid = entity.get_UUID();
		entityBuilder.setUuid(
			TextManager.getValidString(uuid)
		);

		//	Convert attributes
		POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), entity.get_Table_ID());
		entityBuilder.setTableName(
			TextManager.getValidString(
				poInfo.getTableName()
			)
		);

		Struct.Builder rowValues = Struct.newBuilder();
		for(int index = 0; index < poInfo.getColumnCount(); index++) {
			String columnName = poInfo.getColumnName(index);
			int displayTypeId = poInfo.getColumnDisplayType(index);
			Object value = entity.get_Value(index);
			Value.Builder builderValue = ValueManager.getProtoValueFromObject(
				value,
				displayTypeId
			);
			//	add value
			rowValues.putFields(
				columnName,
				builderValue.build()
			);

			// add display value
			if (value != null) {
				String displayValue = null;
				if (columnName.equals(poInfo.getTableName() + "_ID")) {
					displayValue = entity.getDisplayValue();
				} else if (ReferenceUtil.validateReference(displayTypeId) || displayTypeId == DisplayType.Button) {
					int referenceValueId = poInfo.getColumnReferenceValueId(index);
					displayTypeId = ReferenceUtil.overwriteDisplayType(
						displayTypeId,
						referenceValueId
					);
					String tableName = null;
					if(displayTypeId == DisplayType.TableDir) {
						tableName = columnName.replace("_ID", "");
					} else if(displayTypeId == DisplayType.Table || displayTypeId == DisplayType.Search) {
						if(referenceValueId <= 0) {
							tableName = columnName.replace("_ID", "");
						} else {
							MRefTable referenceTable = MRefTable.getById(Env.getCtx(), referenceValueId);
							tableName = MTable.getTableName(Env.getCtx(), referenceTable.getAD_Table_ID());
						}
					}
					if (!Util.isEmpty(tableName, true)) {
						int id = NumberManager.getIntegerFromObject(value);
						MTable referenceTable = MTable.get(Env.getCtx(), tableName);
						PO referenceEntity = referenceTable.getPO(id, null);
						if(referenceEntity != null) {
							displayValue = referenceEntity.getDisplayValue();
						}
					}
				}
				Value.Builder builderDisplayValue = TextManager.getProtoValueFromString(displayValue);
				rowValues.putFields(
					LookupUtil.DISPLAY_COLUMN_KEY + "_" + columnName,
					builderDisplayValue.build()
				);
			}

			// to add client uuid by record
			if (columnName.equals(I_AD_Element.COLUMNNAME_AD_Client_ID)) {
				final int clientId = NumberManager.getIntegerFromObject(value);
				MClient clientEntity = MClient.get(
					entity.getCtx(),
					clientId
				);
				if (clientEntity != null) {
					final String clientUuid = clientEntity.get_UUID();
					Value.Builder valueUuidBuilder = TextManager.getProtoValueFromString(
						clientUuid
					);
					rowValues.putFields(
						LookupUtil.getUuidColumnName(
							I_AD_Element.COLUMNNAME_AD_Client_ID
						),
						valueUuidBuilder.build()
					);
				}
			} else if (columnName.equals(I_AD_ChangeLog.COLUMNNAME_Record_ID)) {
				if (entity.get_ColumnIndex(I_AD_Table.COLUMNNAME_AD_Table_ID) >= 0) {
					MTable tableRow = MTable.get(entity.getCtx(), entity.get_ValueAsInt(I_AD_Table.COLUMNNAME_AD_Table_ID));
					if (tableRow != null) {
						PO entityRow = tableRow.getPO(entity.get_ValueAsInt(I_AD_ChangeLog.COLUMNNAME_Record_ID), null);
						if (entityRow != null) {
							final String recordIdDisplayValue = entityRow.getDisplayValue();
							Value.Builder recordIdDisplayBuilder = TextManager.getProtoValueFromString(
								recordIdDisplayValue
							);
							rowValues.putFields(
								LookupUtil.getDisplayColumnName(
									I_AD_ChangeLog.COLUMNNAME_Record_ID
								),
								recordIdDisplayBuilder.build()
							);
						}

					}
				}
			}
		}

		// TODO: Temporary Workaround
		rowValues = ContextTemporaryWorkaround.setContextAsUnknowColumn(
			poInfo.getTableName(),
			rowValues
		);

		entityBuilder.setValues(rowValues);
		//
		return entityBuilder;
	}

	/**
	 * Convert Document Action
	 * @param value
	 * @param name
	 * @param description
	 * @return
	 */
	public static DocumentAction.Builder convertDocumentAction(String value, String name, String description) {
		return DocumentAction.newBuilder()
			.setValue(
				TextManager.getValidString(value)
			)
			.setName(
				TextManager.getValidString(name)
			)
			.setDescription(
				TextManager.getValidString(description)
			)
		;
	}

	/**
	 * Convert Document Status
	 * @param value
	 * @param name
	 * @param description
	 * @return
	 */
	public static DocumentStatus.Builder convertDocumentStatus(String value, String name, String description) {
		return DocumentStatus.newBuilder()
			.setValue(
				TextManager.getValidString(value)
			)
			.setName(
				TextManager.getValidString(name)
			)
			.setDescription(
				TextManager.getValidString(description)
			)
		;
	}



	/**
	 * Get Converted Amount based on Order currency
	 * @param order
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvertedAmount(MOrder order, PO payment, BigDecimal amount) {
		if(payment.get_ValueAsInt("C_Currency_ID") == order.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(
			payment.getCtx(),
			amount,
			payment.get_ValueAsInt("C_Currency_ID"),
			order.getC_Currency_ID(),
			order.getDateAcct(),
			payment.get_ValueAsInt("C_ConversionType_ID"),
			payment.getAD_Client_ID(),
			payment.getAD_Org_ID()
		);
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}

	/**
	 * Get Converted Amount based on Order currency
	 * @param pos
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvertedAmount(MPOS pos, MPayment payment, BigDecimal amount) {
		MPriceList priceList = MPriceList.get(pos.getCtx(), pos.getM_PriceList_ID(), null);
		if(payment.getC_Currency_ID() == priceList.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(
			pos.getCtx(),
			amount,
			payment.getC_Currency_ID(),
			priceList.getC_Currency_ID(),
			payment.getDateAcct(),
			payment.getC_ConversionType_ID(),
			payment.getAD_Client_ID(),
			payment.getAD_Org_ID()
		);
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}
	
	/**
	 * Get Converted Amount based on Order currency
	 * @param order
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvertedAmount(MOrder order, MPayment payment, BigDecimal amount) {
		if(payment.getC_Currency_ID() == order.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(payment.getCtx(), amount, payment.getC_Currency_ID(), order.getC_Currency_ID(), payment.getDateAcct(), payment.getC_ConversionType_ID(), payment.getAD_Client_ID(), payment.getAD_Org_ID());
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}

	/**
	 * Get Display Currency rate from Sales Order
	 * @param order
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getDisplayConversionRateFromOrder(MOrder order) {
		MPOS pos = MPOS.get(order.getCtx(), order.getC_POS_ID());
		if(order.getC_Currency_ID() == pos.get_ValueAsInt("DisplayCurrency_ID")
				|| pos.get_ValueAsInt("DisplayCurrency_ID") <= 0) {
			return Env.ONE;
		}
		BigDecimal conversionRate = MConversionRate.getRate(order.getC_Currency_ID(), pos.get_ValueAsInt("DisplayCurrency_ID"), order.getDateAcct(), order.getC_ConversionType_ID(), order.getAD_Client_ID(), order.getAD_Org_ID());
		//	
		return Optional.ofNullable(conversionRate).orElse(Env.ZERO);
	}


	/**
	 * Get Order conversion rate for payment
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getOrderConversionRateFromPaymentReference(PO paymentReference) {
		if(paymentReference.get_ValueAsInt("C_Order_ID") <= 0) {
			return Env.ONE;
		}
		MOrder order = new MOrder(Env.getCtx(), paymentReference.get_ValueAsInt("C_Order_ID"), null);
		if(paymentReference.get_ValueAsInt("C_Currency_ID") == order.getC_Currency_ID()) {
			return Env.ONE;
		}
		
		Timestamp conversionDate = TimeManager.getTimestampFromString(
			paymentReference.get_ValueAsString("PayDate")
		);
		BigDecimal conversionRate = MConversionRate.getRate(
			paymentReference.get_ValueAsInt("C_Currency_ID"),
			order.getC_Currency_ID(),
			conversionDate,
			paymentReference.get_ValueAsInt("C_ConversionType_ID"),
			paymentReference.getAD_Client_ID(),
			paymentReference.getAD_Org_ID()
		);
		//	
		return Optional.ofNullable(conversionRate).orElse(Env.ZERO);
	}
	
	/**
	 * Validate conversion
	 * @param order
	 * @param currencyId
	 * @param conversionTypeId
	 * @param transactionDate
	 */
	public static void validateConversion(MOrder order, int currencyId, int conversionTypeId, Timestamp transactionDate) {
		if(currencyId == order.getC_Currency_ID()) {
			return;
		}
		int convertionRateId = MConversionRate.getConversionRateId(currencyId, 
				order.getC_Currency_ID(), 
				transactionDate, 
				conversionTypeId, 
				order.getAD_Client_ID(), 
				order.getAD_Org_ID());
		if(convertionRateId == -1) {
			String error = MConversionRate.getErrorMessage(order.getCtx(), 
					"ErrorConvertingDocumentCurrencyToBaseCurrency", 
					currencyId, 
					order.getC_Currency_ID(), 
					conversionTypeId, 
					transactionDate, 
					null);
			throw new AdempiereException(error);
		}
	}

	/**
	 * Convert customer bank account
	 * @param customerBankAccount
	 * @return
	 * @return CustomerBankAccount.Builder
	 */
	public static CustomerBankAccount.Builder convertCustomerBankAccount(MBPBankAccount customerBankAccount) {
		CustomerBankAccount.Builder builder = CustomerBankAccount.newBuilder();
		if (customerBankAccount == null) {
			return builder;
		}
		builder.setId(
				customerBankAccount.getC_BP_BankAccount_ID()
			)
			.setCity(
				TextManager.getValidString(
					customerBankAccount.getA_City()
				)
			)
			.setCountry(
				TextManager.getValidString(
					customerBankAccount.getA_Country()
				)
			)
			.setEmail(
				TextManager.getValidString(
					customerBankAccount.getA_EMail()
				)
			)
			.setDriverLicense(
				TextManager.getValidString(
					customerBankAccount.getA_Ident_DL()
				)
			)
			.setSocialSecurityNumber(
				TextManager.getValidString(
					customerBankAccount.getA_Ident_SSN()
				)
			)
			.setName(
				TextManager.getValidString(
					customerBankAccount.getA_Name()
				)
			)
			.setState(
				TextManager.getValidString(
					customerBankAccount.getA_State()
				)
			)
			.setStreet(
				TextManager.getValidString(
					customerBankAccount.getA_Street()
				)
			)
			.setZip(
				TextManager.getValidString(
					customerBankAccount.getA_Zip()
				)
			)
			.setBankAccountType(
				TextManager.getValidString(
					customerBankAccount.getBankAccountType()
				)
			)
		;
		if(customerBankAccount.getC_Bank_ID() > 0) {
			builder.setBankId(customerBankAccount.getC_Bank_ID());
		}
		MBPartner customer = MBPartner.get(Env.getCtx(), customerBankAccount.getC_BPartner_ID());
		builder.setCustomerId(
				customer.getC_BPartner_ID()
			)
			.setAddressVerified(
				TextManager.getValidString(
					customerBankAccount.getR_AvsAddr()
				)
			)
			.setZipVerified(
				TextManager.getValidString(
					customerBankAccount.getR_AvsZip()
				)
			)
			.setRoutingNo(
				TextManager.getValidString(
					customerBankAccount.getRoutingNo()
				)
			)
			.setAccountNo(
				TextManager.getValidString(
					customerBankAccount.getAccountNo()
				)
			)
			.setIban(
				TextManager.getValidString(
					customerBankAccount.getIBAN()
				)
			)
		;
		return builder;
	}

	/**
	 * Convert Order from entity
	 * @param shipment
	 * @return
	 */
	public static  Shipment.Builder convertShipment(MInOut shipment) {
		Shipment.Builder builder = Shipment.newBuilder();
		if(shipment == null) {
			return builder;
		}
		MRefList reference = MRefList.get(Env.getCtx(), MOrder.DOCSTATUS_AD_REFERENCE_ID, shipment.getDocStatus(), null);
		MOrder order = (MOrder) shipment.getC_Order();
		//	Convert
		return builder
			.setOrderId(
				order.getC_Order_ID()
			)
			.setId(
				shipment.getM_InOut_ID()
			)
			.setDocumentType(
				CoreFunctionalityConvert.convertDocumentType(
					shipment.getC_DocType_ID()
				)
			)
			.setDocumentNo(
				TextManager.getValidString(
					shipment.getDocumentNo()
				)
			)
			.setSalesRepresentative(
				CoreFunctionalityConvert.convertSalesRepresentative(
					MUser.get(Env.getCtx(), shipment.getSalesRep_ID())
				)
			)
			.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(
					TextManager.getValidString(
						shipment.getDocStatus()
					),
					TextManager.getValidString(
						org.spin.service.grpc.util.base.RecordUtil.getTranslation(
							reference,
							I_AD_Ref_List.COLUMNNAME_Name
						)
					),
					TextManager.getValidString(
						org.spin.service.grpc.util.base.RecordUtil.getTranslation(
							reference,
							I_AD_Ref_List.COLUMNNAME_Description
						)
					)
				)
			)
			.setWarehouse(
				CoreFunctionalityConvert.convertWarehouse(
					shipment.getM_Warehouse_ID()
				)
			)
			.setMovementDate(
				ValueManager.getProtoTimestampFromTimestamp(
					shipment.getMovementDate()
				)
			)
			.setIsProcessed(
				shipment.isProcessed()
			)
			.setIsProcessing(
				shipment.isProcessing()
			)
			.setIsManualDocument(
				shipment.get_ValueAsBoolean(
					ColumnsAdded.COLUMNNAME_IsManualDocument
				)
			)
		;
	}



	/**
	 * Convert key layout from id
	 * @param keyLayoutId
	 * @return
	 */
	public static KeyLayout.Builder convertKeyLayout(int keyLayoutId) {
		KeyLayout.Builder builder = KeyLayout.newBuilder();
		if(keyLayoutId <= 0) {
			return builder;
		}
		return convertKeyLayout(MPOSKeyLayout.get(Env.getCtx(), keyLayoutId));
	}

	/**
	 * Convert Key Layout from PO
	 * @param keyLayout
	 * @return
	 */
	public static KeyLayout.Builder convertKeyLayout(MPOSKeyLayout keyLayout) {
		KeyLayout.Builder builder = KeyLayout.newBuilder();
		if (keyLayout == null) {
			return builder;
		}
		builder
			.setId(
				keyLayout.getC_POSKeyLayout_ID()
			)
			.setName(
				TextManager.getValidString(
					keyLayout.getName()
				)
			)
			.setDescription(
				TextManager.getValidString(
					keyLayout.getDescription()
				)
			)
			.setHelp(
				TextManager.getValidString(
					keyLayout.getHelp()
				)
			)
			.setLayoutType(
				TextManager.getValidString(
					keyLayout.getPOSKeyLayoutType()
				)
			)
			.setColumns(
				keyLayout.getColumns()
			)
		;
		//	TODO: Color
		//	Add keys
		Arrays.asList(keyLayout.getKeys(false)).stream()
			.filter(key -> key.isActive())
			.forEach(key -> {
				builder.addKeys(
					convertKey(key)
				);
			});
		return builder;
	}

	/**
	 * Convet key for layout
	 * @param key
	 * @return
	 */
	public static Key.Builder convertKey(MPOSKey key) {
		if (key == null) {
			return Key.newBuilder();
		}
		String productValue = null;
		if(key.getM_Product_ID() > 0) {
			productValue = MProduct.get(Env.getCtx(), key.getM_Product_ID()).getValue();
		}
		return Key.newBuilder()
			.setId(
				key.getC_POSKeyLayout_ID()
			)
			.setName(
				TextManager.getValidString(
					key.getName()
				)
			)
			//	TODO: Color
			.setSequence(
				key.getSeqNo()
			)
			.setSpanX(
				key.getSpanX()
			)
			.setSpanY(
				key.getSpanY()
			)
			.setSubKeyLayoutId(
				key.getSubKeyLayout_ID()
			)
			.setQuantity(
				NumberManager.getBigDecimalToString(
					Optional.ofNullable(key.getQty()).orElse(Env.ZERO)
				)
			)
			.setProductValue(
				TextManager.getValidString(productValue)
			)
			.setResourceReference(
				FileManagement.convertResourceReference(
					FileUtil.getResourceFromImageId(
						key.getAD_Image_ID()
					)
				)
			)
		;
	}

}
