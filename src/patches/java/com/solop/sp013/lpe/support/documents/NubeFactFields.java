package com.solop.sp013.lpe.support.documents;

/**
 * Centralizes the NubeFact JSON field names (request and response) as constants, so they are not
 * scattered as string literals across the processors and the sender. The constant identifier is in
 * English; its value is the real NubeFact API field name (Spanish).
 *
 * @author Gabriel Escalona
 */
public interface NubeFactFields {

	/*  Header / comprobante  */
	String OPERATION = "operacion";
	String VOUCHER_TYPE = "tipo_de_comprobante";
	String SERIES = "serie";
	String NUMBER = "numero";
	String SUNAT_TRANSACTION = "sunat_transaction";
	String CUSTOMER_DOCUMENT_TYPE = "cliente_tipo_de_documento";
	String CUSTOMER_DOCUMENT_NUMBER = "cliente_numero_de_documento";
	String CUSTOMER_NAME = "cliente_denominacion";
	String CUSTOMER_ADDRESS = "cliente_direccion";
	String CUSTOMER_EMAIL = "cliente_email";
	String ISSUE_DATE = "fecha_de_emision";
	String DUE_DATE = "fecha_de_vencimiento";
	String CURRENCY = "moneda";
	String EXCHANGE_RATE = "tipo_de_cambio";
	String IGV_PERCENTAGE = "porcentaje_de_igv";
	String TOTAL_TAXED = "total_gravada";
	String TOTAL_EXEMPT = "total_exonerada";
	String TOTAL_IGV = "total_igv";
	String TOTAL = "total";
	String OBSERVATIONS = "observaciones";
	String MODIFIED_DOCUMENT_TYPE = "documento_que_se_modifica_tipo";
	String MODIFIED_DOCUMENT_SERIES = "documento_que_se_modifica_serie";
	String MODIFIED_DOCUMENT_NUMBER = "documento_que_se_modifica_numero";
	String CREDIT_NOTE_TYPE = "tipo_de_nota_de_credito";
	String DEBIT_NOTE_TYPE = "tipo_de_nota_de_debito";
	String SEND_AUTOMATICALLY_TO_SUNAT = "enviar_automaticamente_a_la_sunat";
	String SEND_AUTOMATICALLY_TO_CUSTOMER = "enviar_automaticamente_al_cliente";
	String ITEMS = "items";
	String CREDIT_SALE = "venta_al_credito";
	String REASON = "motivo";

	/*  Comprobante items  */
	String MEASURE_UNIT = "unidad_de_medida";
	String CODE = "codigo";
	String DESCRIPTION = "descripcion";
	String QUANTITY = "cantidad";
	String UNIT_VALUE = "valor_unitario";
	String UNIT_PRICE = "precio_unitario";
	String DISCOUNT = "descuento";
	String SUBTOTAL = "subtotal";
	String IGV_TYPE = "tipo_de_igv";
	String IGV = "igv";

	/*  Credit sale (venta al crédito)  */
	String INSTALLMENT = "cuota";
	String INSTALLMENT_PAYMENT_DATE = "fecha_de_pago";
	String AMOUNT = "importe";

	/*  Guía de remisión  */
	String TRANSFER_REASON = "motivo_de_traslado";
	String GROSS_WEIGHT_TOTAL = "peso_bruto_total";
	String GROSS_WEIGHT_UNIT = "peso_bruto_unidad_de_medida";
	String TRANSPORT_TYPE = "tipo_de_transporte";
	String TRANSFER_START_DATE = "fecha_de_inicio_de_traslado";

	/*  Retención / Percepción  */
	String RETENTION_RATE_TYPE = "tipo_de_tasa_de_retencion";
	String PERCEPTION_RATE_TYPE = "tipo_de_tasa_de_percepcion";
	String TOTAL_RETAINED = "total_retenido";
	String TOTAL_PAID = "total_pagado";
	String TOTAL_PERCEIVED = "total_percibido";
	String TOTAL_COLLECTED = "total_cobrado";
	String RELATED_DOCUMENT_TYPE = "documento_relacionado_tipo";
	String RELATED_DOCUMENT_SERIES = "documento_relacionado_serie";
	String RELATED_DOCUMENT_NUMBER = "documento_relacionado_numero";
	String RELATED_DOCUMENT_ISSUE_DATE = "documento_relacionado_fecha_de_emision";
	String RELATED_DOCUMENT_CURRENCY = "documento_relacionado_moneda";
	String RELATED_DOCUMENT_TOTAL = "documento_relacionado_total";
	String RETENTION_PAYMENT_DATE = "pago_fecha";
	String PAYMENT_NUMBER = "pago_numero";
	String PAYMENT_TOTAL_WITHOUT_RETENTION = "pago_total_sin_retencion";
	String PERCEPTION_COLLECTION_DATE = "cobro_fecha";
	String COLLECTION_NUMBER = "cobro_numero";
	String COLLECTION_TOTAL_WITHOUT_PERCEPTION = "cobro_total_sin_percepcion";
	String EXCHANGE_RATE_DATE = "tipo_de_cambio_fecha";
	String RETAINED_AMOUNT = "importe_retenido";
	String RETAINED_AMOUNT_DATE = "importe_retenido_fecha";
	String AMOUNT_PAID_WITH_RETENTION = "importe_pagado_con_retencion";
	String PERCEIVED_AMOUNT = "importe_percibido";
	String PERCEIVED_AMOUNT_DATE = "importe_percibido_fecha";
	String AMOUNT_COLLECTED_WITH_PERCEPTION = "importe_cobrado_con_percepcion";

	/*  Response  */
	String LINK = "enlace";
	String PDF_LINK = "enlace_del_pdf";
	String XML_LINK = "enlace_del_xml";
	String CDR_LINK = "enlace_del_cdr";
	String ACCEPTED_BY_SUNAT = "aceptada_por_sunat";
	String SUNAT_DESCRIPTION = "sunat_description";
	String SUNAT_NOTE = "sunat_note";
	String SUNAT_RESPONSE_CODE = "sunat_responsecode";
	String SUNAT_SOAP_ERROR = "sunat_soap_error";
	String SUNAT_TICKET_NUMBER = "sunat_ticket_numero";
	String HASH_CODE = "codigo_hash";
	String QR_CODE_STRING = "cadena_para_codigo_qr";
	String BARCODE = "codigo_de_barras";
	String VOIDED = "anulado";
	String PDF_ZIP_BASE64 = "pdf_zip_base64";
	String XML_ZIP_BASE64 = "xml_zip_base64";
	String CDR_ZIP_BASE64 = "cdr_zip_base64";
	String ERRORS = "errors";
	String ERROR_CODE = "codigo";
}
