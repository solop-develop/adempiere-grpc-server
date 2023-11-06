FROM envoyproxy/envoy:v1.28.0

LABEL maintainer="ySenih@erpya.com; EdwinBetanc0urt@outlook.com" \
	description="Proxy Transcoding gRPC to JSON via http"


# Init ENV with default values
ENV \
	SERVER_PORT="5555" \
	BACKEND_HOST="localhost" \
	BACKEND_PORT="50059" \
	SERVICES_ENABLED="bank_statement_match.BankStatementMatch; \
		business_partner.BusinessPartner; data.BusinessData; \
		data.CoreFunctionality; dashboarding.Dashboarding; dictionary.Dictionary; \
		enrollment.Register; express_movement.ExpressMovement; \
		express_receipt.ExpressReceipt; express_shipment.ExpressShipment; \
		file_management.FileManagement; general_ledger.GeneralLedger; \
		import_file_loader.ImportFileLoader; in_out.InOut; invoice.Invoice; \
		issue_management.IssueManagement; location_address.LocationAddress; \
		logs.Logs; match_po_receipt_invoice.MatchPORReceiptInvoice; \
		material_management.MaterialManagement; \
		order.Order; payment_allocation.PaymentAllocation; \
		payment_print_export.PaymentPrintExport; \
		payment.Payment; payroll_action_notice.PayrollActionNotice; \
		data.Store; product.Product; report_management.ReportManagement; \
		record_management.RecordManagement; \ security.Security; \
		time_control.TimeControl; time_record.TimeRecord; updates.UpdateCenter; \
		user_customization.UserCustomization; user_interface.UserInterface; \
		wms.WarehouseManagement; store.WebStore; workflow.Workflow;"

#Expose Ports
# EXPOSE 9901 # admin port
EXPOSE ${SERVER_PORT}


# Envoy standard configuration
COPY docker/envoy_template.yaml /etc/envoy/envoy_template.yaml

# Proto gRPC descriptor
COPY docker/adempiere-grpc-server.pb /data/adempiere-grpc-server.pb
COPY docker/start_grpc_proxy.sh /opt/apps/server/start.sh


# Start app
ENTRYPOINT ["sh" , "start.sh"]
