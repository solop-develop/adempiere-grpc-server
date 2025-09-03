package org.spin.server;

import java.util.logging.Logger;

import org.compiere.jr.report.JRViewerProvider;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * This class was created for implement JRViewerProvider to jasper reports
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com , https://github.com/EdwinBetanc0urt
 */
public class ServerReportProvider implements JRViewerProvider {

	private static final Logger logger = Logger.getLogger(ServerReportProvider.class.getName());

	@Override
	public void openViewer(JasperPrint jasperPrint, String title) throws JRException {
		logger.info("Unimplemented method 'openViewer'");
	}

}
