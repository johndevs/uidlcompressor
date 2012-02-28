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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.ApplicationServlet;
import com.vaadin.terminal.gwt.server.CommunicationManager;

/**
 * An application servlet which compresses the uidl payload with GZip + Base64
 * 
 * @author John Ahlroos / www.jasoft.fi
 */
@SuppressWarnings("serial")
public class UIDLCompressorApplicationServlet extends ApplicationServlet {

    public static final String COMPRESSION_STRATEGY = "compression";

    private String strategy;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
	super.init(servletConfig);
	String s = servletConfig.getInitParameter(COMPRESSION_STRATEGY);
	if (s == null) {
	    // No strategy -> adaptive
	    strategy = UIDLCompressorCommunicationManager.COMPRESSION_STRATEGY_ADAPTIVE;
	} else {
	    strategy = s;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.gwt.server.AbstractApplicationServlet#
     * createCommunicationManager(com.vaadin.Application)
     */
    @Override
    public CommunicationManager createCommunicationManager(
	    Application application) {
	return new UIDLCompressorCommunicationManager(application, strategy);
    }
}
