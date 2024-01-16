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

package org.spin.dashboarding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.adempiere.apps.graph.GraphColumn;
import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.X_AD_TreeNodeMM;
import org.adempiere.model.MBrowse;
import org.compiere.model.MChart;
import org.compiere.model.MColorSchema;
import org.compiere.model.MForm;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MWindow;
import org.compiere.model.M_Element;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.MPrintColor;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.dashboarding.ChartData;
import org.spin.backend.grpc.dashboarding.ColorSchema;
import org.spin.backend.grpc.dashboarding.Favorite;
import org.spin.backend.grpc.dashboarding.WindowDashboard;
import org.spin.backend.grpc.dashboarding.WindowDashboardParameter;
import org.spin.backend.grpc.dictionary.Reference;
import org.spin.base.util.ReferenceUtil;
import org.spin.grpc.service.dictionary.DictionaryConvertUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * This class was created for add all convert methods for POS form
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com , https://github.com/EdwinBetanc0urt
 */
public class DashboardingConvertUtil {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(DashboardingConvertUtil.class);

	/**
	 * Get color as hex
	 * @param printColorId
	 * @return
	 */
	public static String getColorAsHex(int printColorId) {
		if (printColorId <= 0) {
			return "";
		}
		MPrintColor printColor = MPrintColor.get(
			Env.getCtx(),
			printColorId
		);
		int color = 0;
		try {
			color = Integer.parseInt(printColor.getCode());
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		}
		return ValueManager.validateNull(
			String.format("#%06X", (0xFFFFFF & color))
		);
	}



	@SuppressWarnings("unchecked")
	public static void addCollectionParameters(Object objectColelction, List<Object> parameters) {
		if (objectColelction instanceof Collection) {
			try {
				Collection<Object> collection = (Collection<Object>) objectColelction;
				// for-each loop
				for (Object rangeValue : collection) {
					parameters.add(rangeValue);
				}
			}
			catch (Exception e) {
			}
		}
	}


	public static Favorite.Builder convertFavorite(X_AD_TreeNodeMM treeNodeMenu) {
		Favorite.Builder builder = Favorite.newBuilder();
		if (treeNodeMenu == null || treeNodeMenu.getNode_ID() <= 0) {
			return builder;
		}
		Properties context = Env.getCtx();

		String menuName = "";
		String menuDescription = "";
		MMenu menu = MMenu.getFromId(context, treeNodeMenu.getNode_ID());
		builder.setMenuId(menu.getAD_Menu_ID());
		String action = MMenu.ACTION_Window;
		if (!menu.isCentrallyMaintained()) {
			menuName = menu.getName();
			menuDescription = menu.getDescription();
			if (!Env.isBaseLanguage(context, "")) {
				String translation = menu.get_Translation("Name");
				if (!Util.isEmpty(translation, true)) {
					menuName = translation;
				}
				translation = menu.get_Translation("Description");
				if (!Util.isEmpty(translation, true)) {
					menuDescription = translation;
				}
			}
		}
		//	Supported actions
		if (!Util.isEmpty(menu.getAction(), true)) {
			action = menu.getAction();
			int referenceId = 0;
			if (menu.getAction().equals(MMenu.ACTION_Form) && menu.getAD_Form_ID() > 0) {
				MForm form = new MForm(context, menu.getAD_Form_ID(), null);
				referenceId = form.getAD_Form_ID();
				if (menu.isCentrallyMaintained()) {
					menuName = form.getName();
					menuDescription = form.getDescription();
					if (!Env.isBaseLanguage(context, "")) {
						String translation = form.get_Translation("Name");
						if (!Util.isEmpty(translation, true)) {
							menuName = translation;
						}
						translation = form.get_Translation("Description");
						if (!Util.isEmpty(translation, true)) {
							menuDescription = translation;
						}
					}
				}
			} else if (menu.getAction().equals(MMenu.ACTION_Window) && menu.getAD_Window_ID() > 0) {
				MWindow window = new MWindow(context, menu.getAD_Window_ID(), null);
				referenceId = window.getAD_Window_ID();
				if (menu.isCentrallyMaintained()) {
					menuName = window.getName();
					menuDescription = window.getDescription();
					if (!Env.isBaseLanguage(context, "")) {
						String translation = window.get_Translation("Name");
						if (!Util.isEmpty(translation, true)) {
							menuName = translation;
						}
						translation = window.get_Translation("Description");
						if (!Util.isEmpty(translation, true)) {
							menuDescription = translation;
						}
					}
				}
			} else if ((menu.getAction().equals(MMenu.ACTION_Process)
				|| menu.getAction().equals(MMenu.ACTION_Report)) && menu.getAD_Process_ID() > 0) {
				MProcess process = MProcess.get(context, menu.getAD_Process_ID());
				referenceId = process.getAD_Process_ID();
				if (menu.isCentrallyMaintained()) {
					menuName = process.getName();
					menuDescription = process.getDescription();
					if (!Env.isBaseLanguage(context, "")) {
						String translation = process.get_Translation("Name");
						if (!Util.isEmpty(translation, true)) {
							menuName = translation;
						}
						translation = process.get_Translation("Description");
						if (!Util.isEmpty(translation, true)) {
							menuDescription = translation;
						}
					}
				}
			} else if (menu.getAction().equals(MMenu.ACTION_SmartBrowse) && menu.getAD_Browse_ID() > 0) {
				MBrowse smartBrowser = MBrowse.get(context, menu.getAD_Browse_ID());
				referenceId = smartBrowser.getAD_Browse_ID();
				if (menu.isCentrallyMaintained()) {
					menuName = smartBrowser.getName();
					menuDescription = smartBrowser.getDescription();
					if (!Env.isBaseLanguage(context, "")) {
						String translation = smartBrowser.get_Translation("Name");
						if (!Util.isEmpty(translation, true)) {
							menuName = translation;
						}
						translation = smartBrowser.get_Translation("Description");
						if (!Util.isEmpty(translation, true)) {
							menuDescription = translation;
						}
					}
				}
			}
			builder.setReferenceId(referenceId);
			builder.setAction(ValueManager.validateNull(action));
		}
		//	Set name and description
		builder.setMenuName(ValueManager.validateNull(menuName));
		builder.setMenuDescription(ValueManager.validateNull(menuDescription));
		return builder;
	}


	public static ColorSchema.Builder convertColorSchema1(MColorSchema colorSchema) {
		ColorSchema.Builder builder = ColorSchema.newBuilder();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return builder;
		}
		builder.setPercent(
				String.valueOf(
					colorSchema.getMark1Percent()
				)
			)
			.setColor(
				getColorAsHex(
					colorSchema.getAD_PrintColor1_ID()
				)
			)
		;
		return builder;
	}

	public static ColorSchema.Builder convertColorSchema2(MColorSchema colorSchema) {
		ColorSchema.Builder builder = ColorSchema.newBuilder();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return builder;
		}
		builder.setPercent(
				String.valueOf(
					colorSchema.getMark2Percent()
				)
			)
			.setColor(
				getColorAsHex(
					colorSchema.getAD_PrintColor2_ID()
				)
			)
		;
		return builder;
	}

	public static ColorSchema.Builder convertColorSchema3(MColorSchema colorSchema) {
		ColorSchema.Builder builder = ColorSchema.newBuilder();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return builder;
		}
		builder.setPercent(
				String.valueOf(
					colorSchema.getMark3Percent()
				)
			)
			.setColor(
				getColorAsHex(
					colorSchema.getAD_PrintColor3_ID()
				)
			)
		;
		return builder;
	}

	public static ColorSchema.Builder convertColorSchema4(MColorSchema colorSchema) {
		ColorSchema.Builder builder = ColorSchema.newBuilder();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return builder;
		}
		builder.setPercent(
				String.valueOf(
					colorSchema.getMark4Percent()
				)
			)
			.setColor(
				getColorAsHex(
					colorSchema.getAD_PrintColor4_ID()
				)
			)
		;
		return builder;
	}


	public static List<ColorSchema> convertColorSchemasList(MColorSchema colorSchema) {
		List<ColorSchema> colorSchemasList = new ArrayList<ColorSchema>();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return colorSchemasList;
		}

		// First mark
		colorSchemasList.add(
			convertColorSchema1(colorSchema).build()
		);

		// Second Mark
		colorSchemasList.add(
			convertColorSchema2(colorSchema).build()
		);

		// Third Mark
		colorSchemasList.add(
			convertColorSchema3(colorSchema).build()
		);

		// Four Mark
		colorSchemasList.add(
			convertColorSchema4(colorSchema).build()
		);

		return colorSchemasList;
	}


	public static ChartData.Builder convertChartData(GraphColumn grapColumn) {
		ChartData.Builder builder = ChartData.newBuilder();
		if (grapColumn == null) {
			return builder;
		}
		
		builder.setName(
				ValueManager.validateNull(
					grapColumn.getLabel()
				)
			)
			.setValue(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromDouble(
						grapColumn.getValue()
					)
				)
			)
		;

		return builder;
	}


	public static WindowDashboardParameter.Builder convertWindowDashboardParameter(PO chartParameter) {
		WindowDashboardParameter.Builder builder = WindowDashboardParameter.newBuilder();
		if (chartParameter == null || chartParameter.get_ID() <= 0) {
			return builder;
		}
		builder.setId(chartParameter.get_ID())
			.setName(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_Name)
				)
			)
			.setDescription(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_Description)
				)
			)
			.setHelp(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_Help)
				)
			)
			.setSequence(
				chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_SeqNo)
			)
			.setColumnName(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ColumnName)
				)
			)
			.setColumnSql(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString("ColumnSQL")
				)
			)
			.setElementId(
				chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Element_ID)
			)
			.setFieldId(
				chartParameter.get_ValueAsInt(I_AD_Field.COLUMNNAME_AD_Field_ID)
			)
			.setIsMandatory(
				chartParameter.get_ValueAsBoolean(I_AD_Process_Para.COLUMNNAME_IsMandatory)
			)
			.setIsRange(
				chartParameter.get_ValueAsBoolean(I_AD_Process_Para.COLUMNNAME_IsRange)
			)
			.setDefaultValue(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_DefaultValue)
				)
			)
			.setDisplayType(
				chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Reference_ID)
			)
			.setVFormat(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_VFormat)
				)
			)
			.setValueMax(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ValueMax)
				)
			)
			.setValueMin(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ValueMin)
				)
			)
			.setDisplayLogic(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_DisplayLogic)
				)
			)
			.setReadOnlyLogic(
				ValueManager.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ReadOnlyLogic)
				)
			)
		;


		int displayTypeId = chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Reference_ID);
		if (ReferenceUtil.validateReference(displayTypeId)) {
			//	Reference Value
			int referenceValueId = chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Reference_Value_ID);
			//	Validation Code
			int validationRuleId = chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Val_Rule_ID);

			String columnName = chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ColumnName);
			if (chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Element_ID) > 0) {
				M_Element element = new M_Element(
					Env.getCtx(), chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Element_ID), null
				);
				if (element != null && element.getAD_Element_ID() > 0) {
					columnName = element.getColumnName();
				}
			}

			MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
				displayTypeId, referenceValueId, columnName, validationRuleId
			);
			if (info != null) {
				Reference.Builder referenceBuilder = DictionaryConvertUtil.convertReference(Env.getCtx(), info);
				builder.setReference(referenceBuilder.build());
			}
		}
		return builder;
	}


	public static WindowDashboard.Builder convertWindowDashboard(MChart chartDefinition) {
		WindowDashboard.Builder builder = WindowDashboard.newBuilder();
		if (chartDefinition == null || chartDefinition.getAD_Chart_ID() <= 0) {
			return builder;
		}

		builder = WindowDashboard.newBuilder()
			.setId(chartDefinition.getAD_Chart_ID())
			.setName(
				ValueManager.validateNull(chartDefinition.getName())
			)
			.setDescription(
				ValueManager.validateNull(chartDefinition.getDescription())
			)
			.setDashboardType("chart")
			.setChartType(
				ValueManager.validateNull(chartDefinition.getChartType())
			)
			.setIsCollapsible(true)
			.setIsOpenByDefault(true)
		;

		return builder;
	}

	public static List<String> getContextColumnsByWindowChart(int windowChartAllocationId) {
		List<String> contextColumnsList = new ArrayList<String>();

		new Query(
				Env.getCtx(),
				"ECA50_WindowChartParameter",
				"ECA50_WindowChart_ID = ? AND ECA50_IsEnableSelection = 'N'",
				null
			)
			.setParameters(windowChartAllocationId)
			.setOnlyActiveRecords(true)
			.<PO>list()
			.forEach(windowChartParameter -> {
				String contextColumn = ValueManager.validateNull(
					windowChartParameter.get_ValueAsString(I_AD_Column.COLUMNNAME_ColumnName)
				);
				contextColumnsList.add(contextColumn);
			})
		;

		return contextColumnsList;
	}

}
