package fi.jasoft.uidlcompressor.demo;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.vaadin.hezamu.mandelbrotwidget.Mandelbrot;

import com.github.wolfie.refresher.Refresher;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;

import fi.jasoft.uidlcompressor.CompressionMonitor;

public class DemoWindow extends Window implements CompressionMonitor {

    private final Mandelbrot fractal = new Mandelbrot();

    private final Timer timer = new Timer();

    private int stage = 1;

    public static final int FRAME_RATE = 1; // fps

    private final Refresher refresher = new Refresher();

    private long frame = 1;

    private Label stats = new Label();

    public DemoWindow(String caption) {
	VerticalLayout root = new VerticalLayout();
	root.setMargin(true);
	root.setSizeFull();
	root.setSpacing(true);
	setContent(root);

	Label header = new Label(caption);
	header.setStyleName(Reindeer.LABEL_H2);
	root.addComponent(header);

	Label description = new Label(
		"This demo application renders the Mandelbrot set on the serverside and sends"
			+ " the rendered image in the UIDL response. This tends to generate quite large UIDLs and long loading times. By compressing"
			+ " the UIDLs we can drastically reduce the bandwidth and improve the responsiveness off the application.");
	root.addComponent(description);

	HorizontalLayout hl = new HorizontalLayout();
	hl.setSpacing(true);
	root.addComponent(hl);
	root.setExpandRatio(hl, 1);

	fractal.setWidth("640px");
	fractal.setHeight("480px");
	fractal.setZoom(1);
	fractal.setCx(-0.55);
	fractal.setCy(-0.55);
	hl.addComponent(fractal);

	stats.setWidth("400px");
	stats.setHeight("400px");
	stats.setImmediate(true);
	stats.setContentMode(Label.CONTENT_PREFORMATTED);
	hl.addComponent(stats);

	refresher.setRefreshInterval(1000 / FRAME_RATE);
	root.addComponent(refresher);
    }

    public synchronized void render() {
	double zoomLvl = fractal.getZoom() + 0.10;
	fractal.setZoom(zoomLvl);
	frame++;
    }

    @Override
    public void attach() {
	super.attach();
	startTimer();
    }

    private boolean isTimerActive = false;

    private synchronized void startTimer() {
	if (!isTimerActive) {
	    stopTimer();
	    timer.schedule(new TimerTask() {
		@Override
		public void run() {
		    render();
		}
	    }, new Date(), 1000 / FRAME_RATE);
	    refresher.setRefreshInterval(1000 / FRAME_RATE);
	    isTimerActive = true;
	    System.out.println("Updating started");
	}
    }

    private synchronized void stopTimer() {
	if (isTimerActive) {
	    try {
		timer.cancel();
	    } catch (Exception e) {
		// IGNORE
	    }

	    isTimerActive = false;
	    refresher.setRefreshInterval(60000);
	    System.out.println("Updating stopped");
	}
    }

    @Override
    public void detach() {
	super.detach();
	stopTimer();
    }

    public void compressionProcessed(long inBytes, long compressedBytes,
	    long encodedBytes) {
	double uncompressedKb = inBytes / 1000.0;
	double compressedKb = compressedBytes / 1000.0;
	double encodedKb = encodedBytes / 1000.0;
	double compressionRate = (uncompressedKb - compressedKb)
		/ uncompressedKb * 100.0;

	String requestStats = "Request " + frame + ": " + uncompressedKb
		+ "kb -> " + compressedKb + "kb -> " + encodedKb
		+ "kb. Compression rate " + (int) compressionRate + "%";

	System.out.println(requestStats);

	StringBuilder str = new StringBuilder();
	str.append("=========  Request #" + frame + "  ==============\n");
	str.append("Uncompressed JSON:\t\t" + uncompressedKb + "kb\n");
	str.append("Compressed JSON:\t\t" + compressedKb + "kb\n");
	str.append("Compressed + Encoded JSON:\t" + encodedKb + "kb\n");
	str.append("----------------------------------------\n");
	str.append("Total compression rate:\t\t" + (int) compressionRate + "%");
	stats.setValue(str.toString());

    }
}
