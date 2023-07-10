/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
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
package org.spin.grpc.logic;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_ImpFormat;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.impexp.MImpFormat;
import org.compiere.model.MLookupInfo;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.common.Value;
import org.spin.backend.grpc.form.import_file_loader.GetImportFromatRequest;
import org.spin.backend.grpc.form.import_file_loader.ImportFormat;
import org.spin.backend.grpc.form.import_file_loader.ListCharsetsRequest;
import org.spin.backend.grpc.form.import_file_loader.ListFilePreviewRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportFormatsRequest;
import org.spin.backend.grpc.form.import_file_loader.ProcessImportRequest;
import org.spin.backend.grpc.form.import_file_loader.ProcessImportResponse;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.base.util.ValueUtil;
import org.spin.form.import_file_loader.ImportFileLoaderConvertUtil;
import org.spin.grpc.service.UserInterfaceServiceImplementation;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service logic of Import File Loader form
 */
public class ImportFileLoaderServiceLogic {

	public static ListLookupItemsResponse.Builder listCharsets(ListCharsetsRequest request) {
		List<Charset> charsetsList = Arrays.asList(
			Ini.getAvailableCharsets()
		);

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(charsetsList.size())
		;

		if (!Util.isEmpty(request.getSearchValue(), true)) {
			final String searchValue = request.getSearchValue().toLowerCase();

			charsetsList = charsetsList.stream().filter(charset -> {
				return charset.name().toLowerCase().contains(searchValue);
			})
			.collect(Collectors.toList());
		}

		charsetsList.stream().forEach(charset -> {
			Value.Builder value = ValueUtil.getValueFromString(
				charset.name()
			);
			LookupItem.Builder builder = LookupItem.newBuilder()
				.putValues(
					LookupUtil.VALUE_COLUMN_KEY,
					value.build()
				)
				.putValues(
					LookupUtil.DISPLAY_COLUMN_KEY,
					value.build()
				)
			;
			builderList.addRecords(builder);
		});

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listImportFormats(ListImportFormatsRequest request) {
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir, 0, I_AD_ImpFormat.COLUMNNAME_AD_ImpFormat_ID, 0
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	public static ImportFormat.Builder getImportFromat(GetImportFromatRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_ImpFormat_ID@");
		}
		MImpFormat importFormat = new MImpFormat(Env.getCtx(), request.getId(), null);
		if (importFormat == null || importFormat.getAD_ImpFormat_ID() <= 0) {
			throw new AdempiereException("@AD_ImpFormat_ID@ @NotFound@");
		}

		ImportFormat.Builder builder = ImportFileLoaderConvertUtil.convertImportFormat(importFormat);

		return builder;
	}


	public static ListEntitiesResponse.Builder listFilePreview(ListFilePreviewRequest request) {
		return ListEntitiesResponse.newBuilder();
	}


	public static ProcessImportResponse.Builder processImport(ProcessImportRequest request) {
		return ProcessImportResponse.newBuilder();
	}
}
