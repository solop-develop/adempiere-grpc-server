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
package org.spin.grpc.service.dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Tab;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MProcessParaCustom;
import org.compiere.model.MReportView;
import org.compiere.model.MValRule;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.backend.grpc.dictionary.DependentField;
import org.spin.backend.grpc.dictionary.Field;
import org.spin.backend.grpc.dictionary.Process;
import org.spin.backend.grpc.dictionary.Reference;
import org.spin.backend.grpc.dictionary.ReportExportType;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceUtil;
import org.spin.dictionary.custom.ProcessParaCustomUtil;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.ASPUtil;
import org.spin.util.AbstractExportFormat;
import org.spin.util.ReportExportHandler;

public class ProcessConvertUtil {
	
	/**
	 * Convert process to builder
	 * @param process
	 * @return
	 */
	public static Process.Builder convertProcess(Properties context, MProcess process, boolean withParams) {
		if (process == null) {
			return Process.newBuilder();
		}
		process = ASPUtil.getInstance(context).getProcess(process.getAD_Process_ID());
		List<MProcessPara> parametersList = ASPUtil.getInstance(context).getProcessParameters(process.getAD_Process_ID());

		Process.Builder builder = Process.newBuilder()
			.setId(process.getAD_Process_ID())
			.setUuid(
				ValueManager.validateNull(process.getUUID())
			)
			.setValue(
				ValueManager.validateNull(process.getValue())
			)
			.setName(
				ValueManager.validateNull(process.getName())
			)
			.setDescription(
				ValueManager.validateNull(process.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(process.getHelp())
			)
			.setAccessLevel(Integer.parseInt(process.getAccessLevel()))
			.setIsDirectPrint(process.isDirectPrint())
			.setIsReport(process.isReport())
			.setIsActive(process.isActive())
			.setIsHaveParameres(
				parametersList != null && parametersList.size() > 0
			)
		;

		if (process.getAD_Browse_ID() > 0) {
			builder.setBrowserId(process.getAD_Browse_ID());
		}
		if (process.getAD_Form_ID() > 0) {
			builder.setFormId(process.getAD_Form_ID());
		}
		if (process.getAD_Workflow_ID() > 0) {
			builder.setWorkflowId(process.getAD_Workflow_ID());
		}
		//	Report Types
		if(process.isReport()) {
			MReportView reportView = null;
			if(process.getAD_ReportView_ID() > 0) {
				reportView = MReportView.get(context, process.getAD_ReportView_ID());
			}
			ReportExportHandler exportHandler = new ReportExportHandler(Env.getCtx(), reportView);
			for(AbstractExportFormat reportType : exportHandler.getExportFormatList()) {
				ReportExportType.Builder reportExportType = ReportExportType.newBuilder()
					.setName(
						ValueManager.validateNull(reportType.getName())
					)
					.setDescription(
						ValueManager.validateNull(reportType.getName())
					)
					.setType(
						ValueManager.validateNull(reportType.getExtension())
					)
				;
				builder.addReportExportTypes(reportExportType.build());
			}
		}
		//	For parameters
		if(withParams && parametersList != null && parametersList.size() > 0) {
			String language = context.getProperty(Env.LANGUAGE);
			for(MProcessPara parameter : parametersList) {
				// TODO: Remove conditional with fix the issue https://github.com/solop-develop/backend/issues/28
				if(!Language.isBaseLanguage(language)) {
					//	Name
					String value = parameter.get_Translation(I_AD_Tab.COLUMNNAME_Name, language);
					if(!Util.isEmpty(value)) {
						parameter.set_ValueOfColumn(I_AD_Tab.COLUMNNAME_Name, value);
					}
					//	Description
					value = parameter.get_Translation(I_AD_Tab.COLUMNNAME_Description, language);
					if(!Util.isEmpty(value)) {
						parameter.set_ValueOfColumn(I_AD_Tab.COLUMNNAME_Description, value);
					}
					//	Help
					value = parameter.get_Translation(I_AD_Tab.COLUMNNAME_Help, language);
					if(!Util.isEmpty(value)) {
						parameter.set_ValueOfColumn(I_AD_Tab.COLUMNNAME_Help, value);
					}
				}
				
				Field.Builder fieldBuilder = ProcessConvertUtil.convertProcessParameter(
					context,
					parameter
				);
				builder.addParameters(fieldBuilder.build());
			}
		}

		//	Add to recent Item
		if (process.isReport()) {
			org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
				MMenu.ACTION_Report,
				process.getAD_Process_ID()
			);
		} else {
			org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
				MMenu.ACTION_Process,
				process.getAD_Process_ID()
			);
		}

		return builder;
	}


	public static List<DependentField> generateDependentProcessParameters(MProcessPara processParameter) {
		List<DependentField> depenentFieldsList = new ArrayList<>();

		String parentColumnName = processParameter.getColumnName();

		MProcess process = ASPUtil.getInstance().getProcess(processParameter.getAD_Process_ID());
		List<MProcessPara> parametersList = ASPUtil.getInstance().getProcessParameters(processParameter.getAD_Process_ID());

		parametersList.stream()
			.filter(currentParameter -> {
				if (!currentParameter.isActive()) {
					return false;
				}
				// Display Logic
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentParameter.getDisplayLogic())) {
					return true;
				}
				// Default Value of Column
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentParameter.getDefaultValue())) {
					return true;
				}
				// ReadOnly Logic
				if (ContextManager.isUseParentColumnOnContext(parentColumnName, currentParameter.getReadOnlyLogic())) {
					return true;
				}
				// Dynamic Validation
				if (currentParameter.getAD_Val_Rule_ID() > 0) {
					MValRule validationRule = MValRule.get(Env.getCtx(), currentParameter.getAD_Val_Rule_ID());
					if (ContextManager.isUseParentColumnOnContext(parentColumnName, validationRule.getCode())) {
						return true;
					}
				}
				return false;
			})
			.forEach(currentParameter -> {
				DependentField.Builder builder = DependentField.newBuilder()
					.setContainerId(
						process.getAD_Process_ID()
					)
					.setContainerUuid(
						ValueManager.validateNull(
							process.getUUID()
						)
					)
					.setContainerName(
						ValueManager.validateNull(
							process.getName()
						)
					)
					.setId(
						currentParameter.getAD_Process_Para_ID()
					)
					.setUuid(
						ValueManager.validateNull(
							currentParameter.getUUID()
						)
					)
					.setColumnName(
						ValueManager.validateNull(
							currentParameter.getColumnName()
						)
					)
				;

				depenentFieldsList.add(builder.build());
			});

		return depenentFieldsList;
	}


	/**
	 * Convert Process Parameter
	 * @param processParameter
	 * @return
	 */
	public static Field.Builder convertProcessParameter(Properties context, MProcessPara processParameter) {
		if (processParameter == null) {
			return Field.newBuilder();
		}
		//	Convert
		Field.Builder builder = Field.newBuilder()
			.setId(processParameter.getAD_Process_Para_ID())
			.setUuid(
				ValueManager.validateNull(processParameter.getUUID())
			)
			.setName(
				ValueManager.validateNull(processParameter.getName())
			)
			.setDescription(
				ValueManager.validateNull(processParameter.getDescription())
			)
			.setHelp(
				ValueManager.validateNull(processParameter.getHelp())
			)
			.setColumnName(
				ValueManager.validateNull(processParameter.getColumnName())
			)
			.setElementName(
				ValueManager.validateNull(processParameter.getColumnName())
			)
			.setDefaultValue(
				ValueManager.validateNull(processParameter.getDefaultValue())
			)
			.setDefaultValueTo(
				ValueManager.validateNull(processParameter.getDefaultValue2())
			)
			.setDisplayLogic(
				ValueManager.validateNull(processParameter.getDisplayLogic())
			)
			.setDisplayType(processParameter.getAD_Reference_ID())
			.setIsDisplayed(true)
			.setIsInfoOnly(processParameter.isInfoOnly())
			.setIsMandatory(processParameter.isMandatory())
			.setIsRange(processParameter.isRange())
			.setReadOnlyLogic(
				ValueManager.validateNull(processParameter.getReadOnlyLogic())
			)
			.setSequence(processParameter.getSeqNo())
			.setValueMax(
				ValueManager.validateNull(processParameter.getValueMax())
			)
			.setValueMin(
				ValueManager.validateNull(processParameter.getValueMin())
			)
			.setVFormat(
				ValueManager.validateNull(processParameter.getVFormat())
			)
			.setFieldLength(processParameter.getFieldLength())
			.setIsActive(processParameter.isActive())
			.addAllContextColumnNames(
				ContextManager.getContextColumnNames(
					Optional.ofNullable(processParameter.getDefaultValue()).orElse("")
					+ Optional.ofNullable(processParameter.getDefaultValue2()).orElse("")
				)
			)
		;
		//	
		int displayTypeId = processParameter.getAD_Reference_ID();
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = processParameter.getAD_Reference_Value_ID();
			//	Validation Code
			int validationRuleId = processParameter.getAD_Val_Rule_ID();

			String columnName = processParameter.getColumnName();
			if (processParameter.getAD_Element_ID() > 0) {
				columnName = processParameter.getAD_Element().getColumnName();
			}

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, columnName, validationRuleId
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(context, info);
				builder.setReference(referenceBuilder.build());
			}
		}

		MProcessParaCustom processParaCustom = ProcessParaCustomUtil.getProcessParaCustom(processParameter.getAD_Process_Para_ID());
		if (processParaCustom != null && processParaCustom.isActive()) {
			// ASP default displayed field as panel
			if (processParaCustom.get_ColumnIndex(org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME) >= 0) {
				builder.setIsDisplayedAsPanel(
					ValueManager.validateNull(
						processParaCustom.get_ValueAsString(
							org.spin.dictionary.util.DictionaryUtil.IS_DISPLAYED_AS_PANEL_COLUMN_NAME
						)
					)
				);
			}
		}

		List<DependentField> dependentProcessParameters = ProcessConvertUtil.generateDependentProcessParameters(
			processParameter
		);
		builder.addAllDependentFields(dependentProcessParameters);

		return builder;
	}

}
