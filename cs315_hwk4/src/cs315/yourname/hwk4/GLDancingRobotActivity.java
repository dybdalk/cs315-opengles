package cs315.yourname.hwk4;

import cs315.kdybdal.hwk4.R;
import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

/**
 * A basic activity for displaying a simple OpenGL rendering. This uses a slightly different structure than
 * with a regular Canvas.
 * 
 * @author Joel
 * @version Fall 2013
 */
public class GLDancingRobotActivity extends Activity
{
	private static final String TAG = "GLActivity"; //for logging/debugging

	private GLSurfaceView _GLView; //the view that we're actually drawing

	/**
	 * Called when the activity is started
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
//		_GLView = (GLSurfaceView)this.findViewById(R.id.gl_view);
		_GLView = new GLBasicView(this); //we set the GLView programmatically rather than with the XML
		setContentView(_GLView);

		//we can build layout systems and add them in here

	}

	protected void onPause() {
		super.onPause();
		_GLView.onPause(); //tell the view to pause
	}

	protected void onResume() {
		super.onResume();
		_GLView.onResume(); //tell the view to resume
	}


	/**
	 * The actual view itself, includes as an inner class. Note that this also controls interaction (but not rendering)
	 * We put the OpenGL rendering in a separate class
	 */
	public static class GLBasicView extends GLSurfaceView
	{
		private Renderer renderer;
		
		public GLBasicView(Context context) {
			super(context);

			setEGLContextClientVersion(2); //specify OpenGL ES 2.0
			super.setEGLConfigChooser(8, 8, 8, 8, 16, 0); //may be needed for some targets; specifies 24bit color

			renderer = new RobotRenderer(context);
			setRenderer(renderer); //set the renderer

			/* 
			 * Render the view only when there is a change in the drawing data.
			 * We comment this out when we don't have UI (just animation)
			 */
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//WHEN_DIRTY);
		}

	}
}
