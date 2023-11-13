/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
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
package org.spin.log;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.core.domains.models.I_AD_PInstance_Log;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.X_AD_PInstance_Log;
import org.compiere.model.MChangeLog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MWindow;
import org.compiere.model.M_Element;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.NamePair;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ProcesInstanceParameter;
import org.spin.backend.grpc.common.ProcessInfoLog;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.ReportOutput;
import org.spin.backend.grpc.logs.ChangeLog;
import org.spin.backend.grpc.logs.EntityEventType;
import org.spin.backend.grpc.logs.EntityLog;
import org.spin.backend.grpc.logs.ListEntityLogsResponse;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * This class was created for add all convert methods for Logs service
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class LogsConvertUtil {
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(LogsConvertUtil.class);


	/** Date Time Format		*/
	private static SimpleDateFormat	dateTimeFormat = DisplayType.getDateFormat
		(DisplayType.DateTime, Env.getLanguage(Env.getCtx()));
	/** Date Format			*/
	private static SimpleDateFormat	dateFormat = DisplayType.getDateFormat
		(DisplayType.DateTime, Env.getLanguage(Env.getCtx()));
	/** Number Format		*/
	private static DecimalFormat		numberFormat = DisplayType.getNumberFormat
		(DisplayType.Number, Env.getLanguage(Env.getCtx()));
	/** Amount Format		*/
	private static DecimalFormat		amountFormat = DisplayType.getNumberFormat
		(DisplayType.Amount, Env.getLanguage(Env.getCtx()));
	/** Number Format		*/
	private static DecimalFormat		intFormat = DisplayType.getNumberFormat
		(DisplayType.Integer, Env.getLanguage(Env.getCtx()));



	/**
	 * Convert a change log for a set of changes to builder
	 * @param recordLog
	 * @return
	 */
	public static EntityLog.Builder convertRecordLogHeader(MChangeLog recordLog) {
		MTable table = MTable.get(recordLog.getCtx(), recordLog.getAD_Table_ID());
		EntityLog.Builder builder = EntityLog.newBuilder();
		builder.setLogId(recordLog.getAD_ChangeLog_ID())
			.setId(recordLog.getRecord_ID())
			.setTableName(
				ValueManager.validateNull(table.getTableName())
			)
		;
		
		String displayedName = table.get_Translation(I_AD_Table.COLUMNNAME_Name);
		if (table.getAD_Window_ID() > 0) {
			MWindow window = MWindow.get(Env.getCtx(), table.getAD_Window_ID());
			displayedName = window.get_Translation(I_AD_Window.COLUMNNAME_Name);
			builder.setWindowId(window.getAD_Window_ID());
		}
		builder.setDisplayedName(
			ValueManager.validateNull(displayedName)
		);

		// created by
		MUser user = MUser.get(recordLog.getCtx(), recordLog.getCreatedBy());
		builder.setCreatedBy(user.getAD_User_ID())
			.setCreatedByName(
				ValueManager.validateNull(user.getName())
			)
		;

		// updated by
		user = MUser.get(recordLog.getCtx(), recordLog.getUpdatedBy());
		builder.setUpdatedBy(user.getAD_User_ID())
			.setUpdatedByName(
				ValueManager.validateNull(user.getName())
			)
		;

		builder.setSessionId(recordLog.getAD_Session_ID())
			.setTransactionName(
				ValueManager.validateNull(recordLog.getTrxName())
			)
			.setLogDate(
				ValueManager.getTimestampFromDate(recordLog.getCreated())
			)
		;
		if(recordLog.getEventChangeLog().endsWith(MChangeLog.EVENTCHANGELOG_Insert)) {
			builder.setEventType(EntityEventType.INSERT);
		} else if(recordLog.getEventChangeLog().endsWith(MChangeLog.EVENTCHANGELOG_Update)) {
			builder.setEventType(EntityEventType.UPDATE);
		} else if(recordLog.getEventChangeLog().endsWith(MChangeLog.EVENTCHANGELOG_Delete)) {
			builder.setEventType(EntityEventType.DELETE);
		}
		//	Return
		return builder;
	}
	
	/**
	 * Convert PO class from change log  list to builder
	 * @param recordLog
	 * @return
	 */
	public static ListEntityLogsResponse.Builder convertRecordLog(List<MChangeLog> recordLogList) {
		Map<Integer, EntityLog.Builder> indexMap = new HashMap<Integer, EntityLog.Builder>();
		recordLogList.stream().filter(recordLog -> !indexMap.containsKey(recordLog.getAD_ChangeLog_ID())).forEach(recordLog -> {
			indexMap.put(recordLog.getAD_ChangeLog_ID(), convertRecordLogHeader(recordLog));
		});
		//	convert changes
		recordLogList.forEach(recordLog -> {
			ChangeLog.Builder changeLog = convertChangeLog(recordLog);
			EntityLog.Builder recordLogBuilder = indexMap.get(recordLog.getAD_ChangeLog_ID());
			recordLogBuilder.addChangeLogs(changeLog);
			indexMap.put(recordLog.getAD_ChangeLog_ID(), recordLogBuilder);
		});
		ListEntityLogsResponse.Builder builder = ListEntityLogsResponse.newBuilder();
		indexMap.values().stream().forEach(recordLog -> builder.addEntityLogs(recordLog));
		return builder;
	}

	/**
	 * Convert PO class from change log to builder
	 * @param recordLog
	 * @return
	 */
	public static ChangeLog.Builder convertChangeLog(MChangeLog recordLog) {
		ChangeLog.Builder builder = ChangeLog.newBuilder();
		MColumn column = MColumn.get(recordLog.getCtx(), recordLog.getAD_Column_ID());
		builder.setColumnName(
			ValueManager.validateNull(column.getColumnName())
		);
		String displayColumnName = column.getName();
		if(column.getColumnName().equals("ProcessedOn")) {
			M_Element element = M_Element.get(recordLog.getCtx(), "LastRun");
			displayColumnName = element.getName();
			if(!Env.isBaseLanguage(recordLog.getCtx(), "")) {
				String translation = element.get_Translation(MColumn.COLUMNNAME_Name);
				if(!Util.isEmpty(translation)) {
					displayColumnName = translation;
				}
			}
		} else {
			if(!Env.isBaseLanguage(recordLog.getCtx(), "")) {
				String translation = column.get_Translation(MColumn.COLUMNNAME_Name);
				if(!Util.isEmpty(translation)) {
					displayColumnName = translation;
				}
			}
		}
		builder.setDisplayColumnName(
				ValueManager.validateNull(displayColumnName)
			)
			.setDescription(
				ValueManager.validateNull(recordLog.getDescription())
			)
		;
		String oldValue = recordLog.getOldValue();
		String newValue = recordLog.getNewValue();
		//	Set Old Value
		builder.setOldValue(
				ValueManager.validateNull(oldValue)
			)
			.setNewValue(
				ValueManager.validateNull(newValue)
			)
		;
		//	Set Display Values
		if (oldValue != null && oldValue.equals(MChangeLog.NULL)) {
			oldValue = null;
		}
		String displayOldValue = oldValue;
		if (newValue != null && newValue.equals(MChangeLog.NULL)) {
			newValue = null;
		}
		String displayNewValue = newValue;
		//
		try {
			if (DisplayType.isText (column.getAD_Reference_ID ())) {
				;
			} else if (column.getAD_Reference_ID() == DisplayType.YesNo) {
				if (oldValue != null) {
					boolean yes = oldValue.equals("true") || oldValue.equals("Y");
					displayOldValue = Msg.getMsg(Env.getCtx(), yes ? "Y" : "N");
				}
				if (newValue != null) {
					boolean yes = newValue.equals("true") || newValue.equals("Y");
					displayNewValue = Msg.getMsg(Env.getCtx(), yes ? "Y" : "N");
				}
			} else if (column.getAD_Reference_ID() == DisplayType.Amount) {
				if (oldValue != null)
					displayOldValue = amountFormat
						.format (new BigDecimal (oldValue));
				if (newValue != null)
					displayNewValue = amountFormat
						.format (new BigDecimal (newValue));
			} else if (column.getAD_Reference_ID() == DisplayType.Integer) {
				if (oldValue != null)
					displayOldValue = intFormat.format(Integer.valueOf(oldValue));
				if (newValue != null)
					displayNewValue = intFormat.format(Integer.valueOf(newValue));
			} else if (DisplayType.isNumeric (column.getAD_Reference_ID ())) {
				if(column.getColumnName().equals(I_C_Order.COLUMNNAME_ProcessedOn)) {
					if (oldValue != null) {
						if(oldValue.indexOf(".") > 0) {
							oldValue = oldValue.substring(0, oldValue.indexOf("."));
						}
						displayOldValue = TimeUtil.formatElapsed(System.currentTimeMillis() - new BigDecimal (oldValue).longValue());
					}
					if (newValue != null) {
						if(newValue.indexOf(".") > 0) {
							newValue = newValue.substring(0, newValue.indexOf("."));
						}
						displayNewValue = TimeUtil.formatElapsed(System.currentTimeMillis() - new BigDecimal (newValue).longValue());
					}
				} else {
					if (oldValue != null)
						displayOldValue = numberFormat.format (new BigDecimal (oldValue));
					if (newValue != null)
						displayNewValue = numberFormat.format (new BigDecimal (newValue));
				}
			} else if (column.getAD_Reference_ID() == DisplayType.Date) {
				if (oldValue != null)
					displayOldValue = dateFormat.format (Timestamp.valueOf (oldValue));
				if (newValue != null)
					displayNewValue = dateFormat.format (Timestamp.valueOf (newValue));
			} else if (column.getAD_Reference_ID() == DisplayType.DateTime) {
				if (oldValue != null)
					displayOldValue = dateTimeFormat.format (Timestamp.valueOf (oldValue));
				if (newValue != null)
					displayNewValue = dateTimeFormat.format (Timestamp.valueOf (newValue));
			} else if (DisplayType.isLookup(column.getAD_Reference_ID())
					&& column.getAD_Reference_ID() != DisplayType.Button
					&& column.getAD_Reference_ID() != DisplayType.List) {
				MLookup lookup = MLookupFactory.get (Env.getCtx(), 0,
						column.getAD_Column_ID(), column.getAD_Reference_ID(),
					Env.getLanguage(Env.getCtx()), column.getColumnName(),
					column.getAD_Reference_Value_ID(),
					column.isParent(), null);
				if (oldValue != null) {
					Object key = oldValue; 
					NamePair pp = lookup.get(key);
					if (pp != null)
						displayOldValue = pp.getName();
				}
				if (newValue != null) {
					Object key = newValue; 
					NamePair pp = lookup.get(key);
					if (pp != null)
						displayNewValue = pp.getName();
				}
			} else if((DisplayType.Button == column.getAD_Reference_ID()
					|| DisplayType.List == column.getAD_Reference_ID())
					&& column.getAD_Reference_Value_ID() != 0) {
				MLookupInfo lookupInfo = MLookupFactory.getLookup_List(Env.getLanguage(Env.getCtx()), column.getAD_Reference_Value_ID());
				MLookup lookup = new MLookup(lookupInfo, 0);
				if (oldValue != null) {
					Object key = oldValue; 
					NamePair pp = lookup.get(key);
					if (pp != null)
						displayOldValue = pp.getName();
				}
				if (newValue != null) {
					Object key = newValue; 
					NamePair pp = lookup.get(key);
					if (pp != null)
						displayNewValue = pp.getName();
				}
			} else if (DisplayType.isLOB (column.getAD_Reference_ID ())) {
				;
			}
		} catch (Exception e) {
			log.log(Level.WARNING, oldValue + "->" + newValue, e);
		}
		//	Set display values
		builder.setOldDisplayValue(
				ValueManager.validateNull(displayOldValue)
			)
			.setNewDisplayValue(
				ValueManager.validateNull(displayNewValue)
			)
		;
		return builder;
	}


	/**
	 * Convert Process Instance
	 * @param instance
	 * @return
	 */
	public static ProcessLog.Builder convertProcessLog(MPInstance instance) {
		ProcessLog.Builder builder = ProcessLog.newBuilder();
		if (instance == null) {
			return builder;
		}

		builder.setInstanceId(instance.getAD_PInstance_ID());
		builder.setIsError(!instance.isOK());
		builder.setIsProcessing(instance.isProcessing());

		builder.setLastRun(
			ValueManager.getTimestampFromDate(
				instance.getUpdated()
			)
		);
		String summary = instance.getErrorMsg();
		if(!Util.isEmpty(summary, true)) {
			summary = Msg.parseTranslation(Env.getCtx(), summary);
		}
		//	for report
		MProcess process = MProcess.get(Env.getCtx(), instance.getAD_Process_ID());
		builder.setId(instance.getAD_Process_ID())
			.setName(
				ValueManager.validateNull(process.getName()))
			.setDescription(
				ValueManager.validateNull(process.getDescription())
			)
		;
		if(process.isReport()) {
			ReportOutput.Builder outputBuilder = ReportOutput.newBuilder();
			outputBuilder.setReportType(
					ValueManager.validateNull(instance.getReportType())
				)
				.setName(
					ValueManager.validateNull(instance.getName())
				)
			;
			builder.setOutput(outputBuilder.build());
		}
		builder.setSummary(
			ValueManager.validateNull(summary)
		);
		List<X_AD_PInstance_Log> logList = new Query(
			Env.getCtx(), I_AD_PInstance_Log.Table_Name,
			I_AD_PInstance.COLUMNNAME_AD_PInstance_ID + " = ?",
			null
		)
			.setParameters(instance.getAD_PInstance_ID())
			.<X_AD_PInstance_Log>list();
		//	Add Output
		for(X_AD_PInstance_Log log : logList) {
			ProcessInfoLog.Builder logBuilder = ProcessInfoLog.newBuilder();
			logBuilder.setRecordId(log.getAD_PInstance_Log_ID());
			String message = log.getP_Msg();
			if(!Util.isEmpty(message, true)) {
				message = Msg.parseTranslation(Env.getCtx(), message);
			}
			logBuilder.setLog(
				ValueManager.validateNull((message))
			);
			builder.addLogs(logBuilder.build());
		}
		//	
		Struct.Builder parametersMap = Struct.newBuilder();
		for(MPInstancePara parameter : instance.getParameters()) {
			Value.Builder parameterBuilder = Value.newBuilder();
			Value.Builder parameterToBuilder = Value.newBuilder();
			boolean hasFromParameter = false;
			boolean hasToParameter = false;
			String parameterName = parameter.getParameterName();
			int displayType = parameter.getDisplayType();
			if(displayType == -1) {
				displayType = DisplayType.String;
			}
			//	Validate
			if(DisplayType.isID(displayType)) {
				BigDecimal number = parameter.getP_Number();
				BigDecimal numberTo = parameter.getP_Number_To();
				//	Validate
				if(number != null && !number.equals(Env.ZERO)) {
					hasFromParameter = true;
					parameterBuilder = ValueManager.getValueFromInteger(
						number.intValue()
					);
				}
				if(numberTo != null && !numberTo.equals(Env.ZERO)) {
					hasToParameter = true;
					parameterBuilder = ValueManager.getValueFromInteger(
						numberTo.intValue()
					);
				}
			} else if(DisplayType.isNumeric(displayType)) {
				BigDecimal number = parameter.getP_Number();
				BigDecimal numberTo = parameter.getP_Number_To();
				//	Validate
				if(number != null && !number.equals(Env.ZERO)) {
					hasFromParameter = true;
					parameterBuilder = ValueManager.getValueFromBigDecimal(number);
				}
				if(numberTo != null && !numberTo.equals(Env.ZERO)) {
					hasToParameter = true;
					parameterBuilder = ValueManager.getValueFromBigDecimal(numberTo);
				}
			} else if(DisplayType.isDate(displayType)) {
				Timestamp date = parameter.getP_Date();
				Timestamp dateTo = parameter.getP_Date_To();
				//	Validate
				if(date != null) {
					hasFromParameter = true;
					parameterBuilder = ValueManager.getValueFromTimestamp(date);
				}
				if(dateTo != null) {
					hasToParameter = true;
					parameterBuilder = ValueManager.getValueFromTimestamp(dateTo);
				}
			} else if(DisplayType.YesNo == displayType) {
				String value = parameter.getP_String();
				if(!Util.isEmpty(value, true)) {
					hasFromParameter = true;
					parameterBuilder = ValueManager.getValueFromStringBoolean(value);
				}
			} else {
				String value = parameter.getP_String();
				String valueTo = parameter.getP_String_To();
				//	Validate
				if(!Util.isEmpty(value)) {
					hasFromParameter = true;
					parameterBuilder = ValueManager.getValueFromString(value);
				}
				if(!Util.isEmpty(valueTo)) {
					hasToParameter = true;
					parameterBuilder = ValueManager.getValueFromString(valueTo);
				}
			}
			//	For parameter
			if(hasFromParameter) {
				parametersMap.putFields(parameterName, parameterBuilder.build());
			}
			//	For to parameter
			if(hasToParameter) {
				parametersMap.putFields(parameterName + "_To", parameterToBuilder.build());
			}

			ProcesInstanceParameter.Builder instanceParaBuilder = convertProcessInstance(
				parameter
			);
			builder.addProcessIntanceParameters(instanceParaBuilder);
		}
		builder.setParameters(parametersMap);
		return builder;
	}


	public static ProcesInstanceParameter.Builder convertProcessInstance(MPInstancePara instancePara) {
		ProcesInstanceParameter.Builder builder = ProcesInstanceParameter.newBuilder();
		if (instancePara == null) {
			return builder;
		}

		builder.setColumnName(
			ValueManager.validateNull(
				instancePara.getParameterName()
			)
		);

		MProcessPara processPara = null;
		MProcess process = (MProcess) instancePara.getAD_PInstance().getAD_Process();
		MProcessPara[] params = process.getParameters();
		for(MProcessPara param : params) {
			if (param.getColumnName().equals(instancePara.getParameterName())) {
				processPara = param;
				break;
			}
		}
		if (processPara != null) {
			builder.setId(processPara.getAD_Process_Para_ID())
				.setName(
					ValueManager.validateNull(
						processPara.get_Translation(I_AD_Process_Para.COLUMNNAME_Name)
					)
				)
			;
		}

		Value.Builder parameterBuilder = Value.newBuilder();
		Value.Builder parameterToBuilder = Value.newBuilder();
		boolean hasFromParameter = false;
		boolean hasToParameter = false;
		int displayType = instancePara.getDisplayType();
		if(displayType == -1) {
			displayType = DisplayType.String;
		}
		//	Validate
		if(DisplayType.isID(displayType)) {
			BigDecimal number = instancePara.getP_Number();
			BigDecimal numberTo = instancePara.getP_Number_To();
			//	Validate
			if(number != null && !number.equals(Env.ZERO)) {
				hasFromParameter = true;
				parameterBuilder = ValueManager.getValueFromInteger(number.intValue());
			}
			if(numberTo != null && !numberTo.equals(Env.ZERO)) {
				hasToParameter = true;
				parameterBuilder = ValueManager.getValueFromInteger(numberTo.intValue());
			}
		} else if(DisplayType.isNumeric(displayType)) {
			BigDecimal number = instancePara.getP_Number();
			BigDecimal numberTo = instancePara.getP_Number_To();
			//	Validate
			if(number != null && !number.equals(Env.ZERO)) {
				hasFromParameter = true;
				parameterBuilder = ValueManager.getValueFromBigDecimal(number);
			}
			if(numberTo != null && !numberTo.equals(Env.ZERO)) {
				hasToParameter = true;
				parameterBuilder = ValueManager.getValueFromBigDecimal(numberTo);
			}
		} else if(DisplayType.isDate(displayType)) {
			Timestamp date = instancePara.getP_Date();
			Timestamp dateTo = instancePara.getP_Date_To();
			//	Validate
			if(date != null) {
				hasFromParameter = true;
				parameterBuilder = ValueManager.getValueFromTimestamp(date);
			}
			if(dateTo != null) {
				hasToParameter = true;
				parameterBuilder = ValueManager.getValueFromTimestamp(dateTo);
			}
		} else if(DisplayType.YesNo == displayType) {
			String value = instancePara.getP_String();
			if(!Util.isEmpty(value, true)) {
				hasFromParameter = true;
				parameterBuilder = ValueManager.getValueFromStringBoolean(value);
			}
		} else {
			String value = instancePara.getP_String();
			String valueTo = instancePara.getP_String_To();
			//	Validate
			if(!Util.isEmpty(value)) {
				hasFromParameter = true;
				parameterBuilder = ValueManager.getValueFromString(value);
			}
			if(!Util.isEmpty(valueTo)) {
				hasToParameter = true;
				parameterBuilder = ValueManager.getValueFromString(valueTo);
			}
		}

		//	For parameter
		if(hasFromParameter) {
			builder.setValue(parameterBuilder.build());
		}
		//	For to parameter
		if(hasToParameter) {
			builder.setValueTo(parameterToBuilder.build());
		}

		return builder;
	}

}
