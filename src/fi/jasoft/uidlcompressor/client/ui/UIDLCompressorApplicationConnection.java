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

package fi.jasoft.uidlcompressor.client.ui;

import java.util.Date;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.VConsole;

/**
 * Client side widget which communicates with the server. Messages from the
 * server are shown as HTML and mouse clicks are sent to the server.
 * 
 * @author John Ahlroos / www.jasoft.fi
 */
public class UIDLCompressorApplicationConnection extends ApplicationConnection {

    /**
     * Decompresses a GZIP+Base64 encoded string into its original form
     * 
     * @param base64String
     *            The input string
     * @return The original string before GZip compression and Base64 encoding
     */
    private static final native String decompressBase64Gzip(String base64String)
    /*-{
         return $wnd.JXG.Util.utf8Decode($wnd.JXG.decompress(base64String));
    }-*/;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.terminal.gwt.client.ApplicationConnection#doAsyncUIDLRequest
     * (java.lang.String, java.lang.String,
     * com.google.gwt.http.client.RequestCallback)
     */
    @Override
    protected void doAsyncUIDLRequest(String uri, String payload,
	    final RequestCallback requestCallback) throws RequestException {

	// Wrap the request callback
	RequestCallback wrappedCallback = new RequestCallback() {
	    public void onResponseReceived(Request request,
		    final Response response) {

		Response jsonResponse = new Response() {

		    /*
		     * Caching the decoded json string in case getText() is
		     * called several times
		     */
		    private String decodedJson;

		    @Override
		    public String getText() {
			String base64 = response.getText();
			if (base64.startsWith("for(")) {
			    // Server is sending json, digress
			    return base64;
			}

			if (decodedJson == null) {
			    long start = new Date().getTime();
			    decodedJson = decompressBase64Gzip(base64);
			    long end = new Date().getTime();
			    VConsole.log("Decoding JSON took " + (end - start)
				    + "ms");
			}

			return decodedJson;
		    }

		    @Override
		    public String getStatusText() {
			return response.getStatusText();
		    }

		    @Override
		    public int getStatusCode() {
			return response.getStatusCode();
		    }

		    @Override
		    public String getHeadersAsString() {
			return response.getHeadersAsString();
		    }

		    @Override
		    public Header[] getHeaders() {
			return response.getHeaders();
		    }

		    @Override
		    public String getHeader(String header) {
			return response.getHeader(header);
		    }
		};

		requestCallback.onResponseReceived(request, jsonResponse);
	    }

	    public void onError(Request request, Throwable exception) {
		requestCallback.onError(request, exception);
	    }
	};

	// TODO Add payload compression here
	super.doAsyncUIDLRequest(uri, payload, wrappedCallback);
    }
}
