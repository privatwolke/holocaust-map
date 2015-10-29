package at.privatwolke.museum;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.events.EventDispatcher;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.interactions.MouseHandler;
import de.fhpotsdam.unfolding.interactions.TuioCursorHandler;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import TUIO.TuioClient;
import TUIO.TuioCursor;
import TUIO.TuioListener;
import TUIO.TuioObject;
import TUIO.TuioTime;
import processing.core.PApplet;
import processing.core.PImage;

/**
 * An interactive map display that shows deportation routes.
 * 
 * @author Stephan Klein
 */
public class Familienalbum extends PApplet implements TuioListener {

	private static final long serialVersionUID = 1L;
	
	// settings
	private static final int APPLET_WIDTH = 1920;
	private static final int APPLET_HEIGHT = 1080;

	// holds the two maps that can be selected
	UnfoldingMap deportationMap;
	UnfoldingMap exileMap;
	EventDispatcher eventDispatcherDeportation;
	EventDispatcher eventDispatcherExile;
	
	// multitouch support
	TuioCursorHandler tuioCursorHandler;
	TuioClient tuioClient;

	
	// background images and map overlays
	PImage start;
	PImage overlay_deportation;
	PImage overlay_exil;
	PImage audioplayer_interviews;
	PImage audioplayer_heldenplatz;
	
	// determines which screen is drawn in draw()
	int screen;
	long lastAction;
	long lastClick;
	
	// the audio player
	Minim minim;
	AudioPlayer player;
	
	/**
	 * Main entry point to the application.
	 * @param args command line arguments
	 */
	public static void main(String args[]) {
		String params[] = { "--present", "--bgcolor=#b8dee6", "--hide-stop", "at.privatwolke.museum.Familienalbum" };
		PApplet.main(params);
	}

	/**
	 * Constructor.
	 * Initializes the screen and the application itself.
	 */
	public Familienalbum() {
		screen = 1;
		lastAction = millis();
		lastClick = millis();
	}


	/**
	 * Creates the deportation map instance.
	 * @return a fully configured UnfoldingMap instance
	 */
	private UnfoldingMap makeDeportationMap() {
		String connStr = "jdbc:sqlite::resource:holocaust-map.mbtiles";
		UnfoldingMap m = new UnfoldingMap(this, new MBTilesMapProvider(connStr));
		tuioCursorHandler = new TuioCursorHandler(this, new UnfoldingMap[] { m });
		eventDispatcherDeportation = new EventDispatcher();
		eventDispatcherDeportation.addBroadcaster(tuioCursorHandler);
		eventDispatcherDeportation.addBroadcaster(new MouseHandler(this, new UnfoldingMap[] { m }));
		m.setZoomRange(5F, 8F);
		m.setBackgroundColor(color(184, 222, 230));
		return m;
	}

	
	/**
	 * Enables panning and zooming on the map.
	 */
	void enableMapEvents() {
		eventDispatcherDeportation.register(deportationMap, "pan", new String[0]);
		eventDispatcherDeportation.register(deportationMap, "zoom", new String[0]);
		eventDispatcherExile.register(exileMap, "pan", new String[0]);
		eventDispatcherExile.register(exileMap, "zoom", new String[0]);
	}

	
	/**
	 * Creates the exile map instance. 
	 * @return a fully configured UnfoldingMap instance
	 */
	UnfoldingMap makeExileMap() {
		String connStr = "jdbc:sqlite::resource:weltkarte-exil.mbtiles";
		UnfoldingMap m = new UnfoldingMap(this, new MBTilesMapProvider(connStr));
		tuioCursorHandler = new TuioCursorHandler(this, new UnfoldingMap[] { m });
		eventDispatcherExile = new EventDispatcher();
		eventDispatcherExile.addBroadcaster(tuioCursorHandler);
		eventDispatcherExile.addBroadcaster(new MouseHandler(this, new UnfoldingMap[] { m }));
		m.setZoomRange(4F, 6F);
		m.setBackgroundColor(color(184, 222, 230));
		return m;
	}

	
	/**
	 * Resets pan and zoom on both maps.
	 */
	private void resetMap() {
		deportationMap.zoomAndPanTo(5, new Location(50F, 20F));
		exileMap.zoomAndPanTo(5, new Location(48F, 16F));
	}

	
	/**
	 * Determine where the user has clicked.
	 * @param x coordinate
	 * @param y coordinate
	 * @return an integer describing the region where the user clicked
	 */
	private int determineClick(int x, int y) {
		if (x >= 0 && x <= 153 && y >= 91 && y <= 144)
			return 1;
		if (x >= 0 && x <= 153 && y >= 149 && y <= 203)
			return 2;
		if (x >= 0 && x <= 153 && y >= 207 && y <= 264)
			return 3;
		if (x >= 410 && x <= 825 && y >= 320 && y <= 400)
			return 4;
		if (x >= 1245 && x <= 1500 && y >= 300 && y <= 390)
			return 5;
		if (x >= 460 && x <= 860 && y >= 500 && y <= 590)
			return 6;
		return x < 1030 || x > 1390 || y < 430 || y > 515 ? -1 : 7;
	}

	
	/**
	 * Stops the audio.
	 */
	private void stopPlayer() {
		if (player != null)
			player.close();
	}

	
	/**
	 * Called when a button is clicked.
	 * 
	 * @param button an integer identifying the button that was clicked
	 */
	private void buttonClicked(int button) {
		if ((long) millis() - lastClick < 600L)
			return;
		lastClick = millis();
		boolean doSwitch = false;
		switch (button) {
		case 1:
			if (screen == 1) {
				doSwitch = true;
				screen = 2;
				resetMap();
			} else if (screen == 2 || screen == 3 || screen == 4) {
				doSwitch = true;
				screen = 1;
				resetMap();
			}
			break;

		case 2:
			if (screen == 1 || screen == 2) {
				doSwitch = true;
				screen = 3;
			} else if (screen == 3 || screen == 4) {
				doSwitch = true;
				screen = 2;
				resetMap();
			}
			break;

		case 3:
			if (screen == 1 || screen == 2 || screen == 3) {
				doSwitch = true;
				screen = 4;
				stopPlayer();
				player = minim.loadFile("silence.mp3");
				player.play();
			} else if (screen == 4) {
				doSwitch = true;
				screen = 3;
			}
			break;

		case 4:
			if (screen == 3) {
				stopPlayer();
				player = minim.loadFile("silence.mp3");
				player.play();
			}
			break;

		case 5:
			if (screen == 3) {
				stopPlayer();
				player = minim.loadFile("silence.mp3");
				player.play();
			}
			break;

		case 6:
			if (screen == 3) {
				stopPlayer();
				player = minim.loadFile("silence.mp3");
				player.play();
			}
			break;

		case 7:
			if (screen == 3) {
				stopPlayer();
				player = minim.loadFile("silence.mp3");
				player.play();
			}
			break;
		}
		if (doSwitch)
			println((new StringBuilder("Switching to: ")).append(screen).toString());
	}

	
	@Override
	public void setup() {
		size(APPLET_WIDTH, APPLET_HEIGHT);
		deportationMap = makeDeportationMap();
		exileMap = makeExileMap();
		enableMapEvents();
		resetMap();
		
		// load images
		start = loadImage("start.png");
		overlay_deportation = loadImage("overlay-deportation.png");
		overlay_exil = loadImage("overlay-exil.png");
		audioplayer_interviews = loadImage("audioplayer-interviews.png");
		audioplayer_heldenplatz = loadImage("audioplayer-heldenplatz.png");
		
		// set up the audio player
		minim = new Minim(this);
		
		// set up multitouch support 
		tuioClient = tuioCursorHandler.getTuioClient();
		tuioClient.addTuioListener(this);
	}
	
	
	@Override
	public void draw() {
		if ((long) millis() - lastClick > 180000l) {
			screen = 0;
			resetMap();
		}
		if (screen == 0) {
			deportationMap.draw();
			image(start, 0, 0);
		}
		if (screen == 1) {
			deportationMap.draw();
			image(overlay_deportation, 0, 11);
		}
		if (screen == 2) {
			exileMap.draw();
			image(overlay_exil, 0, 11);
		}
		if (screen == 3) {
			image(audioplayer_interviews, 0, 0);
			stroke(255);
			strokeWeight(4);
			line(460, 750, 1460, 750);
			if (player != null) {
				stroke(color(41, 104, 119));
				double progress = (double) player.position() / ((double) player.length() * 1.0);
				int position = 460 + (int) (1000 * progress);
				line(460, 750, position, 750);
			}
		}
		if (screen == 4) {
			image(audioplayer_heldenplatz, 0, 0);
			stroke(255);
			strokeWeight(4);
			line(460, 750, 1460, 750);
			if (player != null) {
				stroke(color(10, 10, 10));
				double progress = (double) player.position() / ((double) player.length() * 1.0);
				int position = 460 + (int) (1000 * progress);
				line(460, 750, position, 750);
			}
		}
		fill(255, 100);
		
		for (TuioCursor tcur : tuioClient.getTuioCursors()) {
			ellipse(tcur.getScreenX(width), tcur.getScreenY(height), 20, 20);
		}
	}
	

	@Override
	public void stop() {
		player.close();
		minim.stop();
		super.stop();
	}

	
	@Override
	public void mouseClicked() {
		println("CLICKED");
		if (screen == 0) {
			screen = 1;
			return;
		}
		int button = determineClick(mouseX, mouseY);
		if (button != -1) {
			buttonClicked(button);
		}
	}

	
	
	/* Multitouch support */
	
	@Override
	public void addTuioCursor(TuioCursor tuioCursor) {
		if (screen == 0) {
			screen = 1;
			tuioCursorHandler.addTuioCursor(tuioCursor);
			return;
		}
		int button = determineClick(tuioCursor.getScreenX(width), tuioCursor.getScreenY(height));
		if (button != -1)
			buttonClicked(button);
		else
			tuioCursorHandler.addTuioCursor(tuioCursor);
	}

	
	@Override
	public void removeTuioCursor(TuioCursor tuioCursor) {
		tuioCursorHandler.removeTuioCursor(tuioCursor);
	}

	
	@Override
	public void updateTuioCursor(TuioCursor tuioCursor) {
		tuioCursorHandler.updateTuioCursor(tuioCursor);
	}

	@Override
	public void addTuioObject(TuioObject tobj) {}

	@Override
	public void updateTuioObject(TuioObject tobj) {}

	@Override
	public void removeTuioObject(TuioObject tobj) {}

	@Override
	public void refresh(TuioTime ftime) {}
}
