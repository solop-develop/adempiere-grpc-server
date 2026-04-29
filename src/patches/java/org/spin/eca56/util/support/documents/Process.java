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
 * Copyright (C) 2003-2023 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.spin.eca56.util.support.documents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.model.MBrowse;
import org.compiere.model.MForm;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MReportView;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;
import org.spin.eca56.util.support.DictionaryDocument;
import org.spin.util.AbstractExportFormat;
import org.spin.util.ReportExportHandler;

/**
 * 	the document class for Process senders
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class Process extends DictionaryDocument {

	//	Some default documents key
	public static final String KEY = "new";
	public static final String CHANNEL = "process";
	
	@Override
	public String getKey() {
		return KEY;
	}

	private Map<String, Object> parseDictionaryEntity(PO entity) {
		Map<String, Object> documentEntity = new HashMap<>();
		documentEntity.put("internal_id", entity.get_ID());
		documentEntity.put("id", entity.get_UUID());
		documentEntity.put("uuid", entity.get_UUID());
		documentEntity.put("name", entity.get_Translation(I_AD_Element.COLUMNNAME_Name, getLanguage()));
		documentEntity.put("description", entity.get_Translation(I_AD_Element.COLUMNNAME_Description, getLanguage()));
		documentEntity.put("help", entity.get_Translation(I_AD_Element.COLUMNNAME_Help, getLanguage()));
		return documentEntity;
	}


	@Override
	public DictionaryDocument withEntity(PO entity) {
		MProcess process = (MProcess) entity;
		Map<String, Object> documentDetail = new HashMap<>();
		documentDetail.put("internal_id", process.getAD_Process_ID());
		documentDetail.put("id", process.getUUID());
		documentDetail.put("uuid", process.getUUID());
		documentDetail.put("code", process.getValue());
		documentDetail.put("name", process.get_Translation(I_AD_Process.COLUMNNAME_Name, getLanguage()));
		documentDetail.put("description", process.get_Translation(I_AD_Process.COLUMNNAME_Description, getLanguage()));
		documentDetail.put("help", process.get_Translation(I_AD_Process.COLUMNNAME_Help, getLanguage()));
		documentDetail.put("is_active", process.isActive());
		documentDetail.put("show_help", process.getShowHelp());
		documentDetail.put("is_beta_functionality", process.isBetaFunctionality());

		boolean isMultiSelection = false;
		if (process.get_ColumnIndex("SP003_IsMultiSelection") >= 0) {
			isMultiSelection = process.get_ValueAsBoolean("SP003_IsMultiSelection");
		}
		documentDetail.put("is_multi_selection", isMultiSelection);

		// Report
		documentDetail.put("is_report", process.isReport());
		if(process.isReport()) {
			documentDetail.put("is_process_before_launch", !Util.isEmpty(process.getClassname(), true));
			documentDetail.put("is_jasper_report", !Util.isEmpty(process.getJasperReport(), true));
			documentDetail.put("report_view_id", process.getAD_ReportView_ID());
			documentDetail.put("print_format_id", process.getAD_PrintFormat_ID());
			MReportView reportView = null;
			if(process.getAD_ReportView_ID() > 0) {
				reportView = MReportView.get(entity.getCtx(), process.getAD_ReportView_ID());
			}
			ReportExportHandler exportHandler = new ReportExportHandler(entity.getCtx(), reportView);
			List<Map<String, String>> reportExportsList = new ArrayList<>();
			for(AbstractExportFormat reportType : exportHandler.getExportFormatList()) {
				Map<String, String> reportExportReference = new HashMap<>();
				reportExportReference.put("name", reportType.getName());
				reportExportReference.put("type", reportType.getExtension());
				reportExportsList.add(reportExportReference);
			}
			documentDetail.put("report_export_types", reportExportsList);
		} else {
			// Linked to Process
			documentDetail.put("browser_id", process.getAD_Browse_ID());
			documentDetail.put("form_id", process.getAD_Form_ID());
			documentDetail.put("workflow_id", process.getAD_Workflow_ID());
			if (process.getAD_Browse_ID() > 0) {
				MBrowse browse = MBrowse.get(process.getCtx(), process.getAD_Browse_ID());
				documentDetail.put("browser", parseDictionaryEntity(browse));
			} else if (process.getAD_Form_ID() > 0) {
				MForm form = new MForm(process.getCtx(), process.getAD_Form_ID(), null);
				documentDetail.put("form", parseDictionaryEntity(form));
			} else if (process.getAD_Workflow_ID() > 0) {
				MWorkflow workflow = MWorkflow.get(process.getCtx(), process.getAD_Workflow_ID());
				documentDetail.put("workflow", parseDictionaryEntity(workflow));
			}
		}

		// Process Parameters
		List<MProcessPara> parameters = new Query(
			process.getCtx(),
			I_AD_Process_Para.Table_Name,
			I_AD_Process_Para.COLUMNNAME_AD_Process_ID + "=?",
			null
		)
			.setParameters(process.getAD_Process_ID())
			.setOnlyActiveRecords(true)
			.setOrderBy(I_AD_Process_Para.COLUMNNAME_SeqNo)
			.list()
		;

		boolean hasParameters = parameters != null && !parameters.isEmpty();
		documentDetail.put("has_parameters", hasParameters);

		// Parallel + thread-safe collector. Process parameters are usually few
		// (~10 per process), so the speedup here is modest, but parseProcessParameter
		// triggers ReferenceUtil + DependenceUtil queries which are non-trivial.
		// Using map().collect() also makes the code consistent with Window/Browser
		// and avoids the unsafe forEach + ArrayList.add pattern. Encounter order
		// is preserved (parameters already come ordered by SeqNo from the Query).
		List<Map<String, Object>> parametersDetail = hasParameters
			? parameters.parallelStream()
				.map(this::parseProcessParameter)
				.collect(Collectors.toList())
			: new ArrayList<>()
		;
		documentDetail.put("parameters", parametersDetail);
		putDocument(documentDetail);
		return this;
	}

	Map<String, Object> parseProcessParameter(MProcessPara parameter) {
		Map<String, Object> detail = new HashMap<>();

		detail.put("internal_id", parameter.getAD_Process_Para_ID());
		detail.put("id", parameter.getUUID());
		detail.put("uuid", parameter.getUUID());
		detail.put("column_name", parameter.getColumnName());
		detail.put("name", parameter.get_Translation(I_AD_Process_Para.COLUMNNAME_Name, getLanguage()));
		detail.put("description", parameter.get_Translation(I_AD_Process_Para.COLUMNNAME_Description, getLanguage()));
		detail.put("help", parameter.get_Translation(I_AD_Process_Para.COLUMNNAME_Help, getLanguage()));
		detail.put("is_active", parameter.isActive());
		detail.put("display_type", parameter.getAD_Reference_ID());

		//	Value Properties
		detail.put("is_range", parameter.isRange());
		detail.put("default_value", parameter.getDefaultValue());
		detail.put("default_value_to", parameter.getDefaultValue2());
		detail.put("field_length", parameter.getFieldLength());
		detail.put("value_format", parameter.getVFormat());
		detail.put("min_value", parameter.getValueMin());
		detail.put("max_value", parameter.getValueMax());

		//	Display Properties
		detail.put("display_logic", parameter.getDisplayLogic());
		detail.put("sequence", parameter.getSeqNo());
		//	Custom display
		detail.put("is_displayed_as_panel", parameter.isActive() ? "Y" : "N");

		//	Mandatory Properties
		detail.put("is_mandatory", parameter.isMandatory());

		//	Editable Properties
		detail.put("read_only_logic", parameter.getReadOnlyLogic());
		detail.put("is_info_only", parameter.isInfoOnly());

		// External Info
		int referenceValueId = parameter.getAD_Reference_Value_ID();

		// overwrite display type `Button` to `List`, example `PaymentRule` or `Posted`
		int displayTypeId = ReferenceUtil.overwriteDisplayType(
			parameter.getAD_Reference_ID(),
			referenceValueId
		);
		if (ReferenceUtil.isLookupReference(displayTypeId)) {
			//	Validation Code
			int validationRuleId = parameter.getAD_Val_Rule_ID();

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, parameter.getColumnName(), validationRuleId
			);
			if (info != null) {
				ReferenceValues referenceValues = ReferenceValues.newInstance(info);
				Map<String, Object> referenceDetail = new HashMap<>();
				referenceDetail.put("table_name", referenceValues.getTableName());
				referenceDetail.put("access_level", referenceValues.getAccessLevel());
				referenceDetail.put("reference_id", referenceValues.getDisplayTypeId());
				referenceDetail.put("reference_value_id", referenceValues.getReferenceValueId());
				referenceDetail.put("context_column_names", referenceValues.getContextColumns());
				detail.put("reference", referenceDetail);
			} else {
				// detail.put("display_type", DisplayType.String);
			}
		}

		detail.put("context_column_names", ReferenceUtil.getContextColumnNames(
				Optional.ofNullable(parameter.getDefaultValue()).orElse("")
				+ Optional.ofNullable(parameter.getDefaultValue2()).orElse("")
			)
		);
		List<Map<String, Object>> dependentFieldsList = DependenceUtil.generateDependentProcessParameters(parameter);
		detail.put("dependent_fields", dependentFieldsList);
		return detail;
	}

	private Process() {
		super();
	}

	/**
	 * Default instance
	 * @return
	 */
	public static Process newInstance() {
		return new Process();
	}

	@Override
	public String getChannel() {
		return CHANNEL;
	}
}
