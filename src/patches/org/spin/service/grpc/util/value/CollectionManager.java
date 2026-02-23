package org.spin.service.grpc.util.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.util.Util;
import org.spin.service.grpc.util.query.Filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for handle Collection (List, Map, Struct Enum) values
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class CollectionManager {

	/**
	 * JSON as string to Map(String, Object)
	 * Ej: `{"AD_Field_ID":123,"AD_Column_ID":345}`
	 * @param jsonValues
	 * @return Map<String, Object>
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getMapFromJsomString(String jsonValues) {
		Map<String, Object> fillValues = new HashMap<String, Object>();
		if (Util.isEmpty(jsonValues, true)) {
			return fillValues;
		}
		ObjectMapper fileMapper = new ObjectMapper(
			new JsonFactory()
		);
		if (jsonValues.trim().startsWith("{")) {
			try {
				/*
					{
						"C_BPartner_ID": 1234,
						"C_Invoice": 333
					}
				*/
				fillValues = fileMapper.readValue(
					jsonValues,
					HashMap.class
				);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (jsonValues.trim().startsWith("[")) {
			try {
				/*
					[
						{"columnName: "C_BPartner_ID", "value": 1234 },
						{"columnName": "C_Invoice", , "value": 333 }
					]
				*/
				TypeReference<List<HashMap<String, Object>>> valueType = new TypeReference<List<HashMap<String, Object>>>() {};

				List<HashMap<String, Object>> valuesAsList = fileMapper.readValue(jsonValues, valueType);
				for (HashMap<String,Object> hashMap : valuesAsList) {
					String key = TextManager.getStringFromObject(
						hashMap.get("columnName")
					);
					Object value = hashMap.get("value");
					fillValues.put(
						key,
						value
					);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return fillValues;
	}



	/**
	 * Recursive convert Map to Struct
	 * @param values
	 * @return
	 */
	public static Value.Builder getProtoValueFromMap(Map<?, ?> values) {
		Value.Builder protoValue = Value.newBuilder();
		if (values == null) {
			// TODO: Validate if return null or empty
			// protoValue.setStructValue(
			// 	structBuilder
			// );
			return ValueManager.getProtoValueFromNull();
		}
		Struct.Builder structBuilder = getStructFromMap(values);
		protoValue.setStructValue(
			structBuilder
		);
		return protoValue;
	}

	public static Map<String, Object> getMapFromProtoValueStruct(Struct values) {
		Map<String, Object> valuesMap = new HashMap<String, Object>();
		if (values == null) {
			return valuesMap;
		}
		values.getFieldsMap().forEach((keyItem, protoValueItem) -> {
			Object valueItem = ValueManager.getObjectFromProtoValue(protoValueItem);
			valuesMap.put(
				keyItem,
				valueItem
			);
		});

		return valuesMap;
	}
	public static Map<String, Object> getMapFromProtoValue(Value protoValue) {
		Map<String, Object> valuesMap = new HashMap<String, Object>();
		if (protoValue == null) {
			return valuesMap;
		}

		valuesMap = getMapFromProtoValueStruct(
			protoValue.getStructValue()
		);
		return valuesMap;
	}


	/**
	 * Convert Selection values from gRPC to ADempiere values
	 * @param values
	 * @return
	 */
	public static Map<String, Object> getMapObjectFromMapProtoValue(Map<String, Value> values) {
		Map<String, Object> convertedValues = new HashMap<String, Object>();
		if (values == null || values.size() <= 0) {
			return convertedValues;
		}
		values.forEach((keyItem, protoValueItem) -> {
			Object valueItem = ValueManager.getObjectFromProtoValue(protoValueItem);
			convertedValues.put(
				keyItem,
				valueItem
			);
		});
		//	
		return convertedValues;
	}

	/**
	 * Convert Selection values from gRPC to ADempiere values
	 * @param values
	 * @param displayTypeColumns Map(ColumnName, DisplayType)
	 * @return
	 */
	public static Map<String, Object> getMapObjectFromMapProtoValue(Map<String, Value> values, Map<String, Integer> displayTypeColumns) {
		Map<String, Object> convertedValues = new HashMap<String, Object>();
		if (values == null || values.size() <= 0) {
			return convertedValues;
		}
		if (displayTypeColumns == null || values.size() <= 0) {
			return CollectionManager.getMapObjectFromMapProtoValue(
				values
			);
		}
		values.forEach((keyItem, protoValueItem) -> {
			Object valueItem = ValueManager.getObjectFromProtoValue(protoValueItem);

			Integer displayType = displayTypeColumns.get(keyItem);
			if (displayType != null && displayType.intValue() > 0) {
				valueItem = ValueManager.getObjectFromProtoValue(
					protoValueItem,
					displayType.intValue()
				);
			}

			convertedValues.put(
				keyItem,
				valueItem
			);
		});
		//	
		return convertedValues;
	}


	/**
	 * Recursive convert Map to Struct
	 * @param values
	 * @return
	 */
	public static Struct.Builder getStructFromMap(Map<?, ?> values) {
		Struct.Builder structBuilder = Struct.newBuilder();

		if (values == null) {
			return structBuilder;
		}
		else {
			((Map<?, ?>) values).forEach((keyItem, valueItem) -> {
				// key always is string
				String structKey = "";
				if (keyItem instanceof String) {
					structKey = (String) keyItem;
				} else {
					// Handle error if key not is String
					structKey = TextManager.getStringFromObject(keyItem);
				}

				Value.Builder protoValueItem = ValueManager.getProtoValueFromObject(valueItem);
				structBuilder.putFields(
					structKey,
					protoValueItem.build()
				);
			});
		}
		return structBuilder;
	}

	public static Struct.Builder getStructFromFiltersList(List<Filter> filtersList) {
		Struct.Builder structBuilder = Struct.newBuilder();
		if (filtersList == null || filtersList.isEmpty()) {
			return structBuilder;
		}

		List<Filter> validFilters = filtersList.parallelStream()
			.filter(condition -> {
				return !Util.isEmpty(condition.getColumnName(), true);
			})
			.toList()
		;
		validFilters.forEach(condition -> {
				final String conditionColumnName = condition.getColumnName();
				final Object conditionValue = condition.getValue();
				Value.Builder protoValueItem = ValueManager.getProtoValueFromObject(
					conditionValue
				);
				structBuilder.putFields(
					conditionColumnName,
					protoValueItem.build()
				);
			})
		;

		return structBuilder;
	}

	public static List<?> getListFromProtoValuesList(ListValue values) {
		ArrayList<Object> valuesList = new ArrayList<Object>();
		if (values == null) {
			return valuesList;
		}
		values.getValuesList().forEach(protoValueItem -> {
			Object valueItem = ValueManager.getObjectFromProtoValue(protoValueItem);
			valuesList.add(valueItem);
		});
		;
		return valuesList;
	}
	public static List<?> getListFromProtoValue(Value value) {
		List<?> valuesList = new ArrayList<Object>();
		if (value == null) {
			return valuesList;
		}
		valuesList = getListFromProtoValuesList(
			value.getListValue()
		);
		return valuesList;
	}

	/**
	 * Recursive convert List to ListValue
	 * @param values
	 * @return
	 */
	public static Value.Builder getProtoValueFromList(List<?> values) {
		Value.Builder protoValue = Value.newBuilder();
		ListValue.Builder protoListBuilder = ListValue.newBuilder();
		
		if (values == null) {
			// TODO: Validate if return null or empty
			// protoValue.setListValue(
			// 	protoListBuilder.build()
			// );
			return ValueManager.getProtoValueFromNull();
		}
		else {
			// Each and convert List to ListValue
			((List<?>) values).forEach(valueItem -> {
				Value.Builder protoValueItem = ValueManager.getProtoValueFromObject(valueItem);
				protoListBuilder.addValues(
					protoValueItem
				);
			});
		}
		protoValue.setListValue(
			protoListBuilder.build()
		);
		return protoValue;
	}

}
