/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/

package org.spin.grpc.service.dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Message;
import org.adempiere.model.MBrowse;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MForm;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMenu;
import org.compiere.model.MMessage;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.model.M_Element;
import org.compiere.model.PO;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;
import org.spin.backend.grpc.dictionary.ContextInfo;
import org.spin.backend.grpc.dictionary.DependentField;
import org.spin.backend.grpc.dictionary.DictionaryEntity;
import org.spin.backend.grpc.dictionary.Field;
import org.spin.backend.grpc.dictionary.Form;
import org.spin.backend.grpc.dictionary.MessageText;
import org.spin.backend.grpc.dictionary.Process;
import org.spin.backend.grpc.dictionary.Reference;
import org.spin.backend.grpc.dictionary.SearchColumn;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceUtil;
import org.spin.model.MADContextInfo;
import org.spin.service.grpc.util.value.StringManager;

public class DictionaryConvertUtil {

	/**
	 * TODO: Remove conditional with fix the issue https://github.com/solop-develop/adempiere-grpc-server/issues/28
	 * Translated Name, Description and Help columns
	 * @param entity
	 * @return
	 */
	public static PO translateEntity(PO entity) {
		return translateEntity(Env.getCtx(), entity);
	}
	public static PO translateEntity(Properties context, PO entity) {
		String language = context.getProperty(Env.LANGUAGE);
		boolean isBaseLanguage = Env.isBaseLanguage(context, "");
		if(!isBaseLanguage) {
			//	Name
			String name = entity.get_Translation(I_AD_Element.COLUMNNAME_Name, language);
			if(!Util.isEmpty(name, true)) {
				entity.set_ValueOfColumn(I_AD_Element.COLUMNNAME_Name, name);
			}
			//	Description
			String description = entity.get_Translation(I_AD_Element.COLUMNNAME_Description, language);
			if(!Util.isEmpty(description, true)) {
				entity.set_ValueOfColumn(I_AD_Element.COLUMNNAME_Description, description);
			}
			//	Help
			String help = entity.get_Translation(I_AD_Element.COLUMNNAME_Help, language);
			if(!Util.isEmpty(help, true)) {
				entity.set_ValueOfColumn(I_AD_Element.COLUMNNAME_Help, help);
			}
		}
		return entity;
	}



	public static DictionaryEntity.Builder getDictionaryEntity(MForm form) {
		return getDictionaryEntity(
			(PO) form
		);
	}
	public static DictionaryEntity.Builder getDictionaryEntity(MBrowse browse) {
		return getDictionaryEntity(
			(PO) browse
		);
	}
	public static DictionaryEntity.Builder getDictionaryEntity(MWorkflow workflow) {
		return getDictionaryEntity(
			(PO) workflow
		);
	}
	public static DictionaryEntity.Builder getDictionaryEntity(PO entity) {
		DictionaryEntity.Builder builder = DictionaryEntity.newBuilder();
		if (entity == null) {
			return builder;
		}
		builder
			.setUuid(
				StringManager.getValidString(
					entity.get_UUID()
				)
			)
			.setId(
				StringManager.getValidString(
					entity.get_UUID()
				)
			)
			.setInternalId(
				entity.get_ID()
			)
			.setName(
				StringManager.getValidString(
					entity.get_Translation(I_AD_Element.COLUMNNAME_Name)
				)
			)
			.setDescription(
				StringManager.getValidString(
					entity.get_Translation(I_AD_Element.COLUMNNAME_Description)
				)
			)
			.setHelp(
				StringManager.getValidString(
					entity.get_Translation(I_AD_Element.COLUMNNAME_Help)
				)
			)
		;
		return builder;
	}



	/**
	 * Convert Reference to builder
	 * @param info
	 * @return
	 */
	public static Reference.Builder convertReference(Properties context, MLookupInfo info) {
		Reference.Builder builder = Reference.newBuilder();
		if (info == null) {
			return builder;
		}

		List<String> contextColumnsList = ContextManager.getContextColumnNames(
			Optional.ofNullable(info.QueryDirect).orElse("")
			+ Optional.ofNullable(info.Query).orElse("")
			+ Optional.ofNullable(info.ValidationCode).orElse("")
		);
		builder.setTableName(
				StringManager.getValidString(
					info.TableName
				)
			)
			.setReferenceId(
				info.DisplayType
			)
			.setReferenceValueId(
				info.AD_Reference_Value_ID
			)
			.addAllContextColumnNames(
				contextColumnsList
			)
		;

		//	Return
		return builder;
	}



	/**
	 * Convert Context Info to builder
	 * @param contextInfoId
	 * @return
	 */
	public static ContextInfo.Builder convertContextInfo(Properties context, int contextInfoId) {
		ContextInfo.Builder builder = ContextInfo.newBuilder();
		if(contextInfoId <= 0) {
			return builder;
		}
		MADContextInfo contextInfoValue = MADContextInfo.getById(context, contextInfoId);
		if (contextInfoValue == null) {
			return builder;
		}

		builder = ContextInfo.newBuilder()
			.setId(
				contextInfoValue.getAD_ContextInfo_ID()
			)
			.setUuid(
				StringManager.getValidString(
					contextInfoValue.getUUID()
				)
			)
			.setName(
				StringManager.getValidString(
					contextInfoValue.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					contextInfoValue.getDescription()
				)
			)
			.setSqlStatement(
				StringManager.getValidString(
					contextInfoValue.getSQLStatement()
				)
			)
		;

		MMessage message = MMessage.get(context, contextInfoValue.getAD_Message_ID());
		//	Add message text
		if (message != null && message.getAD_Message_ID() > 0) {
			MessageText.Builder messageText = MessageText.newBuilder()
				.setId(
					message.getAD_Message_ID()
				)
				.setValue(
					StringManager.getValidString(
						message.getValue()
					)
				)
				.setMessageText(
					StringManager.getValidString(
						Msg.parseTranslation(
							context,
							message.get_Translation(I_AD_Message.COLUMNNAME_MsgText)
						)
					)
				)
				.setMessageTip(
					StringManager.getValidString(
						Msg.parseTranslation(
							context,
							message.get_Translation(I_AD_Message.COLUMNNAME_MsgTip)
						)
					)
				)
			;
			builder.setMessageText(
				messageText.build()
			);
		}
		return builder;
	}


	/**
	 * Convert Window from Window Model
	 * @param form
	 * @return
	 */
	public static Form.Builder convertForm(Properties context, MForm form) {
		Form.Builder builder = Form.newBuilder();
		if (form == null) {
			return builder;
		}

		// TODO: Remove with fix the issue https://github.com/solop-develop/adempiere-grpc-server/issues/28
		DictionaryConvertUtil.translateEntity(context, form);

		//	
		builder.setId(
				StringManager.getValidString(
					form.getUUID()
				)
			)
			.setUuid(
				StringManager.getValidString(
					form.getUUID()
				)
			)
			.setInternalId(
				form.getAD_Form_ID()
			)
			.setName(
				StringManager.getValidString(
					org.spin.service.grpc.util.base.RecordUtil.getTranslation(
						form,
						MForm.COLUMNNAME_Name
					)
				)
			)
			.setDescription(
				StringManager.getValidString(
					org.spin.service.grpc.util.base.RecordUtil.getTranslation(
						form,
						MForm.COLUMNNAME_Description
					)
				)
			)
			.setHelp(
				StringManager.getValidString(
					org.spin.service.grpc.util.base.RecordUtil.getTranslation(
						form,
						MForm.COLUMNNAME_Help
					)
				)
			)
			.setIsActive(
				form.isActive()
			)
			.setIsBetaFunctionality(
				form.isBetaFunctionality()
			)
		;
		//	File Name
		String fileName = form.getClassname();
		if(!Util.isEmpty(fileName)) {
			int endIndex = fileName.lastIndexOf(".");
			int beginIndex = fileName.lastIndexOf("/");
			if(beginIndex == -1) {
				beginIndex = fileName.lastIndexOf(".");
				endIndex = -1;
			}
			if(beginIndex == -1) {
				beginIndex = 0;
			} else {
				beginIndex++;
			}
			if(endIndex == -1) {
				endIndex = fileName.length();
			}
			//	Set
			builder.setFileName(
				StringManager.getValidString(
					fileName.substring(beginIndex, endIndex))
				)
			;
		}
		//	Add to recent Item
		org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Form,
			form.getAD_Form_ID()
		);
		//	return
		return builder;
	}

	/**
	 * Convert field to builder
	 * @param column
	 * @param language
	 * @return
	 */
	public static Field.Builder convertFieldByColumn(Properties context, MColumn column) {
		if (column == null) {
			return Field.newBuilder();
		}

		// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/28
		DictionaryConvertUtil.translateEntity(context, column);

		String defaultValue = column.getDefaultValue();
		if(Util.isEmpty(defaultValue)) {
			defaultValue = column.getDefaultValue();
		}
		//	Display Type
		int displayTypeId = column.getAD_Reference_ID();
		// element
		M_Element element = new M_Element(context, column.getAD_Element_ID(), null);
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(
				StringManager.getValidString(
					column.getUUID()
				))
			.setUuid(
				StringManager.getValidString(
					column.getUUID()
				)
			)
			.setInternalId(
				column.getAD_Column_ID()
			)
			.setName(
				StringManager.getValidString(
					column.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					column.getDescription()
				)
			)
			.setHelp(
				StringManager.getValidString(
					column.getHelp()
				)
			)
			.setCallout(
				StringManager.getValidString(
					column.getCallout()
				)
			)
			.setColumnName(
				StringManager.getValidString(
					column.getColumnName()
				)
			)
			.setElementName(
				StringManager.getValidString(
					element.getColumnName()
				)
			)
			.setColumnSql(
				StringManager.getValidString(
					column.getColumnSQL()
				)
			)
			.setDefaultValue(
				StringManager.getValidString(
					defaultValue
				)
			)
			.setDisplayType(displayTypeId)
			.setFormatPattern(
				StringManager.getValidString(
					column.getFormatPattern()
				)
			)
			.setIdentifierSequence(column.getSeqNo())
			.setIsAllowCopy(column.isAllowCopy())
			.setIsAllowLogging(column.isAllowLogging())
			.setIsAlwaysUpdateable(column.isAlwaysUpdateable())
			.setIsEncrypted(column.isEncrypted())
			.setIsIdentifier(column.isIdentifier())
			.setIsKey(column.isKey())
			.setIsMandatory(column.isMandatory())
			.setIsParent(column.isParent())
			.setIsRange(column.isRange())
			.setIsSelectionColumn(column.isSelectionColumn())
			.setIsTranslated(column.isTranslated())
			.setIsUpdateable(column.isUpdateable())
			.setMandatoryLogic(
				StringManager.getValidString(
					column.getMandatoryLogic()
				)
			)
			.setReadOnlyLogic(
				StringManager.getValidString(
					column.getReadOnlyLogic()
				)
			)
			.setSequence(column.getSeqNo())
			.setValueMax(
				StringManager.getValidString(
					column.getValueMax()
				)
			)
			.setValueMin(
				StringManager.getValidString(
					column.getValueMin()
				)
			)
			.setFieldLength(column.getFieldLength())
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
					Optional.ofNullable(column.getDefaultValue()).orElse("")
				)
			)
		;
		//	Process
		if(column.getAD_Process_ID() > 0) {
			MProcess process = MProcess.get(context, column.getAD_Process_ID());
			Process.Builder processBuilder = ProcessConvertUtil.convertProcess(
				context,
				process,
				false
			);
			builder.setProcess(processBuilder.build());
		}
		//	
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = column.getAD_Reference_Value_ID();

			//	Validation Code
			int validationRuleId = column.getAD_Val_Rule_ID();

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, column.getColumnName(), validationRuleId
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(context, info);
				builder.setReference(referenceBuilder.build());
			} else {
				builder.setDisplayType(DisplayType.String);
			}
		}

		List<DependentField> depenentFieldsList = generateDependentColumns(column);
		builder.addAllDependentFields(depenentFieldsList);

		return builder;
	}

	public static List<DependentField> generateDependentColumns(MColumn column) {
		List<DependentField> depenentFieldsList = new ArrayList<>();
		if (column == null) {
			return depenentFieldsList;
		}

		String parentColumnName = column.getColumnName();

		MTable table = MTable.get(column.getCtx(), column.getAD_Table_ID());
		List<MColumn> columnsList = table.getColumnsAsList(false);
		if (columnsList == null || columnsList.isEmpty()) {
			return depenentFieldsList;
		}

		columnsList.stream()
			.filter(currentColumn -> {
				if(currentColumn == null || !currentColumn.isActive()) {
					return false;
				}
				// Default Value
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentColumn.getDefaultValue())) {
					return true;
				}
				// ReadOnly Logic
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentColumn.getReadOnlyLogic())) {
					return true;
				}
				// Mandatory Logic
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentColumn.getMandatoryLogic())) {
					return true;
				}
				// Dynamic Validation
				if (currentColumn.getAD_Val_Rule_ID() > 0) {
					MValRule validationRule = MValRule.get(currentColumn.getCtx(), currentColumn.getAD_Val_Rule_ID());
					if (ContextManager.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
						return true;
					}
				}
				return false;
			})
			.forEach(currentColumn -> {
				DependentField.Builder builder = DependentField.newBuilder()
					.setParentId(
						table.getAD_Table_ID()
					)
					.setParentUuid(
						StringManager.getValidString(
							table.getUUID()
						)
					)
					.setParentName(
						table.getTableName()
					)
					.setId(
						StringManager.getValidString(
							currentColumn.getUUID()
						)
					)
					.setUuid(
						StringManager.getValidString(
							currentColumn.getUUID()
						)
					)
					.setInternalId(
						currentColumn.getAD_Column_ID()
					)
					.setColumnName(
						currentColumn.getColumnName()
					)
				;

				depenentFieldsList.add(builder.build());
			});

		return depenentFieldsList;
	}


	/**
	 * Convert field to builder
	 * @param element
	 * @return
	 */
	public static Field.Builder convertFieldByElemnt(Properties context, M_Element element) {
		if (element == null) {
			return Field.newBuilder();
		}

		// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/28
		DictionaryConvertUtil.translateEntity(context, element);

		//	Display Type
		int displayTypeId = element.getAD_Reference_ID();
		if(element.getAD_Reference_ID() > 0) {
			displayTypeId = element.getAD_Reference_ID();
		}
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(
				StringManager.getValidString(
					element.getUUID()
				))
			.setUuid(
				StringManager.getValidString(
					element.getUUID()
				)
			)
			.setInternalId(
				element.getAD_Element_ID()
			)
			.setName(
				StringManager.getValidString(
					org.spin.service.grpc.util.base.RecordUtil.getTranslation(
						element,
						M_Element.COLUMNNAME_Name
					)
				)
			)
			.setDescription(
				StringManager.getValidString(
					org.spin.service.grpc.util.base.RecordUtil.getTranslation(
						element,
						M_Element.COLUMNNAME_Description
					)
				)
			)
			.setHelp(
				StringManager.getValidString(
					org.spin.service.grpc.util.base.RecordUtil.getTranslation(
						element,
						M_Element.COLUMNNAME_Help
					)
				)
			)
			.setColumnName(
				StringManager.getValidString(
					element.getColumnName()
				)
			)
			.setElementName(
				StringManager.getValidString(
					element.getColumnName()
				)
			)
			.setDisplayType(displayTypeId)
			.setFieldLength(element.getFieldLength())
		;
		//	
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = element.getAD_Reference_Value_ID();
			if(element.getAD_Reference_Value_ID() > 0) {
				referenceValueId = element.getAD_Reference_Value_ID();
			}

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, element.getColumnName(), 0
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(
					context,
					info
				);
				builder.setReference(referenceBuilder.build());
			} else {
				builder.setDisplayType(DisplayType.String);
			}
		}
		return builder;
	}


	public static SearchColumn.Builder convertSearchColumnByFieldId(int fieldId) {
		SearchColumn.Builder builder = SearchColumn.newBuilder();
		if (fieldId <= 0) {
			return builder;
		}
		MField field = new MField(Env.getCtx(), fieldId, null);
		if (field == null || field.getAD_Field_ID() <= 0) {
			return builder;
		}
		MColumn column = MColumn.get(field.getCtx(), field.getAD_Column_ID());
		int displayTypeId = column.getAD_Reference_ID();
		if (field.getAD_Reference_ID() > 0) {
			displayTypeId = field.getAD_Reference_ID();
		}
		String name = StringManager.getValidString(
			Msg.translate(
				field.getCtx(),
				column.getColumnName()
			)
		);
		if (!Util.isEmpty(name, true)) {
			String colHeader = name;
			int index = name.indexOf('&');
			if (index != -1) {
				colHeader = name.substring(0, index) + name.substring(index + 1); 
			}
			name = colHeader;
		}
		builder.setColumnName(
				column.getColumnName()
			)
			.setName(
				StringManager.getValidString(
					name
				)
			)
			.setSequence(
				field.getSeqNo()
			)
			.setDisplayType(displayTypeId)
		;
		return builder;
	}


	public static SearchColumn.Builder convertSearchColumnByColumnId(int columnId) {
		SearchColumn.Builder builder = SearchColumn.newBuilder();
		if (columnId <= 0) {
			return builder;
		}
		MColumn column = MColumn.get(Env.getCtx(), columnId);
		builder.setColumnName(
				column.getColumnName()
			)
			.setName(
				StringManager.getValidString(
					Msg.translate(
						column.getCtx(),
						column.getColumnName()
					)
				)
			)
			.setSequence(
				column.getSeqNo()
			)
			.setDisplayType(
				column.getAD_Reference_ID()
			)
		;
		return builder;
	}

}
