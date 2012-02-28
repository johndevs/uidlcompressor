/*
 * Copyright 2011 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.jasoft.uidlcompressor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.bind.DatatypeConverter;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.AbstractApplicationServlet;
import com.vaadin.terminal.gwt.server.CommunicationManager;
import com.vaadin.ui.Window;

/**
 * A Communication manager which compresses the response payload with GZIP +
 * Base64 encoding
 * 
 * @author John Ahlroos / www.jasoft.fi
 */
@SuppressWarnings("serial")
public class UIDLCompressorCommunicationManager extends CommunicationManager {

    public static final String COMPRESSION_STRATEGY_ADAPTIVE = "adaptive";
    public static final String COMPRESSION_STRATEGY_STRICT = "strict";
    public static final String COMPRESSION_STRATEGY_OFF = "off";

    private static Logger logger = Logger
	    .getLogger(UIDLCompressorCommunicationManager.class.getName());

    /**
     * A wrapper for the response so we can do in the middle compression
     * 
     * @author John Ahlroos / www.jasoft.fi
     */
    private static class UIDLCompressedResponseWrapper extends
	    HttpServletResponseWrapper {

	private ByteArrayOutputStream output;
	private String contentType;

	/**
	 * Default constructor
	 * 
	 * @param response
	 *            Response to wrap
	 */
	public UIDLCompressedResponseWrapper(HttpServletResponse response) {
	    super(response);
	    output = new ByteArrayOutputStream();
	}

	/**
	 * The data written to the output stream
	 */
	public byte[] getData() {
	    return output.toByteArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponseWrapper#getOutputStream()
	 */
	@Override
	public ServletOutputStream getOutputStream() {
	    return new UIDLCompressedServletOutputStream(output);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponseWrapper#getWriter()
	 */
	@Override
	public PrintWriter getWriter() {
	    return new PrintWriter(getOutputStream(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponseWrapper#setContentLength(int)
	 */
	@Override
	public void setContentLength(int length) {
	    super.setContentLength(length);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.ServletResponseWrapper#setContentType(java.lang.String)
	 */
	@Override
	public void setContentType(String type) {
	    this.contentType = type;
	    super.setContentType(type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletResponseWrapper#getContentType()
	 */
	@Override
	public String getContentType() {
	    return contentType;
	}
    }

    /**
     * A servlet output stream which wraps the output stream in
     * {@link UIDLCompressedResponseWrapper} so we can do in the middle
     * compression
     * 
     * @author John Ahlroos / www.jasoft.fi
     */
    private static class UIDLCompressedServletOutputStream extends
	    ServletOutputStream {

	private DataOutputStream stream;

	/**
	 * Default constructor
	 * 
	 * @param output
	 *            Outputstream to compress
	 */
	public UIDLCompressedServletOutputStream(OutputStream output) {
	    stream = new DataOutputStream(output);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
	    stream.write(b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] b) throws IOException {
	    stream.write(b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
	    stream.write(b, off, len);
	}
    }

    /**
     * Strategy used by compression
     */
    private String strategy = COMPRESSION_STRATEGY_ADAPTIVE;

    /**
     * Default constructor
     * 
     * @param application
     *            The application to use
     */
    public UIDLCompressorCommunicationManager(Application application,
	    String compressionStrategy) {
	super(application);
	this.strategy = compressionStrategy;
	logger.info("Using " + compressionStrategy + " compression strategy");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.terminal.gwt.server.CommunicationManager#handleUidlRequest
     * (javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse,
     * com.vaadin.terminal.gwt.server.AbstractApplicationServlet,
     * com.vaadin.ui.Window)
     */
    @Override
    public void handleUidlRequest(HttpServletRequest request,
	    HttpServletResponse response,
	    AbstractApplicationServlet applicationServlet, Window window)
	    throws IOException, ServletException,
	    InvalidUIDLSecurityKeyException {

	if (strategy == COMPRESSION_STRATEGY_OFF) {
	    logger.log(Level.FINEST,
		    "Compression disabled, sending payload uncompressed");
	    super.handleUidlRequest(request, response, applicationServlet,
		    window);
	    return;
	}

	// Wrap the response before passing it along
	UIDLCompressedResponseWrapper responseWrapper = new UIDLCompressedResponseWrapper(
		response);

	// Process UIDL
	super.handleUidlRequest(request, responseWrapper, applicationServlet,
		window);

	// Get bytes
	byte[] data = responseWrapper.getData();

	// Compress GZip
	ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
	GZIPOutputStream gzipStream = new GZIPOutputStream(compressedStream);
	gzipStream.write(data);
	gzipStream.finish();
	gzipStream.close();
	byte[] compressed = compressedStream.toByteArray();

	// Convert to base64
	String base64 = DatatypeConverter.printBase64Binary(compressed);

	// Write to output stream
	byte[] encoded = base64.getBytes();

	if (strategy.equals(COMPRESSION_STRATEGY_STRICT)) {
	    /*
	     * Strict strategy always sends the encoded stream regardless if it
	     * is bigger than the original
	     */
	    response.getOutputStream().write(encoded);
	    logger.log(Level.FINEST,
		    "String compression, sending payload as GZIP+BASE64");

	} else if (strategy.equals(COMPRESSION_STRATEGY_ADAPTIVE)) {
	    /*
	     * Adaptive strategy selects the smallest stream. This is the
	     * default.
	     */
	    if (encoded.length > data.length) {
		response.getOutputStream().write(data);
		logger.log(Level.FINEST,
			"Adaptive compressions, sending payload as JSON");
	    } else {
		response.getOutputStream().write(encoded);
		logger.log(Level.FINEST,
			"Adaptive compressions, sending payload as GZIP+BASE64");
	    }
	} else {
	    // No strategy sends the original stream
	    response.getOutputStream().write(data);
	    logger.log(Level.FINEST,
		    "No compression defined, sending payload as JSON");
	}

	// Notify application if it is a monitor
	if (getApplication() instanceof CompressionMonitor) {
	    ((CompressionMonitor) getApplication()).compressionProcessed(
		    data.length, compressed.length, encoded.length);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.terminal.gwt.server.AbstractCommunicationManager#getRequestPayload
     * (com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Request)
     */
    @Override
    protected String getRequestPayload(Request request) throws IOException {
	String payload = super.getRequestPayload(request);
	// TODO Uncompress payload here when client side compression is
	// implemented
	return payload;
    }
}
