package org.spin.server;

import java.util.List;
import java.util.logging.Logger;

import org.compiere.model.PO;
import org.compiere.process.IPrintDocument;

/**
 * This class was created for implement IPrintDocument to print on process
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com , https://github.com/EdwinBetanc0urt
 */
public class ServerDocumentProvider implements IPrintDocument {

	private static final Logger logger = Logger.getLogger(ServerDocumentProvider.class.getName());


	@Override
	public void print(PO document, int printFormatId, int windowNo, boolean askPrint) {
		logger.info("Unimplemented method 'print'");
	}


	@Override
	public void print(List<PO> documentList, int arg1, int arg2, boolean arg3) {
		logger.info("Unimplemented method 'print'");
	}

}
