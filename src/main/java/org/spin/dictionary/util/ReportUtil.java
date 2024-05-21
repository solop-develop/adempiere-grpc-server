/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
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

package org.spin.dictionary.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.compiere.model.MProcessPara;
import org.compiere.model.MQuery;
import org.compiere.model.MTable;
import org.compiere.print.ReportEngine;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.base.db.WhereClauseUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.util.ASPUtil;
import org.spin.util.AbstractExportFormat;
import org.spin.util.ReportExportHandler;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

public class ReportUtil {

	/**
	 * Default report type format.
	 */
	public static final String DEFAULT_REPORT_TYPE = "pdf";


	/**
	 * Get Report Query from Criteria
	 * @param filters
	 * @return
	 */
	public static MQuery getReportQueryFromCriteria(int reportId, String tableName, String filters) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		MQuery query = new MQuery(table.getTableName());
		List<MProcessPara> reportParameters = ASPUtil.getInstance(Env.getCtx()).getProcessParameters(reportId);
		HashMap<String, MProcessPara> parametersMap = new HashMap<>();
		for (MProcessPara paramerter : reportParameters) {
			parametersMap.put(paramerter.getColumnName(), paramerter);
		}
		List<Filter> conditions = FilterManager.newInstance(filters).getConditions()
			.parallelStream()
			.filter(condition -> {
				return !Util.isEmpty(condition.getColumnName(), true);
			})
			.collect(Collectors.toList())
		;

		// TODO: Add 1=1 to remove `if (whereClause.length() > 0)` and change stream with parallelStream
		StringBuilder whereClause = new StringBuilder();
		HashMap<String, String> rangeAdd = new HashMap<>();

		conditions.forEach(condition -> {
			String columnName = condition.getColumnName();
			MProcessPara reportParameter = parametersMap.get(columnName);
			if (reportParameter == null && columnName.endsWith("_To")) {
				String rangeColumnName = columnName.substring(0, columnName.length() - "_To".length());
				reportParameter = parametersMap.get(rangeColumnName);
			}
			if (reportParameter == null) {
				return;
			}
			if (rangeAdd.containsKey(reportParameter.getColumnName())) {
				return;
			}
			String restriction = WhereClauseUtil.getRestrictionByOperator(condition, reportParameter.getAD_Reference_ID());

			if (whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(restriction);
		});

		query.addRestriction(whereClause.toString());
		return query;
	}


	/**
	 * Create output
	 * @param reportEngine
	 * @param reportType
	 */
	public static File createOutput(ReportEngine reportEngine, String reportType) {
		//	Export
		File file = null;
		try {
			ReportExportHandler exportHandler = new ReportExportHandler(Env.getCtx(), reportEngine);
			AbstractExportFormat exporter = exportHandler.getExporterFromExtension(reportType);
			if(exporter != null) {
				//	Get File
				file = File.createTempFile(reportEngine.getName() + "_" + System.currentTimeMillis(), "." + exporter.getExtension());
				exporter.exportTo(file);
			}	
		} catch (IOException e) {
			return null;
		}
		return file;
	}


	public static OutputStream mergePdfFiles(List<File> inputFilesList, File outputFile) throws Exception {
		List<InputStream> inputStreamsList = new ArrayList<InputStream>();
		inputFilesList.stream().forEach(inputFile -> {
			try {
				inputStreamsList.add(new FileInputStream(inputFile));
			} catch (FileNotFoundException e) {
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
