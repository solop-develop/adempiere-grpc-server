FROM envoyproxy/envoy:v1.27.0

LABEL maintainer="ySenih@erpya.com; EdwinBetanc0urt@outlook.com" \
	description="Proxy to Transcoding gRPC to JSON via http"


# Init ENV with default values
ENV \
	SERVER_PORT="5555" \
	BACKEND_HOST="localhost" \
	BACKEND_PORT="50059" \
	SERVICES_ENABLED="bank_statement_match.BankStatementMatch; \
		business_partner.BusinessPartner; data.BusinessData; \
		data.CoreFunctionality; data.UserInterface; \
		dashboarding.Dashboarding; dictionary.Dictionary; \
		enrollment.Register; express_movement.ExpressMovement; \
		express_receipt.ExpressReceipt; express_shipment.ExpressShipment; \
		file_management.FileManagement; general_ledger.GeneralLedger; \
		import_file_loader.ImportFileLoader; in_out.InOut; \
		invoice.Invoice; issue_management.IssueManagement; \
		logs.Logs; match_po_receipt_invoice.MatchPORReceiptInvoice; \
		material_management.MaterialManagement; \
		order.Order; payment_allocation.PaymentAllocation; \
		payment_print_export.PaymentPrintExport; \
		payment.Payment; payroll_action_notice.PayrollActionNotice; \
		data.Store; product.Product; record_management.RecordManagement; \
		security.Security; time_control.TimeControl; \
		time_record.TimeRecord; updates.UpdateCenter; \
		user_customization.UserCustomization; wms.WarehouseManagement; \
		store.WebStore; workflow.Workflow;"

#Expose Ports
EXPOSE 9901
EXPOSE ${SERVER_PORT}


# Envoy standard configuration
COPY docker/envoy_template.yaml /etc/envoy/envoy_template.yaml

# Proto gRPC descriptor
COPY docker/adempiere-grpc-server.pb /data/adempiere-grpc-server.pb
COPY docker/start_envoy.sh /opt/apps/server/start.sh


RUN addgroup adempiere && \
	adduser --disabled-password --gecos "" --ingroup adempiere --no-create-home adempiere && \
	chown -R adempiere /etc/envoy/ && \
	chmod +x start.sh

USER adempiere

# Start app
ENTRYPOINT ["sh" , "start.sh"]
