/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/
package org.spin.base.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.compiere.model.MClientInfo;
import org.compiere.util.Env;
import org.spin.model.MADAttachmentReference;
import org.spin.util.AttachmentUtil;

import com.google.protobuf.ByteString;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * A helper class for Files
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class FileUtil {

	/**
	 * Get resource UUID from image id
	 * @param imageId
	 * @return
	 */
	public static String getResourceUuidFromImageId(int imageId) {
		MADAttachmentReference reference = getResourceFromImageId(imageId);
		if(reference == null) {
			return null;
		}
		//	Return uuid
		return reference.getUUID();
	}

	/**
	 * Get Attachment reference from image ID
	 * @param imageId
	 * @return
	 */
	public static MADAttachmentReference getResourceFromImageId(int imageId) {
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		if(!AttachmentUtil.getInstance().isValidForClient(clientId)) {
			return null;
		}
		//	
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), Env.getAD_Client_ID(Env.getCtx()));
		return MADAttachmentReference.getByImageId(
			Env.getCtx(),
			clientInfo.getFileHandler_ID(),
			imageId,
			null
		);
	}



	public static ByteString getByteStringByOutputStream(OutputStream outputStream) {
		ByteArrayOutputStream buffer = (ByteArrayOutputStream) outputStream;
		byte[] bytes = buffer.toByteArray();
		ByteString resultFile = ByteString.copyFrom(bytes);
		return resultFile;
	}



	public static OutputStream mergePdfFiles(List<File> inputFilesList, File outputFile) throws Exception {
		List<InputStream> inputStreamsList = new ArrayList<InputStream>();
		inputFilesList.stream().forEach(inputFile -> {
			try {
				inputStreamsList.add(new FileInputStream(inputFile));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		OutputStream outputStream = mergePdfFiles(inputStreamsList, new FileOutputStream(outputFile));
		return outputStream;
	}

	public static OutputStream mergePdfFiles(List<InputStream> inputPdfList, OutputStream outputStream) throws Exception {
		// Create document and pdfReader objects.
		Document document = new Document();
		List<PdfReader> readers = new ArrayList<PdfReader>();
		int totalPages = 0;

		// Create pdf Iterator object using inputPdfList.
		Iterator<InputStream> pdfIterator = inputPdfList.iterator();

		// Create reader list for the input pdf files.
		while (pdfIterator.hasNext()) {
			InputStream pdf = pdfIterator.next();
			PdfReader pdfReader = new PdfReader(pdf);
			readers.add(pdfReader);
			totalPages = totalPages + pdfReader.getNumberOfPages();
		}

		// Create writer for the outputStream
		PdfWriter writer = PdfWriter.getInstance(document, outputStream);

		//Open document.
		document.open();

		//Contain the pdf data.
		PdfContentByte pageContentByte = writer.getDirectContent();

		PdfImportedPage pdfImportedPage;
		int currentPdfReaderPage = 1;
		Iterator<PdfReader> iteratorPDFReader = readers.iterator();

		// Iterate and process the reader list.
		while (iteratorPDFReader.hasNext()) {
			PdfReader pdfReader = iteratorPDFReader.next();
			//Create page and add content.
			while (currentPdfReaderPage <= pdfReader.getNumberOfPages()) {
				document.newPage();
				pdfImportedPage = writer.getImportedPage(
				pdfReader,currentPdfReaderPage);
				pageContentByte.addTemplate(pdfImportedPage, 0, 0);
				currentPdfReaderPage++;
			}
			currentPdfReaderPage = 1;
		}

		// Close document and outputStream.
		outputStream.flush();
		document.close();
		outputStream.close();

		// System.out.println("Pdf files merged successfully.");
		return outputStream;
	}

}
