package fi.jasoft.uidlcompressor.demo;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.Application;

import fi.jasoft.uidlcompressor.CompressionMonitor;
import fi.jasoft.uidlcompressor.UIDLCompressorCommunicationManager;

public class DemoApplication extends Application implements CompressionMonitor {

    private DemoWindow window = new DemoWindow(
	    "Application Servlet with Compressed UIDLs");

    private static Logger logger = Logger
	    .getLogger(UIDLCompressorCommunicationManager.class.getName());

    public DemoApplication() {
	logger.setLevel(Level.FINEST);
    }

    @Override
    public void init() {
	setMainWindow(window);
    }

    public void compressionProcessed(long inBytes, long compressedBytes,
	    long encodedBytes) {
	window.compressionProcessed(inBytes, compressedBytes, encodedBytes);

    }
}
