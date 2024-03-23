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
import org.adempiere.core.domains.models.I_AD_Window;
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
import org.compiere.model.MWindow;
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
import org.spin.backend.grpc.dictionary.ZoomWindow;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceUtil;
import org.spin.model.MADContextInfo;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.ASPUtil;

public class DictionaryConvertUtil {

	/**
	 * TODO: Remove conditional with fix the issue https://github.com/solop-develop/adempiere-grpc-server/issues/28
	 * Translated Name, Description and Help columns
	 * @param entity
	 * @return
	 */
	public static PO translateEntity(PO entity) {
		String language = entity.getCtx().getProperty(Env.LANGUAGE);
		boolean isBaseLanguage = Env.isBaseLanguage(Env.getCtx(), "");
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
				ValueManager.validateNull(
					entity.get_UUID()
				)
			)
			.setId(
				entity.get_ID()
			)
			.setName(
				ValueManager.validateNull(
					entity.get_Translation(I_AD_Element.COLUMNNAME_Name)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					entity.get_Translation(I_AD_Element.COLUMNNAME_Description)
				)
			)
			.setHelp(
				ValueManager.validateNull(
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
				ValueManager.validateNull(
					info.TableName
				)
			)
			.setKeyColumnName(
				ValueManager.validateNull(
					info.KeyColumn
				)
			)
			.setDisplayColumnName(
				ValueManager.validateNull(
					info.DisplayColumn
				)
			)
			.addAllContextColumnNames(
				contextColumnsList
			)
		;

		// reference value
		if (info.AD_Reference_Value_ID > 0) {
			builder.setId(info.AD_Reference_Value_ID);
		}

		//	Window Reference
		if (info.ZoomWindow > 0) {
			builder.addZoomWindows(
				convertZoomWindow(context, info.ZoomWindow).build()
			);
		}
		// window reference Purchase Order
		if (info.ZoomWindowPO > 0) {
			builder.addZoomWindows(
				convertZoomWindow(context, info.ZoomWindowPO).build()
			);
		}
		//	Return
		return builder;
	}



	/**
	 * Convert Zoom Window from ID
	 * @param windowId
	 * @return
	 */
	public static ZoomWindow.Builder convertZoomWindow(Properties context, int windowId) {
		MWindow window = ASPUtil.getInstance(context).getWindow(windowId); // new MWindow(context, windowId, null);
		//	Get translation
		String name = null;
		String description = null;
		String language = Env.getAD_Language(context);
		if (!Util.isEmpty(language, true)) {
			name = window.get_Translation(I_AD_Window.COLUMNNAME_Name, language);
			description = window.get_Translation(I_AD_Window.COLUMNNAME_Description, language);
		}
		//	Validate for default
		if (Util.isEmpty(name, true)) {
			name = window.getName();
		}
		if (Util.isEmpty(description, true)) {
			description = window.getDescription();
		}
		//	Return
		return ZoomWindow.newBuilder()
			.setId(window.getAD_Window_ID())
			.setName(ValueManager.validateNull(name))
			.setDescription(ValueManager.validateNull(description))
			.setIsSalesTransaction(window.isSOTrx())
		;
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
		MMessage message = MMessage.get(context, contextInfoValue.getAD_Message_ID());
		//	Get translation
		String msgText = null;
		String msgTip = null;
		String language = Env.getAD_Language(context);
		if(!Util.isEmpty(language)) {
			msgText = message.get_Translation(I_AD_Message.COLUMNNAME_MsgText, language);
			msgTip = message.get_Translation(I_AD_Message.COLUMNNAME_MsgTip, language);
		}
		//	Validate for default
		if(Util.isEmpty(msgText)) {
			msgText = message.getMsgText();
		}
		if(Util.isEmpty(msgTip)) {
			msgTip = message.getMsgTip();
		}
		//	Add message text
		MessageText.Builder messageText = MessageText.newBuilder();
		if (message != null) {
			messageText
				.setId(message.getAD_Message_ID())
				.setValue(
					ValueManager.validateNull(message.getValue())
				)
				.setMessageText(
					ValueManager.validateNull(msgText)
				)
				.setMessageTip(
					ValueManager.validateNull(msgTip)
				)
			;
		}
		builder = ContextInfo.newBuilder()
			.setId(contextInfoValue.getAD_ContextInfo_ID())
			.setUuid(
				ValueManager.validateNull(contextInfoValue.getUUID())
			)
			.setName(
				ValueManager.validateNull(contextInfoValue.getName())
			)
			.setDescription(
				ValueManager.validateNull(contextInfoValue.getDescription())
			)
			.setMessageText(messageText.build())
			.setSqlStatement(
				ValueManager.validateNull(contextInfoValue.getSQLStatement())
			)
		;
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
		DictionaryConvertUtil.translateEntity(form);

		//	
		builder.setId(form.getAD_Form_ID())
			.setUuid(
				ValueManager.validateNull(form.getUUID()))
			.setName(
				ValueManager.validateNull(
					ValueManager.getTranslation(form, MForm.COLUMNNAME_Name)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					ValueManager.getTranslation(form, MForm.COLUMNNAME_Description)
				)
			)
			.setHelp(
				ValueManager.validateNull(
					ValueManager.getTranslation(form, MForm.COLUMNNAME_Help)
				)
			)
			.setIsActive(form.isActive())
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
				ValueManager.validateNull(
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
		DictionaryConvertUtil.translateEntity(column);

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
			.setId(column.getAD_Column_ID())
			.setUuid(
				ValueManager.validateNull(column.getUUID())
			)
			.setName(
				ValueManager.validateNull(column.getName())
			)
			.setDescription(
				ValueManager.validateNull(column.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(column.getHelp())
			)
			.setCallout(
				ValueManager.validateNull(column.getCallout())
			)
			.setColumnName(
				ValueManager.validateNull(column.getColumnName())
			)
			.setElementName(
				ValueManager.validateNull(element.getColumnName())
			)
			.setColumnSql(
				ValueManager.validateNull(column.getColumnSQL())
			)
			.setDefaultValue(
				ValueManager.validateNull(defaultValue)
			)
			.setDisplayType(displayTypeId)
			.setFormatPattern(
				ValueManager.validateNull(column.getFormatPattern())
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
				ValueManager.validateNull(column.getMandatoryLogic())
			)
			.setReadOnlyLogic(
				ValueManager.validateNull(column.getReadOnlyLogic())
			)
			.setSequence(column.getSeqNo())
			.setValueMax(
				ValueManager.validateNull(column.getValueMax())
			)
			.setValueMin(
				ValueManager.validateNull(column.getValueMin())
			)
			.setFieldLength(column.getFieldLength())
			.setIsActive(column.isActive())
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

		MTable table = MTable.get(Env.getCtx(), column.getAD_Table_ID());
		List<MColumn> columnsList = table.getColumnsAsList(false);

		columnsList.stream()
			.filter(currentColumn -> {
				if(!currentColumn.isActive()) {
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
					MValRule validationRule = MValRule.get(Env.getCtx(), currentColumn.getAD_Val_Rule_ID());
					if (ContextManager.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
						return true;
					}
				}
				return false;
			})
			.forEach(currentColumn -> {
				DependentField.Builder builder = DependentField.newBuilder()
					.setContainerId(
						table.getAD_Table_ID()
					)
					.setContainerUuid(
						ValueManager.validateNull(
							table.getUUID()
						)
					)
					.setContainerName(
						table.getTableName()
					)
					.setId(
						currentColumn.getAD_Column_ID()
					)
					.setUuid(
						ValueManager.validateNull(
							currentColumn.getUUID()
						)
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
		DictionaryConvertUtil.translateEntity(element);

		//	Display Type
		int displayTypeId = element.getAD_Reference_ID();
		if(element.getAD_Reference_ID() > 0) {
			displayTypeId = element.getAD_Reference_ID();
		}
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(element.getAD_Element_ID())
			.setUuid(
				ValueManager.validateNull(element.getUUID())
			)
			.setName(
				ValueManager.validateNull(
					ValueManager.getTranslation(element, M_Element.COLUMNNAME_Name)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					ValueManager.getTranslation(element, M_Element.COLUMNNAME_Description)
				)
			)
			.setHelp(
				ValueManager.validateNull(
					ValueManager.getTranslation(element, M_Element.COLUMNNAME_Help)))
			.setColumnName(
				ValueManager.validateNull(element.getColumnName())
			)
			.setElementName(
				ValueManager.validateNull(element.getColumnName())
			)
			.setDisplayType(displayTypeId)
			.setFieldLength(element.getFieldLength())
			.setIsActive(element.isActive())
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
		MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
		int displayTypeId = column.getAD_Reference_ID();
		if (field.getAD_Reference_ID() > 0) {
			displayTypeId = field.getAD_Reference_ID();
		}
		String name = ValueManager.validateNull(
			Msg.translate(
				Env.getCtx(),
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
				ValueManager.validateNull(
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
				ValueManager.validateNull(
					Msg.translate(
						Env.getCtx(),
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
