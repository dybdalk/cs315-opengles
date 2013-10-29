package cs315.yourname.hwk2;

import java.util.LinkedList;
import java.util.Stack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * A starter template for an Android scan conversion mini-painter. Includes the logic for doing the scan conversions
 * 
 * This class provides a dedicated thread for drawing, though the actual work is still in the UI thread,
 * because it is user driven.
 * 
 * @author Joel and Kyle Dybdal, adapted from code by Dave Akers
 * @version Fall 2013
 */
public class MiniPaintView extends SurfaceView implements SurfaceHolder.Callback
{
	private static final String TAG = "MiniPaintView";

	public static final int POINT_MODE = 0; //mode number (supposedly faster than enums)
	public static final int LINE_MODE = 1;
	public static final int CIRCLE_MODE = 2;
	public static final int POLYLINE_MODE = 3;
	public static final int RECTANGLE_MODE = 4;
	public static final int FLOOD_FILL_MODE = 5;
	public static final int AIRBRUSH_MODE = 6;

	private static final int PIXEL_SIZE = 2; //how "big" to make each pixel; change this for debugging
	private boolean DELAY_MODE = false; //whether to show a delay on the drawing (for debugging)
	private static final int DELAY_TIME = 5; //how long to pause between pixels; delay in ms; 5 is short, 50 is long

	private SurfaceHolder _holder; //basic drawing structure
	private DrawingThread _thread;
	private Context _context;
	
	private Bitmap _bmp; //frame buffer
	private int _width; //size of the image buffer
	private int _height;
	private Matrix _scaleM; //scale based on pixel size

	private int _mode; //drawing mode
	private int _color; //current painting color
	
	private int _startX; //starting points for multi-click operations
	private int _startY;
	

	/**
	 * Respond to touch events
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		int x = (int)event.getX()/PIXEL_SIZE; //scale event to the size of the frame buffer!
		int y = (int)event.getY()/PIXEL_SIZE;

		switch(_mode) {
		case POINT_MODE:
			drawPoint(x,y);
			break;
		case LINE_MODE: 
			if(_startX < 0) { //see if we have a "first click" set of coords
				_startX = x; 
				_startY = y;
			}
			else {
				drawLine(_startX, _startY, x, y);
				_startX = -1;
			}
			break;
		case CIRCLE_MODE:
			if(_startX < 0) { //see if we have a "first click" set of coords
				_startX = x; 
				_startY = y;
			}
			else {
				int radius = (int) Math.sqrt((x - _startX)*(x - _startX) + (y - _startY)*(y - _startY));
				drawCircle(_startX, _startY, radius);
				_startX = -1;
			}
			break;
		case POLYLINE_MODE: 
			if(_startX < 0) { //see if we have a "first click" set of coords
				_startX = x; 
				_startY = y;
			}
			else {
				drawLine(_startX, _startY, x, y);
				_startX = x; 
				_startY = y;
			}

			break;
		case RECTANGLE_MODE:
			if(_startX < 0) { //see if we have a "first click" set of coords
				_startX = x; 
				_startY = y;
			}
			else{
				drawRectangle(_startX,_startY,x,y);
				_startX = -1;
			}

			break;
		case FLOOD_FILL_MODE:
			if(_startX < 0) { //see if we have a "first click" set of coords
				_startX = x; 
				_startY = y;
				floodFill(x,y);
				_startX = -1;
			}
			
			break;
		case AIRBRUSH_MODE:
			if(_startX < 0) { //see if we have a "first click" set of coords
				_startX = x; 
				_startY = y;
				airBrush(x,y);
				_startX = -1;
			}
			
			break;
		}

		return super.onTouchEvent(event); //pass up the tree, as needed
	}

	/**
	 * Draws a single point on the screen in the current paint color
	 * @param x x-coord of the point
	 * @param y y-coord of the point
	 */
	public void drawPoint(int x, int y)
	{
		setPixel(x, y); //I've done this one for you. You're welcome ;)
	}

	/**
	 * Draws a line on the screen in the current paint color
	 * code derived from the example at http://tech-algorithm.com/articles/drawing-line-using-bresenham-algorithm/
	 * @param startX x-coord of starting point
	 * @param startY y-coord of starting point
	 * @param endX x-coord of ending point
	 * @param endY y-coord of ending point
	 */
	public void drawLine(int startX, int startY, int endX, int endY)
	{

		int dx = endX - startX;
		int dy = endY - startY;
		int dstartX = 0, dstartY = 0, dendX = 0, dendY = 0 ;
		//horizontal line
		if(startY == endY){
			if(dx >= 0){
				for(int i = startX; i<=endX; i++){
					setPixel(i, startY);
				}
			}
			else{
				for(int i = endX; i<=startX; i++){
					setPixel(i, startY);
				}
			}
		}
		// vertical line
		if(startX == endX){
			if(dy>=0){
				for(int i = startY; i<=endY; i++){
					setPixel(startX, i);
				}
			}
			else{
				for(int i = endY; i<=startY; i++){
					setPixel(startX, i);
				}
			}
		}
		// neither horizontal nor vertical
		else{
			// checks to see which octant it is, and assigns counting variables based upon that
			if (dx<0){
				dstartX = -1;
			}
			else if (dx>0){
				dstartX = 1 ;
			}
			if (dy<0){
				dstartY = -1;
			}
			else if (dy>0){
				dstartY = 1;
			}
			if (dx<0){
				dendX = -1;
			}
			else if (dx>0){ 
				dendX = 1; 
			}
			// horizontal is longest in the first octant, but that's not always the case
			int longest = Math.abs(dx);
			int shortest = Math.abs(dy);
			// if we have a steep slope, switch longest and shortest
			if (!(longest>shortest)) {
				longest = Math.abs(dy);
				shortest = Math.abs(dx);
				// if the slope is negative, we're counting down
				if (dy<0){ 
					dendY = -1;
				}  
				else if (dy>0){
					dendY = 1 ;
				}
				dendX = 0 ;            
			}
			//longest divided by 2
			int numerator = longest >>1 ;
				// standard bresenham
				for (int i = 0; i <= longest; i++) {
					setPixel(startX, startY);
					numerator += shortest ;
					if (!(numerator<longest)) {
						numerator -= longest;
						startX += dstartX;
						startY += dstartY;
					} else {
						startX += dendX;
						startY += dendY;
					}
				}
		}
	}

	/**
	 * Draws a circle on the screen in the current paint color
	 * code derived from example (which doesn't actually work) at: 
	 * https://en.wikipedia.org/wiki/Midpoint_circle_algorithm/
	 * @param x x-coord of circle center
	 * @param y y-coord of circle center
	 * @param radius radius of the circle
	 */
	public void drawCircle(int x, int y, int radius)
	{
		//System.out.println("drawing circle");
		int error = 1 - radius;
		int errorY = 1;
		int errorX = -2 * radius;
		int x1 = 0;
		int y1 = radius;
		
		//draw 4 starting points
		setPixel(x, y + radius);
		setPixel(x, y - radius);
		setPixel(x + radius, y);
		setPixel(x - radius, y);

		while(x1 < y1)
		{
			if(error > 0)
			{
				y1--;
				errorX += 2;
				error += errorX;
			}
			x1++;
			errorY += 2;
			error += errorY; 
			//draw each of the octants
			setPixel(x + x1, y + y1);
			setPixel(x - x1, y + y1);
			setPixel(x + x1, y - y1);
			setPixel(x - x1, y - y1);
			setPixel(x + y1, y + x1);
			setPixel(x - y1, y + x1);
			setPixel(x + y1, y - x1);
			setPixel(x - y1, y - x1);
			//System.out.println("here");
		}
	}


	/**
	 * Draws a rectangle on the screen in the current paint color
	 * @param startX x-coord of first corner (i.e., upper left)
	 * @param startY y-coord of first corner
	 * @param endX x-coord of second corner (i.e., lower right)
	 * @param endY y-coord of second corner
	 */
	public void drawRectangle(int startX, int startY, int endX, int endY)
	{
		//draw the four sides of the rectangle
//		this.drawLine(startX, startY, endX, startY);
//		this.drawLine(endX, startY, endX, endY);
//		this.drawLine(startX, endY, endX, endY);
//		this.drawLine(startX, startY, startX, endY);
		
		//System.out.println("Start X is: " + startX + ", start y is: " + startY);
		//System.out.println("end X is: " + endX + ", end y is: " + endY);
		
		int temp = 0;
		//fill form the upper left hand corner 
		if(startX - endX > 0){
			temp = startX;
			startX = endX;
			endX = temp;
		}
		if(endY - startY < 0){
			temp = startY;
			startY = endY;
			endY = temp;
		}
		//System.out.println("new Start X is: " + startX + ", start y is: " + startY);
		//System.out.println("new end X is: " + endX + ", end y is: " + endY);
		for(int i = startY; i<=endY; i++){
//			for(int j = startX; j <=endX; j++){
//				setPixel(j,i);
//			}
			drawLine(startX, i, endX, i);
		}

	}

	/**
	 * Flood-fills a space on the canvas in the current paint color
	 * @param x x-coord to start filling from
	 * @param y y-coord to start filling from
	 */
	public void floodFill(int x1, int y1)
	{
//		Stack<Point> testQ = new Stack<Point>();
//		testQ.add(new Point(0,0));
//		System.out.println(testQ.contains(new Point(0,0)));
		Point node = new Point(x1,y1);
		System.out.println("starting flood fill");
		int width = _bmp.getWidth();
		int height = _bmp.getHeight();
		int target = 0;
		int replacement = _color;
		if (target != replacement) {
			Stack<Point> stack = new Stack<Point>();
			stack.push(new Point(-1,-1)); //pop throws an exception if the stack is empty.
			do {
				System.out.println(stack.size());
				int x = node.x;
				int y = node.y;
				//System.out.println(node);
				//System.out.println("in the do while loop, x is: " + x + ". y id: " + y);
				while (x > 0 && _bmp.getPixel(x - 1, y) == target) {
					//System.out.println("in little while");
					x--;
				}
				boolean spanUp = false;
				boolean spanDown = false;
				while (x < width && _bmp.getPixel(x, y) == target) {
					setPixel(x, y);
					if (!spanUp && y > 0 && _bmp.getPixel(x, y - 1) == target) {
							stack.push(new Point(x, y - 1));
						spanUp = true;
					} else if (spanUp && y > 0
							&& _bmp.getPixel(x, y - 1) != target) {
						spanUp = false;
					}
					if (!spanDown && y < height - 1
							&& _bmp.getPixel(x, y + 1) == target) {
						if(!stack.contains(new Point(x, y + 1)))
						{
							stack.push(new Point(x, y + 1));
						}
						spanDown = true;
					} else if (spanDown && y < height - 1
							&& _bmp.getPixel(x, y + 1) != target) {
						spanDown = false;
					}
					x++;
				}
				try{
				node = stack.pop();
				}
				catch(Exception e){
					System.out.println(e);
				}
			} while(!node.equals(-1,-1));
			//System.out.println(node + ", " + stack.poll());
		}
	}
	

	/**
	 * Draws an airbrushed blob in the current paint color (blending with existing colors)
	 * @param x x-coord to center the airbrush
	 * @param y y-coord to center the airbrush
	 */
	public void airBrush(int x, int y)
	{
		int RADIUS = 50;
		int r = Color.red(_color);
		int g = Color.green(_color);
		int b = Color.blue(_color);
		
		for(int i = -RADIUS+x; i <RADIUS+x; i++)
		{
			for(int j = -RADIUS+y; j< RADIUS+y;j++)
			{
				double distance = Math.sqrt((i-x)*(i-x)+(j-y)*(j-y));
				if(distance <= 50)
				{
					int pix = getPixel(i,j);
					int shadeR = (int)(r-(distance/50*r));
					int shadeG = (int)(g-(distance/50*g));
					int shadeB = (int)(b-(distance/50*b));
					
					int avgR = (Color.red(pix)+shadeR)/2;
					int avgG = (Color.green(pix)+shadeG)/2;
					int avgB = (Color.blue(pix)+shadeB)/2;
					int nColor = Color.rgb(avgR,avgG,avgB);
					setPixel(i,j, nColor);
				}
			}
		}
		
	}


	/*********
	 * You shouldn't need to modify anything below this point!
	 ********/
	
	/**
	 * We need to override all the constructors, since we don't know which will be called
	 * All the constructors eventually call init()
	 */
	public MiniPaintView(Context context) {
		this(context, null);
	}

	public MiniPaintView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MiniPaintView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
		init(context);
	}

	/**
	 * Our initialization method (called from constructors)
	 */
	public void init(Context context)
	{
		_context = context;
		_holder = this.getHolder(); //handles control of the surface
		_holder.addCallback(this);
		_thread = new DrawingThread(_holder, this);

		_scaleM = new Matrix();
		_scaleM.setScale(PIXEL_SIZE, PIXEL_SIZE);
		DELAY_MODE = false;
		
		_mode = POINT_MODE;
		_color = Color.WHITE;

		_startX = -1; //initialize as invalid
		_startY = -1;

		setFocusable(true); //just in case for touch events
	}
	
	
	/**
	 * Sets the drawing mode (UI method)
	 */
	public void setMode(int mode)
	{
		_mode = mode;
		_startX = -1;
		_startY = -1;
		//Toast toast = Toast.makeText(_context, "Mode set: "+_mode, Toast.LENGTH_SHORT);
		//toast.show();
	}

	/**
	 * Sets the painting color (UI method)
	 */
	public void setColor(int color)
	{
		_color = color;
		//Toast toast = Toast.makeText(_context, "Color set: "+_color, Toast.LENGTH_SHORT);
		//toast.show();
	}

	/**
	 * Clears the drawing (resets all pixels to Black)
	 */
	public void clearDrawing()
	{
		for(int i=0; i<_width; i++)
			for(int j=0; j<_height; j++)
				_bmp.setPixel(i, j, Color.BLACK);
	}

	/**
	 * Helper method to set a single pixel to a given color.
	 * Performed clipping, and includes debug settings to introduce a delay in pixel drawing
	 * @param x x-coord of pixel
	 * @param y y-coord of pixel
	 * @param color color to apply to pixel
	 */
	public void setPixel(int x, int y, int color)
	{
		/*Can comment out this block to make things go even faster*/
		if(DELAY_MODE) //if we're in delay mode, then pause while drawing
		{
			try{
				Thread.sleep(DELAY_TIME);
			} catch (InterruptedException e){}
		}
		
		if(x >= 0 && x < _width && y >= 0 && y < _height) //clipping for generated shapes (so we don't try and draw outside the bmp)
			_bmp.setPixel(x, y, color);
	}

	/**
	 * Helper method to set a single pixel to the current paint color.
	 * Performed clipping, and includes debug settings to introduce a delay in pixel drawing
	 * @param x x-coord of pixel
	 * @param y y-coord of pixel
	 */
	public void setPixel(int x, int y)
	{
		setPixel(x,y,_color);
	}
	
	/**
	 * Convenience method to get the color of a specific pixel
	 * @param x x-coord of pixel
	 * @param y y-coord of pixel
	 * @return The color of the pixel
	 */
	public int getPixel(int x, int y)
	{
		return _bmp.getPixel(x,y);
	}
	
	//called when the surface changes (like sizes changes due to rotate). Will need to respond accordingly.
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		//store new size for our BitMap
		_width = width/2;
		_height = height/2;

		//create a properly-sized bitmap to draw on
		_bmp = Bitmap.createBitmap(_width, _height, Bitmap.Config.ARGB_8888);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) { //initialization stuff
		_thread.setRunning(true);
		_thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) { //cleanup
		//Tell the thread to shut down, but wait for it to stop!
		_thread.setRunning(false);
		boolean retry = true;
		while(retry) {
			try {
				_thread.join();
				retry = false;
			} catch (InterruptedException e) {
				//will try again...
			}
		}
		Log.d(TAG, "Drawing thread shut down.");
	}

	/**
	 * An inner class representing a thread that does the drawing. Animation timing could go in here.
	 * http://obviam.net/index.php/the-android-game-loop/ has some nice details about using timers to specify animation
	 */
	public class DrawingThread extends Thread 
	{
		private boolean _isRunning; //whether we're running or not (so we can "stop" the thread)
		private SurfaceHolder _holder; //the holder we're going to post updates to
		private MiniPaintView _view; //the view that has drawing details

		/**
		 * Constructor for the Drawing Thread
		 * @param holder
		 * @param view
		 */
		public DrawingThread(SurfaceHolder holder, MiniPaintView view)
		{
			super();
			this._holder = holder;
			this._view = view;
			this._isRunning = false;
		}

		/**
		 * Executed when we call thread.start()
		 */
		@Override
		public void run()
		{
			Canvas canvas;
			while(_isRunning)
			{
				canvas = null;
				try {
					canvas = _holder.lockCanvas();
					synchronized (_holder) {
						canvas.drawBitmap(_bmp,_scaleM,null); //draw the _bitmap onto the canvas. Note that filling the bitmap occurs elsewhere
					}
				} finally { //no matter what (even if something goes wrong), make sure to push the drawing so isn't inconsistent
					if (canvas != null) {
						_holder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}

		/**
		 * Public toggle for whether the thread is running.
		 * @param isRunning
		 */
		public void setRunning(boolean isRunning){
			this._isRunning = isRunning;
		}
	}
}
