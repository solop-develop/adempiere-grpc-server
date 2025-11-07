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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.core.domains.models.I_AD_PInstance_Log;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.X_AD_PInstance_Log;
import org.compiere.model.MChangeLog;
import org.compiere.model.MColumn;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MWindow;
import org.compiere.model.M_Element;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ProcesInstanceParameter;
import org.spin.backend.grpc.common.ProcessInfoLog;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.ReportOutput;
import org.spin.backend.grpc.logs.ChangeLog;
import org.spin.backend.grpc.logs.EntityEventType;
import org.spin.backend.grpc.logs.EntityLog;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * This class was created for add all convert methods for Logs service
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class LogsConvertUtil {

	/**
	 * Convert a change log for a set of changes to builder
	 * @param recordLog
	 * @return
	 */
	public static EntityLog.Builder convertRecordLogHeader(MChangeLog recordLog) {
		MTable table = MTable.get(recordLog.getCtx(), recordLog.getAD_Table_ID());
		EntityLog.Builder builder = EntityLog.newBuilder();
		builder.setLogId(
				recordLog.getAD_ChangeLog_ID()
			)
			.setId(
				recordLog.getRecord_ID()
			)
			.setTableName(
				TextManager.getValidString(
					table.getTableName()
				)
			)
		;

		String displayedName = table.get_Translation(I_AD_Table.COLUMNNAME_Name);
		if (table.getAD_Window_ID() > 0) {
			MWindow window = MWindow.get(Env.getCtx(), table.getAD_Window_ID());
			displayedName = window.get_Translation(I_AD_Window.COLUMNNAME_Name);
			builder.setWindowId(window.getAD_Window_ID());
		}
		builder.setDisplayedName(
			TextManager.getValidString(displayedName)
		);

		// created by
		MUser user = MUser.get(recordLog.getCtx(), recordLog.getCreatedBy());
		builder.setCreatedBy(
				user.getAD_User_ID()
			)
			.setCreatedByName(
				TextManager.getValidString(
					user.getName()
				)
			)
		;

		// updated by
		user = MUser.get(recordLog.getCtx(), recordLog.getUpdatedBy());
		builder.setUpdatedBy(
				user.getAD_User_ID()
			)
			.setUpdatedByName(
				TextManager.getValidString(
					user.getName()
				)
			)
		;

		builder.setSessionId(
				recordLog.getAD_Session_ID()
			)
			.setTransactionName(
				TextManager.getValidString(
					recordLog.getTrxName()
				)
			)
			.setLogDate(
				ValueManager.getProtoTimestampFromTimestamp(
					recordLog.getCreated()
				)
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
	public static List<EntityLog.Builder> convertRecordLog(List<MChangeLog> recordLogList) {
		Map<Integer, EntityLog.Builder> indexMap = new HashMap<Integer, EntityLog.Builder>();
		recordLogList.parallelStream()
			.filter(recordLog -> {
				return !indexMap.containsKey(recordLog.getAD_ChangeLog_ID());
			})
			// .sorted(
			// 	Comparator.comparing(MChangeLog::getCreated)
			// )
			.forEach(recordLog -> {
				indexMap.put(recordLog.getAD_ChangeLog_ID(), convertRecordLogHeader(recordLog));
			});
		//	convert changes
		recordLogList.parallelStream().forEach(recordLog -> {
			ChangeLog.Builder changeLog = convertChangeLog(recordLog);
			EntityLog.Builder recordLogBuilder = indexMap.get(recordLog.getAD_ChangeLog_ID());
			recordLogBuilder.addChangeLogs(changeLog);
			indexMap.put(recordLog.getAD_ChangeLog_ID(), recordLogBuilder);
		});

		List<EntityLog.Builder> entitiesListBuilder = indexMap.values().stream()
			// .sorted(
			// 	Comparator.comparing(EntityLog.Builder::getLogDate)
			// 		.reversed()
			// )
			.sorted((log1, log2) -> {
				Timestamp from = TimeManager.getTimestampFromProtoTimestamp(
					log1.getLogDate()
				);

				Timestamp to = TimeManager.getTimestampFromProtoTimestamp(
					log2.getLogDate()
				);

				if (from == null || to == null) {
					// prevent Null Pointer Exception
					return 1;
				}
				int compared = to.compareTo(from);
				if (compared == 0) {
					// if is insert down
					if (log1.getEventType() == EntityEventType.INSERT) {
						return 1;
					} else if (log2.getEventType() == EntityEventType.INSERT) {
						return -1;
					}
					// if is delete up
					if (log1.getEventType() == EntityEventType.DELETE) {
						return -1;
					} else if (log2.getEventType() == EntityEventType.DELETE) {
						return 1;
					}
				}
				return compared;
			})
			.collect(Collectors.toList())
		;

		return entitiesListBuilder;
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
			TextManager.getValidString(
				column.getColumnName()
			)
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
				TextManager.getValidString(displayColumnName)
			)
			.setDescription(
				TextManager.getValidString(
					recordLog.getDescription()
				)
			)
		;
		String oldValue = recordLog.getOldValue();
		String newValue = recordLog.getNewValue();

		//	Set Display Values
		if (oldValue != null && oldValue.equals(MChangeLog.NULL)) {
			oldValue = null;
		}
		String displayOldValue = ValueManager.getDisplayedValueFromReference(
			recordLog.getCtx(),
			oldValue,
			column.getColumnName(),
			column.getAD_Reference_ID(),
			column.getAD_Reference_Value_ID()
		);
		if (Util.isEmpty(displayOldValue, true)) {
			displayOldValue = oldValue;
		}
		
		if (newValue != null && newValue.equals(MChangeLog.NULL)) {
			newValue = null;
		}
		String displayNewValue = ValueManager.getDisplayedValueFromReference(
			recordLog.getCtx(),
			newValue,
			column.getColumnName(),
			column.getAD_Reference_ID(),
			column.getAD_Reference_Value_ID()
		);
		if (Util.isEmpty(displayOldValue, true)) {
			displayNewValue = newValue;
		}

		builder
			//	Set Old Value
			.setOldValue(
				TextManager.getValidString(oldValue)
			)
			.setNewValue(
				TextManager.getValidString(newValue)
			)	
			//	Set display values
			.setOldDisplayValue(
				TextManager.getValidString(displayOldValue)
			)
			.setNewDisplayValue(
				TextManager.getValidString(displayNewValue)
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
			ValueManager.getProtoTimestampFromTimestamp(
				instance.getUpdated()
			)
		);

		//	for report
		MProcess process = MProcess.get(Env.getCtx(), instance.getAD_Process_ID());
		builder.setId(
				process.getAD_Process_ID()
			)
			.setUuid(
				TextManager.getValidString(
					process.get_UUID()
				)
			)
			.setName(
				TextManager.getValidString(
					process.getName()
				)
			)
			.setDescription(
				TextManager.getValidString(
					process.getDescription()
				)
			)
		;
		if(process.isReport()) {
			ReportOutput.Builder outputBuilder = ReportOutput.newBuilder()
				.setId(
					instance.getAD_PInstance_ID()
				)
				.setUuid(
					TextManager.getValidString(
						instance.getUUID()
					)
				)
				.setReportType(
					TextManager.getValidString(
						instance.getReportType()
					)
				)
				.setName(
					TextManager.getValidString(
						instance.getName()
					)
				)
				.setIsDirectPrint(
					process.isDirectPrint()
				)
			;
			builder.setOutput(
				outputBuilder.build()
			);
		}
		builder.setSummary(
			TextManager.getValidString(
				Msg.parseTranslation(
					Env.getCtx(),
					instance.getErrorMsg()
				)
			)
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
				TextManager.getValidString(message)
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
					parameterBuilder = NumberManager.getProtoValueFromInteger(
						number.intValue()
					);
				}
				if(numberTo != null && !numberTo.equals(Env.ZERO)) {
					hasToParameter = true;
					parameterBuilder = NumberManager.getProtoValueFromInteger(
						numberTo.intValue()
					);
				}
			} else if(DisplayType.isNumeric(displayType)) {
				BigDecimal number = parameter.getP_Number();
				BigDecimal numberTo = parameter.getP_Number_To();
				//	Validate
				if(number != null && !number.equals(Env.ZERO)) {
					hasFromParameter = true;
					parameterBuilder = NumberManager.getProtoValueFromBigDecimal(number);
				}
				if(numberTo != null && !numberTo.equals(Env.ZERO)) {
					hasToParameter = true;
					parameterBuilder = NumberManager.getProtoValueFromBigDecimal(numberTo);
				}
			} else if(DisplayType.isDate(displayType)) {
				Timestamp date = parameter.getP_Date();
				Timestamp dateTo = parameter.getP_Date_To();
				//	Validate
				if(date != null) {
					hasFromParameter = true;
					parameterBuilder = TimeManager.getProtoValueFromTimestamp(date);
				}
				if(dateTo != null) {
					hasToParameter = true;
					parameterBuilder = TimeManager.getProtoValueFromTimestamp(dateTo);
				}
			} else if(DisplayType.YesNo == displayType) {
				String value = parameter.getP_String();
				if(!Util.isEmpty(value, true)) {
					hasFromParameter = true;
					parameterBuilder = BooleanManager.getProtoValueFromBoolean(value);
				}
			} else {
				String value = parameter.getP_String();
				String valueTo = parameter.getP_String_To();
				//	Validate
				if(!Util.isEmpty(value)) {
					hasFromParameter = true;
					parameterBuilder = TextManager.getProtoValueFromString(value);
				}
				if(!Util.isEmpty(valueTo)) {
					hasToParameter = true;
					parameterBuilder = TextManager.getProtoValueFromString(valueTo);
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
			TextManager.getValidString(
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
			builder.setId(
					processPara.getAD_Process_Para_ID()
				)
				.setName(
					TextManager.getValidString(
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
				parameterBuilder = NumberManager.getProtoValueFromInteger(number.intValue());
			}
			if(numberTo != null && !numberTo.equals(Env.ZERO)) {
				hasToParameter = true;
				parameterBuilder = NumberManager.getProtoValueFromInteger(numberTo.intValue());
			}
		} else if(DisplayType.isNumeric(displayType)) {
			BigDecimal number = instancePara.getP_Number();
			BigDecimal numberTo = instancePara.getP_Number_To();
			//	Validate
			if(number != null && !number.equals(Env.ZERO)) {
				hasFromParameter = true;
				parameterBuilder = NumberManager.getProtoValueFromBigDecimal(number);
			}
			if(numberTo != null && !numberTo.equals(Env.ZERO)) {
				hasToParameter = true;
				parameterBuilder = NumberManager.getProtoValueFromBigDecimal(numberTo);
			}
		} else if(DisplayType.isDate(displayType)) {
			Timestamp date = instancePara.getP_Date();
			Timestamp dateTo = instancePara.getP_Date_To();
			//	Validate
			if(date != null) {
				hasFromParameter = true;
				parameterBuilder = TimeManager.getProtoValueFromTimestamp(date);
			}
			if(dateTo != null) {
				hasToParameter = true;
				parameterBuilder = TimeManager.getProtoValueFromTimestamp(dateTo);
			}
		} else if(DisplayType.YesNo == displayType) {
			String value = instancePara.getP_String();
			if(!Util.isEmpty(value, true)) {
				hasFromParameter = true;
				parameterBuilder = BooleanManager.getProtoValueFromBoolean(value);
			}
		} else {
			String value = instancePara.getP_String();
			String valueTo = instancePara.getP_String_To();
			//	Validate
			if(!Util.isEmpty(value)) {
				hasFromParameter = true;
				parameterBuilder = TextManager.getProtoValueFromString(value);
			}
			if(!Util.isEmpty(valueTo)) {
				hasToParameter = true;
				parameterBuilder = TextManager.getProtoValueFromString(valueTo);
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
