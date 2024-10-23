FROM envoyproxy/envoy:v1.32.0

LABEL maintainer="ySenih@erpya.com; EdwinBetanc0urt@outlook.com;" \
	description="Proxy Transcoding gRPC to JSON via http"


# Init ENV with default values
ENV \
	SERVER_PORT="5555" \
	BACKEND_HOST="localhost" \
	BACKEND_PORT="50059" \
	SERVICES_ENABLED="bank_statement_match.BankStatementMatch; \
		core_functionality.CoreFunctionality; data.BusinessData; data.Store; \
		dashboarding.Dashboarding; dictionary.Dictionary; enrollment.Register \
		express_movement.ExpressMovement; express_receipt.ExpressReceipt; \
		express_shipment.ExpressShipment; field.FieldManagementService; \
		field.business_partner.BusinessPartnerInfoService; field.in_out.InOutInfoService; \
		field.invoice.InvoiceInfoService; field.order.OrderInfoService; \
		field.payment.PaymentInfoService; field.product.ProductInfoService; \
		file_management.FileManagement; general_ledger.GeneralLedger; \
		import_file_loader.ImportFileLoader; issue_management.IssueManagement; \
		location_address.LocationAddress; logs.Logs; \
		match_po_receipt_invoice.MatchPORReceiptInvoice; \
		material_management.MaterialManagement; notice_management.NoticeManagement; \
		payment_allocation.PaymentAllocation; payment_print_export.PaymentPrintExport; \
		payroll_action_notice.PayrollActionNotice; preference_management.PreferenceManagement; \
		record_management.RecordManagement; report_management.ReportManagement; \
		security.Security; send_notifications.SendNotifications; store.WebStore; \
		task_management.TaskManagement; time_control.TimeControl; time_record.TimeRecord; \
		trial_balance_drillable.TrialBalanceDrillable; updates.UpdateCenter; \
		user_customization.UserCustomization; user_interface.UserInterface' \
		wms.WarehouseManagement; workflow.Workflow;"

#Expose Ports
# EXPOSE 9901 # admin port
EXPOSE ${SERVER_PORT}


WORKDIR /etc/envoy/


# Envoy standard configuration
COPY docker/envoy_template.yaml /etc/envoy/envoy_template.yaml

# Proto gRPC descriptor
COPY docker/adempiere-grpc-server.pb /data/adempiere-grpc-server.pb
COPY docker/start_grpc_proxy.sh /etc/envoy/start.sh


RUN chmod +x *.sh && \
	chmod +x *.yaml



# Start app
ENTRYPOINT ["sh" , "start.sh"]
