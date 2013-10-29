package cs315.yourname.hwk4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Stack;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class represents a custom OpenGL renderer--all of our "drawing" logic goes here.
 * 
 * @author Joel; code adapted from Google and LearnOpenGLES
 * @version Fall 2013
 */
public class RobotRenderer implements GLSurfaceView.Renderer 
{
	private static final String TAG = "RUBIX Renderer"; //for logging/debugging

	//some constants given our model specifications
	private final int POSITION_DATA_SIZE = 3;	
	private final int NORMAL_DATA_SIZE = 3;
	private final int COLOR_DATA_SIZE = 4; //in case we may want it!
	private final int BYTES_PER_FLOAT = 4;
	
	//Matrix storage
	private float[] mModelMatrix = new float[16]; //to store current model matrix
	private float[] mViewMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mMVMatrix = new float[16]; //to store the current modelview matrix
	private float[] mMVPMatrix = new float[16]; //combined MVP matrix
	private float[] mTempMatrix = new float[16]; //temporary matrix for transformations, if needed
	
	
	//stack to hold the parent's reference frames during animation
	private Stack<float[]> refStack = new Stack();
	
	//boolean variables to help with animation
	boolean upLArmForeward;
	boolean upRArmForeward;
	boolean lowLArmForeward;
	boolean lowRArmForeward;
	boolean upLLegForeward;
	boolean upRLegForeward;
	boolean lowLLegForeward;
	boolean lowRLegForeward;
	
	//starting degrees of various limbs
	float upArmAngleInDegrees = 0.0f;
	float lowArmAngleInDegrees = -45.0f;
	float upLegAngleInDegrees = 0.0f;
	float lowLegAngleInDegrees = 45.0f;
	
	boolean animate = true;
	
	//Buffer for model data
	private final FloatBuffer mCubeData;
	private final int mCubeVertexCount; //vertex count for the buffer
	
	private final FloatBuffer mSphereData;
	private final int mSphereVertexCount;

	private final float[] mColorRed;
	private final float[] mColorBlue;
	private final float[] mColorGrey;

	//axis points (for debugging)
	private final FloatBuffer mAxisBuffer;
	private final int mAxisCount;
	private final float[] lightNormal = {0,0,3};
	
	/**
	 * OpenGL Handles
	 * These are C-style "pointers" (int representing a memory address) to particular blocks of data.
	 * We pass the pointers around instead of the data to increase efficiency (and because OpenGL is
	 * C-based, and that's how they do things).
	 */
	private int mPerVertexProgramHandle; //our "program" (OpenGL state) for drawing (uses some lighting!)
	private int mMVMatrixHandle; //the combined ModelView matrix
	private int mMVPMatrixHandle; //the combined ModelViewProjection matrix
	private int mPositionHandle; //the position of a vertex
	private int mNormalHandle; //the position of a vertex
	private int mColorHandle; //the color to paint the model
	
	//define the source code for the vertex shader
	private final String perVertexShaderCode = 
		    "uniform mat4 uMVMatrix;" + 	// A constant representing the modelview matrix. Used for calculating lights/shading
			"uniform mat4 uMVPMatrix;" +	// A constant representing the combined modelview/projection matrix. We use this for positioning
			"attribute vec4 aPosition;" +	// Per-vertex position information we will pass in
			"attribute vec3 aNormal;" +		// Per-vertex normal information we will pass in.
			"attribute vec4 aColor;" +		// Per-vertex color information we will pass in.
			"varying vec4 vColor;"  + 		//out : the ultimate color of the vertex
			"vec3 lightPos = vec3(0.0,0.0,3.0);" + //the position of the light
			"void main() {" +
			"  vec3 modelViewVertex = vec3(uMVMatrix * aPosition);" + 			//position modified by modelview
			"  vec3 modelViewNormal = vec3(uMVMatrix * vec4(aNormal, 0.0));" +	//normal modified by modelview
			"  vec3 lightVector = normalize(lightPos - modelViewVertex);" +		//the normalized vector between the light and the vertex
			"  float diffuse = max(dot(modelViewNormal, lightVector), 0.1);" +	//the amount of diffuse light to give (based on angle between light and normal)
			"  vColor = aColor * diffuse;"+ 									//scale the color by the light factor and set to output
			"  gl_PointSize = 3.0;" +		//for drawing points
			"  gl_Position = uMVPMatrix * aPosition;" + //gl_Position is built-in variable for the transformed vertex's position.
			"}";

	private final String fragmentShaderCode = 
			"precision mediump float;" + 	//don't need high precision
			"varying vec4 vColor;" + 		//color for the fragment; this was output from the vertexShader
			"void main() {" +
			"  gl_FragColor = vColor;" + 	//gl_fragColor is built-in variable for color of fragment
			"}";

	
	/**
	 * 
	 * Constructor should initialize any data we need, such as model data
	 */
	public RobotRenderer(Context context)
	{	
		/**
		 * Initialize our model data--we fetch it from the factory!
		 */
		ModelFactory models = new ModelFactory();

//		Matrix.setIdentityM(mTorsoMatrix, 0); //Torso is our "root"
		
		// creating Reference frames
//		mHeadMatrix = mTorsoMatrix.clone();
//		Matrix.translateM(mHeadMatrix, 0, 0.0f, 3.0f, 0.0f);
		
//		mLHipMatrix = mTorsoMatrix.clone();
//		Matrix.translateM(mLHipMatrix, 0 , 0.8f, -2.5f, 0.0f);

//		mRHipMatrix = mTorsoMatrix.clone();
//		Matrix.translateM(mRHipMatrix, 0 , -0.8f, -2.5f, 0.0f);
		
//		mLShoulderMatrix = mTorsoMatrix.clone();
//		Matrix.translateM(mLShoulderMatrix, 0, 1.3f, 2.0f, 0.0f);
		
//		mRShoulderMatrix = mTorsoMatrix.clone();
//		Matrix.translateM(mRShoulderMatrix, 0, -1.3f, 2.0f, 0.0f);
		
//		mLUpArmMatrix = mLShoulderMatrix.clone();
//		Matrix.translateM(mLUpArmMatrix, 0, 0.1f, -0.9f, 0.0f);
		
//		mRUpArmMatrix = mRShoulderMatrix.clone();
//		Matrix.translateM(mRUpArmMatrix, 0, -0.1f, -0.9f, 0.0f);
		
//		mLElbowMatrix = mLUpArmMatrix.clone();
//		Matrix.translateM(mLElbowMatrix, 0, 0.0f, -1.0f, 0.0f);
		
//		mRElbowMatrix = mRUpArmMatrix.clone();
//		Matrix.translateM(mRElbowMatrix, 0, 0.0f, -1.0f, 0.0f);
		
//		mLLowArmMatrix = mLElbowMatrix.clone();
//		Matrix.translateM(mLLowArmMatrix, 0, 0.0f, -0.9f, 0.0f);
		
//		mRLowArmMatrix = mRElbowMatrix.clone();
//		Matrix.translateM(mRLowArmMatrix, 0, 0.0f, -0.9f, 0.0f);
		
//		mLUpLegMatrix = mLHipMatrix.clone();
//		Matrix.translateM(mLUpLegMatrix, 0, 0.0f, -0.9f, 0.0f);
		
//		mRUpLegMatrix = mRHipMatrix.clone();
//		Matrix.translateM(mRUpLegMatrix, 0, 0.0f, -0.9f, 0.0f);
		
//		mLKneeMatrix = mLUpLegMatrix.clone();
//		Matrix.translateM(mLKneeMatrix, 0, 0.0f, -1.0f, 0.0f);

//		mRKneeMatrix = mRUpLegMatrix.clone();
//		Matrix.translateM(mRKneeMatrix, 0, 0.0f, -1.0f, 0.0f);
		
//		mLLowLegMatrix = mLKneeMatrix.clone();
//		Matrix.translateM(mLLowLegMatrix, 0, 0.0f, -0.9f, 0.0f);
		
//		mRLowLegMatrix = mRKneeMatrix.clone();
//		Matrix.translateM(mRLowLegMatrix, 0, 0.0f, -0.9f, 0.0f);

		upLArmForeward = true;
		upRArmForeward = false;
		lowLArmForeward = true;
		lowRArmForeward = false;
		upLLegForeward = false;
		upRLegForeward = true;
		lowLLegForeward = false;
		lowRLegForeward = true;
		
		
		float[] cubeData = models.getCubeData();
		mCubeVertexCount = cubeData.length/(POSITION_DATA_SIZE+NORMAL_DATA_SIZE);
		mCubeData = ByteBuffer.allocateDirect(cubeData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer(); //generate buffer
		mCubeData.put(cubeData); //put the float[] into the buffer and set the position
		
		//more models can go here!
		float[] sphereData = models.getSphereData(models.SMOOTH_SPHERE);
		mSphereVertexCount = sphereData.length/(POSITION_DATA_SIZE+NORMAL_DATA_SIZE);
		mSphereData = ByteBuffer.allocateDirect(sphereData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer(); //generate buffer
		mSphereData.put(sphereData);
		
		
		//set up some example colors. Can add more as needed!
		mColorRed = new float[] {0.8f, 0.1f, 0.1f, 1.0f};
		mColorBlue = new float[] {0.1f, 0.1f, 0.8f, 1.0f};
		mColorGrey = new float[] {0.8f, 0.8f, 0.8f, 1.0f};

		//axis
		float[] axisData = models.getCoordinateAxis();
		mAxisCount = axisData.length/POSITION_DATA_SIZE;
		mAxisBuffer = ByteBuffer.allocateDirect(axisData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer(); //generate buffer
		mAxisBuffer.put(axisData); //put the float[] into the buffer and set the position
	}

	/**
	 * This method is called when the rendering surface is first created; more initializing stuff goes here.
	 * I put OpenGL initialization here (with more generic model initialization in the Renderer constructor).
	 * 
	 * Note that the GL10 parameter is unused; this parameter acts sort of like the Graphics2D context for
	 * doing GLES 1.0 operations. But we don't use that class for GLES 2.0+; but in order to keep Android 
	 * working and backwards compatible, the method has the same signature so the unused object is passed in.
	 */
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) 
	{
		//flags to enable depth work
		GLES20.glEnable(GLES20.GL_CULL_FACE); //remove back faces
		GLES20.glEnable(GLES20.GL_DEPTH_TEST); //enable depth testing
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		
		// Set the background clear color
		GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f); //Currently a dark grey so we can make sure things are working

		//This is a good place to compile the shaders from Strings into actual executables. We use a helper method for that
		int vertexShaderHandle = GLUtilities.compileShader(GLES20.GL_VERTEX_SHADER, perVertexShaderCode); //get pointers to the executables		
		int fragmentShaderHandle = GLUtilities.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		mPerVertexProgramHandle = GLUtilities.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle); //and then we throw them into a program

		//Get pointers to the shader's variables (for use elsewhere)
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "uMVPMatrix");
		mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "uMVMatrix");
		mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "aPosition");
		mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "aNormal");
		mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "aColor");
	}

	/**
	 * Called whenever the surface changes (i.e., size due to rotation). Put initialization stuff that
	 * depends on the size here!
	 */
	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) 
	{
		GLES20.glViewport(0, 0, width, height); // Set the OpenGL viewport (basically the canvas) to the same size as the surface.

		/**
		 * Set up the View and Projection matrixes. These matter more for when we're actually constructing
		 * 3D models, rather than 2D models in a 3D world.
		 */
		
		//Set View Matrix
		Matrix.setLookAtM(mViewMatrix, 0, 
				0.0f, 0.0f, 7.0f, //eye's location
				0.0f, 0.0f, -1.0f, //direction we're looking at
				0.0f, 1.0f, 0.0f //direction that is "up" from our head
				); //this gets compiled into the proper matrix automatically
		Matrix.translateM(mViewMatrix, 0, 0.0f, 1.0f, 0.0f);// keep robot in the frame
		Matrix.rotateM(mViewMatrix, 0, -15.0f, 0.0f, 1.0f, 0.0f);

		//Set Projection Matrix. We will talk about this more in the future
		final float ratio = (float) width / height; //aspect ratio
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1;
		final float top = 1;
		final float near = 1.0f;
		final float far = 50.0f;
		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
		
		
	}

	/**
	 * This is like our "onDraw" method; it says what to do each frame
	 */
	@Override
	public void onDrawFrame(GL10 unused) 
	{
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); //start by clearing the screen for each frame

		GLES20.glUseProgram(mPerVertexProgramHandle); //tell OpenGL to use the shader program we've compiled
		
		// Do a complete rotation every 10 seconds.
//		long time = SystemClock.uptimeMillis() % 10000L;        
//		float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
//		Log.i("ANGLE", "" + angleInDegrees);
		if(animate){
		drawTorso();
		}
//		drawAxis(); //so we have guides on coordinate axes, for debugging
	}
	
	/**
	 * draws the torso
	 */
	private void drawTorso()
	{	
		Matrix.setIdentityM(mModelMatrix, 0);
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		drawHead();
		drawLShoulder();
		drawRShoulder();
		drawLHip();
		drawRHip();
		Matrix.scaleM(mModelMatrix, 0, 1.0f, 2.0f, 0.75f); // apply scale
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed); //draw the triangle with the given model matrix
		mModelMatrix = refStack.pop();
	}
	
	private void drawHead()
	{
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, 3.0f, 0.0f);
		Matrix.scaleM(mModelMatrix, 0, -0.7f, -0.7f, -0.7f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();
		
	}
	
	private void drawLHip() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0 , 0.8f, -2.5f, 0.0f);
		if(upLLegForeward){
			upLegAngleInDegrees -= 0.5f;
			if(upLegAngleInDegrees>=-45.0f){
				Matrix.rotateM(mModelMatrix, 0, upLegAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				upLLegForeward = false;
			}
		}
		if(!upLLegForeward){
			upLegAngleInDegrees += 0.5f;
			if(upLegAngleInDegrees<=45.0f){
				Matrix.rotateM(mModelMatrix, 0, upLegAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				upLLegForeward = true;
			}
		}
		drawLUpLeg();
		Matrix.scaleM(mModelMatrix, 0, -0.4f, -0.4f, -0.4f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();
	}

	private void drawLUpLeg() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -0.9f, 0.0f);
		drawLKnee();
		Matrix.scaleM(mModelMatrix, 0, 0.2f, 1.0f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed);
		mModelMatrix = refStack.pop();
		
	}

	private void drawLKnee() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, 0.0f);
		if(lowLLegForeward){
			lowLegAngleInDegrees -= 0.5f;
			if(lowLegAngleInDegrees>=0.0f){
				Matrix.rotateM(mModelMatrix, 0, lowLegAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				lowLLegForeward = false;
			}
		}
		if(!lowLLegForeward){
			lowLegAngleInDegrees += 0.5f;
			if(lowLegAngleInDegrees<=90.0f){
				Matrix.rotateM(mModelMatrix, 0, lowLegAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				lowLLegForeward = true;
			}
		}
		drawLLowLeg();
		Matrix.scaleM(mModelMatrix, 0, -0.35f, -0.35f, -0.35f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();
	}

	private void drawLLowLeg() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -0.9f, 0.0f);
		Matrix.scaleM(mModelMatrix, 0, 0.2f, 1.0f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed);
		mModelMatrix = refStack.pop();
	}

	private void drawRHip() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0 , -0.8f, -2.5f, 0.0f);
		if(upRLegForeward){
			if(upLegAngleInDegrees>=-45.0f){
				Matrix.rotateM(mModelMatrix, 0, -upLegAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				upRLegForeward = false;
			}
		}
		if(!upRLegForeward){
			if(upLegAngleInDegrees<=45.0f){
				Matrix.rotateM(mModelMatrix, 0, -upLegAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				upRLegForeward = true;
			}
		}
		drawRUpLeg();
		Matrix.scaleM(mModelMatrix, 0, -0.4f, -0.4f, -0.4f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();
	}

	private void drawRUpLeg() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -0.9f, 0.0f);
		drawRKnee();
		Matrix.scaleM(mModelMatrix, 0, 0.2f, 1.0f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed);
		mModelMatrix = refStack.pop();
	}

	private void drawRKnee() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, 0.0f);
		if(lowRLegForeward){
			if(lowLegAngleInDegrees>=0.0f){
				Matrix.rotateM(mModelMatrix, 0, 90-lowLegAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				lowRLegForeward = false;
			}
		}
		if(!lowRLegForeward){
			if(lowLegAngleInDegrees<=90.0f){
				Matrix.rotateM(mModelMatrix, 0, 90-lowLegAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				lowRLegForeward = true;
			}
		}
		drawRLowLeg();
		Matrix.scaleM(mModelMatrix, 0, -0.35f, -0.35f, -0.35f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();
	}

	private void drawRLowLeg() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -0.9f, 0.0f);
		Matrix.scaleM(mModelMatrix, 0, 0.2f, 1.0f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed);
		mModelMatrix = refStack.pop();
	}

	private void drawLShoulder() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 1.3f, 2.0f, 0.0f);    
		if(upLArmForeward){
			upArmAngleInDegrees -= 0.5f;
			if(upArmAngleInDegrees>=-45.0f){
				Matrix.rotateM(mModelMatrix, 0, upArmAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				upLArmForeward = false;
			}
		}
		if(!upLArmForeward){
			upArmAngleInDegrees += 0.5f;
			if(upArmAngleInDegrees<=45.0f){
				Matrix.rotateM(mModelMatrix, 0, upArmAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				upLArmForeward = true;
			}
		}
		drawLUpArm();
		Matrix.scaleM(mModelMatrix, 0, -0.4f, -0.4f, -0.4f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();

	}

	private void drawLUpArm() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.1f, -0.9f, 0.0f);
		drawLElbow();
		Matrix.scaleM(mModelMatrix, 0, 0.2f, 1.0f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed);
		mModelMatrix = refStack.pop();
	}

	private void drawLElbow() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, 0.0f);
		if(lowLArmForeward){
			lowArmAngleInDegrees -= 0.5f;
			if(lowArmAngleInDegrees>=-90.0f){
				Matrix.rotateM(mModelMatrix, 0, lowArmAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				lowLArmForeward = false;
			}
		}
		if(!lowLArmForeward){
			lowArmAngleInDegrees += 0.5f;
			if(lowArmAngleInDegrees<=0.0f){
				Matrix.rotateM(mModelMatrix, 0, lowArmAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				lowLArmForeward = true;
			}
		}
		drawLLowArm();
		Matrix.scaleM(mModelMatrix, 0, -0.35f, -0.35f, -0.35f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();

	}

	private void drawLLowArm() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -0.9f, 0.0f);
		Matrix.scaleM(mModelMatrix, 0, 0.2f, 1.0f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed);
		mModelMatrix = refStack.pop();
	}

	private void drawRShoulder() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, -1.3f, 2.0f, 0.0f);
		if(upRArmForeward){
			if(upArmAngleInDegrees>=-45.0f){
				Matrix.rotateM(mModelMatrix, 0, -upArmAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				upRArmForeward = false;
			}
		}
		if(!upRArmForeward){
			if(upArmAngleInDegrees<=45.0f){
				Matrix.rotateM(mModelMatrix, 0, -upArmAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				upRArmForeward = true;
			}
		}
		drawRUpArm();
		Matrix.scaleM(mModelMatrix, 0, -0.4f, -0.4f, -0.4f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();
	}

	

	private void drawRUpArm() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, -0.1f, -0.9f, 0.0f);
		drawRElbow();
		Matrix.scaleM(mModelMatrix, 0, 0.2f, 1.0f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed);
		mModelMatrix = refStack.pop();
	}

	private void drawRElbow() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, 0.0f);
		if(lowRArmForeward){
			if(lowArmAngleInDegrees>=-90.0f){
				Matrix.rotateM(mModelMatrix, 0, -90-lowArmAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				lowRArmForeward = false;
			}
		}
		if(!lowRArmForeward){
			if(lowArmAngleInDegrees<=0.0f){
				Matrix.rotateM(mModelMatrix, 0, -90-lowArmAngleInDegrees, 1.0f, 0.0f, 0.0f);
			}
			else{
				lowRArmForeward = true;
			}
		}
		drawRLowArm();
		Matrix.scaleM(mModelMatrix, 0, -0.35f, -0.35f, -0.35f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mModelMatrix, mColorBlue);
		mModelMatrix = refStack.pop();
	}

	private void drawRLowArm() {
		float[] temp = new float[16];
		System.arraycopy(mModelMatrix, 0,temp,0, mModelMatrix.length);
		refStack.push(temp);
		Matrix.translateM(mModelMatrix, 0, 0.0f, -0.9f, 0.0f);
		Matrix.scaleM(mModelMatrix, 0, 0.2f, 1.0f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mModelMatrix, mColorRed);
		mModelMatrix = refStack.pop();
	}
	

	/**
	 * Draws a triangle buffer with the given modelMatrix and single color. 
	 * Note the view matrix is defined per program.
	 */			
	private void drawPackedTriangleBuffer(FloatBuffer buffer, int vertexCount, float[] modelMatrix, float[] color)
	{		
		//Calculate MV and MVPMatrix. Note written as MVP, but really P*V*M
		//Matrix.scaleM(mModelMatrix, 0, 0.0f,1.0f,0.0f);
		Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, modelMatrix, 0);  //"M * V"
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0); //"MV * P"

		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0); //put combined matrixes in the shader variables
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		
		final int stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE) * BYTES_PER_FLOAT; //how big of steps we take through the buffer
		
		buffer.position(0); //reset buffer start to 0 (where the position data starts)
		GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, stride, buffer); //note the stride lets us step over the normal data!
		GLES20.glEnableVertexAttribArray(mPositionHandle);

		buffer.position(POSITION_DATA_SIZE); //shift pointer to where the normal data starts
		GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, stride, buffer); //note the stride lets us step over the position data!
		GLES20.glEnableVertexAttribArray(mNormalHandle);

		//put color data in the shader variable
		GLES20.glVertexAttrib4fv(mColorHandle, color, 0);
		
		//This the OpenGL command to draw the specified number of vertices (as triangles; that is, every 3 coordinates). 
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
	}

	
	//draws the coordinate axis (for debugging)
	private void drawAxis()
	{
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.multiplyMM(mMVMatrix, 0, mModelMatrix, 0, mViewMatrix, 0);  //M * V
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0); //P * MV 

		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Pass in the position information
		mAxisBuffer.position(0); //reset buffer start to 0 (just in case)
		GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mAxisBuffer); 
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mNormalHandle); //turn off the buffer version of normals
		GLES20.glVertexAttrib3fv(mNormalHandle, lightNormal, 0); //pass particular normal (so points are bright)

		//GLES20.glDisableVertexAttribArray(mColorHandle); //just in case it was enabled earlier
		GLES20.glVertexAttrib4fv(mColorHandle, mColorGrey, 0); //put color in the shader variable
		
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mAxisCount); //draw the axis (as points!)
	}

}