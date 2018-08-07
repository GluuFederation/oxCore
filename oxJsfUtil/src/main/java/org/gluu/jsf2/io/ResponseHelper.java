/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.jsf2.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.StringHelper;
import org.xdi.util.io.DownloadWrapper;
import org.xdi.util.io.FileDownloader;
import org.xdi.util.io.FileDownloader.ContentDisposition;

/**
 * @author Yuriy Movchan Date: 11.24.2010
 */
public class ResponseHelper {

	private static Logger log = LoggerFactory.getLogger(ResponseHelper.class);

	public static boolean downloadFile(String filePath, String contentType, FacesContext facesContext) {
		if (filePath == null) {
			return false;
		}

		File file = new File(filePath);
		if (!file.exists()) {
			log.error(String.format("Failed to send file %s. File doesn't exist.", file.getAbsolutePath()));
			return true;
		}

		HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		InputStream is = null;
		try {
			is = FileUtils.openInputStream(file);
			DownloadWrapper downloadWrapper = new DownloadWrapper(is, file.getName(), contentType, new Date(file.lastModified()),
					(int) file.length());
			FileDownloader.writeOutput(downloadWrapper, false, response);

			facesContext.responseComplete();
		} catch (Exception ex) {
			log.error(String.format("Failed to send file %s", file.getAbsolutePath()), ex);
			return false;
		} finally {
			IOUtils.closeQuietly(is);
		}

		return true;
	}

	public static boolean downloadFile(String fileName, String contentType, byte[] file, FacesContext facesContext) {
		return downloadFile(fileName, contentType, file, FileDownloader.ContentDisposition.ATTACHEMENT, facesContext);
	}

	public static boolean downloadFile(String fileName, String contentType, byte[] file, FileDownloader.ContentDisposition contentDisposition, FacesContext facesContext) {
		HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(file);
			DownloadWrapper downloadWrapper = new DownloadWrapper(is, fileName, contentType, new Date(), file.length);
			FileDownloader.writeOutput(downloadWrapper, contentDisposition, response);

			facesContext.responseComplete();
		} catch (Exception ex) {
			log.error("Failed to send file", ex);
			return false;
		} finally {
			IOUtils.closeQuietly(is);
		}

		return true;
	}

	public static ZipOutputStream createZipStream(OutputStream output, String comment) {
		ZipOutputStream zos = new ZipOutputStream(output);

		zos.setComment("Shibboleth2 SP configuration files");
		zos.setMethod(ZipOutputStream.DEFLATED);
		zos.setLevel(Deflater.DEFAULT_COMPRESSION);

		return zos;
	}

	public static boolean addFileToZip(String filePath, ZipOutputStream zos, String fileName) {
		if (StringHelper.isEmpty(filePath)) {
			return false;
		}

		File file = new File(filePath);
		if (!file.exists()) {
			log.error(String.format("Failed to add file %s to zip archive", file.getName()));
			return false;
		}

		boolean result = false;
		InputStream is = null;
		try {
			is = FileUtils.openInputStream(file);
			String zipFileName = fileName == null ? file.getName() : fileName;
			result = addInputStream(zos, zipFileName, is);
		} catch (IOException ex) {
			log.error(String.format("Failed to add file %s to zip archive", file.getName()), ex);
		} finally {
			IOUtils.closeQuietly(is);
		}

		return result;
	}

	public static boolean addFileContentToZip(String fileContent, ZipOutputStream zos, String fileName) {
		if (StringHelper.isEmpty(fileContent) || StringHelper.isEmpty(fileName)) {
			return false;
		}

		boolean result = false;
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(fileContent.getBytes("UTF-8"));
			result = addInputStream(zos, fileName, is);
		} catch (IOException ex) {
			log.error(String.format("Failed to add file %s to zip archive", fileName), ex);
		} finally {
			IOUtils.closeQuietly(is);
		}

		return result;
	}

	public static boolean addResourceToZip(InputStream is, String fileName, ZipOutputStream zos) {
		boolean result = false;

//		String fileName = (new File(resourceName)).getName();
//		InputStream is = ResponseHelper.class.getClassLoader().getResourceAsStream(resourceName);
		try {
			result = addInputStream(zos, fileName, is);
		} catch (Exception ex) {
			log.error(String.format("Failed to add resource %s to zip archive", fileName), ex);
		} finally {
			IOUtils.closeQuietly(is);
		}

		return result;
	}

	private static boolean addInputStream(ZipOutputStream zos, String fileName, InputStream is) {
		try {
			ZipEntry configurationFileEntry = new ZipEntry(fileName);
			zos.putNextEntry(configurationFileEntry);

			IOUtils.copy(is, zos);
		} catch (IOException ex) {
			log.error(String.format("Failed to add file %s to zip archive", fileName), ex);
			return false;
		}

		return true;
	}

}
