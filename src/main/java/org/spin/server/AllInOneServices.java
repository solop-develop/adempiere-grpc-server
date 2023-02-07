/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.server;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.spin.base.setup.SetupLoader;
import org.spin.base.util.Services;
import org.spin.grpc.service.AccessServiceImplementation;
import org.spin.grpc.service.BusinessDataServiceImplementation;
import org.spin.grpc.service.BusinessPartnerServiceImplementation;
import org.spin.grpc.service.CoreFunctionalityImplementation;
import org.spin.grpc.service.DashboardingServiceImplementation;
import org.spin.grpc.service.DictionaryServiceImplementation;
import org.spin.grpc.service.EnrollmentServiceImplementation;
import org.spin.grpc.service.FileManagementServiceImplementation;
import org.spin.grpc.service.GeneralLedgerServiceImplementation;
import org.spin.grpc.service.InOutServiceImplementation;
import org.spin.grpc.service.InvoiceServiceImplementation;
import org.spin.grpc.service.IssueManagementServiceImplementation;
import org.spin.grpc.service.LogsServiceImplementation;
import org.spin.grpc.service.MaterialManagementServiceImplementation;
import org.spin.grpc.service.OrderServiceImplementation;
import org.spin.grpc.service.PaymentPrintExportServiceImplementation;
import org.spin.grpc.service.PaymentServiceImplementation;
import org.spin.grpc.service.PayrollActionNoticeServiceImplementation;
import org.spin.grpc.service.PointOfSalesServiceImplementation;
import org.spin.grpc.service.ProductServiceImplementation;
import org.spin.grpc.service.TimeControlServiceImplementation;
import org.spin.grpc.service.UpdateImplementation;
import org.spin.grpc.service.UserCustomizationImplementation;
import org.spin.grpc.service.UserInterfaceServiceImplementation;
import org.spin.grpc.service.WebStoreServiceImplementation;
import org.spin.grpc.service.WorkflowServiceImplementation;

import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;

public class AllInOneServices {
	private static final Logger logger = Logger.getLogger(AllInOneServices.class.getName());

	private Server server;
	/**
	  * Get SSL / TLS context
	  * @return
	  */
	  private SslContextBuilder getSslContextBuilder() {
	        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File(SetupLoader.getInstance().getServer().getCertificate_chain_file()),
	                new File(SetupLoader.getInstance().getServer().getPrivate_key_file()));
	        if (SetupLoader.getInstance().getServer().getTrust_certificate_collection_file() != null) {
	            sslClientContextBuilder.trustManager(new File(SetupLoader.getInstance().getServer().getTrust_certificate_collection_file()));
	            sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
	        }
	        return GrpcSslContexts.configure(sslClientContextBuilder);
	  }

	private void start() throws IOException {
		  NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(SetupLoader.getInstance().getServer().getPort());
		  //	For Access
		  if(SetupLoader.getInstance().getServer().isValidService(Services.ACCESS.getServiceName())) {
			  serverBuilder.addService(new AccessServiceImplementation());
			  logger.info("Service " + Services.ACCESS.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		  //	Business Logic
		  if(SetupLoader.getInstance().getServer().isValidService(Services.BUSINESS.getServiceName())) {
			  serverBuilder.addService(new BusinessDataServiceImplementation());
			  logger.info("Service " + Services.BUSINESS.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		  //	Core Implementation
		  if(SetupLoader.getInstance().getServer().isValidService(Services.CORE.getServiceName())) {
			  serverBuilder.addService(new CoreFunctionalityImplementation());
			  logger.info("Service " + Services.CORE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		//	Dictionary
		if(SetupLoader.getInstance().getServer().isValidService(Services.DICTIONARY.getServiceName())) {
			serverBuilder.addService(new DictionaryServiceImplementation());
			logger.info("Service " + Services.DICTIONARY.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Enrollment
		if(SetupLoader.getInstance().getServer().isValidService(Services.ENROLLMENT.getServiceName())) {
			serverBuilder.addService(new EnrollmentServiceImplementation());
			logger.info("Service " + Services.ENROLLMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	File Management
		if(SetupLoader.getInstance().getServer().isValidService(Services.FILE_MANAGEMENT.getServiceName())) {
			serverBuilder.addService(new FileManagementServiceImplementation());
			logger.info("Service " + Services.FILE_MANAGEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		  //	Dashboarding
		  if(SetupLoader.getInstance().getServer().isValidService(Services.DASHBOARDING.getServiceName())) {
			  serverBuilder.addService(new DashboardingServiceImplementation());
			  logger.info("Service " + Services.DASHBOARDING.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		  //	Workflow
		  if(SetupLoader.getInstance().getServer().isValidService(Services.WORKFLOW.getServiceName())) {
			  serverBuilder.addService(new WorkflowServiceImplementation());
			  logger.info("Service " + Services.WORKFLOW.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		//	General Ledger
		if(SetupLoader.getInstance().getServer().isValidService(Services.GENERAL_LEDGER.getServiceName())) {
			serverBuilder.addService(new GeneralLedgerServiceImplementation());
			logger.info("Service " + Services.GENERAL_LEDGER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		  //	Log
		  if(SetupLoader.getInstance().getServer().isValidService(Services.LOG.getServiceName())) {
			  serverBuilder.addService(new LogsServiceImplementation());
			  logger.info("Service " + Services.LOG.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		//	Material Management
		if(SetupLoader.getInstance().getServer().isValidService(Services.MATERIAL_MANAGEMENT.getServiceName())) {
			serverBuilder.addService(new MaterialManagementServiceImplementation());
			logger.info("Service " + Services.MATERIAL_MANAGEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		  //	Store
		  if(SetupLoader.getInstance().getServer().isValidService(Services.STORE.getServiceName())) {
			  serverBuilder.addService(new WebStoreServiceImplementation());
			  logger.info("Service " + Services.STORE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		  //	POS
		  if(SetupLoader.getInstance().getServer().isValidService(Services.POS.getServiceName())) {
			  serverBuilder.addService(new PointOfSalesServiceImplementation());
			  logger.info("Service " + Services.POS.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		  //	Updater
		  if(SetupLoader.getInstance().getServer().isValidService(Services.UPDATER.getServiceName())) {
			  serverBuilder.addService(new UpdateImplementation());
			  logger.info("Service " + Services.UPDATER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		  }
		//	Business Partner
		if(SetupLoader.getInstance().getServer().isValidService(Services.BUSINESS_PARTNER.getServiceName())) {
			serverBuilder.addService(new BusinessPartnerServiceImplementation());
			logger.info("Service " + Services.BUSINESS_PARTNER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	In-Out
		if(SetupLoader.getInstance().getServer().isValidService(Services.IN_OUT.getServiceName())) {
			serverBuilder.addService(new InOutServiceImplementation());
			logger.info("Service " + Services.IN_OUT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Invoice
		if(SetupLoader.getInstance().getServer().isValidService(Services.INVOICE.getServiceName())) {
			serverBuilder.addService(new InvoiceServiceImplementation());
			logger.info("Service " + Services.INVOICE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Issue Management
		if (SetupLoader.getInstance().getServer().isValidService(Services.ISSUE_MANAGEMENT.getServiceName())) {
			serverBuilder.addService(new IssueManagementServiceImplementation());
			logger.info("Service " + Services.ISSUE_MANAGEMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Order
		if(SetupLoader.getInstance().getServer().isValidService(Services.ORDER.getServiceName())) {
			serverBuilder.addService(new OrderServiceImplementation());
			logger.info("Service " + Services.ORDER.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Payment
		if(SetupLoader.getInstance().getServer().isValidService(Services.PAYMENT.getServiceName())) {
			serverBuilder.addService(new PaymentServiceImplementation());
			logger.info("Service " + Services.PAYMENT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Product
		if(SetupLoader.getInstance().getServer().isValidService(Services.PRODUCT.getServiceName())) {
			serverBuilder.addService(new ProductServiceImplementation());
			logger.info("Service " + Services.PRODUCT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Payment Print/Export
		if(SetupLoader.getInstance().getServer().isValidService(Services.PAYMENT_PTINT_EXPORT.getServiceName())) {
			serverBuilder.addService(new PaymentPrintExportServiceImplementation());
			logger.info("Service " + Services.PAYMENT_PTINT_EXPORT.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Payroll Action Notice
		if(SetupLoader.getInstance().getServer().isValidService(Services.PAYROLL_ACTION_NOTICE.getServiceName())) {
			serverBuilder.addService(new PayrollActionNoticeServiceImplementation());
			logger.info("Service " + Services.PAYROLL_ACTION_NOTICE.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	Time Control
		if(SetupLoader.getInstance().getServer().isValidService(Services.TIME_CONTROL.getServiceName())) {
			serverBuilder.addService(new TimeControlServiceImplementation());
			logger.info("Service " + Services.TIME_CONTROL.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	User Customization
		if(SetupLoader.getInstance().getServer().isValidService(Services.USER_CUSTOMIZATION.getServiceName())) {
			serverBuilder.addService(new UserCustomizationImplementation());
			logger.info("Service " + Services.USER_CUSTOMIZATION.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}
		//	User Interface
		if(SetupLoader.getInstance().getServer().isValidService(Services.UI.getServiceName())) {
			serverBuilder.addService(new UserInterfaceServiceImplementation());
			logger.info("Service " + Services.UI.getServiceName() + " added on " + SetupLoader.getInstance().getServer().getPort());
		}

		  //	Add services
		  if(SetupLoader.getInstance().getServer().isTlsEnabled()) {
			  
			  server = serverBuilder.sslContext(getSslContextBuilder().build())
		                .build()
		                .start();
		  } else {
			  server = serverBuilder.build()
				        .start();
		  }
		  logger.info("Server started, listening on " + SetupLoader.getInstance().getServer().getPort());
		    Runtime.getRuntime().addShutdownHook(new Thread() {
		      @Override
		      public void run() {
		        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
		    	  logger.info("*** shutting down gRPC server since JVM is shutting down");
		    	  AllInOneServices.this.stop();
		        logger.info("*** server shut down");
		      }
		    });
	  }

	  private void stop() {
	    if (server != null) {
	      server.shutdown();
	    }
	  }

	  /**
	   * Await termination on the main thread since the grpc library uses daemon threads.
	   */
	  private void blockUntilShutdown() throws InterruptedException {
	    if (server != null) {
	      server.awaitTermination();
	    }
	  }

	  /**
	   * Main launches the server from the command line.
	 * @throws Exception 
	   */
	  public static void main(String[] args) throws Exception {
		if (args == null) {
			throw new Exception("Arguments Not Found");
		}
		//
		if (args.length == 0) {
			throw new Exception("Arguments Must Be: [property file name]");
		}
		  String setupFileName = args[0];
		  if(setupFileName == null || setupFileName.trim().length() == 0) {
			  throw new Exception("Setup File not found");
		  }
		  SetupLoader.loadSetup(setupFileName);
		  //	Validate load
		  SetupLoader.getInstance().validateLoad();
		  final AllInOneServices server = new AllInOneServices();
		  server.start();
		  server.blockUntilShutdown();
	  }
}
