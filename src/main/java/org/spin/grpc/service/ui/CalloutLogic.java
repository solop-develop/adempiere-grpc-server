package org.spin.grpc.service.ui;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.GridTab;
import org.compiere.model.GridTabVO;
import org.compiere.model.GridWindow;
import org.compiere.model.GridWindowVO;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRefList;
import org.compiere.model.MRule;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.user_interface.RunCalloutRequest;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.interim.ContextTemporaryWorkaround;
import org.spin.base.util.ContextManager;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.dictionary.util.WindowUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.CollectionManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class CalloutLogic {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(CalloutLogic.class);

	/**	Window emulation	*/
	private static AtomicInteger windowNoEmulation = new AtomicInteger(1);

	private static final CCache<Integer, Map<String, MField>> TAB_FIELDS_CACHE = new CCache<Integer, Map<String, MField>>("Tab-Fields-Cache", 30, 120);

	/**
	 * Verify if a value has been changed
	 * @param gridField
	 * @return
	 */
	public static boolean isValidChange(GridField gridField) {
		//	Standard columns
		if(gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_Created)
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_CreatedBy)
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_Updated)
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_UpdatedBy)
				|| gridField.getColumnName().equals(I_AD_Element.COLUMNNAME_UUID)) {
			return false;
		}
		//	Oly Displayed
		// if(!gridField.isDisplayed()) {
		// 	return false;
		// }
		//	Key
		if(gridField.isKey()) {
			return false;
		}

		//	validate with old value
		if(gridField.getOldValue() != null
				&& gridField.getValue() != null
				&& gridField.getValue().equals(gridField.getOldValue())) {
			return false;
		}
		//	Default
		return true;
	}


	/**
	 * Process Callout
	 * @param gridTab
	 * @param field
	 * @return
	 */
	private static String processCallout (int windowNo, GridTab gridTab, GridField field) {
		String callout = field.getCallout();
		if (Util.isEmpty(callout, true)) {
			return "";
		}

		//
		Object value = field.getValue();
		Object oldValue = field.getOldValue();
		log.fine(field.getColumnName() + "=" + value
			+ " (" + callout + ") - old=" + oldValue);

		StringTokenizer st = new StringTokenizer(callout, ";,", false);
		while (st.hasMoreTokens()) {
			String cmd = st.nextToken().trim();
			String retValue = "";
			// FR [1877902]
			// CarlosRuiz - globalqss - implement beanshell callout
			// Victor Perez  - vpj-cd implement JSR 223 Scripting
			if (cmd.toLowerCase().startsWith(MRule.SCRIPT_PREFIX)) {
				MRule rule = MRule.get(Env.getCtx(), cmd.substring(MRule.SCRIPT_PREFIX.length()));
				if (rule == null) {
					retValue = "Callout " + cmd + " not found"; 
					log.log(Level.SEVERE, retValue);
					return retValue;
				}
				if ( !  (rule.getEventType().equals(MRule.EVENTTYPE_Callout) 
					  && rule.getRuleType().equals(MRule.RULETYPE_JSR223ScriptingAPIs))) {
					retValue = "Callout " + cmd + " must be of type JSR 223 and event Callout"; 
					log.log(Level.SEVERE, retValue);
					return retValue;
				}

				ScriptEngine engine = rule.getScriptEngine();

				// Window Env.getCtx() are    W_
				// Login Env.getCtx()  are    G_
				MRule.setContext(engine, Env.getCtx(), windowNo);
				// now add the callout parameters windowNo, tab, field, value, oldValue to the engine 
				// Method arguments Env.getCtx() are A_
				engine.put(MRule.ARGUMENTS_PREFIX + "WindowNo", windowNo);
				engine.put(MRule.ARGUMENTS_PREFIX + "Tab", gridTab);
				engine.put(MRule.ARGUMENTS_PREFIX + "Field", field);
				engine.put(MRule.ARGUMENTS_PREFIX + "Value", value);
				engine.put(MRule.ARGUMENTS_PREFIX + "OldValue", oldValue);
				engine.put(MRule.ARGUMENTS_PREFIX + "Ctx", Env.getCtx());

				try {
					retValue = engine.eval(rule.getScript()).toString();
				} catch (Exception e) {
					log.log(Level.SEVERE, "", e);
					e.printStackTrace();
					retValue = 	"Callout Invalid: " + e.toString();
					return retValue;
				}
			} else {
				Callout call = null;
				String method = null;
				int methodStart = cmd.lastIndexOf('.');
				try {
					if (methodStart != -1) {
						Class<?> cClass = Class.forName(cmd.substring(0, methodStart));
						call = (Callout) cClass.getDeclaredConstructor().newInstance();
						method = cmd.substring(methodStart+1);
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "class", e);
					e.printStackTrace();
					return "Callout Invalid: " + cmd + " (" + e.toString() + ")";
				}

				if (call == null || Util.isEmpty(method, true)) {
					return "Callout Invalid: " + method;
				}

				try {
					retValue = call.start(Env.getCtx(), method, windowNo, gridTab, field, value, oldValue);
				} catch (Exception e) {
					log.log(Level.SEVERE, "start", e);
					e.printStackTrace();
					retValue = 	"Callout Invalid: " + e.toString();
					return retValue;
				}
			}
			if (!Util.isEmpty(retValue)) {	//	interrupt on first error
				log.severe (retValue);
				return retValue;
			}
		}   //  for each callout
		return "";
	}	//	processCallout


	private static MField getFieldFromCache(MTab tab, String columnName) {
		try {
			String upperColumnName = columnName.toUpperCase();
			
			// Get the field map for this tab (create if it does not exist)
			Map<String, MField> fieldsMap = TAB_FIELDS_CACHE.computeIfAbsent(
				tab.getAD_Tab_ID(), 
				tabId -> buildFieldIndex(tab)
			);
			
			// Search for the field in the index
			MField field = fieldsMap.get(upperColumnName);
			
			// If not found, refresh the cache and search again.
			if (field == null) {
				synchronized (TAB_FIELDS_CACHE) {
					fieldsMap = buildFieldIndex(tab); // Rebuild index
					TAB_FIELDS_CACHE.put(tab.getAD_Tab_ID(), fieldsMap);
					field = fieldsMap.get(upperColumnName);
				}
			}
			
			return field;
		} catch (Exception e) {
			throw new AdempiereException("Error getting field " + columnName, e);
		}
	}

	private static Map<String, MField> buildFieldIndex(MTab tab) {
		// Make defensive copy of the array before parallel processing
		MField[] originalFields = tab.getFields(false, null);
		MField[] fieldsCopy = Arrays.copyOf(originalFields, originalFields.length);
		
		return Arrays.stream(fieldsCopy)
			.parallel() // Parallel processing only here
			.filter(Objects::nonNull)
			.filter(f -> f.getAD_Column() != null)
			.collect(Collectors.toConcurrentMap(
				f -> f.getAD_Column().getColumnName().toUpperCase(),
				Function.identity(),
				(existing, replacement) -> existing
			));
	}

	/**
	 * Run callout with data from server
	 * @param request
	 * @return
	 */
	public static org.spin.backend.grpc.user_interface.Callout.Builder runcallout(RunCalloutRequest request) {
		if (Util.isEmpty(request.getCallout(), true)) {
			throw new AdempiereException("@FillMandatory@ @Callout@");
		}
		if (Util.isEmpty(request.getColumnName(), true)) {
			throw new AdempiereException("@FillMandatory@ @ColumnName@");
		}
		org.spin.backend.grpc.user_interface.Callout.Builder calloutBuilder = org.spin.backend.grpc.user_interface.Callout.newBuilder();
		Trx.run(transactionName -> {
			if (request.getTabId() <= 0) {
				throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
			}
			MTab tab = MTab.get(Env.getCtx(), request.getTabId());
			if (tab == null || tab.getAD_Tab_ID() <= 0) {
				throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
			}

			/*
			MField field = Arrays.asList(tab.getFields(false, null))
				.parallelStream()
				.filter(searchField -> {
					return 
						searchField != null && 
						searchField.getAD_Column() != null && 
						request.getColumnName().equals(
							searchField.getAD_Column().getColumnName()
						)
					;
				})
				.findFirst()
				.orElse(null)
			;
			*/
			// Get the field using the cache
			MField field = getFieldFromCache(tab, request.getColumnName());
			if (field == null || field.getAD_Field_ID() <= 0) {
				// System.out.println(fieldsCopy);
				throw new AdempiereException("@AD_Field_ID@ @NotFound@: " + request.getColumnName());
			}

			//	window
			int windowNo = request.getWindowNo();
			if(windowNo <= 0) {
				windowNo = windowNoEmulation.getAndIncrement();
			}

			// set values on Env.getCtx()
			Map<String, Integer> displayTypeColumns = WindowUtil.getTabFieldsDisplayType(tab);
			Map<String, Object> attributes = CollectionManager.getMapObjectFromMapProtoValue(
				request.getContextAttributes().getFieldsMap(),
				displayTypeColumns
			);
			ContextManager.setContextWithAttributesFromObjectMap(
				windowNo,
				Env.getCtx(),
				attributes
			);

			MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
			int columnDisplayTypeId = column.getAD_Reference_ID();
			//
			Object oldValue = ValueManager.getObjectFromProtoValue(
				request.getOldValue(),
				columnDisplayTypeId
			);
			Object value = ValueManager.getObjectFromProtoValue(
				request.getValue(),
				columnDisplayTypeId
			);

			// TODO: Correct `tabNo` with get ASP Tabs List and isActive tab
			int tabNo = (tab.getSeqNo() / 10) - 1;
			if(tabNo < 0) {
				tabNo = 0;
			}
			ContextManager.setTabContextByObject(Env.getCtx(), windowNo, tabNo, request.getColumnName(), value);

			//	Initial load for callout wrapper
			GridWindowVO gridWindowVo = GridWindowVO.create(Env.getCtx(), windowNo, tab.getAD_Window_ID());
			GridWindow gridWindow = new GridWindow(gridWindowVo, true);
			// gridWindow.initTab(tabNo); // TODO: Set more precise link column
			GridTabVO gridTabVo = GridTabVO.create(gridWindowVo, tabNo, tab, false, true);
			// TODO: Fix Convert_PostgreSQL.convertRowNum with multiple row num on first restriction as `WHERE ROWNUM >= 1 AND ROWNUM <= 1`
			gridTabVo.WhereClause = "1=1 AND ROWNUM >= 1 AND ROWNUM <= 1";
			GridFieldVO gridFieldVo = GridFieldVO.create(Env.getCtx(), windowNo, tabNo, tab.getAD_Window_ID(), tab.getAD_Tab_ID(), false, field);
			GridField gridField = new GridField(gridFieldVo);
			//	Init tab
			GridTab gridTab = new GridTab(gridTabVo, gridWindow, true);

			// set link column name by tab
			gridTab.setLinkColumnName(null);
			String linkColumn = gridTab.getLinkColumnName();
			if (Util.isEmpty(linkColumn, true)) {
				// set link column name by parent column
				MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
				List<MColumn> columnsList = table.getColumnsAsList();
				MColumn parentColumn = columnsList.parallelStream()
					.filter(columnItem -> {
						return columnItem.isParent();
					})
					.findFirst()
					.orElse(null);
				if (parentColumn != null && parentColumn.getAD_Column_ID() > 0) {
					linkColumn = parentColumn.getColumnName();
					gridTab.setLinkColumnName(linkColumn);
				}
			}

			gridTab.query(false);
			gridTab.clearSelection();
			gridTab.dataNew(false);

			//	load values
			for (Entry<String, Object> attribute : attributes.entrySet()) {
				String columnNameEntity = attribute.getKey();
				Object valueEntity = attribute.getValue();
				gridTab.setValue(
					columnNameEntity,
					valueEntity
				);
			}
			// gridTab.setValue(request.getColumnName(), value);
			gridTab.setValue(gridField, value);

			//	Load value for field
			Object oldValueChange = oldValue;
			if(oldValueChange != null
					&& value != null
					&& value.equals(oldValueChange)) {
				oldValueChange = null;
			}
			gridField.setValue(oldValueChange, false);
			gridField.setValue(value, false);

			//	Run it
			String result = processCallout(windowNo, gridTab, gridField);
			Struct.Builder contextValues = Struct.newBuilder();
			List<GridField> list = Arrays.asList(gridTab.getFields())
				.stream()
				.filter(fieldValue -> {
					return CalloutLogic.isValidChange(fieldValue);
				})
				.collect(Collectors.toList())
			;
			list.forEach(gridFieldItem -> {
				Object contextValue = gridFieldItem.getValue();
				Value.Builder valueBuilder = ValueManager.getProtoValueFromObject(
					contextValue,
					gridFieldItem.getDisplayType()
				);
				contextValues.putFields(
					gridFieldItem.getColumnName(),
					valueBuilder.build()
				);

				// overwrite display type `Button` to `List`, example `PaymentRule` or `Posted`
				int displayTypeId = ReferenceUtil.overwriteDisplayType(
					gridFieldItem.getDisplayType(),
					gridFieldItem.getAD_Reference_Value_ID()
				);
				if (ReferenceUtil.validateReference(displayTypeId)) {
					String contextDisplayValue = null;
					if (contextValue != null) {
						if (displayTypeId == DisplayType.List ||
							(displayTypeId == DisplayType.Button && gridFieldItem.getAD_Reference_Value_ID() > 0)) {
							MRefList referenceList = MRefList.get(
								Env.getCtx(),
								gridFieldItem.getAD_Reference_Value_ID(),
								TextManager.getStringFromObject(
									contextValue
								),
								null
							);
							if (referenceList != null) {
								contextDisplayValue = referenceList.get_Translation(
									MRefList.COLUMNNAME_Name
								);
							}
						} else {
							MLookupInfo lookupInfo = ReferenceUtil.getReferenceLookupInfo(
								displayTypeId,
								gridFieldItem.getAD_Reference_Value_ID(),
								gridFieldItem.getColumnName(),
								0
							);
							if(lookupInfo != null && !Util.isEmpty(lookupInfo.QueryDirect, true)) {
								final String sql = WhereClauseUtil.removeIsActiveRestriction(
									lookupInfo.TableName,
									lookupInfo.QueryDirect
								);
								PreparedStatement pstmt = null;
								ResultSet rs = null;
								try {
									//	SELECT Key, Value, Name FROM ...
									pstmt = DB.prepareStatement(sql.toString(), null);
									DB.setParameter(pstmt, 1, contextValue);

									//	Get from Query
									rs = pstmt.executeQuery();
									if (rs.next()) {
										//	3 = Display Value
										contextDisplayValue = rs.getString(3);
									}
								} catch (Exception e) {
									log.severe(e.getLocalizedMessage());
									e.printStackTrace();
									// throw new AdempiereException(e);
								} finally {
									DB.close(rs, pstmt);
									rs = null;
									pstmt = null;
								}
							}
						}
					}

					Value.Builder displayValueBuilder = TextManager.getProtoValueFromString(
						contextDisplayValue
					);
					contextValues.putFields(
						LookupUtil.getDisplayColumnName(
							gridFieldItem.getColumnName()
						),
						displayValueBuilder.build()
					);
				}
			});

			// always add is sales transaction on context
			String isSalesTransaction = Env.getContext(Env.getCtx(), windowNo, "IsSOTrx", true);
			if (!Util.isEmpty(isSalesTransaction, true)) {
				Value.Builder valueBuilder = BooleanManager.getProtoValueFromBoolean(isSalesTransaction);
				contextValues.putFields(
					"IsSOTrx",
					valueBuilder.build()
				);
			}
			calloutBuilder.setResult(
					TextManager.getValidString(
						Msg.parseTranslation(
							Env.getCtx(),
							result
						)
					)
				)
				.setValues(contextValues)
			;

			// TODO: Temporary Workaround
			ContextTemporaryWorkaround.setAdditionalContext(
				request.getCallout(),
				windowNo,
				calloutBuilder
			);
		});
		return calloutBuilder;
	}

}
